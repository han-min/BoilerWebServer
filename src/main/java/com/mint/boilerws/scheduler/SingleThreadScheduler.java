package com.mint.boilerws.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SingleThreadScheduler {

    private final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    final Thread t = new Thread(r);
                    t.setDaemon(false);
                    t.setName("SingleThreadScheduler");
                    return t;
                }
            });
    
    private SingleThreadScheduler() {}
    
    private static SingleThreadScheduler INSTANCE = new SingleThreadScheduler();
    
    public static SingleThreadScheduler getInstance() {
        return INSTANCE;
    }
    
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return exec.schedule(command, delay, unit);
    }
}
