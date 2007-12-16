///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-net.h"
#include "../util/rem-util.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

static const guint8 REM_IO_PREFIX[] = { 0xFF, 0xFF, 0xFF, 0xFF};
static const guint8 REM_IO_SUFFIX[] = { 0xFE, 0xFE, 0xFE, 0xFE};

#define REM_IO_PREFIX_LEN	4
#define REM_IO_SUFFIX_LEN	4

#define REM_IO_RETRY_WAIT	25000	// us => 25 ms
#define REM_IO_RETRY_MAX	20		// waiting at most 500 ms for data

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static gint
priv_rx(GIOChannel *chan, guint8 *data, guint len)
{
	g_assert_debug(chan && data && len); 
	
	GIOStatus	ret;
	guint		read, retry;
	
	retry = 0;
	
	while (retry < REM_IO_RETRY_MAX) {
		
		read = 0;
		ret = g_io_channel_read_chars(chan, (gchar*) data, len, &read, NULL);
		
		LOG_NOISE("read returned %i (read %u bytes)", ret, read);

		////////// handle: IO error //////////
		
		if (ret == G_IO_STATUS_EOF || ret == G_IO_STATUS_ERROR) {
			LOG_WARN("IO error on client channel");
			return -1;
		}
		
		////////// handle: ok, but need to read again //////////
		
		if (read < len) {
			
			LOG_NOISE("could not read all data, retry");
			g_usleep(REM_IO_RETRY_WAIT);
			retry++;
			data += read;
			len -= read;
			continue;
		}
		
		////////// handle: ok, got all data //////////
	
		return 0;
		
	}
	
	////////// handle: missing data, though we've waited a while //////////
	
	LOG_WARN("could not read all data");

	return -1;
}

static gint
priv_tx(GIOChannel *chan, const guint8 *data, guint len)
{
	g_assert_debug(chan && data && len); 
	
	GIOStatus	ret;
	guint		written, retry;

	retry = 0;
	
	while (retry < REM_IO_RETRY_MAX) {
		
		written = 0;
		ret = g_io_channel_write_chars(chan, (gchar*) data, len, &written, NULL);
		
		LOG_NOISE("write returned %i (wrote %u bytes)", ret, written);

		////////// handle: IO error //////////
		
		if (ret == G_IO_STATUS_EOF || ret == G_IO_STATUS_ERROR) {
			LOG_WARN("IO error on client channel");
			return -1;
		}
		
		////////// handle: ok, but need to write again //////////
		
		if (written < len) {
			
			LOG_NOISE("could not write all data, retry");
			g_usleep(REM_IO_RETRY_WAIT);
			retry++;
			data += written;
			len -= written;
			continue;
		}
		
		////////// handle: ok, written all data //////////
	
		return 0;
		
	}

	////////// handle: missing data, though we've waited a while //////////
	
	LOG_WARN("could not write all data");

	return -1;
	
}

