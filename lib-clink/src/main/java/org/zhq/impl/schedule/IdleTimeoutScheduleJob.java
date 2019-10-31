package org.zhq.impl.schedule;

import org.zhq.core.Connector;
import org.zhq.core.ScheduleJob;

public class IdleTimeoutScheduleJob extends ScheduleJob {
    public IdleTimeoutScheduleJob(long idleTimeOutSeconds, Connector connector) {
        super(idleTimeOutSeconds, connector);
    }

    @Override
    public void run() {
        long lastActiveTime = connector.getLastActiveTime();
        long idleTimeOutSeconds = this.idleTimeOutSeconds;
        long nextDelay = idleTimeOutSeconds - (System.currentTimeMillis() - lastActiveTime);
        if (nextDelay <= 0) {
            schedule(idleTimeOutSeconds);
            try {
                connector.fireIdleTimeoutEvent();
            }catch (Throwable e){
                connector.fireExceptionCaught(e);
            }
        } else {
            schedule(nextDelay);
        }
    }
}
