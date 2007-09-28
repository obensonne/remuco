///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <string.h>
#include <unistd.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>

#include <remuco/layer/rem-net.h>


///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

struct _rem_io_priv_t {
	rem_net_t*	net;
	guint		select_timeout;
};

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

static const guint8 REM_IO_PREFIX[] = { 0xFF, 0xFF, 0xFF, 0xFF};
static const guint8 REM_IO_SUFFIX[] = { 0xFE, 0xFE, 0xFE, 0xFE};

#define REM_IO_PREFIX_LEN	4
#define REM_IO_SUFFIX_LEN	4

#define REM_IO_WAIT		50000	// micro sec
#define REM_IO_RETRY_COUNT_MAX	20

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

/**
 * @return
 * 	-1	client connection is broken
 * 	 0	data successfully read
 */
static inline gint
rem_net_recv_priv(rem_net_t *net, guint cn, guint8 *buf, guint len)
{
	g_assert_debug(net && net->client[cn].sock > 0 && buf); 
	
	gint ret, retry, sd;

	retry = 0;
	sd = net->client[cn].sock;

	LOG_NOISE("called\n");
	
	while (retry < REM_IO_RETRY_COUNT_MAX) {
		errno = 0;
		ret = read(sd, buf, len);
		LOG_NOISE("read returned %i\n", ret);
		if (ret > 0 && ret < len) {
			LOG_DEBUG("could not read all data, retry\n");
			g_usleep(REM_IO_WAIT);
			retry++;
			buf += ret;
			len -= ret;
			continue;
		} else if (ret == 0) {
			LOG_DEBUG("EOF on socket\n");
			rem_net_client_disconnect(net, cn);
			return -1;
		} else if (errno == EINTR /* ret < 0 */) {
			LOG_WARN("interrupted before reading, retry\n");
			g_usleep(REM_IO_WAIT);
			retry++;
			continue;
		} else if (errno /* ret < 0 */) {
			LOG_WARN("IO error on socket\n");
			rem_net_client_disconnect(net, cn);
			return -1;
		} else { /* ret == len */
			return 0;
		}
	}
	
	LOG_WARN("could not read all data from %s in %i attempts",
		net->client[cn].addr_str, retry);

	return -1; // data error
}

//static gint
//rem_io_read_until_prefix(int sd)
//{
//	guint8	prefix[REM_IO_PREFIX_LEN];
//	gint ret;
//	
//	ret = rem_io_read_priv(sd, prefix, REM_IO_PREFIX_LEN);
//	if (ret < 0) return ret;
//
//	if (memcmp(prefix, REM_IO_PREFIX, REM_IO_PREFIX_LEN)) {
//		LOG_WARN("wrong io prefix\n");
//		return REM_IO_ERROR;
//	}
//	
//
//}

static inline gint
rem_net_send_priv(rem_net_t *net, guint cn, const guint8 *buf, guint len)
{
	g_assert_debug(net && net->client[cn].sock > 0 && buf);
	
	gint ret, sd;

	sd = net->client[cn].sock;

	while (TRUE) {
		errno = 0;
		ret = write(sd, buf, len);
		if (ret >= 0 && ret < len) {
			LOG_WARN("could not send all data\n");
			g_usleep(REM_IO_WAIT);
			buf += ret;
			len -= ret;
			continue;
		} else if (errno == EINTR) {
			LOG_WARN("interrupted before writing\n");
			g_usleep(REM_IO_WAIT);
			continue;
		} else if (errno == EPIPE) {
			LOG_DEBUG("reading end of socket closed\n");
			rem_net_client_disconnect(net, cn);
			return -1;
		} else if (errno) {
			LOG_WARN("IO error on socket");
			rem_net_client_disconnect(net, cn);
			return -1;
		} else { /* ret == len */
			return 0;
		}
	}
	
}

///////////////////////////////////////////////////////////////////////////////
//
// using the io layer
//
///////////////////////////////////////////////////////////////////////////////

/**
 * @param ba (out param)
 * 	the GByteArray pointer ba will point to a GByteArray containing the
 * 	read data or will be null if the read data is malformed 
 * @return
 * 	-1 if client connection is broken
 * 	 0 if client connection is ok but data is malformed
 * 	 1 if client connection is ok and data is ok
 */
