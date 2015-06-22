package net.symplifier.connection.tcp;

import net.symplifier.connection.AbstractConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by ranjan on 6/22/15.
 */
public class TCPConnection extends AbstractConnection {
  SocketChannel socket;
  SocketAddress socketAddress;

  private volatile boolean isOpen = false;

//  TCPConnection(SocketChannel socket) {
//    this.socket = socket;
//    isOpen = true;
//  }

  private final TCPServer server;

  TCPConnection(TCPServer server) {
    this.server = server;
  }

  public TCPConnection(String host, int port) {
    this.server = null;
    socketAddress = new InetSocketAddress(host, port);
  }

  public TCPServer getServer() {
    return server;
  }

  void open(SocketChannel socket) {
    this.socket = socket;
    this.isOpen = true;
  }

  public boolean isOpen() {
    return isOpen;
  }

  @Override
  public void open() {
    try {
      socket = SocketChannel.open();
      socket.configureBlocking(false);
      socket.connect(socketAddress);
      TCPManager.hook(this, SelectionKey.OP_CONNECT);
    } catch (IOException e) {
      raiseErrorEvent(e);
    }
  }

  void finishConnect() {
    try {
      this.socket.finishConnect();
      isOpen = true;
      raiseOpenEvent();
    } catch (IOException e) {
      raiseErrorEvent(e);
    }
  }

  void propagateOpenEvent() {
    if (isOpen) {
      raiseOpenEvent();
    }
  }

  void propagateReceiveEvent() {
    if (isOpen) {
      raiseReceiveEvent();
    }
  }

  void propagateSendEvent() {
    if (isOpen) {
      raiseSendEvent();
    }
  }



  @Override
  public void close() {
    if (isOpen) {
      isOpen = false;

      raiseCloseEvent();
      try {
        socket.close();
      } catch (IOException e) {
        // We are not going to do anything even
        // if there is an error here
      }
      TCPManager.unhook(this);
    }
  }

  @Override
  public void send(ByteBuffer buffer) {
    try {
      socket.write(buffer);
      TCPManager.hook(this, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    } catch (IOException e) {
      //TCPManager.hook(this, SelectionKey.OP_READ);
      raiseErrorEvent(e);
    }
  }

  @Override
  public void receive(ByteBuffer buffer) {
    try {
      int n = socket.read(buffer);
      if (n == -1) {
        close();
      }
    } catch (IOException e) {
      raiseErrorEvent(e);
    }
  }
}
