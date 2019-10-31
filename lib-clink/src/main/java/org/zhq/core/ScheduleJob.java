package org.zhq.core;


import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class ScheduleJob implements Runnable {
    protected final long idleTimeOutSeconds;
    protected final Connector connector;

    private volatile Scheduler scheduler;
    private volatile ScheduledFuture scheduledFuture;

    protected ScheduleJob(long idleTimeOutSeconds, Connector connector) {
        this.idleTimeOutSeconds = idleTimeOutSeconds;
        this.connector = connector;
    }

    synchronized void schedule(Scheduler scheduler) {
        this.scheduler = scheduler;
        schedule(idleTimeOutSeconds);
    }

    synchronized void unSchedule() {
        if (scheduler != null) {
            scheduler = null;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    public synchronized void schedule(long delay){
        if (scheduler != null) {
            scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
        }
    }
}
