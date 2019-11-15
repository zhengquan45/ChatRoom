package org.zhq.impl.stealing;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntFunction;

public class StealingService {

    private final int minSafetyThreshold;

    private final StealingSelectorThread[] threads;

    private final LinkedBlockingQueue<IoTask>[] queues;

    private volatile boolean terminated = false;

    public StealingService(int minSafetyThreshold, StealingSelectorThread[] threads) {
        this.minSafetyThreshold = minSafetyThreshold;
        this.threads = threads;
        this.queues = Arrays.stream(threads)
                .map(StealingSelectorThread::getReadyTaskQueue)
                .toArray((IntFunction<LinkedBlockingQueue<IoTask>[]>) LinkedBlockingQueue[]::new);
    }


    IoTask steal(final LinkedBlockingQueue<IoTask> excludedQueue) {
        final int minSafetyThreshold = this.minSafetyThreshold;
        final LinkedBlockingQueue<IoTask>[] queues = this.queues;
        for (LinkedBlockingQueue<IoTask> queue : queues) {
            if (queue == excludedQueue) {
                continue;
            }

            int size = queue.size();
            if (size > minSafetyThreshold) {
                IoTask task = queue.poll();
                return task;
            }
        }
        return null;
    }

    public StealingSelectorThread getNotBusyThread() {
        StealingSelectorThread targetThread = null;
        long curSaturatingCapacity = Long.MAX_VALUE;
        for (StealingSelectorThread thread : threads) {
            long saturatingCapacity = thread.getSaturatingCapacity();
            if (saturatingCapacity != -1 && saturatingCapacity < curSaturatingCapacity) {
                curSaturatingCapacity = saturatingCapacity;
                targetThread = thread;
            }
        }
        return targetThread;
    }

    public void shutdown(){
        if(terminated){
            return;
        }
        terminated = true;
        for (StealingSelectorThread thread : threads) {
            thread.exit();
        }
    }

    public boolean isTerminated() {
        return terminated;
    }
}
