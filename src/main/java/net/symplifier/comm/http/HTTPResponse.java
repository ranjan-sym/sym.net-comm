package net.symplifier.comm.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by ranjan on 6/11/15.
 */
public class HTTPResponse {
  String version;
  int responseStatusCode = 200;
  String responseStatusText = "OK";

  private long contentLength = 0;
  ByteBuffer data = null;

  HashMap<String, String> headers = new LinkedHashMap<>();

  public HTTPResponse() {

  }

  public void clear() {
    this.version = "1.1";
    this.responseStatusCode = 200;
    this.responseStatusText = "OK";
    this.contentLength = 0;
    this.data = null;
    headers.clear();
  }





  public void setContentLength(long length) {
    assert(length >= 0);
    this.contentLength = length;

    setHeader("Content-Length", Long.toString(length));
  }

  public void setHeader(String name, String value) {
    headers.put(name, value);
  }

  public void setResponse(int code, String text) {
    this.responseStatusCode = code;
    this.responseStatusText = text;
  }



  public void setResponseText(String text) {
    if (text != null) {
      setResponseData(text.getBytes(Charset.defaultCharset()));
    } else {
      setResponseData(null);
    }
  }

  public void setResponseData(byte[] data) {
    if (data != null) {
      this.data = ByteBuffer.wrap(data);
      setContentLength(data.length);
    } else {
      this.data = null;
      setContentLength(0);
    }
  }
}
