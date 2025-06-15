package com.mint.boilerws.switcher;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.mint.boilerws.config.Config;
import com.mint.boilerws.scheduler.SingleThreadScheduler;

public abstract class Switcher {
    private static final long DEFAULT_SWITCH_DELAY = 10 * 1000;
    
    private static final Logger LOG = Logger.getLogger(Switcher.class);

    private final Config config;
    
    public enum SwitchOnOffState {
        ON, PENDING_ON, PENDING_OFF, OFF
    }
    
    private final long switchDelayMs;
    
    private long lastTimeSwitched = 0;
    
    private SingleThreadScheduler singleThreadExec = SingleThreadScheduler.getInstance();
    
    private long scheduledTime = -1;
    private boolean toScheduleSwitchOn = false;
    private ScheduledFuture<?> scheduledAction = null;

    private boolean stateIsCurrentlyOn;
    
    private AtomicInteger repeatCount = new AtomicInteger(0);
    private ScheduledFuture<?> scheduledRepeatState;
    
    
    protected Switcher(final Config config) {
        this.config = config;
        this.switchDelayMs = config.get("gpio.boiler.ch.switch.delay.ms", DEFAULT_SWITCH_DELAY);
    }

    // each implementation of doing a switch
    abstract protected boolean doSwitch(final boolean switchOn);
    
    // allow override if there's a way to check properly
    protected boolean isCurrentlyOn() {
        return stateIsCurrentlyOn;
    }
    
    public synchronized SwitchOnOffState getOnOffState() {
        final long now = System.currentTimeMillis();
        if (scheduledTime > now) {
            return (toScheduleSwitchOn) ? SwitchOnOffState.PENDING_ON : SwitchOnOffState.PENDING_OFF;
        }
        return (isCurrentlyOn()) ? SwitchOnOffState.ON : SwitchOnOffState.OFF;
    }

    /**
     * Repeat what the current state is. This is to ensure the radio 
     * wave is well received by the receiver.
     * 
     * @return the current state
     */
    public SwitchOnOffState repeatSwitch() {
        final SwitchOnOffState s = getOnOffState();
        if (s == SwitchOnOffState.ON) {
            doSwitch(true);
        } else if (s == SwitchOnOffState.OFF) {
            doSwitch(false);
        }
        return s;
    }
    
    public SwitchOnOffState switchOn(final boolean isToSwitchOn) {
        if (isCurrentlyOn() == isToSwitchOn) {
            return (isToSwitchOn ? SwitchOnOffState.ON : SwitchOnOffState.OFF);
        }
        final long now = System.currentTimeMillis();
        final long timeLapsed = now - lastTimeSwitched;
        if (timeLapsed < switchDelayMs) {
            // if the switch on/off keep hitting, it keeps extending it
            this.lastTimeSwitched = now;
            final long delayToSwitch = switchDelayMs + 50; //50ms tolerance
            LOG.info("Switch toggle too soon, delaying action by: " + delayToSwitch);
            scheduleSwitch(delayToSwitch, isToSwitchOn);
            return (isToSwitchOn ? SwitchOnOffState.ON : SwitchOnOffState.OFF);
        } else {
            this.lastTimeSwitched = now;
            final boolean ok = activateSwitch(isToSwitchOn);
            if (!ok) {
                LOG.error("Switch not responding. To switch on: " + isToSwitchOn);
                return (this.isCurrentlyOn() ? SwitchOnOffState.ON : SwitchOnOffState.OFF);
            } else {
                //success, update state
                this.stateIsCurrentlyOn = isToSwitchOn;
            }
            return (isToSwitchOn ? SwitchOnOffState.ON : SwitchOnOffState.OFF);
        }
    }
    
    private boolean activateSwitch(final boolean switchOn) {
        // set the number of repeat
        final int repeatCount = this.config.get("switch.repeatstate.count", 10);
        this.repeatCount.set(repeatCount);
        // schedule the repeat
        scheduleRepeatState();
        // do the switch
        return doSwitch(switchOn);
    }
    
    private void scheduleRepeatState() {
        repeatSwitch();
        final int remaintRepeatCount = repeatCount.getAndDecrement();
        if (remaintRepeatCount < 0) {
            // do not reschedule
            LOG.info("Not reschedule repeat switch anymore");
            return;
        }
        // reschedule itself
        final long delay = this.config.get("switch.repeatstate.delay", 120 * 10_000);
        if (scheduledRepeatState != null && !scheduledRepeatState.isDone()) {
            // cancel if there's any to avoid multiple, but this
            // thread can be the very one, so 'false' not to interrupt it
            scheduledRepeatState.cancel(false);
        }
        scheduledRepeatState = this.singleThreadExec.schedule(()->{
            LOG.info("Reschedule repeat (" + remaintRepeatCount + ") switch in: " + delay);
            scheduleRepeatState();
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    private synchronized void scheduleSwitch(final long delay, final boolean isToSwitchOn) {
        if (scheduledAction != null && !scheduledAction.isDone()) {
            scheduledAction.cancel(true);
        }
        toScheduleSwitchOn = isToSwitchOn;
        scheduledTime = System.currentTimeMillis() + delay;
        scheduledAction = this.singleThreadExec.schedule(()->{
            this.scheduledTime = -1; //reset
            switchOn(isToSwitchOn);
        }, delay, TimeUnit.MILLISECONDS);
    }

    
}
