package net.symplifier.connection;

import net.symplifier.core.application.Application;
import net.symplifier.core.application.ExitHandler;
import net.symplifier.core.application.threading.ThreadPool;

/**
 * Created by ranjan on 6/21/15.
 */
class Director implements ExitHandler {

  private final static Director SELF = new Director();

  private final ThreadPool<Director, Connection> pool;

  private Director() {
    pool = new ThreadPool<>(this);
    pool.start(10);

    if (Application.app() != null) {
      Application.app().addExitHandler(this);
    }
  }

  public static void raiseEvent(AbstractConnection connection) {
    SELF.pool.queue(connection, connection);
  }

  @Override
  public void onExit(Application app) {
    pool.stop();
  }
}
