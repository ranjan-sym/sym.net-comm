package net.symplifier.comm.tcp;

import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.PortTransmitter;
import net.symplifier.core.application.threading.ThreadTarget;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by ranjan on 6/9/15.
 */
public class TCPPort extends DigitalPort implements ThreadTarget<TCPManager, Object> {

  SocketChannel socket;

  private String  remoteHost;
  private int     remotePort;

  private volatile boolean waitForTransmitEvent = false;

  public String getRemoteHost() {
    return remoteHost;
  }

  public TCPPort(String name) throws InvalidPortNameException {
    super(name);

    if(name.indexOf(':') > 0) {
      String parts[] = name.split(":", 2);

      remoteHost = parts[0].trim();
      if (remoteHost.isEmpty()) {
        throw new InvalidPortNameException(this, name);
      }
      try {
        remotePort = Integer.parseInt(parts[1]);
        if (remotePort <=0 || remotePort > 65535) {
          throw new InvalidPortNameException(this, name);
        }
      } catch(NumberFormatException ex) {
        throw new InvalidPortNameException(this, name);
      }
    } else {
      throw new InvalidPortNameException(this, name);
    }

  }

  public TCPPort(String host, int port) throws InvalidPortNameException {
    this(host + ":" + port);
  }

  private static String extractName(SocketChannel channel) {
    try {
      return channel.getRemoteAddress().toString();
    } catch(IOException e) {
      return Integer.toString(channel.hashCode());
    }
  }

  protected TCPPort(TCP server, SocketChannel socket) {
    super(extractName(socket));
    this.socket = socket;
    try {
      this.socket.configureBlocking(false);
    } catch(IOException e) {
      getAttachment().onPortError(this, e);
    }
    this.setAttachment(server.getAttachment());
  }


  @Override
  protected void onPrepareReception(PortReceiver receiver) {

  }

  @Override
  protected boolean finalizeReception(PortReceiver receiver) {
    return true;
  }

  @Override
  protected boolean onPrepareTransmission(PortTransmitter transmitter) {
    return true;
  }

  @Override
  protected void finalizeTransmission(PortTransmitter transmitter) {

  }

  protected void flush() {
    try {
      socket.write(getTransmitterBuffer());
      waitForTransmitEvent = true;
    } catch(IOException e) {
      getAttachment().onPortError(this, e);
    }
  }

  @Override
  protected int getReceiverBufferLength() {
    return 16384;    // The optimum packet size for TCP is 1380 bytes
  }

  @Override
  protected int getTransmitterBufferLength() {
    return 16384;    // The optimum packet size for TCP is 1380 bytes
  }

  @Override
  public void open() {
    try {
      SocketAddress remoteAddr = new InetSocketAddress(remoteHost, remotePort);
      socket = SocketChannel.open();
      socket.configureBlocking(false);
      TCPManager.hook(this, SelectionKey.OP_CONNECT);
      socket.connect(remoteAddr);
    } catch (IOException e) {
      getAttachment().onPortError(this, e);
    }
  }

  @Override
  public void close() {
    try {
      socket.close();
    } catch(IOException e) {
      // We are going to ignore the close event here
    }

    TCPManager.unhook(this);
    socket = null;
    getAttachment().onPortClose(this);
  }

  /**
   * The worker thread for the TCPPort operation
   *
   * @param source The TCPManager responsible for managing all the TCP sessions
   * @param attachment The attachment is provides the interestOps code namely
   *                   - SelectionKey.OP_CONNECT
   *                   - SelectionKey.OP_READ
   *                   - SelectionKey.OP_WRITE
   */
  @Override
  public void onRun(TCPManager source, Object attachment) {
    Integer interestOps = (Integer)attachment;

    int newInterest = 0;
    waitForTransmitEvent = false;

    if ((interestOps & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
      try {
        if (this.socket.finishConnect()) {
          getAttachment().onPortOpen(this);
          newInterest = SelectionKey.OP_READ;
        }
      } catch(IOException e) {
        getAttachment().onPortError(this, e);
      }
    }

    if ((interestOps & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
      try {
        ByteBuffer buffer = getReceiverBuffer();
        int n = socket.read(buffer);
        if (n == -1) {
          // Looks like the socket is closing
          // if there is any data available, try to process it one last time
          if (buffer.position() > 0) {
            receiver.onReceive();
          }
          close();
        } else {
          System.out.println("Received - " + n + " bytes of data");
          receiver.onReceive();
          newInterest = SelectionKey.OP_READ;
        }
      } catch (IOException e) {
        getAttachment().onPortError(this, e);
        newInterest = SelectionKey.OP_READ;
      }
    }

    if ((interestOps & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
      transmitter.onTransmit();

      newInterest = SelectionKey.OP_READ;
    }

    if (waitForTransmitEvent) {
      newInterest |= SelectionKey.OP_WRITE;
    }

    if (newInterest != 0) {
      TCPManager.hook(this, newInterest);
    }
  }

  public String toString() {
    return getName();
  }
}
