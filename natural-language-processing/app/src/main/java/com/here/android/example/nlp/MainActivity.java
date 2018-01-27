/*
 * Copyright (c) 2011-2017 HERE Global B.V. and its affiliate(s).
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

// HERE maps NLP example

package com.here.android.example.nlp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    MapFragmentView m_mapFragmentView;

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        requestPermissions();
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private void requestPermissions() {

        final List<String> requiredSDKPermissions = new ArrayList<String>();
        requiredSDKPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        requiredSDKPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requiredSDKPermissions.add(Manifest.permission.INTERNET);
        requiredSDKPermissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        requiredSDKPermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        requiredSDKPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        requiredSDKPermissions.add(Manifest.permission.READ_CONTACTS);
        requiredSDKPermissions.add(Manifest.permission.RECORD_AUDIO);
        ActivityCompat.requestPermissions(this,
                requiredSDKPermissions.toArray(new String[requiredSDKPermissions.size()]),
                REQUEST_CODE_ASK_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /**
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                permissions[index])) {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted. "
                                            + "Please go to settings and turn on for sample app",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }

                /**
                 * All permission requests are being handled. Create map fragment view. Please note
                 * the HERE SDK requires all permissions defined above to operate properly.
                 */
                m_mapFragmentView = new MapFragmentView(this);
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        if (m_mapFragmentView != null && isFinishing()) {
            // Back button pressed
            m_mapFragmentView.removeNlpListeners();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (m_mapFragmentView != null) {
            m_mapFragmentView.pauseNlp();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (m_mapFragmentView != null) {
            m_mapFragmentView.resumeNlp();
        }
    }

    /**
     * Use Volume UP key as an example to start listening to voice input
     */

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyActivatesSpeech(keyCode)) {
            // Just consume the event and listen to voice
            return m_mapFragmentView.startNlpListening();
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Checks HW keys to activate the Speech Recognition Mode.
     *
     * @param keyCode
     *         specified Key code
     *
     * @return TRUE if key activates speech
     */
    private boolean keyActivatesSpeech(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                // Volume UP key triggers Nlp to listen to
                // voice input
                return true;
            default:
                return false;
        }
    }
}
