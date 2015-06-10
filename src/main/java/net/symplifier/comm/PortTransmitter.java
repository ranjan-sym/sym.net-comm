package net.symplifier.comm;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/10/15.
 */
public class PortTransmitter {

  final ByteBuffer buffer;
  private final DigitalPort port;
  private Port.Responder responder;

  private int state = STATE_COMPLETED;
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

  public void mark() {
    buffer.mark();
  }

  public void reset() {
    buffer.reset();
  }

  public void put(byte value) throws BufferOverflowException {
    buffer.put(value);
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
    this.buffer.put(buffer);
  }

  public void onTransmit() {
    buffer.compact();

    if (state == STATE_COMPLETED) {
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

    if (state == STATE_COMPLETING && buffer.position()==0) {
      state = STATE_COMPLETED;
      if (responder != null) {
        responder.onTransmissionComplete(port);
      }
    }

    buffer.flip();

    if (buffer.hasRemaining()) {
      port.flush();
    }

  }
}
