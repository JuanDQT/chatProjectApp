package com.juan.chatproject;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by juandaniel on 21/9/17.
 */

public class CustomMigration implements RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema realmSchema = realm.getSchema();

//        if (oldVersion == 1) {
//            realmSchema.get("PalabraSearch").
//                    addField("languageCode", String.class);
//            oldVersion++;
//        }
//        if (oldVersion == 2) {
//            realmSchema.get("PalabraSearch").removeField("id");
//            oldVersion++;
//        }
    }
}
