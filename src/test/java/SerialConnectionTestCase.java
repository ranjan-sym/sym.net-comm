import net.symplifier.connection.*;
import net.symplifier.connection.serial.SerialConnection;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by ranjan on 6/21/15.
 */
public class SerialConnectionTestCase {


  /**
   * This test requires virtual serial port installed on the testing PC with
   * 4 pairs of ports
   *
   * <p>
   * Use the tty0tty-1.2 library available freely. tty0tty-1.2 module could be
   * loaded with insmod to create 4 pairs of ports. The tty0tty library needs
   * to be compiled on the same PC to create the binaries. The make files are
   * available with the library so compilation is very easy.
   * </p>
   * <p>
   * {@code $ sudo insmod tty0tty.ko}
   * </p>
   * <p>
   * This module will create four pairs of virtual ports
   * </p>
   * <ul>
   *   <li>{@code /dev/tnt0 <-> /dev/tnt1}</li>
   *   <li>{@code /dev/tnt2 <-> /dev/tnt3}</li>
   *   <li>{@code /dev/tnt4 <-> /dev/tnt5}</li>
   *   <li>{@code /dev/tnt6 <-> /dev/tnt7}</li>
   * </ul>
   *
   */
  @Test
  public void testLargeRandomTransfers() throws InterruptedException {
    ConnectionPair pair = new ConnectionPair(0, 250);

    Thread.currentThread().join();
  }

  private static class ConnectionPair implements OpenHandler, CloseHandler, ErrorHandler, ReceiveHandler, SendHandler {
    Connection A;
    Connection B;

    private Random random = new Random();
    ByteBuffer txBuffer = ByteBuffer.allocate(10240);
    ByteBuffer rxBuffer = ByteBuffer.allocate(10240);

    private int count = 0;

    public ConnectionPair(int idx, int connectionCount) {
      count = connectionCount;
      A = new SerialConnection("/dev/tnt" + (idx * 2));
      B = new SerialConnection("/dev/tnt" + (idx * 2 + 1));

      A.setOpenHandler(this);
      A.setCloseHandler(this);
      A.setErrorHandler(this);
      A.setReceiveHandler(this);
      A.setSendHandler(this);

      B.setOpenHandler(this);
      B.setCloseHandler(this);
      B.setErrorHandler(this);
      B.setReceiveHandler(this);
      B.setSendHandler(this);

      B.open();
      A.open();

    }

    public void send(Connection connection) {
      int size = random.nextInt(1000) + 500;
      byte data[] = new byte[size];
      random.nextBytes(data);
      txBuffer.clear();

      txBuffer.put(data);
      txBuffer.flip();
      connection.send(txBuffer);
    }

    @Override
    public void onOpen(Connection connection) {
      System.out.println("Port " + connection + " opened.");
      if (connection == A) {
        send(A);
      }
    }

    @Override
    public void onClose(Connection connection) {
      System.out.println("Port " + connection + " closed.");
    }

    @Override
    public void onError(Connection connection) {
      System.out.println("Port " + connection + " ERROR - " + connection.getLastError());
    }

    @Override
    public void onReceive(Connection connection) {
      txBuffer.position(0);

      rxBuffer.clear();
      connection.receive(rxBuffer);
      rxBuffer.flip();

      String res = "OK";
      if (rxBuffer.remaining() != txBuffer.remaining()) {
        res = "ERROR LENGTH tx=" + txBuffer.remaining() + ",rx=" + rxBuffer.remaining();
      } else {
        for(int i=0; i<rxBuffer.remaining(); ++i) {
          if (rxBuffer.get() != txBuffer.get()) {
            res = "ERROR data at " + i;
            break;
          }
        }
      }
      System.out.println(count + " Port " + connection + " RX - " + res);

      count -= 1;

      if (count > 0) {
        send(connection);
      }
    }

    @Override
    public void onSend(Connection connection) {
      System.out.println("Port " + connection + " TX - " + txBuffer.limit());
    }
  }

}
