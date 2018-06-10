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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.socket.client.IO;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Common extends Application {

    public static final String SHARED_PREFERENCES_NAME = "PREFERENCES";
    public static final String SHARED_PREFERENCES_ACTIVITY_ACTIVE = "ACTIVE";
    public static final String SHARED_PREFERENCES_ACTIVITY_IN_MAIN = "MAIN_ACTIVE";
    private static final String TAGGER = "TAGGER";
    private static final String IP = "http://192.168.1.116:3000"; // 192.168.44.122
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
        Realm.init(mContext);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder().
                name("chat").
                schemaVersion(1).
                migration(new CustomMigration()).
                build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    public static Context getContext() {
        return mContext;
    }

    public static void connectWebSocket() {
        try {

            socket = IO.socket(IP);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject jsonLogin = new JSONObject();
                        jsonLogin.put("socketID", socket.id());
                        jsonLogin.put("clientID", getClientId());
                        socket.emit("LOGIN", jsonLogin);
//                        socket.emit("login", "{\"socketID\":  \"" + socket.id() + "\", \"clientID\": \"" + getClientId() + "\" }");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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

            }).on("GET_ALL_CHATS_AVAILABLE", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    JSONArray array = (JSONArray) args[0];
                    List<User> users = getUsersFromJSONArray(array);
                    Realm realm = Realm.getDefaultInstance();
                    LocalDataBase.access.updateUsers(realm, users);

                    if (Common.isAppForeground()) {
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("MAIN_ACTIVITY_GET_CONTACTS"));
                    }
                    realm.close();
                }

            }).on("GET_SINGLE_MESSAGE", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    try {

                        JSONObject obj = (JSONObject) args[0];
                        Log.e(TAGGER, "Nos ha llegado un simple mensaje!!");
                        Date temp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(obj.getString("date_created"));

                        Realm realm = Realm.getDefaultInstance();
                        Message m = Message.Static.getMessageConstuctor(realm, obj.getString("from"), obj.getString("to"), obj.getString("message"), temp, new Date());
                        int messageId = m.getID();
//                        int messageId = LocalDataBase.access.saveMessage(realm, m);
                        realm.close();
                        Log.e(TAGGER, "[MESSAGE_ID_CREATED]: " + messageId);

                        if (Common.isAppForeground()) {
                            Intent intent = new Intent("INTENT_GET_SINGLE_MESSAGE");
                            intent.putExtra("MESSAGE_ID", messageId);
                            intent.putExtra("MESSAGE_TEXT", obj.getString("message"));
                            intent.putExtra("MESSAGE_CLIENT_ID", obj.getString("from"));
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));
                        } else {
                            // Aplicacion cerrada o en background
                            showNotification(obj.getString("from"), obj.getString("message"));
                        }

                    } catch (JSONException | ParseException e) {
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


    // Mensaje siempre del local
    public static Message addNewMessageToServer(Message m) {
        JSONObject json = new JSONObject();
        Realm realm = Realm.getDefaultInstance();
//        LocalDataBase.access.saveMessage(realm, m);
        try {

            DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            json.put("from", getClientId());
            json.put("to", m.getUserToId());
            json.put("message", m.getText());
            json.put("date_created", df.format(new Date()));

            socket.emit("MESSAGE_TO", json);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            realm.close();
            return m;
        }
    }

    public static void requestAllChatsAvailable() {

        if (socket == null || !Common.isAppForeground())
            return;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("FROM", getClientId());
            socket.emit("ALL_CHATS_AVAILABLE", jsonObject);
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

        Intent messageIntent = new Intent(mContext, (Common.isActivityInMain()) ? ChatWindowActivity.class : MainActivity.class);

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

        if (socket == null)
            return;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("FROM", getClientId());
            jsonObject.put("LAST_SEEN", isActive);
            socket.emit("CLIENT_SET_LAST_SEEN", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }


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

    // Converisons:

    private static ArrayList<User> getUsersFromJSONArray(JSONArray jsonArray) {
        ArrayList<User> users = new ArrayList<>();

        try {

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject object = jsonArray.getJSONObject(i);
//                DateFormat sdf = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

                Date date = null;
                if (object.getString("last_seen") != null && !object.getString("last_seen").equals("null") && !object.getString("last_seen").trim().isEmpty()) {
                    String replaced = object.getString("last_seen").replaceAll("-", "/");
                    date = sdf.parse(replaced);
                }

                User u = new User(object.getString("code"),
                        object.getString("name"),
                        object.getString("avatar"),
                        object.getInt("online") == 1,
                        date,
                        null,
                        object.getInt("banned")
                        );
                users.add(u);

            }

        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }

        return users;
    }
}
