package net.symplifier.connection.serial;

import net.symplifier.core.application.Application;
import net.symplifier.core.application.ExitHandler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * SerialManager class used for providing onSend event when the data has been
 * transmitted successfully. jssc.SerialPort implementation provides
 * getOutputBufferBytesCount method, which is used for tracking the size
 * of remaining data in the output buffer of the serial port. Once this value
 * reaches 0, the onSend event is triggerd.
 *
 * <p>A wait interval based on the baudrate is calculated to make efficient
 * waiting decisions</p>
 *
 * Created by ranjan on 6/21/15.
 */
class SerialManager implements Runnable, ExitHandler {
  static final SerialManager SELF = new SerialManager();
  private volatile boolean exit = false;

  private final List<SerialConnection> outputBufferWaitingConnections = new LinkedList<>();

  private SerialManager() {
    new Thread(this).start();
  }

  void wait(SerialConnection connection) {
    synchronized (outputBufferWaitingConnections) {
      if (outputBufferWaitingConnections.contains(connection)) {
        return;
      }

      outputBufferWaitingConnections.add(connection);
      outputBufferWaitingConnections.notify();
    }
  }

  @Override
  public void run() {
    Application.app().addExitHandler(this);

    while(!exit) {
      synchronized (outputBufferWaitingConnections) {
        int waitInterval = Integer.MAX_VALUE;
        Iterator<SerialConnection> iterator = outputBufferWaitingConnections.iterator();
        while (iterator.hasNext()) {
          SerialConnection connection = iterator.next();

          int count = connection.getOutputBufferBytesCount();
          if (count == 0) {
            connection.propagateSendEvent();
            iterator.remove();
          } else if (count == -1) {
            // In case of an error, the error event will have been sent
            iterator.remove();
          } else {
            int baudRate = connection.getBaudRate();

            int duration = count * 10 * 1000 / baudRate;
            if (waitInterval > duration) {
              waitInterval = duration;
            }
          }
        }

        try {
          if (outputBufferWaitingConnections.size() == 0) {
            outputBufferWaitingConnections.wait();
          } else {
            outputBufferWaitingConnections.wait(waitInterval);
          }
        } catch(InterruptedException e) {
          // Looks like we need to quit the thread
          break;
        }
      }
    }
  }

  @Override
  public void onExit(Application app) {
    exit = true;
    outputBufferWaitingConnections.notify();
  }
}
