package net.symplifier.connection;

/**
 * Created by ranjan on 6/22/15.
 */
public interface Server<T extends Connection> {

  T createConnection();

  void setOpenHandler(OpenHandler handler);
  void setCloseHandler(CloseHandler closeHandler);
  void setReceiveHandler(ReceiveHandler receiveHandler);
  void setSendHandler(SendHandler sendHandler);
  void setErrorHandler(ErrorHandler errorHandler);

  boolean open();

  void close();

}
