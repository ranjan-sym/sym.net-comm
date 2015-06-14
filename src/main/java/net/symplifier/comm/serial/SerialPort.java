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
public class SerialPort extends DigitalPort implements SerialPortEventListener, HasAttribute {

  private AttributeManager attributeManager = new AttributeManager();

  public final static AttributeOptions<Integer> BAUD_RATE_OPTIONS = new AttributeOptions<Integer>() {{
    add(jssc.SerialPort.BAUDRATE_1200, "1200");
    add(jssc.SerialPort.BAUDRATE_4800, "4800");
    add(jssc.SerialPort.BAUDRATE_9600, "9600");
    add(jssc.SerialPort.BAUDRATE_19200, "19200");
    add(jssc.SerialPort.BAUDRATE_38400, "38400");
    add(jssc.SerialPort.BAUDRATE_57600, "57600");
    add(jssc.SerialPort.BAUDRATE_115200, "115200");
    add(jssc.SerialPort.BAUDRATE_256000, "256000");
  }};

  public final static AttributeOptions<Integer> DATA_BIT_OPTIONS = new AttributeOptions<Integer>(){{
    add(jssc.SerialPort.DATABITS_8, "8");
    add(jssc.SerialPort.DATABITS_7, "7");
    add(jssc.SerialPort.DATABITS_6, "6");
    add(jssc.SerialPort.DATABITS_5, "5");
  }};

  public final static AttributeOptions<Integer> STOP_BITS_OPTIONS = new AttributeOptions<Integer>(){{
    add(jssc.SerialPort.STOPBITS_1, "1");
    add(jssc.SerialPort.STOPBITS_1_5, "1.5");
    add(jssc.SerialPort.STOPBITS_2, "2");
  }};

  public final static AttributeOptions<Integer> PARITY_OPTIONS = new AttributeOptions<Integer>() {{
    add(jssc.SerialPort.PARITY_NONE, "None");
    add(jssc.SerialPort.PARITY_EVEN, "Even");
    add(jssc.SerialPort.PARITY_ODD, "Odd");
    add(jssc.SerialPort.PARITY_MARK, "Mark");
    add(jssc.SerialPort.PARITY_SPACE, "Space");
  }};

  private Attribute.Integer baudRate = new Attribute.Integer(this, "general", "Baud Rate", jssc.SerialPort.BAUDRATE_9600, BAUD_RATE_OPTIONS);
  private Attribute.Integer dataBits = new Attribute.Integer(this, "general", "Data Bits", jssc.SerialPort.DATABITS_8, DATA_BIT_OPTIONS);
  private Attribute.Integer stopBits = new Attribute.Integer(this, "general", "Stop Bits", jssc.SerialPort.STOPBITS_1, STOP_BITS_OPTIONS);
  private Attribute.Integer parity = new Attribute.Integer(this, "general", "Parity", jssc.SerialPort.PARITY_NONE, PARITY_OPTIONS);

  @Override
  public AttributeManager getAttributeManager() {
    return attributeManager;
  }

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
      port.setParams(baudRate.get(), dataBits.get(), stopBits.get(), parity.get());
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
