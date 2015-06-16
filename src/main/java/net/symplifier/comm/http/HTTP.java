package net.symplifier.comm.http;

import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.Port;
import net.symplifier.comm.tcp.TCP;
import net.symplifier.comm.tcp.TCPPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.CORBA.Request;

import java.nio.channels.SocketChannel;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ranjan on 6/10/15.
 */
public class HTTP extends TCP {
  public static final Logger LOGGER = LogManager.getLogger("HTTP");

  public static final String METHOD_GET = "GET";
  public static final String METHOD_POST = "POST";
  public static final String METHOD_PUT = "PUT";
  public static final String METHOD_DELETE = "DELETE";

  private static class RequestHandlerMap {
    private HashMap<String, RequestHandlerMap> children = new HashMap<>();
    private HTTPRequest.Handler requestHandler;

    public RequestHandlerMap get(String name) {
      return children.get(name);
    }

    public RequestHandlerMap confirmGet(String name) {
      if (children.containsKey(name)) {
        return children.get(name);
      } else {
        RequestHandlerMap ch = new RequestHandlerMap();
        children.put(name, ch);
        return ch;
      }
    }

  }

  private RequestHandlerMap requestHandlers = new RequestHandlerMap();

  public void addRequestHandler(String path, HTTPRequest.Handler requestHandler) {
    RequestHandlerMap r = requestHandlers;
    String parts[] = path.split("/");
    for(String p:parts) {
      p = p.trim();
      if (p.isEmpty()) {
        continue;
      }
      r = r.confirmGet(p);
    }

    r.requestHandler = requestHandler;
  }

  public Port.Responder getResponder(HTTPServerPort port, HTTPRequest request, HTTPResponse response) {
    String parts[] = request.getPath().split("/");

    RequestHandlerMap r = requestHandlers;
    HTTPRequest.Handler requestHandler = r.requestHandler;
    request.basePath = "";
    String currentPath = "";
    request.relativePath = "";
    for(String p:parts) {
      p = p.trim();
      if (p.isEmpty()) {
        continue;
      }

      request.relativePath += "/" + p;
      if (r == null) {
        continue;
      }

      r = r.get(p);

      if (r != null) {
        currentPath += "/" + p;
        if (r.requestHandler != null) {
          request.basePath = currentPath;
          requestHandler = r.requestHandler;
          request.relativePath = "";
        }
      }
    }

    if(requestHandler != null) {
      return requestHandler.onRequest(request, response);
    } else {
      return null;
    }
  }


  public HTTP(Owner owner, String name) throws InvalidPortNameException {
    super(owner, name);
  }

  public HTTP(Owner owner, String host, int port) throws InvalidPortNameException {
    super(owner, host, port);
  }

  public HTTP(Owner owner, int port) throws InvalidPortNameException {
    super(owner, port);
  }

  public TCPPort createPort(SocketChannel socketChannel) {
    return new HTTPServerPort(this, socketChannel);
  }


}
