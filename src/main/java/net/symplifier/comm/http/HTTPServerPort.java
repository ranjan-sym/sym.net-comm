package net.symplifier.comm.http;

import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.Port;
import net.symplifier.comm.PortTransmitter;
import net.symplifier.comm.tcp.TCPPort;

import java.nio.BufferUnderflowException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/10/15.
 */
public class HTTPServerPort extends TCPPort {
  public static final int STATE_BEGIN = 0;
  public static final int STATE_HEADER = 1;
  public static final int STATE_BODY = 2;

  private String requestMethod;
  private String requestPath;
  private String requestVersion;

  private int state = 0;


  public HTTPServerPort(Owner owner, String name) throws InvalidPortNameException {
    super(owner, name);
  }

  public HTTPServerPort(HTTP server, SocketChannel socketChannel) {
    super(server, socketChannel);
  }

  public void onPrepareReception() throws BufferUnderflowException {
    if (state == STATE_BEGIN || state == STATE_HEADER) {
      String line = receiver.getLine(Charset.defaultCharset());
      if (state == STATE_BEGIN) {
        state = STATE_HEADER;
      } else {

      }

    }
  }

  @Override
  public boolean onPrepareTransmission(PortTransmitter transmitter) {
    return true;
  }

}
