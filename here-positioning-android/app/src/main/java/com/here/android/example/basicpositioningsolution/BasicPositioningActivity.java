/*
 * Copyright (c) 2011-2017 HERE Europe B.V.
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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapState;
import com.here.android.positioning.StatusListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BasicPositioningActivity extends Activity implements PositioningManager.OnPositionChangedListener, Map.OnTransformListener {

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    // map embedded in the map fragment
    private Map map;

    // map fragment embedded in this activity
    private MapFragment mapFragment;

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

    // permissions that need to be explicitly requested from end user.
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
        case REQUEST_CODE_ASK_PERMISSIONS:
            for (int index = permissions.length - 1; index >= 0; --index) {
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Required permission '" + permissions[index] + "' not granted, exiting", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            initializeMapsAndPositioning();
            break;
        }
    }

    /**
     * Checks the dynamically controlled permissions and requests missing
     * permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (missingPermissions.isEmpty()) {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                grantResults);
        } else {
            final String[] permissions = missingPermissions.toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    /**
     * Initializes HERE Maps and HERE Positioning. Called after permission check.
     */
    private void initializeMapsAndPositioning() {
        setContentView(R.layout.activity_main);
        mLocationInfo = (TextView) findViewById(R.id.textViewLocationInfo);
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(
            R.id.mapfragment);
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
                    Toast.makeText(BasicPositioningActivity.this, "onEngineInitializationCompleted: error: " + error + ", exiting", Toast.LENGTH_LONG).show();
                    finish();
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
