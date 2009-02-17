import bluetooth
import socket
import gobject
import struct

import log
import message
import remuco
import serial

# TODO: remove unused debug messages

def build_message(id, serializable):
    """ Create a message ready to send on a socket.
    
    @param id: message id
    @param serializable: message content (object of type Serializable)
    
    @return: the message as a binary string (incl. IO prefix and suffix) or
             None if serialization failed
    """
    
    # This is not included in ClientConnection.send() because if there are
    # multiple clients, each client would serialize the data to send again.
    # Using this method, a message can be serialized once and send to many
    # clients.
    
    if serializable is not None:
        ba = serial.pack(serializable);
        if ba is None:
            log.warning("failed to serialize (msg-id %d)" % id)
            return None
    else:
        ba = ""
    
    header = struct.pack("!ii", id, len(ba))
    
#    hex = ""
#    for c in ba:
#        hex = "%s %X" % (hex, ord(c))
#    log.debug("built message (id %d, size %d, data '%s')" % (id, len(ba), hex))
    
    return "%s%s%s%s" % (ClientConnection.IO_PREFIX, header, ba,
                         ClientConnection.IO_SUFFIX)

class ReceiveBuffer():
    """ A box to pool some receive buffer related data. """
    
    def __init__(self):
        
        self.data = ""
        self.rest = 0
        
