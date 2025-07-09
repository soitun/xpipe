package io.xpipe.app.util;

import io.xpipe.core.FailableRunnable;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public class GlobalTimer {

    private static Timer TIMER;

    public static void init() {
        TIMER = new Timer("global-timer", true);
    }

    public static void reset() {
        if (TIMER == null) {
            return;
        }

        TIMER.cancel();
        TIMER = null;
    }

    private static TimerTask createDelayedTask(Duration interval, Supplier<Boolean> s) {
        return new TimerTask() {
            @Override
            public void run() {
                if (!s.get()) {
                    // Use this approach instead of scheduleAtFixedRate
                    // to prevent it from being run rapidly in case the timer is trying
                    // to catch up. For example with system hibernation
                    TIMER.schedule(createDelayedTask(interval, s), interval.toMillis());
                }
            }
        };
    }

    public static void scheduleUntil(Duration interval, boolean runInstantly, Supplier<Boolean> s) {
        var task = createDelayedTask(interval, s);
        TIMER.schedule(task, runInstantly ? interval.toMillis() : 0);
    }

    public static void delay(Runnable r, Duration delay) {
        TIMER.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        r.run();
                    }
                },
                delay.toMillis());
    }

    public static void delayAsync(Runnable r, Duration delay) {
        TIMER.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        ThreadHelper.runAsync(r);
                    }
                },
                delay.toMillis());
    }

    public static void delayFailableAsync(FailableRunnable<Throwable> r, Duration delay) {
        TIMER.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        ThreadHelper.runFailableAsync(r);
                    }
                },
                delay.toMillis());
    }
}
