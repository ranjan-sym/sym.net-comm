import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.Port;
import net.symplifier.comm.PortTransmitter;
import net.symplifier.comm.serial.SerialPort;
import org.junit.Test;

import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/12/15.
 */
public class SerialPortTestApp implements Port.Owner, Port.Attachment {

  @Test
  public void testSerialPort() throws InterruptedException {
    SerialPort port = new SerialPort(this, "/dev/ttyUSB0");
    port.attach(this);
    port.open();

    Thread.currentThread().join();
  }

  @Override
  public void onPortOpen(DigitalPort port) {
    port.startTransmission(new Port.Responder() {
      @Override
      public boolean onTransmitterReady(DigitalPort port, PortTransmitter transmitter) {
        transmitter.putLine("AT", Charset.defaultCharset());
        return true;
      }

      @Override
      public void onTransmissionComplete(DigitalPort port) {

      }
    });
  }

  @Override
  public void onPortError(DigitalPort port, Throwable error) {

  }

  @Override
  public void onPortClose(DigitalPort port) {

  }
}
