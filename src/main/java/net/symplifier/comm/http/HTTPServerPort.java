package net.symplifier.comm.http;

import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.PortTransmitter;
import net.symplifier.comm.tcp.TCPPort;

import java.nio.BufferUnderflowException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * One HTTPServerPort is created for each HTTPRequest
 *
 * Created by ranjan on 6/10/15.
 */
public class HTTPServerPort extends TCPPort {

  public static final int STATE_BEGIN = 0;
  public static final int STATE_HEADER = 1;
  public static final int STATE_BODY_VALIDATION = 2;
  public static final int STATE_BODY = 3;


  private final HTTP server;
  private HTTPRequest request = new HTTPRequest();
  private HTTPResponse response = new HTTPResponse();


  private int state = 0;


  public HTTPServerPort(HTTP server, SocketChannel socketChannel) {
    super(server, socketChannel);
    this.server = server;
  }

  private Responder errorResponder = new Responder() {

    @Override
    public boolean onTransmitterReady(DigitalPort port, PortTransmitter transmitter) {
      return true;
    }

    @Override
    public void onTransmissionComplete(DigitalPort port) {

    }
  };

  private void error(String text) {
    receiver.purge();
    startTransmission(errorResponder);
  }

  private void httpError(int code, String text) {
    startTransmission(errorResponder);
  }

  @Override
  public void onPrepareReception(PortReceiver receiver) throws BufferUnderflowException {
    request.clear();
    response.clear();

    do {
      if (state == STATE_BEGIN) {
        String line = receiver.getLine(Charset.defaultCharset());
        String parts[] = line.split("\\s+");

        if (parts.length != 3) {
          // Invalid request
          error("Invalid Request");
          return;
        } else if (!request.validateMethod(parts[0])) {
          error("Unrecognized verb " + parts[0]);
          return;
        } else {
          request.setRequestURI(parts[1]);
          request.version = (parts[2]);
          state = receiver.markState(STATE_HEADER);
        }
      }

      if (state == STATE_HEADER) {
        String line = receiver.getLine(Charset.defaultCharset());
        if (line.isEmpty()) {
          state = receiver.markState(STATE_BODY_VALIDATION);
        } else {
          int p = line.indexOf(':');
          if (p > 0) {
            String name = line.substring(0, p).trim();
            String value = line.substring(p + 1).trim();

            request.processHeader(name, value);
          }
        }
      }

      if (state == STATE_BODY_VALIDATION) {
        if (request.contentLength >= 0) {
          receiver.setLimit(request.contentLength);
        } else if (request.isChunked) {
          int chunkLength = Integer.parseInt(receiver.getLine(Charset.defaultCharset()), 16);
          if (chunkLength == 0) {
            receiver.getLine(Charset.defaultCharset());
            state = receiver.markState(STATE_BEGIN);
          } else {
            receiver.setLimit(chunkLength);
          }
        }
        state = receiver.markState(STATE_BODY);
        break;
      }
    } while(true);
  }

  private boolean finalizeRequest() {
    if (state == STATE_BODY) {
      if (request.contentLength >= 0) {
        if (receiver.getLimit() == 0) {
          state = receiver.markState(STATE_BEGIN);
          return true;
        } else {
          return false;
        }
      } else if (request.isChunked) {
        if (receiver.getLimit() == 0) {
          receiver.mark();
          // Supposed to be an empty line
          receiver.getLine(Charset.defaultCharset());
          int chunkLength = Integer.parseInt(receiver.getLine(Charset.defaultCharset()));
          if (chunkLength == 0) {
            receiver.getLine(Charset.defaultCharset());
            state = receiver.markState(STATE_BEGIN);
            return true;
          } else {
            receiver.setLimit(chunkLength);
            return false;
          }
        } else {
          return false;
        }
      } else {
        state = receiver.markState(STATE_BEGIN);
        return true;
      }
    } else {
      return false;
    }
  }

  @Override
  public boolean finalizeReception(PortReceiver receiver) throws BufferUnderflowException {
    if (finalizeRequest()) {
      // the request is complete
      // let's start the response
      Responder responder = server.getResponder(this, request, response);
      if (responder == null) {
        responder = defaultResponder;
      }
      startTransmission(responder);

      return true;
    }

    return false;
  }

  @Override
  public boolean onPrepareTransmission(PortTransmitter transmitter) {
    transmitter.putLine("HTTP/" + response.version + " " + response.responseStatusCode + " " + response.responseStatusText, Charset.defaultCharset());
    for(Map.Entry<String, String> header:response.headers.entrySet()) {
      transmitter.putLine(header.getKey() + ": " + header.getValue(), Charset.defaultCharset());
    }
    transmitter.putLine("", Charset.defaultCharset());
    return true;
  }

  private Responder defaultResponder = new Responder() {
    @Override
    public boolean onTransmitterReady(DigitalPort port, PortTransmitter transmitter) {
      if (response.data == null) {
        return true;
      } else {
        transmitter.putBuffer(response.data);

        return !response.data.hasRemaining();
      }
    }

    @Override
    public void onTransmissionComplete(DigitalPort port) {
      System.out.println("Default transmission complete");
    }
  };

  @Override
  public void finalizeTransmission(PortTransmitter transmitter) {

  }

}
