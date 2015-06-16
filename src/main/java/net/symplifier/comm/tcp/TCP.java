package net.symplifier.comm.tcp;

import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.ServerPort;
import net.symplifier.core.application.threading.ThreadTarget;
import org.omg.CORBA.DynAnyPackage.Invalid;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ranjan on 6/9/15.
 */
public class TCP extends ServerPort implements ThreadTarget<TCPManager, Object> {

  private List<TCPPort> children = new LinkedList<>();

  private String bindAddress;
  private int    port;

  ServerSocketChannel serverSocket;
  private volatile boolean exit = false;

  public TCP(Owner owner, String name) throws InvalidPortNameException {
    super(owner, name);
    validate(name.split(":"));
  }


  private void validate(String parts[]) throws InvalidPortNameException {
    if (parts.length > 2) {
      throw new InvalidPortNameException(this, getName());
    }

    try {
      if (parts.length == 1) {
        this.validate("0.0.0.0", Integer.parseInt(parts[0]));
      } else {
        this.validate(parts[0], Integer.parseInt(parts[1]));
      }
    } catch(NumberFormatException e) {
      throw new InvalidPortNameException(this, getName());
    }
  }

  private void validate(String bindAddress, int port) throws InvalidPortNameException {
    if (port <=0 || port > 65535) {
      throw new InvalidPortNameException(this, getName());
    }

    this.bindAddress = bindAddress;
    this.port = port;
  }

  public TCP(Owner owner, int port) throws InvalidPortNameException {
    this(owner, "0.0.0.0", port);

  }

  public TCP(Owner owner, String bindAddress, int port) throws InvalidPortNameException {
    super(owner, bindAddress + ":" + port);
    validate(bindAddress, port);
  }

  public boolean start() {

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

    // Keep track of all the children port
    children.add(port);

    // Raise event
    getAttachment().onPortOpen(port);

    // Wait for incoming connections
    TCPManager.hook(port, SelectionKey.OP_READ);
  }

  public String toString() {
    return getName();
  }
}