static gint
priv_flush(GIOChannel *chan)
{
	GIOStatus ret;
	
	ret = g_io_channel_flush(chan, NULL);
	if (ret != G_IO_STATUS_NORMAL) {
		LOG_ERROR("flushing channel failed");
		return -1;
	}
	
	return 0;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

/** Use this if you want a net message with an initialized GByteArray. */
RemNetMsg*
rem_net_msg_new(void)
{
	RemNetMsg	*msg;
	
	msg = g_slice_new0(RemNetMsg);
	
	msg->ba = g_byte_array_sized_new(1024); // avoid avoidable mem allocations
	
	return msg;
}

void
rem_net_msg_reset(RemNetMsg *msg)
{
	msg->id = 0;
	g_byte_array_set_size(msg->ba, 0);
}

void
rem_net_msg_destroy(RemNetMsg *msg)
{
	g_byte_array_free(msg->ba, TRUE);
	g_slice_free(RemNetMsg, msg);
}


/**
 * Receives a message and writes its id and received bytes into @a msg.
 * 
 * @param client The client to receive a message from.
 * @param msg A RemNetMsg created with rem_net_msg_new(), i.e. the contained
 *            GByteArray must already be initialized.
 */
gint
rem_net_client_rxmsg(RemNetClient *client, RemNetMsg *msg)
{
	g_assert_debug(client && msg);

	gint	ret;
	gint32	len, len_nbo, msg_id_nbo;
	guint8	prefix[REM_IO_PREFIX_LEN];
	guint8	suffix[REM_IO_PREFIX_LEN];
	
	rem_net_msg_reset(msg);
	
	////////// IO prefix //////////
	
	LOG_NOISE("read io prefix");
	
	ret = priv_rx(client->chan, prefix, REM_IO_PREFIX_LEN);
	if (ret < 0) return ret;

	rem_dump(prefix, REM_IO_PREFIX_LEN);
	
	if (memcmp(prefix, REM_IO_PREFIX, REM_IO_PREFIX_LEN)) {
		LOG_WARN("wrong io prefix");
		return -1;
	}
	
	////////// message id //////////
	
	LOG_NOISE("read msg type");
	
	ret = priv_rx(client->chan, (guint8*) &msg_id_nbo, 4);
	if (ret < 0) return ret;
	
	rem_dump((guint8*) &msg_id_nbo, 4);

	msg->id = g_ntohl(msg_id_nbo);
	
	////////// message len //////////
	
	LOG_NOISE("read msg len");
	
	ret = priv_rx(client->chan, (guint8*) &len_nbo, 4);
	if (ret < 0) return ret;
	
	rem_dump((guint8*) &len_nbo, 4);

	len = g_ntohl(len_nbo);
	
	////////// data //////////
	
	if (len) {
		
		g_byte_array_set_size(msg->ba, len);
		
		LOG_NOISE("read msg data (%i bytes)", len);
		
		ret = priv_rx(client->chan, msg->ba->data, len);
		if (ret < 0) {
			rem_net_msg_reset(msg);
			return ret;
		}
		
	}
	
	////////// IO suffix //////////
	
	LOG_NOISE("read io suffix");
	
	ret = priv_rx(client->chan, suffix, REM_IO_SUFFIX_LEN);
	if (ret < 0) {
		rem_net_msg_reset(msg);
		return ret;
	}

	rem_dump(suffix, REM_IO_SUFFIX_LEN);

	if (memcmp(suffix, REM_IO_SUFFIX, REM_IO_SUFFIX_LEN)) {
		LOG_WARN("wrong io suffix");
		rem_net_msg_reset(msg);
		return -1;
	}

	return 1;		
}

gint
rem_net_client_txmsg(RemNetClient *client, const RemNetMsg *msg)
{
	g_assert_debug(client && msg);

	gint ret;
	gint32 len_nbo, msg_id_nbo;

	////////// IO prefix //////////
	
	LOG_NOISE("write io prefix");
	
	ret = priv_tx(client->chan, REM_IO_PREFIX, REM_IO_PREFIX_LEN);
	if (ret < 0) return ret;

	////////// message id //////////
	
	LOG_NOISE("write msg type(%i)", msg->id);
	
	msg_id_nbo = g_htonl(msg->id);

	ret = priv_tx(client->chan, (guint8*) &msg_id_nbo, 4);
	if (ret < 0) return ret;
	
	////////// message len //////////
	
	LOG_NOISE("write msg size (%i)", (msg->ba ? msg->ba->len : 0));
	
	len_nbo = msg->ba ? g_htonl(msg->ba->len) : 0;

	ret = priv_tx(client->chan, (guint8*) &len_nbo, 4);
	if (ret < 0) return ret;
	
	////////// data //////////
	
	if (msg->ba && msg->ba->len) {
		
		LOG_NOISE("write msg data (%u bytes)", msg->ba->len);
		
		ret = priv_tx(client->chan, msg->ba->data, msg->ba->len);
		if (ret < 0) return ret;
	}
	
	////////// IO suffix //////////
	
	LOG_NOISE("write io suffix");
	
	ret = priv_tx(client->chan, REM_IO_SUFFIX, REM_IO_SUFFIX_LEN);
	if (ret < 0) return ret;

	ret = priv_flush(client->chan);
	if (ret < 0) return ret;	
	
	return 0;	
}

/** Sends HELLO message to a client */
gint
rem_net_client_hello(RemNetClient* client)
{
	gint			ret;
	const guint8	pv = REM_PROTO_VERSION;
	
	LOG_DEBUG("send hello code to %s", client->addr);
	
	ret = priv_tx(client->chan, REM_IO_PREFIX, REM_IO_PREFIX_LEN);
	if (ret < 0) return ret;
	
	ret = priv_tx(client->chan, &pv, 1);
	if (ret < 0) return ret;

	ret = priv_tx(client->chan, REM_IO_SUFFIX, REM_IO_SUFFIX_LEN);
	if (ret < 0) return ret;

	ret = priv_flush(client->chan);
	if (ret < 0) return ret;	

	return 0;
}
