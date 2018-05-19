package com.juan.chatproject;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.juan.chatproject.chat.Message;
import com.juan.chatproject.chat.User;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Common extends Application {

    private static final String TAGGER = "TAGGER";
    private static Context mContext;

    private static final String IP = "192.168.1.116"; // 192.168.44.122
    private static final String PUERTO = "3000";

    private static final String ID_DEVICE = "Moto";

    private static Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }

    public static void connectWebSocket(final String fromClient, final String toClient) {
        try {

            socket = IO.socket("http://192.168.1.116:3000");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.emit("login", "{\"socketID\":  \"" + socket.id() + "\", \"clientID\": \"" + fromClient + "\" }");
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

            }).on("GET_SINGLE_MESSAGE", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    try {

                        JSONObject obj = (JSONObject) args[0];
                        Log.e(TAGGER, "Nos ha llegado un simple mensaje!!");
                        Intent intent = new Intent("INTENT_GET_SINGLE_MESSAGE");
                        intent.putExtra("MESSAGE_TO_ACTIVITY", obj.getString("message"));
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));
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
        socket.disconnect();
    }

    public static Message getMessageConstuctor(Boolean inputMessage, String targetID, String message) {
        Message m1 = new Message();
        User user;

        if (inputMessage) {
            Log.e(TAGGER, "mensaje de salida");
            m1.setMId("1");
            user = new User("0", targetID, null, true);

        } else {
            Log.e(TAGGER, "mensaje de entrada");
            m1.setMId("0");
            user = new User("1", targetID, null, true);
        }


        // 0 porque es el dueno del mensk
        m1.setMMessage(message == null ? "" : message);
        m1.setMIuser(user);

        return m1;
    }

    public static void addNewMessageToServer(String message, String to) {
        socket.emit("MESSAGE_TO", "{ \"from\": \"" + socket.id() + "\", \"to\": \"" + to + "\", \"message\": \"" + message + "\" }");
    }

}
