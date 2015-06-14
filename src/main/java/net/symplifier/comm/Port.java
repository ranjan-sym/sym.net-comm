package net.symplifier.comm;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/9/15.
 */
public interface Port {

  interface Owner {

  }

  interface Factory<T extends Port> {
    T create(String name);
  }

  interface Poller {
    boolean onTransmitterReady(DigitalPort port, PortTransmitter transmitter);
  }

  interface Responder extends Poller {
    void onTransmissionComplete(DigitalPort port);
  }

  interface Parser {
    void onReceiverReady(DigitalPort port, PortReceiver receiver) throws BufferUnderflowException;
    void onReceiveComplete(DigitalPort port);

    void onResponseTimeout(DigitalPort port);
  }

  interface Attachment {

    void onPortOpen(DigitalPort port);
    void onPortError(DigitalPort port, Throwable error);
    void onPortClose(DigitalPort port);

  }


  void attach(Attachment attachment);

  Attachment getAttachment();

  Owner getOwner();

  String getName();

}
