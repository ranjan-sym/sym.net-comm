package net.symplifier.comm;

import net.symplifier.core.application.scheduler.Schedule;
import net.symplifier.core.application.scheduler.ScheduledTask;
import net.symplifier.core.application.scheduler.Scheduler;
import net.symplifier.core.application.scheduler.Timer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * A Transport Layer responsible for duplex data communication
 *
 * Created by ranjan on 6/9/15.
 */
public abstract class DigitalPort implements Port {

  private int responseTimeoutInterval = -1;
  private boolean responseTimeoutRunning = false;
  private Timer pollTimer;
  private Timer responseTimeoutTimer;
  private boolean isPolling = false;

  protected abstract void onPrepareReception(PortReceiver receiver);

  /**
   * This method returns true to mark the end of a session. Throws an exception
   * if the finalization has not been completed or returns false if the read
   * cycle has to be thrown back to the Parser
   *
   * @param receiver
   * @return
   * @throws BufferUnderflowException
   */
  protected abstract boolean finalizeReception(PortReceiver receiver) throws BufferUnderflowException;

  protected abstract boolean onPrepareTransmission(PortTransmitter transmitter);
  protected abstract void finalizeTransmission(PortTransmitter transmitter);
  protected abstract void flush();

  protected abstract int getReceiverBufferLength();
  protected abstract int getTransmitterBufferLength();

  private final String      name;
  private final Port.Owner  owner;

  protected final PortReceiver    receiver = new PortReceiver(this, getReceiverBufferLength());
  protected final PortTransmitter transmitter = new PortTransmitter(this, getTransmitterBufferLength());

  private Attachment attachment;

  public String getName() {
    return name;
  }

  protected void setAttachment(Attachment attachment) {
    assert(this.attachment == null);
    this.attachment = attachment;
  }


  void cancelResponseTimeoutTimer() {
    if (responseTimeoutTimer != null) {
      responseTimeoutTimer.cancel();
    }
  }

  void startResponseTimeoutTimer() {
    if (responseTimeoutInterval > 0) {
      if (responseTimeoutRunning) {
        return;
      } else {
        responseTimeoutRunning = true;
        if (responseTimeoutTimer != null) {
          responseTimeoutTimer.start(responseTimeoutInterval);
        }
      }
    }
  }

  public void setResponseTimeoutInterval(int responseTimeoutInterval) {
    this.responseTimeoutInterval = responseTimeoutInterval;
    responseTimeoutTimer = new Timer(receiver);
  }

  public boolean isPolling() {
    return isPolling;
  }

  public DigitalPort(Port.Owner owner, String name) {
    this.owner = owner;
    this.name = name;
  }

  public Port.Owner getOwner() {
    return owner;
  }

  public Attachment getAttachment() {
    return attachment;
  }

  @Override
  public void attach(Attachment attachment) {
    this.attachment = attachment;
  }

  public void defineParser(Parser parser) {
    receiver.setParser(parser);
  }

  public Parser setParser(Parser parser) {
    Parser old = receiver.getParser();
    receiver.setParser(parser);
    return old;
  }

  public Parser getParser() {
    return receiver.getParser();
  }

  public Responder setResponder(Responder responder) {
    Responder old = transmitter.getResponder();
    transmitter.setResponder(responder);
    return old;
  }

  public Responder getResponder() {
    return transmitter.getResponder();
  }

  public void startTransmission(Responder responder) {
    transmitter.setResponder(responder);
    transmitter.onTransmit();
  }

  public void startPoll(Responder responder, int pollInterval, int responseTimeout) {
    transmitter.setResponder(responder);
    if (pollTimer == null) {
      pollTimer = new Timer(transmitter);
    }

    isPolling = true;
    setResponseTimeoutInterval(responseTimeout);
    pollTimer.start(100, pollInterval);
  }

  public void cancelPoll() {
    isPolling = false;
    pollTimer.cancel();
  }


  protected ByteBuffer getTransmitterBuffer() {
    return transmitter.buffer;
  }

  protected ByteBuffer getReceiverBuffer() {
    return receiver.buffer;
  }

  public abstract void open();

  public abstract void close();

  @Override
  public String toString() {
    return name;
  }
}
