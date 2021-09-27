/*
 * Copyright (c) 2011-2021 HERE Europe B.V.
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

package com.here.android.example.basicpositioningsolution;

import android.Manifest;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.LocationDataSourceHERE;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.MapState;
import com.here.android.positioning.StatusListener;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class BasicPositioningActivity extends AppCompatActivity implements PositioningManager.OnPositionChangedListener, Map.OnTransformListener {

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    // map embedded in the map fragment
    private Map map;

    // map fragment embedded in this activity
    private AndroidXMapFragment mapFragment;

    // positioning manager instance
    private PositioningManager mPositioningManager;

    // HERE location data source instance
    private LocationDataSourceHERE mHereLocation;

    // flag that indicates whether maps is being transformed
    private boolean mTransforming;

    // callback that is called when transforming ends
    private Runnable mPendingUpdate;

    // text view instance for showing location information
    private TextView mLocationInfo;

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
    protected void onPause() {
        super.onPause();
        if (mPositioningManager != null) {
            mPositioningManager.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPositioningManager != null) {
            mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mHereLocation == null) {
            return false;
        }
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.action_set_location_method:
            setLocationMethod();
            return true;
        case R.id.action_set_indoor_mode:
            setIndoorMode();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPositionUpdated(final PositioningManager.LocationMethod locationMethod, final GeoPosition geoPosition, final boolean mapMatched) {
        final GeoCoordinate coordinate = geoPosition.getCoordinate();
        if (mTransforming) {
            mPendingUpdate = new Runnable() {
                @Override
                public void run() {
                    onPositionUpdated(locationMethod, geoPosition, mapMatched);
                }
            };
        } else {
            map.setCenter(coordinate, Map.Animation.BOW);
            updateLocationInfo(locationMethod, geoPosition);
        }
    }

    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
        // ignored
    }

    @Override
    public void onMapTransformStart() {
        mTransforming = true;
    }

    @Override
    public void onMapTransformEnd(MapState mapState) {
        mTransforming = false;
        if (mPendingUpdate != null) {
            mPendingUpdate.run();
            mPendingUpdate = null;
        }
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

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment)getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    /**
     * Initializes HERE Maps and HERE Positioning. Called after permission check.
     */
    private void initializeMapsAndPositioning() {
        setContentView(R.layout.activity_main);
        mLocationInfo = (TextView) findViewById(R.id.textViewLocationInfo);
        mapFragment = getMapFragment();
        mapFragment.setRetainInstance(false);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    map = mapFragment.getMap();
                    map.setCenter(new GeoCoordinate(61.497961, 23.763606, 0.0), Map.Animation.NONE);
                    map.setZoomLevel(map.getMaxZoomLevel() - 1);
                    map.addTransformListener(BasicPositioningActivity.this);
                    mPositioningManager = PositioningManager.getInstance();
                    mHereLocation = LocationDataSourceHERE.getInstance(
                            new StatusListener() {
                                @Override
                                public void onOfflineModeChanged(boolean offline) {
                                    // called when offline mode changes
                                }

                                @Override
                                public void onAirplaneModeEnabled() {
                                    // called when airplane mode is enabled
                                }

                                @Override
                                public void onWifiScansDisabled() {
                                    // called when Wi-Fi scans are disabled
                                }

                                @Override
                                public void onBluetoothDisabled() {
                                    // called when Bluetooth is disabled
                                }

                                @Override
                                public void onCellDisabled() {
                                    // called when Cell radios are switch off
                                }

                                @Override
                                public void onGnssLocationDisabled() {
                                    // called when GPS positioning is disabled
                                }

                                @Override
                                public void onNetworkLocationDisabled() {
                                    // called when network positioning is disabled
                                }

                                @Override
                                public void onServiceError(ServiceError serviceError) {
                                    // called on HERE service error
                                }

                                @Override
                                public void onPositioningError(PositioningError positioningError) {
                                    // called when positioning fails
                                }

                                @Override
                                @SuppressWarnings("deprecation")
                                public void onWifiIndoorPositioningNotAvailable() {
                                    // called when running on Android 9.0 (Pie) or newer
                                }

                                @Override
                                public void onWifiIndoorPositioningDegraded() {
                                    // called when running on Android 9.0 (Pie) or newer
                                }
                            });
                    if (mHereLocation == null) {
                        Toast.makeText(BasicPositioningActivity.this, "LocationDataSourceHERE.getInstance(): failed, exiting", Toast.LENGTH_LONG).show();
                        finish();
                    }
                    mPositioningManager.setDataSource(mHereLocation);
                    mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(
                            BasicPositioningActivity.this));
                    // start position updates, accepting GPS, network or indoor positions
                    if (mPositioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR)) {
                        mapFragment.getPositionIndicator().setVisible(true);
                    } else {
                        Toast.makeText(BasicPositioningActivity.this, "PositioningManager.start: failed, exiting", Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    new AlertDialog.Builder(BasicPositioningActivity.this).setMessage(
                            "Error : " + error.name() + "\n\n" + error.getDetails())
                            .setTitle(R.string.engine_init_error)
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            finish();
                                        }
                                    }).create().show();
                }
            }
        });
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
     * Called when set location method -menu item is selected.
     */

    private void setLocationMethod() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String[] names = getResources().getStringArray(R.array.locationMethodNames);
        builder.setTitle(R.string.title_select_location_method)
            .setItems(names, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        final String[] values = getResources().getStringArray(R.array.locationMethodValues);
                        final PositioningManager.LocationMethod method =
                                PositioningManager.LocationMethod.valueOf(values[which]);
                        setLocationMethod(method);
                    } catch (IllegalArgumentException ex) {
                        Toast.makeText(BasicPositioningActivity.this, "setLocationMethod failed: "
                                + ex.getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        dialog.dismiss();
                    }
                }
            });
        builder.create().show();
    }

    /**
     * Called when set indoor mode -menu item is selected.
     */
    private void setIndoorMode() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String[] names = getResources().getStringArray(R.array.indoorPositioningModeNames);
        builder.setTitle(R.string.title_select_indoor_mode)
            .setItems(names, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        final String[] values = getResources().getStringArray(R.array.indoorPositioningModeValues);
                        final LocationDataSourceHERE.IndoorPositioningMode mode =
                                LocationDataSourceHERE.IndoorPositioningMode.valueOf(values[which]);
                        setIndoorMode(mode);
                    } catch (IllegalArgumentException ex) {
                        Toast.makeText(BasicPositioningActivity.this, "setIndoorMode failed: "
                                + ex.getMessage(), Toast.LENGTH_LONG).show();
                    } finally {
                        dialog.dismiss();
                    }
                }
            });
        builder.create().show();
    }

    /**
     * Sets location method for the PositioningManager.
     * @param method New location method.
     */
    private void setLocationMethod(PositioningManager.LocationMethod method) {
        if (!mPositioningManager.start(method)) {
            Toast.makeText(BasicPositioningActivity.this, "PositioningManager.start(" + method + "): failed", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets indoor positioning method.
     * @param mode New indoor positioning mode.
     */
    private void setIndoorMode(LocationDataSourceHERE.IndoorPositioningMode mode) {
        final LocationDataSourceHERE.IndoorPositioningModeSetResult result = mHereLocation.setIndoorPositioningMode(mode);
        switch (result) {
        case FEATURE_NOT_LICENSED:
            Toast.makeText(BasicPositioningActivity.this, mode + ": is not licensed", Toast.LENGTH_LONG).show();
            break;
        case INTERNAL_ERROR:
            Toast.makeText(BasicPositioningActivity.this, mode + ": internal error", Toast.LENGTH_LONG).show();
            break;
        case MODE_NOT_ALLOWED:
            Toast.makeText(BasicPositioningActivity.this, mode + ": is not allowed", Toast.LENGTH_LONG).show();
            break;
        case PENDING:
        case OK:
        default:
            break;
        }
    }
}
