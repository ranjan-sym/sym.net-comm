package net.symplifier.comm.tcp;

import net.symplifier.core.application.threading.ThreadPool;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by ranjan on 6/10/15.
 */
class TCPManager implements Runnable {
  private final static TCPManager manager;


  static {
    manager = new TCPManager();
  }

  private TCPManager() {
    doStart();
  }

  public static void stop() {
    manager.doStop();
  }

  public static void unhook(TCP tcp) {
    manager.unhook(tcp.serverSocket);
  }

  public static void unhook(TCPPort port) {
    manager.unhook(port.socket);
  }

  private void unhook(SelectableChannel channel) {
    SelectionKey key = channel.keyFor(selector);
    if (key != null) {
      selector.wakeup();
    }
  }

  public static void hook(TCP tcp) {
    manager.hook(tcp, tcp.serverSocket, SelectionKey.OP_ACCEPT);
  }

  public static void hook(TCPPort port, int interestOps) {
    manager.hook(port, port.socket, interestOps);
  }

  private void hook(Object owner, SelectableChannel channel, int interestOps) {
    SelectionKey key = channel.keyFor(selector);
    if (key == null) {
      try {
        // The selector needs to be woken up before registering a new key
        selector.wakeup();

        // Register the new key
        key = channel.register(selector, interestOps);

        // Attach the player into the key for extraction later
        key.attach(owner);
      } catch(ClosedChannelException ex) {
        // No need to do anything
      } catch(IllegalBlockingModeException ex) {
        System.err.println("Only non blocking type sockets are allowed");
        assert(true):"Blocking mode sockets are not supported by TCPManager";
      }
    } else {
      // There is already a key
      if (key.interestOps() != interestOps) {
        key.interestOps(interestOps);
        selector.wakeup();
      }
    }
  }

  private final ThreadPool<TCPManager, Object> pool = new ThreadPool<>(this);
  private volatile boolean exit = false;
  private volatile boolean running = false;
  private Selector selector;

  private synchronized boolean doStart() {

    if (running) {
      return true;
    }

    try {
      selector = Selector.open();
    } catch(IOException e) {
      // Unexpected error
      System.err.println("Unexpected error while starting TCP Manager");
      e.printStackTrace();
      return false;
    }

    // Start the manager thread
    new Thread(this).start();

    // and the worker thread pool
    pool.start(10);

    // everything seems ok
    return true;
  }

  private synchronized void doStop() {
    exit = true;
    if (running) {
      selector.wakeup();
    }
  }



  @Override
  public void run() {

    running = true;

    while (!exit) {

      int readyChannels = 0;
      try {
        System.out.println("Waiting for TCP event - " + selector.keys().size() + " keys");
        readyChannels = selector.select();
      } catch (IOException e) {
        // Can't have this exception, must be pretty bad
        System.err.println("Unexpected error in TCPManager");
        e.printStackTrace();
      }

      System.out.println("Something is available - number of readyChannels = " + readyChannels);

      if (readyChannels == 0) {
        // might be an exit event or the new selection event has been added
        continue;
      }

      Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
      while(keyIterator.hasNext()) {
        SelectionKey key = keyIterator.next();
        keyIterator.remove();

        if (key.isAcceptable()) {
          // Looks like we got a new connection request on a server
          TCP tcp = (TCP) key.attachment();
          try {
            pool.queue(tcp, tcp.serverSocket.accept());
          } catch (IOException e) {
            System.err.println("Error while trying to open a new connection for a server");
            e.printStackTrace();
          }
        } else if (key.isConnectable() || key.isReadable() || key.isWritable()) {
          key.interestOps(0);     // Disable any event on this port for a moment
          TCPPort port = (TCPPort)key.attachment();
          pool.queue(port, key.readyOps());
        }
      }


    }

    // Cleanup
    try {
      selector.close();
    } catch(IOException e) {
      System.err.println("Unexpected error while stopping TCP Manager");
      e.printStackTrace();
    }

    selector = null;
    running = false;

  }
}