gint
rem_net_recv(rem_net_t *net, guint cn, rem_net_msg_t *nmsg)
{
	g_assert_debug(net && nmsg && !nmsg->ba && !nmsg->id);
	
	gint ret;
	gint32 len, len_nbo, nmsg_id_nbo;
	guint8	prefix[REM_IO_PREFIX_LEN];
	guint8	suffix[REM_IO_PREFIX_LEN];
	GByteArray dba; // debug byte array
	
	LOG_NOISE("read io prefix\n");
	
	ret = rem_net_recv_priv(net, cn, prefix, REM_IO_PREFIX_LEN);
	if (ret < 0) return ret;

	dba.data = prefix; dba.len = REM_IO_PREFIX_LEN; dump_gba(&dba);
	
	if (memcmp(prefix, REM_IO_PREFIX, REM_IO_PREFIX_LEN)) {
		LOG_WARN("wrong io prefix\n");
		rem_net_client_disconnect(net, cn);
		return -1;
	}
	
	LOG_NOISE("read msg type\n");
	
	ret = rem_net_recv_priv(net, cn, (guint8*) &nmsg_id_nbo, 4);
	if (ret < 0) return ret;
	
	dba.data = (guint8*) &nmsg_id_nbo; dba.len = 4; dump_gba(&dba);

	nmsg->id = g_ntohl(nmsg_id_nbo);
	
	LOG_NOISE("read msg size\n");
	
	ret = rem_net_recv_priv(net, cn, (guint8*) &len_nbo, 4);
	if (ret < 0) return ret;
	
	dba.data = (guint8*) &len_nbo; dba.len = 4; dump_gba(&dba);

	len = g_ntohl(len_nbo);
	
	if (len) {
		
		// read data
		
		nmsg->ba = g_byte_array_sized_new(len);
	
		LOG_NOISE("read msg data (%i bytes)\n", len);
		ret = rem_net_recv_priv(net, cn, nmsg->ba->data, len);
		if (ret < 0) {
			g_byte_array_free(nmsg->ba, TRUE);
			nmsg->ba = NULL;
			nmsg->id = 0;
			return ret;
		}
		
		g_byte_array_set_size(nmsg->ba, len);
		g_assert_debug(nmsg->ba->len == len);
	}
	
	LOG_NOISE("read io suffix\n");
	
	ret = rem_net_recv_priv(net, cn, suffix, REM_IO_SUFFIX_LEN);
	if (ret < 0) {
		g_byte_array_free(nmsg->ba, TRUE);
		nmsg->ba = NULL;
		nmsg->id = 0;
		return ret;
	}

	dba.data = suffix; dba.len = REM_IO_SUFFIX_LEN; dump_gba(&dba);

	return 1;	
}

gint
rem_net_send(rem_net_t *net, guint cn, rem_net_msg_t *nmsg)
{
	g_assert_debug(net && nmsg);

	LOG_NOISE("called\n");

	gint ret;
	gint32 len_nbo, nmsg_id_nbo;

	ret = rem_net_send_priv(net, cn, REM_IO_PREFIX, REM_IO_PREFIX_LEN);
	if (ret < 0) return -1;
	
	LOG_NOISE("write msg id %i\n", nmsg->id);
	
	nmsg_id_nbo = g_htonl(nmsg->id);
	ret = rem_net_send_priv(net, cn, (guint8*) &nmsg_id_nbo, 4);
	if (ret < 0) return -1;

	LOG_NOISE("write msg size %i\n", nmsg->ba ? nmsg->ba->len : 0);
	
	len_nbo = nmsg->ba ? g_htonl(nmsg->ba->len) : 0;
	ret = rem_net_send_priv(net, cn, (guint8*) &len_nbo, 4);
	if (ret < 0) return -1;
	
	g_assert(concl(nmsg->ba, nmsg->ba->len));
	
	// here occurs a valgrind error:
	// "Syscall param write(buf) points to uninitialised byte(s)"
	// I guess this error is caused by glib, since the "uninitialized bytes"
	// are within a GByteArray
	// The valgrind error causes no error functionality!
	if (nmsg->ba) {
		ret = rem_net_send_priv(net, cn, nmsg->ba->data, nmsg->ba->len);
		if (ret < 0) return -1;
	}
	
	ret = rem_net_send_priv(net, cn, REM_IO_SUFFIX, REM_IO_SUFFIX_LEN);
	if (ret < 0) return -1;
	
	return 0;
}

/**
 * Having a look into 'man 2 select_tut' there is file descriptor set in use
 * to look for exceptions and this function processes this set used in the
 * select call below .. TODO: cannot imagine a real situation where this
 * function has something to do .. ?
 * 
 */
