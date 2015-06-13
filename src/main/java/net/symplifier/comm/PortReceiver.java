package net.symplifier.comm;

import net.symplifier.core.util.HexDump;

import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/10/15.
 */
public class PortReceiver {

  final ByteBuffer buffer;
  private int localMark = -1;
  private final DigitalPort port;
  private Port.Parser parser;


  private int state = STATE_COMPLETED;
  //private static final int STATE_NONE     = 0;
  private static final int STATE_STARTED    = 1;
  private static final int STATE_COMPLETING = 2;
  private static final int STATE_COMPLETED  = 3;

  public PortReceiver(DigitalPort port, int bufferSize) {
    assert(bufferSize > 0):"Check the value of the buffer size set by the getReceiveBufferLength in the DigitalPort implementation";
    this.port = port;
    this.buffer = ByteBuffer.allocate(bufferSize);
  }

  public void setParser(Port.Parser parser) {
    this.parser = parser;
  }

  public Port.Parser getParser() {
    return parser;
  }

  public int markState(int state) {
    this.mark();
    return state;
  }

  public void mark() {
    this.localMark = this.buffer.position();
  }

  public void reset() {
    if (this.localMark != -1) {
      this.buffer.position(this.localMark);
    }
  }

  public void flush() {

  }

  public byte get() throws BufferUnderflowException {
    return this.buffer.get();
  }

  public short getShort() throws BufferUnderflowException {
    return this.buffer.getShort();
  }

  public float getFloat() throws BufferUnderflowException {
    return this.buffer.getFloat();
  }

  public double getDouble() throws BufferUnderflowException {
    return this.buffer.getDouble();
  }

  public int getInt() throws BufferUnderflowException {
    return this.buffer.getInt();
  }

  public long getLong() throws BufferUnderflowException {
    return this.buffer.getLong();
  }

  public byte[] getAll() {
    byte all[] = new byte[buffer.remaining()];
    buffer.get(all);
    return all;
  }

  private final static byte CR = 0x0D;
  private final static byte LF = 0x0A;

  private int parserLimit = -1;

  private byte[] excessData;
  /**
   * Sets a provision to limit the number of bytes allowed to be
   * read by the Parser, the Receiver won't allow the parser to read
   * bytes more than the given limit
   * @param limit
   */
  public void setLimit(int limit) {
    if (limit >= 0) {
      if (buffer.remaining() > limit) {
        excessData = new byte[buffer.remaining() - limit];
        System.arraycopy(buffer.array(), buffer.position() + limit, excessData, 0, excessData.length);
      } else {
        excessData = null;
      }
    } else {
      excessData = null;
    }

    parserLimit = limit;
  }

  public int getLimit() {
    return parserLimit;
  }

  public String getLine(Charset charset) throws BufferUnderflowException {

    this.buffer.mark();
    int startPos = this.buffer.position();
    int trimCR = 0;
    try {
      do {
        byte b = buffer.get();
        if (b == LF) {
          // Got the end of the line, return the string
          byte ar[] = this.buffer.array();
          return new String(ar, startPos, this.buffer.position() - startPos - trimCR - 1, charset);
        } else if (b == CR) {
          // Ignore the CR characters
          trimCR += 1;
        } else {
          // if the CR are inbetween lines, we consider that as a part of line
          trimCR = 0;
        }
      } while (true);
    } catch(BufferUnderflowException ex){
      this.buffer.reset();
      throw ex;
    }
  }

  public ByteBuffer getBuffer() {
    return this.buffer;
  }

  public void onReceive() {
    // Set the buffer into read mode
    buffer.flip();
    System.out.println("Buffer has " + buffer.remaining() + " bytes of data ready to process");
    // Debug dump
    HexDump.dump(buffer.array(), buffer.position(), buffer.remaining());

    // Just check for excess data
    setLimit(parserLimit);

    do {
      int startPos = buffer.position();

      if (state == STATE_COMPLETED) {
        try {
          // Try to start the response timeout timer
          port.startResponseTimeoutTimer();
          this.mark();
          port.onPrepareReception(this);
          state = STATE_STARTED;
        } catch(BufferUnderflowException ex) {
          this.reset();
          break;
        }
      }

      if (state == STATE_STARTED) {
        int used = buffer.position();
        try {
          this.mark();
          if (parser != null) {
            parser.onReceiverReady(port, this);
          }
          used = buffer.position() - used;
          if (parserLimit >= 0) {
            parserLimit -= used;
          }
          if (parserLimit <= 0) {
            if (excessData != null) {
              assert(parserLimit == 0);
              buffer.compact();
              buffer.put(excessData);
              buffer.flip();
            }
            state = STATE_COMPLETING;
          }
        } catch(BufferUnderflowException ex) {
          this.reset();
          used = buffer.position() - used;
          if (parserLimit >= 0) {
            parserLimit -= used;
          }
          break;
        }
      }

      if (state == STATE_COMPLETING) {
        try {
          mark();
          if (port.finalizeReception(this)) {
            state = STATE_COMPLETED;
            if (parser != null) {
              parser.onReceiveComplete(port);
            }

            if (!buffer.hasRemaining()) {
              break;
            }
          } else {
            state = STATE_STARTED;
          }
        } catch(BufferUnderflowException e) {
          reset();
          break;
        }
      }

      if (buffer.position() == startPos) {
        // Looks like the application is not doing anything about the data
        // we cannot loop forever in such cases
        // just break the loop
        break;
      }

    } while (buffer.remaining() > 0);

    buffer.compact();
  }

  public void purge() {
    // Check which mode is the buffer and empty it accordingly
    buffer.clear();
    buffer.limit(0);
    excessData = null;
    parserLimit = -1;
    state = STATE_COMPLETED;
  }
}
