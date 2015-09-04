package net.symplifier.connection;

import net.symplifier.core.application.threading.ThreadTarget;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by ranjan on 6/21/15.
 */
public abstract class AbstractConnection implements Connection, ThreadTarget<Director, Connection> {
  private static final int EVENT_OPEN = 1;
  private static final int EVENT_CLOSE = 2;
  private static final int EVENT_ERROR = 3;
  private static final int EVENT_RECEIVE = 4;
  private static final int EVENT_SEND = 5;

  private static class Event {
    private Queue<Integer> events = new LinkedList<>();

    public Event() {

    }

    public boolean set(int event) {
      events.offer(event);
      return events.size() == 1;
    }

    public int peek() {
      return events.peek();
    }

    public int poll() {
      this.notify();
      return events.poll();
    }

    public boolean available() {
      return events.size() > 0;
    }

  }

  private OpenHandler openHandler;
  private CloseHandler closeHandler;
  private ReceiveHandler receiveHandler;
  private SendHandler sendHandler;
  private ErrorHandler errorHandler;

  private final Event event = new Event();
  private Throwable lastError;

  public Throwable getLastError() {
    return lastError;
  }

  protected void raiseOpenEvent() {
    if (openHandler != null) {
      raiseEvent(EVENT_OPEN);
    }
  }

  protected void raiseCloseEvent() {
    if (closeHandler != null) {
      raiseEvent(EVENT_CLOSE);
    }
  }

  protected void raiseReceiveEvent() {
    if(this.receiveHandler != null) {
      raiseEvent(EVENT_RECEIVE);
    }
  }

  protected void raiseSendEvent() {
    if (this.sendHandler != null) {
      raiseEvent(EVENT_SEND);
    }
  }

  protected void raiseErrorEvent(Throwable error) {
    this.lastError = error;

    if (this.errorHandler != null) {
      raiseEvent(EVENT_ERROR);
    }
  }

  private void raiseEvent(int event) {
    synchronized (this.event) {
      if (this.event.set(event)) {
        Director.raiseEvent(this);

        try {
          this.event.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private volatile int counter = 0;

  @Override
  public void onRun(Director source, Connection attachment) {
    synchronized (event) {

      while(event.available()) {

        // Start processing the event
        switch (event.peek()) {
          case EVENT_RECEIVE:
            receiveHandler.onReceive(attachment);
            break;
          case EVENT_SEND:
            sendHandler.onSend(attachment);
            break;
          case EVENT_ERROR:
            errorHandler.onError(attachment);
            break;
          case EVENT_OPEN:
            openHandler.onOpen(attachment);
            break;
          case EVENT_CLOSE:
            closeHandler.onClose(attachment);
            break;
        }

        // Consume the event
        event.poll();
      }
    }
  }

  @Override
  public void setOpenHandler(OpenHandler handler) {
    assert(handler != null);
    this.openHandler = handler;
  }

  @Override
  public void setCloseHandler(CloseHandler handler) {
    assert(handler != null);
    this.closeHandler = handler;
  }

  @Override
  public void setReceiveHandler(ReceiveHandler handler) {
    assert(handler != null);
    this.receiveHandler = handler;
  }

  @Override
  public void setSendHandler(SendHandler handler) {
    assert(handler != null);
    this.sendHandler = handler;
  }

  public void setErrorHandler(ErrorHandler handler) {
    assert(handler != null);
    this.errorHandler = handler;
  }


}
