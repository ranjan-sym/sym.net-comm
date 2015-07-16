package net.symplifier.server;

import net.symplifier.connection.Connection;
import net.symplifier.connection.tcp.TCPConnection;
import org.json.JSONTokener;

import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * Created by ranjan on 6/22/15.
 */
public class HTTPRequest {
  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PUT = "PUT";
  public static final String DELETE = "DELETE";

  private final static int STATE_HEAD_REQUEST = 0;
  private final static int STATE_HEAD_HEADER = 1;
  private final static int STATE_BODY = 2;
  private final static int STATE_RESPONSE = 3;
  private final static int STATE_COMPLETE = 4;

  private final Connection connection;
  private final ByteBuffer rxBuffer = ByteBuffer.allocate(10240);

  HTTPRequest(Connection connection) {
    this.connection = connection;
  }

  private int state = STATE_HEAD_REQUEST;

  void process() {
    connection.receive(rxBuffer);
    if (state == STATE_HEAD_REQUEST) {


    }
  }
}