class ClientConnection():
    
    IO_PREFIX = '\xff\xff\xff\xff'
    IO_PREFIX_LEN = len(IO_PREFIX)
    IO_SUFFIX = '\xfe\xfe\xfe\xfe'
    IO_SUFFIX_LEN = len(IO_SUFFIX)
    IO_HEADER_LEN = 8
    IO_MSG_MAX_SIZE = 10240 # prevent DOS
    PROTO_VERSION = '\x08'
    IO_HELLO = "%s%s%s" % (IO_PREFIX, PROTO_VERSION, IO_SUFFIX) # hello message
    
    def __init__(self, sock, addr, clients, player_info_msg, msg_handler_fn):
        
        self.__sock = sock
        self.__addr = addr
        self.__addr_str = str(addr)
        self.__clients = clients
        self.__player_info_msg = player_info_msg
        self.__msg_handler_fn = msg_handler_fn
        
        # the following fields are used for iterative receiving on message data
        # see io_recv() and io_recv_buff()
        self.__rcv_buff_prefix = ReceiveBuffer()
        self.__rcv_buff_header = ReceiveBuffer()
        self.__rcv_buff_data = ReceiveBuffer()
        self.__rcv_buff_suffix = ReceiveBuffer()
        self.__rcv_msg_id = message.MSG_ID_IGNORE
        self.__rcv_msg_size = 0
        
        self.__snd_buff = "" # buffer for outgoing data
        
        # source IDs for IO related events
        self.__sids = (
            gobject.io_add_watch(self.__sock, gobject.IO_IN, self.__io_recv),
            gobject.io_add_watch(self.__sock, gobject.IO_ERR, self.__io_error),
            gobject.io_add_watch(self.__sock, gobject.IO_HUP, self.__io_hup)
            )
        self.__sid_out = 0
        
        log.debug("send 'hello' to %s" % self.__addr_str)
        
        self.send(ClientConnection.IO_HELLO)
        
    #==========================================================================
    # io
    #==========================================================================
    
    def __recv_buff(self, rcv_buff):
        """ Receive some data and put it into the given ReceiveBuffer.
        
        @param rcv_buff: the receive buffer to put received data into
        
        @return: true if some data has been received, false if an error occurred
        """
       
        try:
            log.debug("try to receive %d bytes" % rcv_buff.rest)
            data = self.__sock.recv(rcv_buff.rest)
        except socket.timeout, e:
            log.warning("connection to %s broken (%s)" % (self.__addr_str, e))
            self.disconnect()
            return False
        except socket.error, e:
            log.warning("connection to %s broken (%s)" % (self.__addr_str, e))
            self.disconnect()
            return False
        
        received = len(data)
        
        log.debug("received %d bytes" % received)
        
        if received == 0:
            log.warning("connection to %s broken (no data)" % self.__addr_str)
            self.disconnect()
            return False
        
        rcv_buff.data = "%s%s" % (rcv_buff.data, data)
        rcv_buff.rest -= received
        
        return True
        
    
    def __io_recv(self, fd, cond):
        """ GObject callback function (when there is data to receive). """
        
        log.debug("client %s is knocking" % self.__addr_str)

        if self.__rcv_buff_prefix.rest + self.__rcv_buff_header.rest + \
                    self.__rcv_buff_data.rest + self.__rcv_buff_suffix.rest == 0:
            # new message
            self.__rcv_msg_id = message.MSG_ID_IGNORE
            self.__rcv_msg_size = 0 # variable, will be set later
            self.__rcv_buff_prefix.data = ""
            self.__rcv_buff_prefix.rest = ClientConnection.IO_PREFIX_LEN
            self.__rcv_buff_header.data = ""
            self.__rcv_buff_header.rest = ClientConnection.IO_HEADER_LEN
            self.__rcv_buff_data.data = ""
            self.__rcv_buff_data.rest = 0 # variable, will be set later
            self.__rcv_buff_suffix.data = ""
            self.__rcv_buff_suffix.rest = ClientConnection.IO_SUFFIX_LEN
    
        if self.__rcv_buff_prefix.rest > 0:
            
            return self.__recv_buff(self.__rcv_buff_prefix)
            
        if self.__rcv_buff_header.rest > 0:
            
            ok = self.__recv_buff(self.__rcv_buff_header)
            if not ok: return False
            if self.__rcv_buff_header.rest == 0:
                # completely received header
                self.__rcv_msg_id, self.__rcv_msg_size = \
                    struct.unpack('!ii', self.__rcv_buff_header.data)
                if self.__rcv_msg_size > ClientConnection.IO_MSG_MAX_SIZE:
                    log.warning("msg from %s too big (%d bytes)" %
                                (self.__addr_str ,self.__rcv_msg_size))
                    self.disconnect()
                    return False
                self.__rcv_buff_data.rest = self.__rcv_msg_size
            return True
        
        if self.__rcv_buff_data.rest > 0:
            
            return self.__recv_buff(self.__rcv_buff_data)
        
        if self.__rcv_buff_suffix.rest > 0:
            
            ok = self.__recv_buff(self.__rcv_buff_suffix)
            if not ok: return False
            if self.__rcv_buff_suffix.rest != 0:
                # still need to get some bytes
                return True
            else:
                # message complete
                msg_id = self.__rcv_msg_id
                msg_data = self.__rcv_buff_data.data

        # handle message
        
        if msg_id == message.MSG_ID_IFC_CINFO:
            
            log.debug("received client info from %s" % self.__addr_str)
            
            self.__clients.append(self)
            
            log.debug("sending player info to %s" % self.__addr_str)
            
            self.send(self.__player_info_msg)
            
            self.__msg_handler_fn(message.MSG_ID_PRIV_REQ_INITIAL_DATA, None,
                                  self)
            
        else:
            
            self.__msg_handler_fn(msg_id, msg_data, self)
        
        return True

    def __io_error(self, fd, cond):
        """ GObject callback function (when there is an error). """
        log.error("connection to client %s broken" % self.__addr_str)
        self.disconnect()
        return False
        
    def __io_hup(self, fd, cond):
        """ GObject callback function (when other side disconnected). """
        log.info("client %s disconnected" % self.__addr_str)
        self.disconnect()
        return False
    
    def __io_send(self, fd, cond):
        """ GObject callback function (when data can be written). """
        
        if len(self.__snd_buff) == 0:
            self.__sid_out = 0
            return False

        log.debug("try to send %d bytes to %s" %
                  (len(self.__snd_buff), self.__addr_str))

        try:
            sent = self.__sock.send(self.__snd_buff)
        except socket.error, e:
            log.warning("failed to send data to %s (%s)" % (self.__addr_str, e))
            self.disconnect()
            return False

        log.debug("sent %d bytes" % sent)
        
        if sent == 0:
            log.warning("failed to send data to %s" % self.__addr_str)
            self.disconnect()
            return False
        
        self.__snd_buff = self.__snd_buff[sent:]
        
        if len(self.__snd_buff) == 0:
            self.__sid_out = 0
            return False
        else:
            return True
    
    def send(self, msg):
        """ Send a message to a client.
        
        @param msg: complete message (incl. IO prefix and suffix, ID and length)
                    in binary format (net.build_message() is your friend here)
        
        @see net.build_message()
        """
        
        if msg is None:
            log.warning("oops, looks like a bug: msg is None")
            return
        
        if self.__sock is None:
            log.debug("cannot send message to %s, already disconnected" %
                      self.__addr_str)
            return

        self.__snd_buff = "%s%s" % (self.__snd_buff, msg)
        
        # if not already trying to send data ..
        if self.__sid_out == 0:
            # .. do it when it is possible:
            self.__sid_out = gobject.io_add_watch(self.__sock, gobject.IO_OUT,
                                                  self.__io_send)
        
        
