package com.juan.chatproject;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

class MyService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Common.connectWebSocket();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Common.requestAllChatsAvailable();
            }
        };

        Timer timer = new Timer();
        timer.schedule(timerTask, 0,3000);
        return Service.START_STICKY;
    }
}

