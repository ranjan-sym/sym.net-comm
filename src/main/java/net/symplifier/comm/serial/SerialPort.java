package net.symplifier.comm.serial;

import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.PortTransmitter;
import net.symplifier.core.util.attribute.Attribute;
import net.symplifier.core.util.attribute.AttributeManager;
import net.symplifier.core.util.attribute.AttributeOptions;
import net.symplifier.core.util.attribute.HasAttribute;

import java.nio.ByteBuffer;

/**
 * Created by ranjan on 6/9/15.
 */
public class SerialPort extends DigitalPort implements SerialPortEventListener {

  private int baudRate = jssc.SerialPort.BAUDRATE_9600;
  private int dataBits = jssc.SerialPort.DATABITS_8;
  private int stopBits = jssc.SerialPort.STOPBITS_1;
  private int parity = jssc.SerialPort.PARITY_NONE;

  @Override
  public void serialEvent(SerialPortEvent serialPortEvent) {
    if (serialPortEvent.isRXCHAR()) {
      try {
        do {
          int available = port.getInputBufferBytesCount();
          int capable = getReceiverBuffer().remaining();

          if (available == 0 || capable == 0) {
            break;
          }

          byte[] data;
          if (available > capable) {
            // more data on the serial buffer than what we can read
            data = port.readBytes(capable);
          } else {
            data = port.readBytes(available);
          }
          getReceiverBuffer().put(data);
          receiver.onReceive();
        } while (true);
      } catch (SerialPortException ex) {
        getAttachment().onPortError(this, ex);
      }
    } else if (serialPortEvent.isTXEMPTY()) {
      // It looks like this event is not working
      transmitter.onTransmit();
    }
  }

  @Override
  protected void onPrepareReception(PortReceiver receiver) {

  }

  @Override
  protected boolean finalizeReception(PortReceiver receiver) {
    return true;
  }

  @Override
  protected boolean onPrepareTransmission(PortTransmitter transmitter) {
    return true;
  }

  @Override
  protected void finalizeTransmission(PortTransmitter transmitter) {

  }

  protected void flush() {
    ByteBuffer buffer = getTransmitterBuffer();
    byte[] d = new byte[buffer.remaining()];
    buffer.get(d);
    try {
      port.writeBytes(d);
      transmitter.onTransmit();
    } catch(SerialPortException ex) {
      getAttachment().onPortError(SerialPort.this, ex);
    }
  }

  @Override
  protected int getReceiverBufferLength() {
    return 512;
  }

  @Override
  protected int getTransmitterBufferLength() {
    return 512;
  }

  private final jssc.SerialPort port;


  public SerialPort(String name) {
    super(name);

    port = new jssc.SerialPort(name);
  }

  public SerialPort setParameters(int baudRate, int dataBits, int stopBits, int parity) {
    this.baudRate = baudRate;
    this.dataBits = dataBits;
    this.stopBits = stopBits;
    this.parity = parity;
    if (port.isOpened()) {
      try {
        port.setParams(baudRate, dataBits, stopBits, parity);
      } catch(SerialPortException ex) {
        System.err.println("This error should not be happening");
        ex.printStackTrace();;
      }
    }

    return this;
  }

  @Override
  public void open() {
    try {
      port.openPort();
      port.purgePort(jssc.SerialPort.PURGE_TXCLEAR);
      port.purgePort(jssc.SerialPort.PURGE_RXCLEAR);

      port.setParams(baudRate, dataBits, stopBits, parity);
      port.addEventListener(this, SerialPortEvent.RXCHAR);

      getAttachment().onPortOpen(this);
    } catch(SerialPortException ex) {
      getAttachment().onPortError(this, ex);
    }
  }

  public void close() {
    try {
      port.closePort();
    } catch (SerialPortException e) {
      getAttachment().onPortError(this, e);
    }

    getAttachment().onPortClose(this);
  }
}
