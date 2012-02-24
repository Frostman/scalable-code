package ru.frostman.scalable.reactor;

import ru.frostman.scalable.reactor.handlers.AcceptHandler;
import ru.frostman.scalable.reactor.handlers.ConnectHandler;
import ru.frostman.scalable.reactor.handlers.SelectorAttachment;
import ru.frostman.scalable.reactor.utils.EventExecutor;
import ru.frostman.scalable.reactor.utils.impl.StaticThreadEventExecutor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is I/O Event queue for selector. It provides high level
 * asynchronized methods to manage all operations on the selector, like
 * registering and unregistering channels, or updating the events of
 * interest for each monitored socket.
 *
 * It supports pending invocation of methods. Only thread of ExtSelector
 * can access selector object and invoke methods on it or access all
 * sockets managed by it.
 *
 * @author slukjanov aka Frostman
 */

public class ExtSelector implements Runnable {
    /**
     * Logging handler.
     */
  //  private static final Logger log = Logger.getLogger(ExtSelector.class);

    /**
     * Selector used for I/O working.
     */
    private final Selector selector;

    /**
     * Flag telling that selector must work
     * or gracefully terminated.
     */
    private boolean work = false;

    /**
     * Executor for processing handlers.
     */
    private final EventExecutor executor;

    /**
     * Creates a new selector and the associated thread started by this
     * constructor.
     *
     * @param threads number of threads for executor
     *
     * @throws IOException iff Selector.open() crushed.
     */
    public ExtSelector(int threads) throws IOException {
        executor = new StaticThreadEventExecutor(threads, 300000);
        selector = Selector.open();
    }

    /**
     * Starts main loop.
     */
    public void start() {
        if (work) {
            throw new IllegalStateException();
        }

        work = true;
        //log.info("Server starts successfully");
        run();
    }

    /**
     * Changes work flag to false. All pending tasks will be executed
     * before dying.
     */
    public void stop() {
        work = false;
        selector.wakeup();
    }

    /**
     * Main loop. This is where i/o events dispatching and executes
     * pending
     */
    @Override
    public void run() {
        try {
            while (true) {
                if (!work) {
                    break;
                }

                int selectedKeys;
                try {
                    selectedKeys = selector.select();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    continue;
                }

                if (selectedKeys == 0) {
                    // here we can check idle connections
                    continue;
                }

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    it.remove();
                    try {
                        SelectorAttachment attachment = (SelectorAttachment) sk.attachment();

                        if (sk.isAcceptable()) {
                            ((AcceptHandler) attachment).doAccept();
                        } else if (sk.isConnectable()) {
                            ((ConnectHandler) attachment).doConnect();
                        } else {
                            ConnectionHandler connection = (ConnectionHandler) attachment;
                            if (sk.isValid() && sk.isReadable() && connection.removeReadInterest()) {
                                executor.execute(connection.getReadEvent());
                            }

                            if (sk.isValid() && sk.isWritable() && connection.removeWriteInterest()) {
                                executor.execute(connection.getWriteEvent());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception in processing SelectionKey", e);
                    }
                }
            }
            log.info("Server stop");
        } catch (Throwable t) {
            closeSelector();

            log.error("Throwable in main selector loop", t);
        }
    }

    /**
     * Safely close all selection keys and selector.
     */
    private void closeSelector() {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            try {
                key.channel().close();
            } catch (IOException e) {
                // no operation
            }
        }
        try {
            selector.close();
        } catch (IOException e) {
            // no operation
        }
    }

    public Selector getSelector() {
        return selector;
    }

    public void wakeup() {
        //TODO check it, if select(timeout)
        selector.wakeup();
    }
}
