package net.symplifier.server;

import net.symplifier.connection.*;
import net.symplifier.connection.tcp.TCPServer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ranjan on 6/22/15.
 */
public class HTTP {
  private final TCPServer server;
  private final Handler handler = new Handler();

  private class Handler implements OpenHandler, CloseHandler, ReceiveHandler, SendHandler, ErrorHandler {

    private Map<Connection, HTTPRequest> requests = new HashMap<>();

    @Override
    public void onClose(Connection connection) {

    }

    @Override
    public void onError(Connection connection) {

    }

    @Override
    public void onOpen(Connection connection) {

    }

    @Override
    public void onReceive(Connection connection) {
      HTTPRequest request = requests.get(connection);
      if (request == null) {
        request = new HTTPRequest(connection);
        request.process();
      }
    }

    @Override
    public void onSend(Connection connection) {

    }
  }



  public HTTP(String bindAddress, int port) {
    server = new TCPServer(bindAddress, port);
    server.setOpenHandler(handler);
    server.setCloseHandler(handler);
    server.setReceiveHandler(handler);
    server.setSendHandler(handler);
    server.setErrorHandler(handler);
  }

  public boolean open() {
    return server.open();
  }

  public void close() {
    server.close();
  }




}
