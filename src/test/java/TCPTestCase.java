import net.symplifier.comm.*;
import net.symplifier.comm.tcp.TCP;
import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/10/15.
 */
public class TCPTestCase implements Port.Owner, Port.Attachment {

  private String rx;

  private Port.Responder responder = new Port.Responder() {

    @Override
    public boolean onTransmitterReady(DigitalPort port, PortTransmitter transmitter) {
      transmitter.putLine(rx, Charset.defaultCharset());
      return true;
    }

    @Override
    public void onTransmissionComplete(DigitalPort port) {
      System.out.println("One session is complete here");
    }
  };

  private Port.Parser parser = new Port.Parser() {

    @Override
    public void onReceiverReady(DigitalPort port, PortReceiver receiver) throws BufferUnderflowException {
      String line = receiver.getLine(Charset.defaultCharset());
      rx = line.toUpperCase();
    }

    @Override
    public void onReceiveComplete(DigitalPort port) {
      port.startTransmission(responder);
    }

    @Override
    public void onResponseTimeout(DigitalPort port) {
      System.out.println("Response timed out on " + port.toString());
    }
  };

  @Test
  public void testServer() throws InterruptedException, InvalidPortNameException {
    TCP tcp = new TCP(this, 9009);
    tcp.attach(this);
    tcp.start();

    Thread.currentThread().join();
  }

  @Override
  public void onPortOpen(DigitalPort port) {
    System.out.println("Port opened - " + port.getName());
    port.setParser(parser);
  }

  @Override
  public void onPortError(DigitalPort port, Throwable error) {
    System.out.println("Error on port - " + port.getName());
    error.printStackTrace();
  }

  @Override
  public void onPortClose(DigitalPort port) {
    System.out.println("Port closed - " + port.getName());
  }
}
