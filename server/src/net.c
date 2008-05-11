///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <unistd.h> // close()

#define REM_NET_PRIV

#include "net.h"
#include "bt.h"
#include "ip.h"
#include "util.h"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

enum _RemNetServerType {
	REM_NET_SERVER_TYPE_NONE = 0,
	REM_NET_SERVER_TYPE_BT,
	REM_NET_SERVER_TYPE_IP
};

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_MSG_ID_BYE		0xFF

static const guint8			REM_IO_PREFIX[] = { 0xFF, 0xFF, 0xFF, 0xFF};
static const guint8			REM_IO_SUFFIX[] = { 0xFE, 0xFE, 0xFE, 0xFE};

#define REM_PROTO_VERSION	0x07

#define REM_IO_PREFIX_LEN	4
#define REM_IO_SUFFIX_LEN	4

static const guint8			REM_IO_HELLO[] = {
		0xFF, 0xFF, 0xFF, 0xFF,
		REM_PROTO_VERSION,
		0xFE, 0xFE, 0xFE, 0xFE
};

#define REM_IO_HELLO_LEN	9

#define REM_MSG_ID_IFS_BYE	0x07

/** Bye message (valid RemNetMsg with id = 0x07) */
static const guint8			REM_IO_BYE[] = {
		0xFF, 0xFF, 0xFF, 0xFF,
		0x00, 0x00, 0x00, REM_MSG_ID_IFS_BYE,
		0x00, 0x00, 0x00, 0x00,
		0xFE, 0xFE, 0xFE, 0xFE
};

#define REM_IO_BYE_LEN	16

#define REM_IO_RETRY_WAIT	25000	// us => 25 ms
#define REM_IO_RETRY_MAX	20		// waiting at most 500 ms for data


///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
rx(GIOChannel *chan, guint8 *data, guint len)
{
	GIOStatus	ret;
	guint		read, retry;
	
	g_assert(chan && data && len); 
	
	retry = 0;
	
	while (retry < REM_IO_RETRY_MAX) {
		
		read = 0;
		ret = g_io_channel_read_chars(chan, (gchar*) data, len, &read, NULL);
		
		//LOG_DEBUG("read returned %i (read %u bytes)", ret, read);

		////////// handle: IO error //////////
		
		if (ret == G_IO_STATUS_EOF || ret == G_IO_STATUS_ERROR) {
			LOG_WARN("IO error on client channel");
			return FALSE;
		}
		
		////////// handle: ok, but need to read again //////////
		
		if (read < len) {
			
			//LOG_DEBUG("could not read all data, retry");
			g_usleep(REM_IO_RETRY_WAIT);
			retry++;
			data += read;
			len -= read;
			continue;
		}
		
		////////// handle: ok, got all data //////////
	
		return TRUE;
		
	}
	
	////////// handle: missing data, though we've waited a while //////////
	
	LOG_WARN("could not read all data");

	return FALSE;
}

static gboolean
tx(GIOChannel *chan, const guint8 *data, guint len)
{
	g_assert(chan && data && len); 
	
	GIOStatus	ret;
	guint		written, retry;

	retry = 0;
	
	while (retry < REM_IO_RETRY_MAX) {
		
		written = 0;
		ret = g_io_channel_write_chars(chan, (gchar*) data, len, &written, NULL);
		
		//LOG_DEBUG("write returned %i (wrote %u bytes)", ret, written);

		////////// handle: IO error //////////
		
		if (ret == G_IO_STATUS_EOF || ret == G_IO_STATUS_ERROR) {
			LOG_WARN("IO error on client channel");
			return FALSE;
		}
		
		////////// handle: ok, but need to write again //////////
		
		if (written < len) {
			
			//LOG_DEBUG("could not write all data, retry");
			retry++;
			data += written;
			len -= written;
			g_usleep(REM_IO_RETRY_WAIT);
			continue;
		}
		
		////////// handle: ok, written all data //////////
	
		return TRUE;
		
	}

	////////// handle: missing data, though we've waited a while //////////
	
	LOG_WARN("could not write all data");

	return FALSE;
	
}

