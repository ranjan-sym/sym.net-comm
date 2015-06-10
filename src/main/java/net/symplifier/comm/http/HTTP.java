package net.symplifier.comm.http;

import net.symplifier.comm.tcp.TCP;
import net.symplifier.comm.tcp.TCPPort;

import java.nio.channels.SocketChannel;

/**
 * Created by ranjan on 6/10/15.
 */
public class HTTP extends TCP {
  public static final String METHOD_GET = "GET";
  public static final String METHOD_POST = "POST";
  public static final String METHOD_PUT = "PUT";
  public static final String METHOD_DELETE = "DELETE";



  public HTTP(Owner owner, String name) {
    super(owner, name);
  }

  public HTTP(Owner owner, String host, int port) {
    super(owner, host, port);
  }

  public HTTP(Owner owner, int port) {
    super(owner, port);
  }

  public TCPPort createPort(SocketChannel socketChannel) {
    return new HTTPServerPort(this, socketChannel);
  }
}
