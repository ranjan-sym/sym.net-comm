package net.symplifier.comm;

/**
 * Created by ranjan on 6/10/15.
 */
public class InvalidPortNameException extends Exception {
  private Port port;
  private String name;
  public InvalidPortNameException(Port port, String name) {
    super("The port class " + port.getClass().getCanonicalName() + " cannot accept " + name + " as its identity");
    this.port = port;
    this.name = name;
  }

  public Port getPort() {
    return port;
  }

  public String getName() {
    return name;
  }
}