/**
 * This flush function considers a strange bug occuring on bluez created
 * sockets. The bug is that write on such a socket may return 0 while
 * writing no bytes - glib catches such a situation with an assertion.
 * Here we allow such behaviour since a repeated attempt some millis later
 * finally writes some data. The curious thing is that the "return 0 and write
 * nothing"-situation also occurs if glib says it is possible to write
 * on the socket (however, maybe this is because the socket has been set up
 * to be non-blocking). 
 */
static gboolean
flush(RemNetClient *client)
{
	GIOStatus		ret;
	GIOChannel		*chan;
	gsize			bytes_written = 0;

	chan = client->chan;
	
	LOG_DEBUG("chan %p buffer size: %u", chan, chan->write_buf->len);

	if (chan->write_buf == NULL || chan->write_buf->len == 0) {
		
		client->flushing = FALSE;
		return FALSE;
	}
	
	ret = chan->funcs->io_write(chan,
								chan->write_buf->str, chan->write_buf->len,
								&bytes_written, NULL);
	
	if (bytes_written > 0)
		g_string_erase(chan->write_buf, 0, bytes_written);

	g_assert(ret != G_IO_STATUS_EOF);
	
	if (ret == G_IO_STATUS_ERROR) {
		LOG_WARN("failed to flush channel for %s", client->addr);
		client->flushing = FALSE;		
		return FALSE;
	}
	
	if (ret == G_IO_STATUS_NORMAL && bytes_written == 0) {
		// this case is caught in the orig function by an assertion
		LOG_DEBUG("assert would fail here");
	}
	
	if (chan->write_buf->len > 0) {
		
		LOG_DEBUG("%u more bytes to flush", chan->write_buf->len);
		return TRUE;
		
	} else {
		
		LOG_DEBUG("flushed all bytes");
		client->flushing = FALSE;
		return FALSE;
	}
	
}

/** Callback for server hashtable value destroy function */
static void
shutdown_server(RemNetServer *server)
{
	switch (server->priv_type) {
		case REM_NET_SERVER_TYPE_BT:
			LOG_DEBUG("shutting down bluetooth");
			rem_net_bt_down(server);
		break;
		case REM_NET_SERVER_TYPE_IP:
			LOG_DEBUG("shutting down ip");
			rem_net_ip_down(server);
		break;
		default:
			g_assert_not_reached();
		break;
	}
}

