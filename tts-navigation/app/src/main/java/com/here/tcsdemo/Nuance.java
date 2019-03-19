/*
 * Copyright (c) 2011-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.tcsdemo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.nuance.speechkit.Session;
import com.nuance.speechkit.Transaction;
import com.nuance.speechkit.TransactionException;

/**
 * Contains logic for Nuance TTS Engine
 */
public class Nuance {

    private final String TAG = getClass().getSimpleName();
    private boolean isInitialized;
    private Session session;

    /**
     * Constructor what initialize Nuance TTS
     *
     * @param context Android context
     */
    public Nuance(Context context) {
        Bundle bundle = null;
        String appKey = "";
        String appServer = "";
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            bundle = ai.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Exception: " + e.toString());
        }
        if (bundle != null) {
            try {
                appKey = bundle.getString("nuance.app.key");
                appServer = String.format("nmsps://%s@%s:%s",
                        bundle.getString("nuance.app.id"),
                        bundle.getString("nuance.app.host"),
                        String.valueOf(bundle.getInt("nuance.app.port", 443)));
            } catch (NullPointerException e) {
                Log.e(TAG, "Can't access Nuance credentials from AndroidManifest.xml: " + e.toString());
            }
        } else {
            Log.e(TAG, "Can't access Nuance credentials from AndroidManifest.xml");
        }
        /**
         * Create session for Nuance TTS engine
         */
        try {
            session = Session.Factory.session(context, Uri.parse(appServer), appKey);
            isInitialized = true;
        } catch (IllegalArgumentException e) {
            isInitialized = false;
        }
    }

    /**
     * Return status of Nuance TTS engine initialization
     *
     * @return boolean Will be true if TTS engine was initialized successfully, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Pronounce text with Nuance TTS
     *
     * @param text Text for pronunciation
     */
    public void speak(String text) {
        /**
         *  Set options
         */
        Transaction.Options options = new Transaction.Options();

        /**
         * Play text
         */
        session.speakString(text, options, new Transaction.Listener() {
            @Override
            public void onSuccess(Transaction transaction, String suggestion) {
                super.onSuccess(transaction, suggestion);
                Log.i(TAG, "Success!");
            }

            @Override
            public void onError(Transaction transaction, String suggestion, TransactionException e) {
                super.onError(transaction, suggestion, e);
                Log.e(TAG, "Error: " + e.toString());
            }
        });
    }

}
