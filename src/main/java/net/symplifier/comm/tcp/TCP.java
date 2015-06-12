package net.symplifier.comm.tcp;

import net.symplifier.comm.ServerPort;
import net.symplifier.core.application.threading.ThreadTarget;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by ranjan on 6/9/15.
 */
public class TCP extends ServerPort implements ThreadTarget<TCPManager, Object> {
  private String bindAddress;
  private int    port;

  ServerSocketChannel serverSocket;
  private volatile boolean exit = false;

  public TCP(Owner owner, String name) {
    super(owner, name);
  }

  public TCP(Owner owner, int port) {
    super(owner, Integer.toString(port));
  }

  public TCP(Owner owner, String bindAddress, int port) {
    super(owner, bindAddress + ":" + port);
  }

  public boolean start() {
    String name = getName();
    int p = name.indexOf(':');
    if (p >= 0) {
      bindAddress = name.substring(0, p);
      port = Integer.parseInt(name.substring(p+1));
    } else {
      bindAddress = "0.0.0.0";
      port = Integer.parseInt(name);
    }

    SocketAddress address = new InetSocketAddress(bindAddress, port);

    try {
      serverSocket = ServerSocketChannel.open();
      serverSocket.configureBlocking(false);
      serverSocket.bind(address);

      System.out.println("Listening on - " + address.toString());
    } catch(IOException e) {
      System.out.println("Port already used - " + address.toString());
      return false;
    }

    // Wait for events
    TCPManager.hook(this);

    return true;
  }

  public void stop() {

    // TODO close all the child ports
    try {
      serverSocket.close();
    } catch(IOException e) {

    }
    TCPManager.unhook(this);
    serverSocket = null;
  }

  /**
   * Override this method to create your own port for the Server implementation
   *
   * @param channel
   * @return
   */
  public TCPPort createPort(SocketChannel channel) {
    return new TCPPort(this, channel);
  }


  @Override
  public void onRun(TCPManager source, Object attachment) {
    // the attachment is expected to be a SocketChannel
    SocketChannel socket = (SocketChannel)attachment;
    TCPPort port = createPort(socket);

    // Raise event
    getAttachment().onPortOpen(port);

    // Wait for incoming connections
    TCPManager.hook(port, SelectionKey.OP_READ);
  }

  public String toString() {
    return getName();
  }
}
