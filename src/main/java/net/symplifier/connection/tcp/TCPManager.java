package net.symplifier.connection.tcp;

import net.symplifier.core.application.Application;
import net.symplifier.core.application.ExitHandler;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Created by ranjan on 6/22/15.
 */
class TCPManager implements Runnable, ExitHandler {
  private static final TCPManager SELF = new TCPManager();

  private volatile boolean exit = false;

  public TCPManager() {
    try {
      selector = Selector.open();
    } catch (IOException e) {
      e.printStackTrace();
    }
    new Thread(this).start();
  }

  private Selector selector;

  static void hook(TCPServer server) {
    SELF.hookImpl(server, server.serverSocket, SelectionKey.OP_ACCEPT);
  }

  static void unhook(TCPServer server) {
    SELF.unhookImpl(server.serverSocket);
  }
  static void hook(TCPConnection connection, int options) {
    SELF.hookImpl(connection, connection.socket, options);
  }

  static void unhook(TCPConnection connection) {
    SELF.unhookImpl(connection.socket);
  }

  private void unhookImpl(SelectableChannel channel) {
    SelectionKey key = channel.keyFor(selector);
    if (key != null) {
      selector.wakeup();
    }
  }

  private synchronized void hookImpl(Object owner, SelectableChannel channel, int options) {
    SelectionKey key = channel.keyFor(selector);

    if (key != null) {
      if (key.interestOps() != options) {
        key.interestOps(options);
        selector.wakeup();
      }
    } else {
      try {
        key = channel.register(selector, options);
        key.attach(owner);
        key.interestOps(options);
        selector.wakeup();
      } catch (ClosedChannelException e) {
        e.printStackTrace();
      }
    }
  }

  public void run() {
    Application.app().addExitHandler(this);

    while(!exit) {
      int readyChannels = 0;
      synchronized (this) {
      try {
        //System.out.println("Waiting for " + selector.keys().size() + " keys.");
        readyChannels = selector.select();
      } catch (IOException e) {
        //System.err.println("Unexpected error in TCP manager");
        e.printStackTrace();
      }

        //System.out.println("Ready Channels = " + readyChannels);
        if (readyChannels == 0) {
          continue;
        }
      }

      Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
      while(keyIterator.hasNext()) {
        SelectionKey key = keyIterator.next();
        keyIterator.remove();

        TCPConnection conn = null;

        if (key.isAcceptable()) {
          // Server site code will go here
          TCPServer server = (TCPServer)key.attachment();
          try {
            SocketChannel socket = server.serverSocket.accept();
            socket.configureBlocking(false);
            conn = server.createConnection();
            conn.open(socket);
            conn.setErrorHandler(server.errorHandler);
            conn.setOpenHandler(server.openHandler);
            conn.setReceiveHandler(server.receiveHandler);
            conn.setSendHandler(server.sendHandler);
            conn.setCloseHandler(server);   // The server also needs to know when the connection is closing

            conn.propagateOpenEvent();
          } catch(IOException e) {
            // Looks like out of memory, nowhere to report this error
            // TODO Report this error on log files
          }
        } else if (key.isConnectable()) {
          key.interestOps(SelectionKey.OP_READ);
          conn = (TCPConnection)key.attachment();
          conn.finishConnect();
        } else if (key.isWritable()) {
          key.interestOps(SelectionKey.OP_READ);
          conn = (TCPConnection)key.attachment();
          conn.propagateSendEvent();
        } else if (key.isReadable()) {
          key.interestOps(SelectionKey.OP_READ);
          conn = (TCPConnection)key.attachment();
          conn.propagateReceiveEvent();
        }
      }
    }

    try {
      selector.close();
    } catch(IOException e) {

    }
    selector = null;
  }


  @Override
  public void onExit(Application app) {
    exit = true;
    selector.wakeup();
  }
}
