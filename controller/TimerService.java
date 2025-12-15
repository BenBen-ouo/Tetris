package controller;

import java.awt.event.ActionListener;
import javax.swing.*;

// 簡易計時服務：集中管理 Swing Timer，外部提供 callback
public class TimerService {
    private final int intervalMs;
    private final Runnable callback;
    private Timer timer;

    public TimerService(int intervalMs, Runnable callback) {
        this.intervalMs = intervalMs;
        this.callback = callback;
        this.timer = new Timer(intervalMs, buildListener());
    }

    private ActionListener buildListener() {
        return e -> {
            if (callback != null) callback.run();
        };
    }

    public void start() {
        if (timer != null && !timer.isRunning()) timer.start();
    }

    public void stop() {
        if (timer != null && timer.isRunning()) timer.stop();
    }

    public boolean isRunning() { return timer != null && timer.isRunning(); }
}