#        try:
#            log.debug("message: %s (len %d)" % (msg, len(msg)))
#            log.debug("try to send message to %s" % self.__addr_str)
#            self.__sock.sendall(msg)
#            #self.__sock.sendall(ClientConnection.IO_SUFFIX)
#            log.debug("sent message to %s" % self.__addr_str)
#        except IOError, e:
#            log.warning("failed to send message to client (%s)" % e)
#            self.disconnect()
            
    def disconnect(self, remove_from_list=True):
        """ Disconnect the client.
        
        @keyword remove_from_list: whether to remove the client from the client
                                   list or not (default is true)
        """
        
        log.debug("disconnect %s" % self.__addr_str)
        
        if remove_from_list:
            try:
                self.__clients.remove(self)
            except ValueError:
                # not yet received client info -> not in client list
                pass
        
        for sid in self.__sids:
            gobject.source_remove(sid)
        
        self.__sids = ()

        if (self.__sid_out > 0):
            gobject.source_remove(self.__sid_out)
            self.__sid_out = 0
            
        
        if self.__sock is not None:
            try:
                self.__sock.shutdown(socket.SHUT_RDWR)
            except socket.error, e:
                pass
            self.__sock.close()
            self.__sock = None

    #==========================================================================
    # miscellaneous
    #==========================================================================

    def get_address(self):
        """ Get the address of this client as a string. """
        return self.__addr_str
    
class Server():
    
    SOCKET_TIMEOUT = 2.5
    
    def __init__(self, clients, player_info, msg_handler_fn):
        """ Create a new server.
        
        @param clients: a list to add connected clients to
        @param player_info: player_info (type data.PlayerInfo)
        @param msg_handler_fn: callback function for passing received messages to
                                 
        """
        
        self.__clients = clients
        self.__msg_handler_fn = msg_handler_fn
        self._player_info = player_info # needed by derived classes
        self.__player_info_msg = build_message(message.MSG_ID_IFS_PINFO,
                                               player_info)
        self._sock = None # needed by derived classes
        self.__sid = None
        
        # set up socket
        
        try:
            self._sock = self._create_socket()
            self._sock.settimeout(Server.SOCKET_TIMEOUT)
        except IOError, e:
            log.error("failed to set up %s server (%s)" %
                          (self._get_type(), e))
            return
        except socket.error, e:
            log.error("failed to set up %s server (%s)" %
                          (self._get_type(), e))
            return
        
        # watch socket
        
        self.__sid = gobject.io_add_watch(self._sock,
                            gobject.IO_IN | gobject.IO_ERR | gobject.IO_HUP,
                            self.__handle_io)
        
    #==========================================================================
    # io
    #==========================================================================

    def __handle_io(self, fd, condition):
        """ GObject callback function (when there is a socket event). """
        
        if condition == gobject.IO_IN:
            
            try:
                log.debug("connection request from client")
                client_sock, addr = self._sock.accept();
                log.debug("connection request accepted")
                # TODO: timeout does not seem to work
                client_sock.setblocking(0)
                #client_sock.settimeout(Server.SOCKET_TIMEOUT)
                ClientConnection(client_sock, addr, self.__clients,
                                 self.__player_info_msg, self.__msg_handler_fn)
            except IOError, e:
                log.error("accepting %s client failed: %s" %
                              (self._get_type(), e))
            
            return True
        
        else:
            
            log.error("%s server socket broken" % self._get_type())
            self.__sid = None
            return False
    
    def down(self):
        """ Shut down the server. """
        
        if self.__sid is not None:
            gobject.source_remove(self.__sid) 

        if self._sock is not None:
            log.debug("closing %s server socket" % self._get_type())
            try:
                self._sock.shutdown(socket.SHUT_RDWR)
            except socket.error, e:
                pass
            self._sock.close()
            self._sock = None

    def _create_socket(self):
        """ Create the server socket.
        
        @return: a socket object
        
        @attention: To be overwritten by sub classes.
        """
        pass
    
    #==========================================================================
    # miscellaneous
    #==========================================================================

    def _get_type(self):
        """ Get type name.
        
        @return: descriptive type name
        
        @attention: To be overwritten by sub classes.
        """
        return ""
    
class BluetoothServer(Server):
    
    UUID = "025fe2ae-0762-4bed-90f2-d8d778f020fe"

    def _create_socket(self):
        
        sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        
        sock.settimeout(0.33)
        
        sock.bind(("", bluetooth.PORT_ANY))
        sock.listen(1)
        
        bluetooth.advertise_service(sock, self._player_info.get_name(),
            service_id = BluetoothServer.UUID,
            service_classes = [ BluetoothServer.UUID, bluetooth.SERIAL_PORT_CLASS ],
            profiles = [ bluetooth.SERIAL_PORT_PROFILE ])
        
        log.debug("created bluetooth server")
        
        return sock
        
    def down(self):
        
        if self._sock is not None:
            try:
                bluetooth.stop_advertising(self._sock)
            except bluetooth.BluetoothError:
                pass
        
        Server.down(self)
        
    def _get_type(self):
        return "bluetooth"
                
class WifiServer(Server):
    
    PORT = 34271

    def _create_socket(self):
        
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.bind(('', WifiServer.PORT))
        sock.listen(1)
        
        log.debug("created wifi server")
        
        return sock

    def _get_type(self):
        return "wifi"

