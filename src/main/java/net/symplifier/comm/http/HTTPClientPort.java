package net.symplifier.comm.http;

import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.PortTransmitter;
import net.symplifier.comm.tcp.TCPPort;

import java.nio.BufferUnderflowException;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/10/15.
 */
public class HTTPClientPort extends TCPPort {
  public HTTPClientPort(String name) throws InvalidPortNameException {
    super(name);
    this.path = "/";
  }

  public HTTPClientPort(String host, int port, String path) throws InvalidPortNameException {
    super(host + ":" + port);
    this.path = path;
  }

  private int state = STATE_START;
  private static final int STATE_START = 0;
  private static final int STATE_HEADER = 1;
  private static final int STATE_BODY_VALIDATION = 2;
  private static final int STATE_BODY = 3;


  private String path;

  private int contentLength = -1;   // content length is not defined and the content
                                    // is expected to end with the closing
  private boolean isChunked = false;

  private void processHeader(String name, String value) {
    if (name.equalsIgnoreCase("content-length")) {
      try {
        this.contentLength = Integer.parseInt(value);
      } catch(NumberFormatException ex) {
        // How about a bad content length error
        this.contentLength = -1;
      }
    } else if(name.equalsIgnoreCase("transfer-encoding")) {
      if (value.equalsIgnoreCase("chunked")) {
        isChunked = true;
      }
    }
  }

  @Override
  public void onPrepareReception(PortReceiver receiver) throws BufferUnderflowException {
    do {
      if (state == STATE_START) {
        String resp = receiver.getLine(Charset.defaultCharset());
        System.out.println("Response - " + resp);

        state = receiver.markState(STATE_HEADER);
      }

      if (state == STATE_HEADER) {
        String header = receiver.getLine(Charset.defaultCharset());
        int p = header.indexOf(':');
        if (p > 0) {
          String name = header.substring(0, p).trim();
          String value = header.substring(p + 1).trim();
          processHeader(name, value);
        }

        if (header.isEmpty()) {
          state = receiver.markState(STATE_BODY_VALIDATION);
        } else {
          state = receiver.markState(STATE_HEADER);
        }
      }

      if (state == STATE_BODY_VALIDATION) {
        if (contentLength >= 0) {
          receiver.setLimit(contentLength);
        } else if (isChunked) {
          receiver.mark();
          int chunkLength = Integer.parseInt(receiver.getLine(Charset.defaultCharset()), 16);
          if (chunkLength == 0) {     // We do not expect to get a chunk length 0 straight away but handle it anyway
            receiver.getLine(Charset.defaultCharset());
            state = receiver.markState(STATE_START);
          }
          receiver.setLimit(chunkLength);
        }
        state = receiver.markState(STATE_BODY);
        break;
      }

    } while (true);
  }

  @Override
  public boolean finalizeReception(PortReceiver receiver) {
    if (state == STATE_BODY) {
      if (contentLength >= 0) {
        if (receiver.getLimit() == 0) {
          state = receiver.markState(STATE_START);
          return true;
        } else {
          return false;
        }
      } else if (isChunked) {
        if (receiver.getLimit() == 0) {
          receiver.mark();
          // Supposed to be an empty line
          receiver.getLine(Charset.defaultCharset());
          int chunkLength = Integer.parseInt(receiver.getLine(Charset.defaultCharset()));
          if (chunkLength == 0) {
            receiver.getLine(Charset.defaultCharset());
            state = receiver.markState(STATE_START);
            return true;
          } else {
            receiver.setLimit(chunkLength);
            return false;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public boolean onPrepareTransmission(PortTransmitter transmitter) {
    transmitter.putLine("GET " + path + " HTTP/1.1", Charset.defaultCharset());
    transmitter.putLine("Host: " + getRemoteHost(), Charset.defaultCharset());
    transmitter.putLine("", Charset.defaultCharset());

    return true;
  }
}
