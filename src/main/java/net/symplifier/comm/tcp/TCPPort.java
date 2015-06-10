package net.symplifier.comm.tcp;

import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.PortTransmitter;
import net.symplifier.core.application.threading.ThreadTarget;

import java.io.IOException;
import java.nio.BufferUnderflowException;
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

  public TCPPort(Owner owner, String name) throws InvalidPortNameException {
    super(owner, name);

    if(name.indexOf(':') > 0) {
      String parts[] = name.split(":", 2);

      remoteHost = parts[0].trim();
      if (remoteHost.isEmpty()) {
        throw new InvalidPortNameException();
      }
      try {
        remotePort = Integer.parseInt(parts[1]);
        if (remotePort <=0 || remotePort > 65535) {
          throw new InvalidPortNameException();
        }
      } catch(NumberFormatException ex) {
        throw new InvalidPortNameException();
      }
    } else {
      throw new InvalidPortNameException();
    }

  }

  public TCPPort(Owner owner, String host, int port) throws InvalidPortNameException {
    this(owner, host + ":" + port);
  }

  private static String extractName(SocketChannel channel) {
    try {
      return channel.getRemoteAddress().toString();
    } catch(IOException e) {
      return Integer.toString(channel.hashCode());
    }
  }

  protected TCPPort(TCP server, SocketChannel socket) {
    super(server.getOwner(), extractName(socket));
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
    return 1024+256;
  }

  @Override
  protected int getTransmitterBufferLength() {
    return 1024+256;
  }

  @Override
  public void open() {
    try {
      socket = SocketChannel.open();
      socket.configureBlocking(true);
      TCPManager.hook(this, SelectionKey.OP_CONNECT);
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

  @Override
  public void onRun(TCPManager source, Object attachment) {
    Integer interestOps = (Integer)attachment;

    int newInterest = 0;
    waitForTransmitEvent = false;

    if ((interestOps & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
      getAttachment().onPortOpen(this);

      newInterest = SelectionKey.OP_READ;
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
