package net.symplifier.connection;

import java.nio.ByteBuffer;

/**
 * Created by ranjan on 6/21/15.
 */
public interface Connection {

  void setOpenHandler(OpenHandler handler);

  void setCloseHandler(CloseHandler handler);

  void setReceiveHandler(ReceiveHandler handler);

  void setSendHandler(SendHandler handler);

  void setErrorHandler(ErrorHandler handler);

  Throwable getLastError();

  void open();

  void close();

  void send(ByteBuffer buffer);

  void receive(ByteBuffer buffer);

}
