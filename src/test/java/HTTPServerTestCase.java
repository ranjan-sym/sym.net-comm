import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.Port;
import net.symplifier.comm.http.HTTP;
import net.symplifier.comm.http.StaticFileRequestHandler;
import org.junit.Test;

import java.io.File;

/**
 * Created by ranjan on 6/11/15.
 */
public class HTTPServerTestCase implements Port.Attachment {

  @Test
  public void testHTTPServer() throws InterruptedException, InvalidPortNameException {
    HTTP server = new HTTP(null, 8008);
    server.attach(this);
    server.start();

    server.addRequestHandler("/", new StaticFileRequestHandler(new File("www")));

    Thread.currentThread().join();
  }

  @Override
  public void onPortOpen(DigitalPort port) {
    System.out.println("NEW HTTP Request - " + port.getName());
  }

  @Override
  public void onPortError(DigitalPort port, Throwable error) {

  }

  @Override
  public void onPortClose(DigitalPort port) {
    System.out.println("HTTP connection closed - " + port.getName());
  }
}
