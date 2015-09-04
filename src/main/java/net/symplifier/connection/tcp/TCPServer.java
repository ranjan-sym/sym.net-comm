package net.symplifier.connection.tcp;

import net.symplifier.connection.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ranjan on 6/22/15.
 */
public class TCPServer implements Server<TCPConnection>, CloseHandler {
  OpenHandler openHandler;
  CloseHandler closeHandler;
  ErrorHandler errorHandler;
  ReceiveHandler receiveHandler;
  SendHandler sendHandler;

  ServerSocketChannel serverSocket;
  private InetSocketAddress address;

  private volatile boolean isOpen;

  private final List<TCPConnection> children = new LinkedList<>();


  public TCPServer(String bindAddress, int port) {
    this.address = new InetSocketAddress(bindAddress, port);
  }

  @Override
  public synchronized boolean open() {
    if (isOpen) {
      return true;
    }

    try {
      serverSocket = ServerSocketChannel.open();
      serverSocket.configureBlocking(false);
      serverSocket.bind(address);
      TCPManager.hook(this);
      isOpen = true;
      return true;
    } catch(IOException e) {
      return false;
    }
  }

  @Override
  public synchronized void close() {
    // Close all the child connections
    synchronized (children) {
      Iterator<TCPConnection> iterator = children.iterator();
      while(iterator.hasNext()) {
        iterator.next().close();
        iterator.remove();
      }
    }

    isOpen = false;
  }

  @Override
  public TCPConnection createConnection() {
    TCPConnection conn;
    synchronized (children) {
      conn = new TCPConnection(this);
      this.children.add(conn);
    }
    return conn;
  }

  @Override
  public void setOpenHandler(OpenHandler handler) {
    this.openHandler = handler;
  }

  @Override
  public void setCloseHandler(CloseHandler closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  public void setReceiveHandler(ReceiveHandler receiveHandler) {
    this.receiveHandler = receiveHandler;
  }

  @Override
  public void setSendHandler(SendHandler sendHandler) {
    this.sendHandler = sendHandler;
  }

  @Override
  public void setErrorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }



  @Override
  public void onClose(Connection connection) {
    // Remove the connection from its list
    synchronized (children) {
      children.remove(connection);
    }
  }
}
