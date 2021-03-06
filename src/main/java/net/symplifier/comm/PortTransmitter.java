package net.symplifier.comm;

import net.symplifier.core.application.scheduler.Schedule;
import net.symplifier.core.application.scheduler.ScheduledTask;
import net.symplifier.core.application.scheduler.Scheduler;
import net.symplifier.core.util.HexDump;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/10/15.
 */
public class PortTransmitter implements ScheduledTask {

  final ByteBuffer buffer;
  private int localMark;
  private final DigitalPort port;
  private Port.Responder responder;

  private int state = STATE_COMPLETED;
  public static final int STATE_START = 0;
  public static final int STATE_STARTED = 1;
  public static final int STATE_COMPLETING = 2;
  public static final int STATE_COMPLETED = 3;


  public PortTransmitter(DigitalPort port, int bufferSize) {
    assert(bufferSize > 0);
    this.port = port;
    this.buffer = ByteBuffer.allocate(bufferSize);

    this.buffer.flip();
  }

  public void setResponder(Port.Responder responder) {
    this.responder = responder;
  }

  public Port.Responder getResponder() {
    return responder;
  }

  public ByteBuffer getBuffer() {
    return buffer;
  }

  public void mark() {
    this.localMark = this.buffer.position();
  }

  public void reset() {
    buffer.position(this.localMark);
  }

  public void put(byte value) throws BufferOverflowException {
    buffer.put(value);
  }

  public void put(byte[] data) throws BufferOverflowException {
    buffer.put(data);
  }

  public void putShort(short value) throws BufferOverflowException {
    buffer.putShort(value);
  }

  public void putInt(int value) throws BufferOverflowException {
    buffer.putInt(value);
  }

  public void putFloat(float value) throws BufferOverflowException {
    buffer.putFloat(value);
  }

  public void putDouble(double value) throws BufferOverflowException {
    buffer.putDouble(value);
  }

  public void putLong(long value) throws BufferOverflowException {
    buffer.putDouble(value);
  }

  public void putLine(String line, Charset charset) throws BufferOverflowException {
    buffer.mark();
    try {
      buffer.put(line.getBytes(charset));
      buffer.put((byte)'\r');
      buffer.put((byte)'\n');
    } catch(BufferOverflowException ex) {
      buffer.reset();
    }
  }

  public void putBuffer(ByteBuffer buffer) {
    if(buffer.remaining() > this.buffer.remaining()) {
      this.buffer.put(buffer.array(), buffer.position(), this.buffer.remaining());
      buffer.position(buffer.position() + this.buffer.remaining());
    } else {
      this.buffer.put(buffer);
    }
  }

  public void start() {
    this.state = STATE_START;
    onTransmit();
  }

  public void onTransmit() {

    if (state == STATE_COMPLETING && !buffer.hasRemaining()) {
      state = STATE_COMPLETED;
      if (responder != null) {
        responder.onTransmissionComplete(port);
      }

      // if the port is on the poll mode, start the response timeout timer
      if (port.isPolling()) {
        port.startResponseTimeoutTimer();
      }
    }

    buffer.compact();

    if (state == STATE_START) {
      if(port.onPrepareTransmission(this)) {
        state = STATE_STARTED;
      }
    }

    if (state == STATE_STARTED) {
      if (responder == null || responder.onTransmitterReady(port, this)) {
        port.finalizeTransmission(this);
        state = STATE_COMPLETING;
      }
    }

    buffer.flip();

    if (buffer.hasRemaining()) {
      // Do a hex dump of transmission bytes
      if (buffer.remaining() < 512) {
        HexDump.dump(buffer.array(), buffer.position(), buffer.remaining());
      }
      System.out.println("Try to Transmit - " + buffer.remaining() + " bytes of data");
      port.flush();
      System.out.println("Transmit remain - " + buffer.remaining() + " bytes of data");
    }

    // Do a hex dump of transmission bytes
    //HexDump.dump(buffer.array(), buffer.position(), buffer.remaining());


  }

  @Override
  public void onRun(Scheduler scheduler, Schedule schedule) {
    // Got a poll event
    start();
  }
}
