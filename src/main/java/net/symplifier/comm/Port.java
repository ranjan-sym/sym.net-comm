package net.symplifier.comm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ranjan on 6/9/15.
 */
public interface Port {
  /* Do all the port based logging using this logger */
  Logger LOGGER = LogManager.getLogger("Port");

  /**
   * Attach the given attachment to this port. The {@link net.symplifier.comm.Port.Attachment}
   * should receive the port opening, port closing and any error on the port
   * event
   *
   * @param attachment The attachment that handles this port
   */
  void attach(Attachment attachment);

  /**
   * Get the attachment attached to this port. This value can be null be if
   * nothing has been attached to this port
   *
   * @return {@link net.symplifier.comm.Port.Attachment} or {@code null}
   */
  Attachment getAttachment();

  /**
   * Get the name of this port. The name should uniquely identify the port
   *
   * @return
   */
  String getName();

  /**
   * A Responder is responsible for sending data over the port.
   */
  interface Responder {
    boolean onTransmitterReady(DigitalPort port, PortTransmitter transmitter);
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


}