static gint
rem_net_sockets_check(rem_net_t *net, fd_set *sds)
{
	g_assert_debug(net && sds);
	
	LOG_NOISE("called\n");
	
	gint ret, retval, ss;
	guint u;
	gchar c;
	
	retval = 0;
	ss = net->server.sock;
	
	// check rfcomm/bluetooth server socket
	if (FD_ISSET(ss, sds)) {
		
		ret = recv(ss, &c, 1, MSG_OOB);
		
		if (ret < 0) {	// socket is down/broken
			
			LOG_ERROR("server socket broken\n");
			
			retval = -1;
			
		} else {	// actually not possible
			
			LOG_WARN("rx'ed OOB data .. ignore\n");
			
			g_assert_not_reached();
		}
	}

	// check rfcomm/bluetooth client sockets
	for (u = 0; u < REM_MAX_CLIENTS; u++) {
		
		if (!net->client[u].sock) continue;
		
		if (FD_ISSET(net->client[u].sock, sds)) {
			
			ret = recv(net->client[u].sock, &c, 1, MSG_OOB);

			if (ret < 0) {	// socket is down/broken
				
				LOG_INFO("lost connection to a client\n");
				
				rem_net_client_disconnect(net, u);
				
			} else {	// actually not possible
				
				LOG_WARN("rx'ed OOB data .. ignore\n");
				
				g_assert_not_reached();	
			}
		}
	}
	
	return retval;
}

static gint
rem_net_sockets_process(rem_net_t *net, fd_set *sds)
{
	g_assert_debug(net && sds);
	
	LOG_NOISE("called\n");

	gint		ss, ret, i;
	guint		u;
	const guint8	pvn = REM_PROTO_VERSION;
	
	ss = net->server.sock;
	

	// check if a client has some data for us
	for (u = 0; u < REM_MAX_CLIENTS; u++) {

		g_assert_debug(
			concl(!net->client[u].sock, !net->client[u].has_data));
		
		if (!rem_net_client_is_connected(net, u)) continue;
		
		net->client[u].has_data = FD_ISSET(net->client[u].sock, sds);
		
	}
	
	// check if a clients wants to connect
	if (FD_ISSET(ss, sds)) {
		
		LOG_DEBUG("process client connection request\n");
		
		i = rem_net_client_accept(net);

		g_assert_debug(concl(i >= 0, i < REM_MAX_CLIENTS));
		
		if (i >= 0) {  
		
			LOG_DEBUG("send hello code\n");
			
			ret = rem_net_send_priv(net, i, REM_IO_PREFIX,
							REM_IO_PREFIX_LEN);
			ret = rem_net_send_priv(net, i, &pvn, 1);
			ret = rem_net_send_priv(net, i, REM_IO_SUFFIX,
							REM_IO_SUFFIX_LEN);
			
			LOG_DEBUG("send hello code - done\n");
		}
		
	}
	
	return 0;
}

gint
rem_net_select(rem_net_t *net, guint select_timeout)
{
	g_assert_debug(net);
	
	gint			ret, sd_max, ss, cs;
	guint			u;
	fd_set			sds_r, sds_e;
	struct timeval		tv;

	ss = net->server.sock;

	// setup the socket set to watch
	
	sd_max = 0;
	FD_ZERO(&sds_r); FD_ZERO(&sds_e);
	FD_SET(ss, &sds_r); FD_SET(ss, &sds_e);
	sd_max = sd_max < ss ? ss : sd_max;
	for (u = 0; u < REM_MAX_CLIENTS; u++) {

		cs = net->client[u].sock;

		if (!cs) continue;
		
		FD_SET(cs, &sds_r);
		FD_SET(cs, &sds_e);
		sd_max = sd_max < cs ? cs : sd_max;
	}
			
	// wait for any socket activity or timeout
	
	tv.tv_sec = select_timeout;
	tv.tv_usec = 0;	
	
	LOG_NOISE("select..\n");
	
	ret = select(sd_max + 1, &sds_r, NULL, &sds_e, &tv);
	
	LOG_NOISE("select returned\n");
	
	if (ret < 0) {
		if (errno == EINTR) {
			// don't report this as an error to the calling function
			// if the interruption has been caused by a SIGINT,
			// the calling function should know this
			return 0;
		} else {
			LOG_ERRNO("select failed");
			return -1;
		}
	}
	
	if (ret) {
		LOG_DEBUG("there is live on the socks\n");
		
		if (rem_net_sockets_check(net, &sds_e) < 0) {
			return -1;
		}
		
		if (rem_net_sockets_process(net, &sds_r) < 0) {
			return -1;
		}
	}
	
	return 0;
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////


