/*
 * Copyright (c) 2011-2020 HERE Europe B.V.
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

package com.here.android.example.backgroundpositioningexample;

import android.Manifest;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.PositioningManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Activity for controlling positioning service.
 */
public class BackgroundPositioningActivity extends AppCompatActivity implements PositioningManager.OnPositionChangedListener {

    private final static String TAG = BackgroundPositioningActivity.class.getSimpleName();

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    // text view instance for showing location information
    private TextView mLocationInfo;

    // toggle view instance for controlling positioning
    private ToggleButton mTogglePositioning;

    /** Foreground service interface instance. May be null. */
    private IPositioningServiceControl mController;

    /** Static listener for logging updates when there is no UI (i.e. when activity has been destroyed. */
    private final static PositioningManager.OnPositionChangedListener sListener = new PositioningManager.OnPositionChangedListener() {
        @Override
        public void onPositionUpdated(PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition, boolean mapMatched) {
            Log.i(TAG, "onPositionUpdated: " + locationMethod.name() + ", " + geoPosition.toString());
        }

        @Override
        public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
            Log.i(TAG, "onPositionFixChanged: " + locationMethod.name() + ", " + locationStatus.name());
        }
    };

    /* Controller binder connection. */
    private final PositioningService.ApiConnection mControllerConnection = new PositioningService.ApiConnection() {

        @Override
        protected void onDisconnected() {
            mController = null;
            updateButtons();
        }

        @Override
        public void onConnected(IPositioningServiceControl controller) {
            mController = controller;
            try {
                mController.setListener(new IPositioningServiceListener.Stub() {
                    @Override
                    public void onEngineIntialized() throws RemoteException {
                        updateButtons();
                    }
                });
            } catch (RemoteException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            initializeMapsAndPositioning();
        } else {
            ActivityCompat
                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListening(this);
        stopListening(sListener);
        updateButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        startListening(sListener);
        stopListening(this);
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat
                                .shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                                   + " not granted. "
                                                   + "Please go to settings and turn on for sample app",
                                           Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                initializeMapsAndPositioning();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Initializes HERE Maps and HERE Positioning. Called after permission check.
     */
    private void initializeMapsAndPositioning() {
        setContentView(R.layout.activity_main);
        mLocationInfo = findViewById(R.id.textViewLocationInfo);
        mTogglePositioning = findViewById(R.id.toggleButtonPos);
        mTogglePositioning.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!buttonView.isPressed()) {
                    return;
                }
                if (mController == null) {
                    return;
                }
                if (!MapEngine.isInitialized()) {
                    return;
                }
                try {
                    if (isChecked) {
                        startListening(BackgroundPositioningActivity.this);
                        mController.startBackground();
                    } else {
                        mController.stopBackground();
                        stopListening(BackgroundPositioningActivity.this);
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        });
        updateButtons();
        // Set path of disk cache
        String diskCacheRoot = this.getFilesDir().getPath()
                + File.separator + ".isolated-here-maps";
        com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot);
        startForegroundService();
    }

    /**
     * Starts service and connect a binder.
     */
    private void startForegroundService() {
        final Intent intent = new Intent(getApplicationContext(), BackgroundPositioningActivity.class);
        final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        PositioningService.start(getApplicationContext(), contentIntent);
        openBinder();
    }

    @Override
    public void onPositionUpdated(PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition, boolean b) {
        updateLocationInfo(locationMethod, geoPosition);
    }

    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
        if (mLocationInfo == null) {
            return;
        }
        switch (locationStatus) {
            case OUT_OF_SERVICE:
            case TEMPORARILY_UNAVAILABLE:
                final StringBuffer sb = new StringBuffer();
                sb.append("onPositionFixChanged: " + locationMethod.name() + " " + locationStatus.name());
                mLocationInfo.setText(sb.toString());
                break;
            default:
                break;
        }
    }

    /**
     * Update location information.
     * @param geoPosition Latest geo position update.
     */
    private void updateLocationInfo(PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition) {
        if (mLocationInfo == null) {
            return;
        }
        final StringBuffer sb = new StringBuffer();
        final GeoCoordinate coord = geoPosition.getCoordinate();
        sb.append("Type: ").append(String.format(Locale.US, "%s\n", locationMethod.name()));
        sb.append("Coordinate:").append(String.format(Locale.US, "%.6f, %.6f\n", coord.getLatitude(), coord.getLongitude()));
        if (coord.getAltitude() != GeoCoordinate.UNKNOWN_ALTITUDE) {
            sb.append("Altitude:").append(String.format(Locale.US, "%.2fm\n", coord.getAltitude()));
        }
        if (geoPosition.getHeading() != GeoPosition.UNKNOWN) {
            sb.append("Heading:").append(String.format(Locale.US, "%.2f\n", geoPosition.getHeading()));
        }
        if (geoPosition.getSpeed() != GeoPosition.UNKNOWN) {
            sb.append("Speed:").append(String.format(Locale.US, "%.2fm/s\n", geoPosition.getSpeed()));
        }
        if (geoPosition.getBuildingName() != null) {
            sb.append("Building: ").append(geoPosition.getBuildingName());
            if (geoPosition.getBuildingId() != null) {
                sb.append(" (").append(geoPosition.getBuildingId()).append(")\n");
            } else {
                sb.append("\n");
            }
        }
        if (geoPosition.getFloorId() != null) {
            sb.append("Floor: ").append(geoPosition.getFloorId()).append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        mLocationInfo.setText(sb.toString());
    }

    /**
     * Bind collector service's controller interface.
     */
    private void openBinder() {
        if (!PositioningService.bind(getApplicationContext(), mControllerConnection)) {
            createErrorDialog(R.string.errorServiceBindFailedMessage, android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            }).show();
        }
    }

    /**
     *  Unbind collector service's controller interface.
     */
    private void closeBinder() {
        mControllerConnection.unBind(getApplicationContext());
    }

    /**
     * Create and return error dialog instance. Error dialog contains message and single button.
     * @param messageId Message resource ID.
     * @param buttonId Button text resource ID.
     * @param clickListener Button click listener instance.
     * @return Android dialog instance.
     */
    private Dialog createErrorDialog(int messageId, int buttonId, DialogInterface.OnClickListener clickListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(messageId)
                .setPositiveButton(buttonId, clickListener);
        return builder.create();
    }

    /**
     * Update UI buttons based on positioning service state.
     */
    private void updateButtons() {
        if (mTogglePositioning == null) {
            return;
        }
        mTogglePositioning.setEnabled(false);
        if (mController == null) {
            mTogglePositioning.setChecked(false);
            return;
        }
        if (!MapEngine.isInitialized()) {
            return;
        }
        mTogglePositioning.setEnabled(true);
        final PositioningManager posManager = PositioningManager.getInstance();
        if (posManager == null) {
            return;
        }
        mTogglePositioning.setChecked(posManager.isActive());
    }

    /**
     * Start listening for position updates, if map engine is initialized.
     * @param listener Positioning listener reference to which position events will be delivered
     *                 to.
     */
    private void startListening(PositioningManager.OnPositionChangedListener listener) {
        if (!MapEngine.isInitialized()) {
            return;
        }
        final PositioningManager posManager = PositioningManager.getInstance();
        if (posManager != null) {
            posManager.addListener(new WeakReference<>(listener));
        }
    }

    /**
     * Stop listening for position updates.
     * @param listener Listener reference from which events will be stopped.
     */
    private void stopListening(PositioningManager.OnPositionChangedListener listener) {
        if (!MapEngine.isInitialized()) {
            return;
        }
        final PositioningManager posManager = PositioningManager.getInstance();
        if (posManager != null) {
            posManager.removeListener(listener);
        }
    }
}
