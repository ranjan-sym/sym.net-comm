package net.symplifier.comm.http;

import net.symplifier.comm.Port;

import java.util.HashMap;

/**
 * Created by ranjan on 6/10/15.
 */
public class HTTPRequest {

  public interface Handler {
    Port.Responder onRequest(HTTPRequest request, HTTPResponse response);
  }

  String method;
  String path;
  String basePath;
  String relativePath;
  String version;

  int contentLength = -1;
  boolean isChunked = false;

  private HashMap<String, String> headers = new HashMap<>();

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