static inline void
reset_msg(RemNetMsg *msg)
{
	msg->id = 0;
	msg->ba->len = 0;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Net up. Sets up server channels and returns them in a hashtable with the
 * channels as keys and RemNetServer as values. To shutdown a server, just
 * remove it from the hashtable. To shutdown all servers / net, just destroy the
 * hashtable - callback functions do the low level stuff.
 */ 
GHashTable*
rem_net_up(RemNetConfig *config)
{
	GHashTable		*sht;
	RemNetServer	*server;
	
	sht = g_hash_table_new_full(g_direct_hash, &g_direct_equal,
								NULL, (GDestroyNotify) &shutdown_server);
	
	if (config->bt_on) {
		server = rem_net_bt_up();
		if (!server) {
			g_hash_table_destroy(sht);
			return NULL;
		}
		server->priv_type = REM_NET_SERVER_TYPE_BT;
		g_hash_table_insert(sht, server->chan, server);
	}
	
	
	if (config->ip_on) {
		server = rem_net_ip_up(config->ip_port);
		if (!server) {
			g_hash_table_destroy(sht);
			return NULL;
		}
		server->priv_type = REM_NET_SERVER_TYPE_IP;
		g_hash_table_insert(sht, server->chan, server);
	}
	
	return sht;
}

/**
 * Receives a message and writes its id and received bytes into @a msg.
 * 
 * @param client
 * 		The client to receive a message from.
 * @param msg
 * 		A RemNetMsg which will be reset (but contained byte array must not
 * 		be @p NULL).
 * @return
 * 		TRUE if rx was successfull, FALSE otherwise.
 */
gboolean
rem_net_rx(RemNetClient *client, RemNetMsg *msg)
{
	gboolean	ok;
	gint32		len, len_nbo, msg_id_nbo;
	guint8		prefix[REM_IO_PREFIX_LEN];
	guint8		suffix[REM_IO_PREFIX_LEN];
	
	g_assert(msg->ba);
	
	reset_msg(msg);
	
	////////// IO prefix //////////
	
	//LOG_DEBUG("read io prefix");
	
	ok = rx(client->chan, prefix, REM_IO_PREFIX_LEN);
	if (!ok) return FALSE;

	//rem_dump(prefix, REM_IO_PREFIX_LEN);
	
	if (memcmp(prefix, REM_IO_PREFIX, REM_IO_PREFIX_LEN)) {
		LOG_WARN("wrong io prefix");
		return FALSE;
	}
	
	////////// message id //////////
	
	//LOG_DEBUG("read msg type");
	
	ok = rx(client->chan, (guint8*) &msg_id_nbo, 4);
	if (!ok) return FALSE;
	
	//rem_dump((guint8*) &msg_id_nbo, 4);

	msg->id = g_ntohl(msg_id_nbo);
	
	////////// message len //////////
	
	//LOG_DEBUG("read msg len");
	
	ok = rx(client->chan, (guint8*) &len_nbo, 4);
	if (!ok) return FALSE;
	
	//rem_dump((guint8*) &len_nbo, 4);

	len = g_ntohl(len_nbo);
	
	////////// data //////////
	
	if (len) {
		
		g_byte_array_set_size(msg->ba, len);
		
		//LOG_DEBUG("read msg data (%i bytes)", len);
		
		ok = rx(client->chan, msg->ba->data, len);
		if (!ok) {
			reset_msg(msg);
			return FALSE;
		}
		
	}
	
	////////// IO suffix //////////
	
	//LOG_DEBUG("read io suffix");
	
	ok = rx(client->chan, suffix, REM_IO_SUFFIX_LEN);
	if (!ok) {
		reset_msg(msg);
		return FALSE;
	}

	//rem_dump(suffix, REM_IO_SUFFIX_LEN);

	if (memcmp(suffix, REM_IO_SUFFIX, REM_IO_SUFFIX_LEN)) {
		LOG_WARN("wrong io suffix");
		reset_msg(msg);
		return FALSE;
	}

	LOG_DEBUG("rx'ed msg (%u, %u)", msg->id, msg->ba ? msg->ba->len : 0);
	
	rem_util_dump_ba(msg->ba);
	
	return TRUE;		
}

/**
 * Sends a message to a client.
 * 
 * @return
 * 		TRUE if tx was successfull, FALSE otherwise.
 * 
 * TODO: split sending in smaller pieces .. if much data (> 100K) gets sent,
 * the server blocks to long (longer than a DBus timeout)
 */
gboolean
rem_net_tx(RemNetClient *client, const RemNetMsg *msg)
{
	gboolean	ok, again;
	gint32		len_nbo, msg_id_nbo;

	LOG_DEBUG("tx msg (%u, %u)", msg->id, msg->ba ? msg->ba->len : 0);

	////////// IO prefix //////////
	
	//LOG_DEBUG("write io prefix");
	
	ok = tx(client->chan, REM_IO_PREFIX, REM_IO_PREFIX_LEN);
	if (!ok) return FALSE;

	////////// message id //////////
	
	//LOG_DEBUG("write msg type(%i)", msg->id);
	
	msg_id_nbo = g_htonl(msg->id);

	ok = tx(client->chan, (guint8*) &msg_id_nbo, 4);
	if (!ok) return FALSE;
	
	////////// message len //////////
	
	//LOG_DEBUG("write msg size (%i)", (msg->ba ? msg->ba->len : 0));
	
	len_nbo = msg->ba ? g_htonl(msg->ba->len) : 0;

	ok = tx(client->chan, (guint8*) &len_nbo, 4);
	if (!ok) return FALSE;
	
	////////// data //////////
	
	if (msg->ba && msg->ba->len) {
		
		//LOG_DEBUG("write msg data (%u bytes)", msg->ba->len);
		
		ok = tx(client->chan, msg->ba->data, msg->ba->len);
		if (!ok) return FALSE;
	}
	
	////////// IO suffix //////////
	
	//LOG_DEBUG("write io suffix");
	
	ok = tx(client->chan, REM_IO_SUFFIX, REM_IO_SUFFIX_LEN);
	if (!ok) return FALSE;

	if (!client->flushing) {
		again = flush(client);
		if (again) {
			LOG_DEBUG("set flush timeout");
			g_timeout_add(200, (GSourceFunc) &flush, client);
			client->flushing = TRUE;
		}
	}
	
	LOG_DEBUG("done");

	//ret = flush(client->chan);
	//if (ret < 0) return ret;	
	
	return TRUE;	
}

/** Accept a client and send HELLO message to a client */
RemNetClient*
rem_net_hello(RemNetServer *server)
{
	RemNetClient	*client;
	gboolean		ok;
	
	switch (server->priv_type) {
		case REM_NET_SERVER_TYPE_BT:
			client = rem_net_bt_accept(server);
			break;
		case REM_NET_SERVER_TYPE_IP:
			client = rem_net_ip_accept(server);
			break;
		default:
			g_assert_not_reached();
			break;
	}
	
	if (!client)
		return NULL;
	
	LOG_DEBUG("send 'hello' to %s", client->addr);
	
	g_io_channel_set_buffered(client->chan, FALSE);
	
	ok = tx(client->chan, REM_IO_HELLO, REM_IO_HELLO_LEN);
	if (!ok) {
		rem_net_bye(client);
		return NULL;
	}

	g_io_channel_set_buffered(client->chan, TRUE);

	LOG_DEBUG("done");

	return client;
}

void
rem_net_bye(RemNetClient *client)
{
	gboolean	buffer_has_data;
	
	if (!client)
		return;
	
	// this removes the client's flush callback (if any)
	g_source_remove_by_user_data(client);
	
	if (client->chan) {
	
		if (client->say_bye) {
			
			LOG_DEBUG("send 'bye' to %s", client->addr);
			
			// g_io_channel_set_buffered() is not allowed if buffer is not empty
			buffer_has_data = flush(client);
			if (!buffer_has_data) {
				g_io_channel_set_buffered(client->chan, FALSE);
				tx(client->chan, REM_IO_BYE, REM_IO_BYE_LEN);
				g_usleep(10000); // wait a moment before closing the channel
			}
			
			LOG_DEBUG("done");
		}
		
		g_io_channel_shutdown(client->chan, FALSE, NULL);
		g_io_channel_unref(client->chan);
	}
	
	if (client->addr)
		g_free(client->addr);
	
	g_slice_free(RemNetClient, client);
}

GIOChannel*
rem_net_chan_from_sock(gint sock)
{
	GIOChannel	*chan;
	GIOStatus	ret;
	
	chan = g_io_channel_unix_new(sock);
	if (!chan) {
		LOG_ERROR("failed to create channel from socket");
		close(sock);
		return NULL;
	}
	
	g_io_channel_set_close_on_unref(chan, TRUE);
	
	ret = g_io_channel_set_encoding(chan, NULL, NULL);
	if (ret != G_IO_STATUS_NORMAL) {
		LOG_ERROR("failed to set encoding for channel");
		return NULL;
	}
	
	ret = g_io_channel_set_flags(chan, G_IO_FLAG_NONBLOCK, NULL);
	if (ret != G_IO_STATUS_NORMAL) {
		LOG_ERROR("failed to set flags for channel");
		return NULL;
	}
	
	return chan;
}
