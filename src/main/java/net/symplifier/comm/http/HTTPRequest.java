package net.symplifier.comm.http;

import net.symplifier.comm.Port;

import java.net.URI;
import java.util.HashMap;

/**
 * Created by ranjan on 6/10/15.
 */
public class HTTPRequest {

  public interface Handler {
    Port.Responder onRequest(HTTPRequest request, HTTPResponse response);
  }

  String method;
  private String requestURI;
  private String path;

  String basePath;
  String relativePath;
  String version;

  int contentLength = -1;
  boolean isChunked = false;

  private HashMap<String, String> queries = new HashMap<>();
  private HashMap<String, String> headers = new HashMap<>();

  public boolean hasQuery(String name) {
    return queries.containsKey(name);
  }

  public String getQuery(String name, String defaultValue) {
    if (queries.containsKey(name)) {
      return queries.get(name);
    } else {
      return defaultValue;
    }
  }

  public int getQuery(String name, int defaultValue) {
    if (queries.containsKey(name)) {
      return Integer.parseInt(queries.get(name));
    } else {
      return defaultValue;
    }
  }

  private String decodeURI(String raw) {
    StringBuilder res = new StringBuilder();

    boolean expectingHex = false;
    String hexNum = "";
    for(int i=0; i<requestURI.length(); ++i) {
      char ch = requestURI.charAt(i);
      if (expectingHex) {
        if ((ch >= '0' && ch <='9') ||
                (ch>='A' && ch <='F') ||
                (ch>='a' && ch <='f')) {

          hexNum += ch;
          if (hexNum.length() == 2) {
            res.append((char)Integer.parseInt(hexNum, 16));
            expectingHex = false;
          }
        } else {
          res.append('%');
          res.append(hexNum);
          res.append(ch == '+' ? ' ' : ch);
          expectingHex = false;
        }
      } else if (ch == '+') {
        res.append(' ');
      } else if (ch == '%') {
        expectingHex = true;
        hexNum = "";
      } else {
        res.append(ch);
      }
    }

    return res.toString();
  }

  public void setRequestURI(String requestURI) {
    int p = requestURI.indexOf('?');
    queries.clear();
    if (p >= 0) {
      this.path = decodeURI(requestURI.substring(0, p));

      String queries[] = requestURI.substring(p+1).split("&");
      for(String q:queries) {
        String parts[] = q.split("=", 2);
        if (parts.length == 2) {
          String name = decodeURI(parts[0]);
          String value = decodeURI(parts[1]);
          this.queries.put(name, value);
        }
      }
    } else {
      this.path = decodeURI(requestURI);
    }
  }

  public String getPath() {
    return path;
  }


  void processHeader(String name, String value) {
    headers.put(name, value);
    if (name.equalsIgnoreCase("content-length")) {
      contentLength = Integer.parseInt(value);
    } else if (name.equalsIgnoreCase("transfer-encoding")) {
      isChunked = value.equalsIgnoreCase("chunked");
    }
  }

  boolean validateMethod(String txt) {
    if (txt.equalsIgnoreCase(HTTP.METHOD_GET)) {
      method = HTTP.METHOD_GET;
    } else if (txt.equalsIgnoreCase(HTTP.METHOD_POST)) {
      method = HTTP.METHOD_POST;
    } else if (txt.equalsIgnoreCase(HTTP.METHOD_PUT)) {
      method = HTTP.METHOD_PUT;
    } else if (txt.equalsIgnoreCase(HTTP.METHOD_DELETE)) {
      method = HTTP.METHOD_DELETE;
    } else {
      return false;
    }

    return true;
  }


  public HTTPRequest() {

  }

  public void clear() {
    method = null;
    path = null;
    basePath = null;
    relativePath = null;
    version = null;
    contentLength = -1;
    isChunked = false;
    headers.clear();
  }
}
