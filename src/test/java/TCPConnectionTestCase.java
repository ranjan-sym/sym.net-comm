import net.symplifier.connection.*;
import net.symplifier.connection.tcp.TCPConnection;
import net.symplifier.connection.tcp.TCPServer;
import net.symplifier.core.util.HexDump;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/22/15.
 */
public class TCPConnectionTestCase implements OpenHandler, CloseHandler, ReceiveHandler, SendHandler, ErrorHandler {

  private TCPConnection client;
  @Test
  public void testTCPClient() throws InterruptedException {
    TCPServer server = new TCPServer("localhost", 5050);
    server.open();
    server.setOpenHandler(this);
    server.setCloseHandler(this);
    server.setReceiveHandler(this);
    server.setSendHandler(this);
    server.setErrorHandler(this);

    client = new TCPConnection("localhost", 5050);
    client.setOpenHandler(this);
    client.setCloseHandler(this);
    client.setReceiveHandler(this);
    client.setSendHandler(this);
    client.setErrorHandler(this);

    client.open();

    Thread.currentThread().join();
  }

  ByteBuffer rxBuffer = ByteBuffer.allocate(10240);
  ByteBuffer txBuffer = ByteBuffer.allocate(10240);

  @Override
  public void onOpen(Connection connection) {
    System.out.println("Port opened");

    if (connection != client) {
      String res = "GET / HTTP/1.1\r\nHost: 192.168.0.1\r\n\r\n";

      txBuffer.clear();
      txBuffer.put(res.getBytes(Charset.defaultCharset()));

      txBuffer.flip();
      connection.send(txBuffer);
    }
  }

  @Override
  public void onClose(Connection connection) {
//    try {
//      Thread.sleep(5000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
    System.out.println(Thread.currentThread().getId() + " - Port closed");
  }

  private int count1 = 0;
  private int count2 = 0;

  @Override
  public void onReceive(Connection connection) {
    System.out.println(Thread.currentThread().getId() + " - " + (++count1) + " RX EVENT");
    connection.receive(rxBuffer);
    rxBuffer.flip();
    System.out.println(Thread.currentThread().getId() + " - " + (++count2) + " Received " + rxBuffer.remaining() + " bytes of data");
    //System.out.println(rxBuffer.)
    //HexDump.dump(rxBuffer.array(), rxBuffer.position(), rxBuffer.remaining());
    //rxBuffer.position(rxBuffer.limit());
    rxBuffer.clear();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onSend(Connection connection) {
    System.out.println(Thread.currentThread().getId() + " - Data sent");
  }

  @Override
  public void onError(Connection connection) {
    System.out.println(Thread.currentThread().getId() + " - Error on connection - " + connection);
    connection.getLastError().printStackTrace();
  }
}
