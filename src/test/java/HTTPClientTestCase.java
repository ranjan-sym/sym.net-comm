import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.InvalidPortNameException;
import net.symplifier.comm.Port;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.http.HTTPClientPort;
import org.junit.Test;

import java.nio.BufferUnderflowException;

/**
 * Created by ranjan on 6/10/15.
 */
public class HTTPClientTestCase implements Port.Attachment {

  private long startTime;
  public Port.Parser parser = new Port.Parser() {

    private int total = 0;

    @Override
    public void onReceiverReady(DigitalPort port, PortReceiver receiver) throws BufferUnderflowException {
      System.out.println("Received content from server");
      byte[] d = receiver.getAll();
      System.out.println(d.length + " byte of data");
      total += d.length;
      System.out.println("Total is now " + total);

    }

    @Override
    public void onReceiveComplete(DigitalPort port) {
      System.out.println("Content reception is complete with " + total + " bytes of data");
      total = 0;
      System.out.println("Total time take = " + (System.currentTimeMillis() - startTime) + " ms");
    }

    @Override
    public void onResponseTimeout(DigitalPort port) {
      System.out.println("Response timed out on " + port);
    }
  };

  @Test
  public void checkHTTPRequest() throws InvalidPortNameException, InterruptedException {
    HTTPClientPort port = new HTTPClientPort(null, "www.mfd.gov.np", 80, "/mfd.gov.np/_files/0f8207ee86bf76097055c962df157415.jpg");
    port.attach(this);
    port.open();

    Thread.currentThread().join();
  }

  @Override
  public void onPortOpen(DigitalPort port) {
    port.setParser(parser);
    port.startTransmission(null);
    startTime = System.currentTimeMillis();
  }

  @Override
  public void onPortError(DigitalPort port, Throwable error) {

  }

  @Override
  public void onPortClose(DigitalPort port) {

  }
}
