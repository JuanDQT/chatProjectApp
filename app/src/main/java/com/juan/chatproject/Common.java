package com.juan.chatproject;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.juan.chatproject.chat.Contact;
import com.juan.chatproject.chat.Message;
import com.juan.chatproject.chat.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.socket.client.IO;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Common extends Application {

    public static final String SHARED_PREFERENCES_NAME = "PREFERENCES";
    public static final String SHARED_PREFERENCES_ACTIVITY_ACTIVE = "ACTIVE";
    public static final String SHARED_PREFERENCES_ACTIVITY_IN_MAIN = "MAIN_ACTIVE";
    public static final String TAGGER = "TAGGER";
    private static final String IP = "http://192.168.1.116:3000"; // 192.168.44.122

    private static Context mContext;

    private static String CLIENT_ID;

    private static Socket socket;
    private static SharedPreferences sharedPreferences;
    public static final String ACEPTAR_CONTACTO = "ACEPTAR_CONTACTO";
    public static final String DENEGAR_CONTACTO = "DENEGAR_CONTACTO";
    public static final String CANCELAR_ENVIO_SOLICITUD = "CANCELAR_ENVIO_SOLICITUD";


    public static final String SOLICITAR_CONTACTO = "SOLICITAR_CONTACTO";

    public static final String FIRST_ON = "FIRST_ON";

    // 25/03/2020: Now, we are gonna use KotlinCourtines
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

                        if (!isOnline() || !socket.connected())
                            return;

                        Realm realm = Realm.getDefaultInstance();
                        JSONObject data = new JSONObject();

                        if (sharedPreferences != null) {
                            if (!sharedPreferences.getBoolean(FIRST_ON, false)) {

                                try (Realm r = Realm.getDefaultInstance()) {
                                    List<Contact> contacts = r.copyFromRealm(r.where(Contact.class).findAll());
                                    List<User> users = r.copyFromRealm(r.where(User.class).findAll());
                                }

                                JSONArray usersId = Contact.access.getUsersIdJSONArray(realm);
                                data.put("users_id", usersId);
                                data.put("id_user_from", getClientId());
                                socket.emit("RECONNECT", data);
                                sendAllMessagesPending(realm);
                            } else {
                                data.put("id_user", getClientId());
                                socket.emit("LOGIN", data);
                            }
                        }

                        if (Common.isAppForeground()) {
                            // Actualiza las vistas de main
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("MAIN_ACTIVITY_GET_CONTACTS"));
                        }

                        realm.close();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).on("GET_CONNECT_RESPONSE", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    JSONObject response = (JSONObject) args[0];

                    if (sharedPreferences != null && sharedPreferences.getBoolean(FIRST_ON, false)) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(FIRST_ON, false);
                        editor.apply();
                    }

                    try (Realm realm = Realm.getDefaultInstance()) {

//                        JSONObject data = new JSONObject();
                        final JSONArray usersToAdd = response.getJSONArray("users");

                        List<User> users = getUsersFromJSONArray(usersToAdd);
                        LocalDataBase.access.updateUsers(realm, users);

                        List<Contact> contacts = getContactsByJSON(response.getJSONArray("contacts"));
                        LocalDataBase.access.insertContacts(realm, contacts);

//                        data.put("id_user_from", Common.getClientId());
//                        socket.emit("ALL_MESSAGES", data);

                    } catch (JSONException exc) {
                        Log.e(TAGGER, "Error: " + exc.getMessage());
                    }

                    // TESTING
                    try (Realm r = Realm.getDefaultInstance()) {
                        List<Contact> contacts = r.copyFromRealm(r.where(Contact.class).findAll());
                        List<User> users = r.copyFromRealm(r.where(User.class).findAll());
                        Log.e(TAGGER, "*** Contactos: " + contacts.size());
                        Log.e(TAGGER, "*** Users: " + users.size());
                    }

                }

                // Preguntamos si tienen algun mensaje leido pendiente de enviar
            }).on("GET_ALL_MESSAGES", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ArrayList<String> froms = new ArrayList<>();

                    JSONArray response = (JSONArray) args[0];
                    // Procesamos mensajes

                    try (Realm realm = Realm.getDefaultInstance()) {
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                processMessage(realm, response.getJSONObject(i));

                                // Por cada uno generamis el evento
                                String fromParsed = response.getJSONObject(i).getString("from");
                                if (!froms.contains(fromParsed)) {
                                    froms.add(fromParsed);
                                }

                            } catch (JSONException | ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    try (Realm r = Realm.getDefaultInstance()) {
                        for (String id : froms) {
                            LocalDataBase.access.getLastMessageAsync(r, new ArrayList<User>(r.where(User.class).equalTo("id", id).findAll()));
                        }
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

            }).on("GET_ASK_REQUEST_CONTACT_STATUS", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    try {

                        final JSONObject obj = (JSONObject) args[0];

                        final String type = obj.getString("type");

                        if (obj.has("error")) {
                            Log.e(TAGGER, "Datos no sincronizados");
                            return;
                        }

                        switch (type) {
                            case ACEPTAR_CONTACTO:
                                Log.e(TAGGER, "[ACEPTAR_CONTACTO]");
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            try {
                                                Contact contact = realm.where(Contact.class).
                                                        equalTo("id_user_from", obj.getString("id_user_from"))
                                                        .and()
                                                        .equalTo("id_user_to", obj.getString("id_user_to")).
                                                                findFirst();

                                                Log.e(TAGGER, "En execute...");
                                                if (contact != null) {
                                                    contact.setStatus(obj.getString("value"));
                                                    realm.insertOrUpdate(contact);
                                                    Log.e(TAGGER, "Actualizando...");
                                                }

                                                User u = realm.where(User.class).equalTo("id", contact.getIdUserFrom().equals(getClientId()) ? contact.getIdUserTo() : getClientId()).findFirst();
                                                if (u != null && u.getId().equals(obj.getString("id_user_from"))) {
                                                    showNotificationContact(u.getName(), R.string.solicitud_aceptada);
                                                }

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } finally {
                                    notifyUserContactView(obj.getString("id_user_from"));
                                }

                                break;
                            case DENEGAR_CONTACTO:
                            case CANCELAR_ENVIO_SOLICITUD:
                                Log.e(TAGGER, "[DENEGAR CONTACTO][CANCELAR_ENVIO_SOLICITUD]");
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            try {
                                                Contact contact = realm.where(Contact.class).
                                                        equalTo("id_user_from", obj.getString("id_user_from"))
                                                        .and()
                                                        .equalTo("id_user_to", obj.getString("id_user_to")).
                                                                findFirst();
                                                Log.e(TAGGER, "En execute...");
                                                if (contact != null) {
                                                    contact.deleteFromRealm();
                                                    Log.e(TAGGER, "Actualizando...");
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } finally {
                                    Log.e(TAGGER, "***Entre: notifyUserContactView: " + obj.getString("id_user_from"));
                                    notifyUserContactView(obj.getString("id_user_from"));
                                }
                                break;
                            case SOLICITAR_CONTACTO:
                                Log.e(TAGGER, "[SOLICITAR_CONTACTO]");
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            try {
                                                Contact contact = new Contact();
                                                contact.setIdUserFrom(obj.getString("id_user_from"));
                                                contact.setIdUserTo(obj.getString("id_user_to"));
                                                contact.setStatus(obj.getString("value"));
                                                realm.insertOrUpdate(contact);
                                                Log.e(TAGGER, "Actualizando...");

                                                if (obj.getString("id_user_to").equals(getClientId()) && obj.has("user")) {
                                                    for (User u : getUsersFromJSONArray(obj.getJSONArray("user"))) {
                                                        realm.insertOrUpdate(u);
                                                        showNotificationContact(u.getName(), R.string.solicitud_nueva);
                                                    }
                                                }

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } finally {
                                    notifySearchUserView(obj.getString("id_user_from"));
                                }
                                break;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on("GET_PENDING_MESSAGES_READED", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    JSONArray array = (JSONArray) args[0];
                    if (array.length() <= 0)
                        return;

                    Realm realm = Realm.getDefaultInstance();
                    Integer[] idList = new Integer[array.length()];
                    try {
                        for (int i = 0; i < array.length(); i++) {
                            idList[i] = array.getJSONObject(i).getInt("id");
                        }
                    } catch (JSONException e) {
                        Log.e(TAGGER, "Error convirtiendo ids en int");
                    }

                    List<Message> messages = realm.where(Message.class).in("idServidor", idList).and().isNotNull("fechaLectura").findAll();

                    for (Message m : messages) {
                        notifyMessageReaded(m.getIdServidor(), m.getFechaLectura());
                    }

                    realm.close();
                }

            }).on("GET_SEARCH_USERS_BY_NAME", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    Realm realm = Realm.getDefaultInstance();

                    JSONArray array = (JSONArray) args[0];

                    ArrayList<User> users = getUsersFromJSONArray(array);
                    Intent data = new Intent("SERCH_USERS_DATA");
                    data.putExtra("users", users);
                    Log.i(TAGGER, "***Total encontrados: " + users.size());
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(data));

                    realm.close();
                }

            }).on("GET_SEARCH_USERS_BY_ID", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    Realm realm = Realm.getDefaultInstance();
                    try {

                        JSONObject response = (JSONObject) args[0];
                        JSONArray array = response.getJSONArray("data");


                        ArrayList<User> users = getUsersFromJSONArray(array);
                        //LocalDataBase.access.updateUsers(realm, users); Delete?
                        Intent data = new Intent("GET_SEARCH_USERS_BY_ID");
                        data.putExtra("users", users);
                        data.putExtra("type", response.getString("type"));
                        Log.i(TAGGER, "***Total encontrados: " + users.size());
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(data));

                        realm.close();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on("GET_SINGLE_MESSAGE", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        Realm realm = Realm.getDefaultInstance();
                        if (messageExist(realm, obj.getInt("id"))) {
                            return;
                        }

/*
                        // Check if User exist in our database.
                        // TODO: quizas se puede mejorar.. Si te escriben un mensaje se entiende que se establece los contactos para los dos. Validar
                        User user = realm.where(User.class).equalTo("id", obj.getString("from")).findFirst();
                        if (user == null) {
                            final User newUser = new User(obj.getString("from"), null, null, false, null, null, false);
                            realm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm r) {
                                    r.insertOrUpdate(newUser);
                                }
                            });
                        }
*/


                        processMessage(realm, obj);
                        LocalDataBase.access.getLastMessageAsync(realm, new ArrayList<User>(realm.where(User.class).equalTo("id", obj.getString("from")).findAll()));

                        realm.close();

                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }

            }).on("GET_UPDATE_MESSAGE_ID_SERVER", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    Realm realm = Realm.getDefaultInstance();
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        LocalDataBase.access.updateMessageAsSent(realm, obj.getInt("id_pda"), obj.getInt("id_server"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    realm.close();
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

    private static List<Contact> getContactsByJSON(JSONArray contacts) {
        List<Contact> mList = new ArrayList<>();
        try {

            for (int i = 0; i < contacts.length(); i++) {
                JSONObject jsonObject = contacts.getJSONObject(i);
                mList.add(new Contact(jsonObject.getString("id_user_from"), jsonObject.getString("id_user_to"), jsonObject.getString("status")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            return mList;
        }
    }

    public static void disconnectWebSocket() {
        //socket.disconnect();
    }

    public static void sendAllMessagesPending(Realm realm) {
        if (realm != null) {
            RealmResults<Message> list = realm.where(Message.class).equalTo("idServidor", 0).and().equalTo("userFromId", Common.getClientId()).findAll();
            if (list.size() == 0)
                return;
            for (Message m : list) {
                addNewMessageToServer(m);
            }
        }
    }

    private static void processMessage(Realm realm, JSONObject obj) throws JSONException, ParseException {
        // Confirma recibido
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JSONObject jsonConfirm = new JSONObject();
        jsonConfirm.put("id_server", obj.getInt("id"));
        jsonConfirm.put("fecha_recepcion", df.format(new Date()));
        socket.emit("MESSAGE_CONFIRM_RECEPCION", jsonConfirm);

        // Procesamos aviso
        Date temp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(obj.getString("date_created"));
        Message m = Message.Static.getMessageConstuctor(realm, obj.getInt("id"), obj.getString("from"), obj.getString("to"), obj.getString("message"), temp, new Date());
        int messageId = m.getID();
        Log.e(TAGGER, "[MESSAGE_ID_CREATED]: " + messageId);

        if (Common.isAppForeground()) {
            Intent intent = new Intent("INTENT_GET_SINGLE_MESSAGE");
            intent.putExtra("MESSAGE_ID", messageId);
            intent.putExtra("MESSAGE_TEXT", obj.getString("message"));
            intent.putExtra("MESSAGE_CLIENT_ID", obj.getString("from"));
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));
        } else {
            // Aplicacion cerrada o en background
            User u = realm.where(User.class).equalTo("id", obj.getString("from")).findFirst();
            if (u != null) {
                showNotificationMessage(u.getName(), u.getId(), obj.getString("message"));
            }
        }
    }

    private static boolean messageExist(Realm realm, int idServidor) {
        Message m = realm.where(Message.class).equalTo("idServidor", idServidor).findFirst();
        return m != null;
    }

    // Mensaje siempre del local
    public static Message addNewMessageToServer(Message m) {
        JSONObject json = new JSONObject();
        Realm realm = Realm.getDefaultInstance();
//        LocalDataBase.access.saveMessage(realm, m);
        try {
            DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            json.put("id_pda", m.getID());
            json.put("from", getClientId());
            json.put("to", m.getUserToId());
            json.put("message", m.getText());
            json.put("date_created", m.getCreatedAt() == null ? df.format(new Date()) : df.format(m.getCreatedAt()));

            if (isOnline() && socket.connected())
                socket.emit("MESSAGE_TO", json);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            realm.close();
            return m;
        }
    }

    public static void notifyMessageReaded(int idServidor, Date fechaLectura) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        if (Common.isOnline() && socket.connected()) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id_server", idServidor);
                jsonObject.put("fecha_lectura", sdf.format(fechaLectura));
                socket.emit("MESSAGE_CONFIRM_LECTURA", jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
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

    private static void showNotificationMessage(String from, String id, String message) {
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

        messageIntent.putExtra("TO", id);
        messageIntent.putExtra("MESSAGE", message);
        messageIntent.putExtra("GO_CHAT_WINDOW", true);

        mBuilder.setContentIntent(PendingIntent.getActivity(mContext, 0, messageIntent, 0));

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, mBuilder.build());
    }

    private static void showNotificationContact(String from, int message) {

        if (Common.isAppForeground())
            return;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_send)
                        .setContentTitle(mContext.getString(R.string.solicitud))
                        .setContentText(mContext.getString(message, from))
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL);


        Intent messageIntent = new Intent(mContext, message == R.string.solicitud_aceptada ? MainActivity.class : ContactosActivity.class);

        mBuilder.setContentIntent(PendingIntent.getActivity(mContext, 0, messageIntent, 0));

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, mBuilder.build());
    }

    public static void setAppForeground(Boolean isForeground) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SHARED_PREFERENCES_ACTIVITY_ACTIVE, isForeground);
        editor.apply();

        if (socket == null)
            return;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id_user_from", getClientId());
            jsonObject.put("is_foreground", isForeground);
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

                User u = new User(
                        object.getString("id"),
                        object.getString("name"),
                        object.getString("avatar"),
                        object.getInt("online") == 1,
                        date,
                        null,
                        object.getInt("banned") == 1
                );
                users.add(u);

            }

        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }

        return users;
    }

    public static void searchUsersByName(String name) {
        if (Common.isOnline() && socket.connected()) {
            JSONObject json = new JSONObject();
            try {
                json.put("id_user_from", Common.getClientId());
                json.put("name", name);
                socket.emit("SEARCH_USERS_BY_NAME", json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setContactoStatus(String idUserFrom, String idUserTo, String action) {
        if (Common.isOnline() && socket.connected()) {
            JSONObject json = new JSONObject();
            try {
                json.put("id_user_from", idUserFrom);
                json.put("id_user_to", idUserTo);
                json.put("action", action);
                socket.emit("SET_CONTACTO_STATUS", json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private static void notifyUserContactView(String userFrom) {
        Intent intent = new Intent("RELOAD_SOLICITUDES");
        Log.e(TAGGER, "*** userFrom" + userFrom);

        if (getClientId().equals(userFrom)) {

            intent.putExtra("TYPE", ContactosActivity.access.getTIPO_ENVIADAS());
        } else {
            intent.putExtra("TYPE", ContactosActivity.access.getTIPO_PENDIENTES());
        }
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));
    }

    private static void notifySearchUserView(String userFrom) {
        Intent intent = null;
        if (getClientId().equals(userFrom)) {
            intent = new Intent("UPDATE_LIST_SERCHED");
        } else {
            intent = new Intent("RELOAD_SOLICITUDES");
            intent.putExtra("TYPE", ContactosActivity.access.getTIPO_PENDIENTES());
        }
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(intent));
    }

    public static void clearNotifications() {
        NotificationManager nMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nMgr != null)
            nMgr.cancelAll();
    }

}
