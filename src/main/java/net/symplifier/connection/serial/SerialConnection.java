package net.symplifier.connection.serial;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import net.symplifier.connection.*;

import java.nio.ByteBuffer;

/**
 * Created by ranjan on 6/21/15.
 */
public class SerialConnection extends AbstractConnection implements SerialPortEventListener {

  SerialPort port;

  private int baudRate = SerialPort.BAUDRATE_9600;
  private int dataBits = SerialPort.DATABITS_8;
  private int stopBits = SerialPort.STOPBITS_1;
  private int parity = SerialPort.PARITY_NONE;

  public SerialConnection(String com) {
    this.port = new SerialPort(com);
  }

  void propagateSendEvent() {
    raiseSendEvent();
  }

  int getOutputBufferBytesCount() {
    try {
      return port.getOutputBufferBytesCount();
    } catch (SerialPortException e) {
      raiseErrorEvent(e);
      return -1;
    }
  }

  public int getBaudRate() {
    return baudRate;
  }

  public int getDataBits() {
    return dataBits;
  }

  public int getStopBits() {
    return stopBits;
  }

  public int getParity() {
    return parity;
  }

  public void setParameters(int baudRate, int dataBits, int parity, int stopBits) {
    this.baudRate = baudRate;
    this.dataBits = dataBits;
    this.parity = parity;
    this.stopBits = stopBits;
    if (port.isOpened()) {
      try {
        port.setParams(baudRate, dataBits, parity, stopBits);
      } catch (SerialPortException e) {
        raiseErrorEvent(e);
      }
    }
  }

  @Override
  public void open() {
    // No need to open an already opened port
    if (port.isOpened()) {
      return;
    }

    try {
      if (port.openPort()) {
        port.setParams(baudRate, dataBits, stopBits, parity);
        raiseOpenEvent();

        port.setEventsMask(SerialPort.MASK_RXCHAR);
        port.addEventListener(this);
      } else {
        raiseErrorEvent(null);
      }
    } catch (SerialPortException e) {
      raiseErrorEvent(e);
    }

  }

  @Override
  public void close() {
    if(port.isOpened()) {
      raiseCloseEvent();
      try {
        port.closePort();
      } catch (SerialPortException e) {
        // Ignore any error that occurs during the closing of the port
        e.printStackTrace();
      }
    }
  }

  @Override
  public void send(ByteBuffer buffer) {
    if (buffer.remaining() == 0) {
      return;
    }

    byte b[] = new byte[buffer.remaining()];
    buffer.put(b);
    try {
      port.writeBytes(b);
      if (port.getOutputBufferBytesCount() == 0) {
        raiseSendEvent();
      } else {
        // Trigger the send event waiting thread
        SerialManager.SELF.wait(this);
      }
    } catch(SerialPortException e) {
      raiseErrorEvent(e);
    }
  }

  @Override
  public void receive(ByteBuffer buffer) {
    try {
      int available = port.getInputBufferBytesCount();
      int capacity = buffer.remaining();
      int n = available > capacity ? capacity : available;
      byte[] data = port.readBytes(n);
      //System.out.println("Reading " + n + " bytes of data");
      buffer.put(data);

      // If there is still data available in the buffer, raise the receive event
      if ( n < available) {
        raiseReceiveEvent();
      }
    } catch(SerialPortException e) {
      raiseErrorEvent(e);
    }
  }

  @Override
  public void serialEvent(SerialPortEvent serialPortEvent) {
    raiseReceiveEvent();
  }

  public String toString() {
    return port.getPortName();
  }
}
