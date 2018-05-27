package com.juan.chatproject;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.juan.chatproject.chat.Message;
import com.juan.chatproject.chat.User;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;

import java.net.URISyntaxException;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Common extends Application {

    public static final String SHARED_PREFERENCES_NAME = "PREFERENCES";
    public static final String SHARED_PREFERENCES_ACTIVITY_ACTIVE = "ACTIVE";
    public static final String SHARED_PREFERENCES_ACTIVITY_IN_MAIN = "MAIN_ACTIVE";
    private static final String TAGGER = "TAGGER";
    private static final String IP = "192.168.1.116"; // 192.168.44.122
    private static final String PUERTO = "3000";
    private static final String ID_DEVICE = "Moto";

    private static Context mContext;

    private static String CLIENT_ID;

    private static Socket socket;
    private static SharedPreferences sharedPreferences;

    @Override

    public void onCreate() {
        super.onCreate();
        this.mContext = getApplicationContext();
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    public static Context getContext() {
        return mContext;
    }

    public static void connectWebSocket() {
        try {

            socket = IO.socket("http://192.168.1.116:3000");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.emit("login", "{\"socketID\":  \"" + socket.id() + "\", \"clientID\": \"" + getClientId() + "\" }");
                }

                // NOT USED
            }).on("home", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    JSONObject obj = (JSONObject) args[0];

                    try {
                        Intent intent = new Intent("GET_MESSAGES");
                        intent.putExtra("DATA_TO_ACTIVITY", obj.getString("data"));
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on("GET_USER_IS_TYPING", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    try {

                        JSONObject obj = (JSONObject) args[0];
                        Log.e(TAGGER, "El otro user esta escribiendo");

                        // TODO: Guardar en bbdd como no leidos...
                        if (Common.isAppForeground()) {
                            Intent intent = new Intent("INTENT_GET_USER_IS_TYPING");
                            intent.putExtra("ID_FROM_TO_ACTIVITY", obj.getString("from"));
                            intent.putExtra("ID_TO_TO_ACTIVITY", obj.getString("to"));
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }).on("GET_SINGLE_MESSAGE", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    try {

                        JSONObject obj = (JSONObject) args[0];
                        Log.e(TAGGER, "Nos ha llegado un simple mensaje!!");

                        // TODO: Guardar en bbdd como no leidos...
                        if (Common.isAppForeground()) {
                            Intent intent = new Intent("INTENT_GET_SINGLE_MESSAGE");
                            intent.putExtra("MESSAGE_TO_ACTIVITY", obj.getString("message"));
                            intent.putExtra("ID_FROM_TO_ACTIVITY", obj.getString("from"));
                            intent.putExtra("ID_TO_TO_ACTIVITY", obj.getString("to"));
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));
                        } else {
                            // Aplicacion cerrada o en background
                            showNotification(obj.getString("from"), obj.getString("message"));
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                }
            });
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void disconnectWebSocket() {
        //socket.disconnect();
    }

    public static Message getMessageConstuctor(String clientFrom, String clientTo, String message) {
        Message m1 = new Message();
        User user;

        if (clientTo.equals(Common.getClientId())) {
            Log.e(TAGGER, "mensaje de entrada");
            m1.setMId(clientFrom);
            user = new User(clientFrom, clientFrom, null, true);
        } else {
            Log.e(TAGGER, "mensaje de salida");
            m1.setMId(clientTo);
            user = new User(clientFrom, clientFrom, null, true);
        }

        m1.setMMessage(message == null ? "" : message);
        m1.setMIuser(user);

        return m1;
    }

    public static void addNewMessageToServer(String message, String to) {
        JSONObject json = new JSONObject();
        try {
            json.put("from", getClientId());
            json.put("to", to);
            json.put("message", message);

            socket.emit("MESSAGE_TO", json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void notifyTyping(String to) {
        JSONObject json = new JSONObject();
        try {
            json.put("from", getClientId());
            json.put("to", to);
            socket.emit("USER_IS_TYPING", json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getClientId() {
        if (CLIENT_ID == null) {
            CLIENT_ID = sharedPreferences.getString("FROM", "");
        }
        return CLIENT_ID;
    }

    private static void showNotification(String from, String message) {

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_send)
                        .setContentTitle(from)
                        .setContentText(message)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL);

        if (Common.isActivityInMain()) {
            Log.e(TAGGER, "SOLO ABRIMOS EL CHAT");
        } else {
            Log.e(TAGGER, "ABRIMOS TODO");
        }

        Intent messageIntent = new Intent(mContext, (Common.isActivityInMain())? ChatWindowActivity.class: MainActivity.class);

        messageIntent.putExtra("TO", from);
        messageIntent.putExtra("MESSAGE", message);
        messageIntent.putExtra("GO_CHAT_WINDOW", true);

        mBuilder.setContentIntent(PendingIntent.getActivity(mContext, 0, messageIntent, 0));

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, mBuilder.build());
    }

    public static void setAppForeground(Boolean isActive) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SHARED_PREFERENCES_ACTIVITY_ACTIVE, isActive);
        editor.apply();
    }

    public static void setActivityInMain(Boolean isActive) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SHARED_PREFERENCES_ACTIVITY_IN_MAIN, isActive);
        editor.apply();
    }

    public static boolean isAppForeground() {
        return sharedPreferences.getBoolean(SHARED_PREFERENCES_ACTIVITY_ACTIVE, true);
    }

    public static boolean isActivityInMain() {
        return sharedPreferences.getBoolean(SHARED_PREFERENCES_ACTIVITY_IN_MAIN, true);
    }
}
