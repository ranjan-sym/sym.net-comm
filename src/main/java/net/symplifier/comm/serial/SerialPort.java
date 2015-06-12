package net.symplifier.comm.serial;

import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import net.symplifier.comm.DigitalPort;
import net.symplifier.comm.PortReceiver;
import net.symplifier.comm.PortTransmitter;

import java.nio.ByteBuffer;

/**
 * Created by ranjan on 6/9/15.
 */
public class SerialPort extends DigitalPort implements SerialPortEventListener {

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


  public SerialPort(Owner owner, String name) {
    super(owner, name);

    port = new jssc.SerialPort(name);
  }

  public void open() {
    try {
      port.openPort();
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
