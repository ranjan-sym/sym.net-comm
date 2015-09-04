package net.symplifier.comm;

/**
 * Created by ranjan on 6/9/15.
 */
public abstract class ServerPort<T extends DigitalPort> implements Port {

  private final String name;
  private Attachment attachment;

  public ServerPort(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract boolean start();

  public abstract void stop();


  @Override
  public void attach(Attachment attachment) {
    this.attachment = attachment;
  }

  public Attachment getAttachment() {
    return attachment;
  }

  /**
   * Helper methods for retrieving the Receiver for the underlying transport
   * layer port of this server
   *
   * @param port
   * @return
   */
  protected PortReceiver getReceiver(DigitalPort port) {
    return port.receiver;
  }

  protected PortTransmitter getTransmitter(DigitalPort port) {
    return port.transmitter;
  }
}
