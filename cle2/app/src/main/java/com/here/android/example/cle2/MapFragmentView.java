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

package com.here.android.example.cle2;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.customlocation2.CLE2DataManager;
import com.here.android.mpa.customlocation2.CLE2Geometry;
import com.here.android.mpa.customlocation2.CLE2OperationResult;
import com.here.android.mpa.customlocation2.CLE2PointGeometry;
import com.here.android.mpa.customlocation2.CLE2ProximityRequest;
import com.here.android.mpa.customlocation2.CLE2Request;
import com.here.android.mpa.customlocation2.CLE2Result;
import com.here.android.mpa.customlocation2.CLE2Task;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.MapCircle;
import com.here.android.mpa.mapping.MapMarker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class encapsulates the properties and functionality of the Map view.
 */
public class MapFragmentView {
    private AndroidXMapFragment m_mapFragment;
    private AppCompatActivity m_activity;
    private Map m_map;
    private EditText m_layerEdit;
    private SeekBar m_radiusSeekBar;
    private Spinner m_connectivityModeSpinner;

    private ArrayList<CLE2Geometry> m_geometryList = new ArrayList<>();

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        initMapFragment();

        m_activity.findViewById(R.id.buttonDownload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadLayer();
            }
        });

        m_activity.findViewById(R.id.buttonUpload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadLayer();
            }
        });

        m_activity.findViewById(R.id.addGeometryButton).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addGeometry();
                    }
                });

        m_activity.findViewById(R.id.clearMapButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearMap();
            }
        });

        m_activity.findViewById(R.id.purgeLocalStorageButton).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        purgeLocalStorage();
                    }
                });

        m_activity.findViewById(R.id.searchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proximitySearch();
            }
        });

        m_layerEdit = m_activity.findViewById(R.id.editTextCleLayer);
        m_radiusSeekBar = m_activity.findViewById(R.id.searchRadiusSeekBar);
        m_connectivityModeSpinner = m_activity.findViewById(R.id.connectivityModeSpinner);

        ArrayAdapter<CLE2Request.CLE2ConnectivityMode> dataAdapter =
                new ArrayAdapter<>(m_activity, android.R.layout.simple_spinner_item,
                                   CLE2Request.CLE2ConnectivityMode.values());

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_connectivityModeSpinner.setAdapter(dataAdapter);
        m_connectivityModeSpinner.setSelection(2);
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager()
                .findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = getMapFragment();
        // This will use external storage to save map cache data, it is also possible to set
        // private app's path
        String path = new File(m_activity.getExternalFilesDir(null), ".here-map-data")
                .getAbsolutePath();
        // This method will throw IllegalArgumentException if provided path is not writable
        com.here.android.mpa.common.MapSettings.setDiskCacheRootPath(path);
        if (m_mapFragment != null) {
            /* Initialize the AndroidXMapFragment, results will be given via the called back. */
            m_mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {

                    if (error == Error.NONE) {
                        /*
                         * If no error returned from map fragment initialization, the map will be
                         * rendered on screen at this moment.Further actions on map can be provided
                         * by calling Map APIs.
                         */
                        m_map = m_mapFragment.getMap();

                        /*
                         * Map center can be set to a desired location at this point.
                         * It also can be set to the current location ,which needs to be delivered
                         * by the PositioningManager.
                         * Please refer to the user guide for how to get the real-time location.
                         */

                        m_map.setCenter(new GeoCoordinate(49.258576, -123.008268), Map.Animation.NONE);
                    } else {
                        new AlertDialog.Builder(m_activity).setMessage(
                                "Error : " + error.name() + "\n\n" + error.getDetails())
                                .setTitle(R.string.engine_init_error)
                                .setNegativeButton(android.R.string.cancel,
                                                   new DialogInterface.OnClickListener() {
                                                       @Override
                                                       public void onClick(
                                                               DialogInterface dialog,
                                                               int which) {
                                                           m_activity.finish();
                                                       }
                                                   }).create().show();
                    }
                }
            });
        }
    }

    /**
     * Download all layer data to remote storage
     */
    private void downloadLayer() {
        clearMap();

        // get instance of the data manager
        final CLE2DataManager dataManager = CLE2DataManager.getInstance();
        final String layerName = m_layerEdit.getText().toString();

        // create download layer task
        CLE2Task<CLE2OperationResult> task = dataManager.newDownloadLayerTask(layerName);

        // start downloading
        task.start(new CLE2Task.Callback<CLE2OperationResult>() {
            @Override
            public void onTaskFinished(@Nullable CLE2OperationResult cle2OperationResult,
                    @NonNull CLE2Request.CLE2Error cle2Error) {
                if (cle2Error.getErrorCode() != CLE2Request.CLE2Error.CLE2ErrorCode.NONE
                        || cle2OperationResult == null) {
                    Toast.makeText(m_activity, "Error : " + cle2Error.getErrorMessage(),
                                   Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(m_activity, "Operation completed successfully.\n"
                                       + "Number of downloaded objects is " + cle2OperationResult
                                       .getAffectedItemCount(),
                               Toast.LENGTH_SHORT).show();

                // now fetch geometry from local storage
                CLE2Task<List<CLE2Geometry>> fetchTask =
                        dataManager.newFetchLocalLayersTask(Collections.singletonList(layerName));

                fetchTask.start(new CLE2Task.Callback<List<CLE2Geometry>>() {
                    @Override
                    public void onTaskFinished(@Nullable List<CLE2Geometry> cle2Geometries,
                            @NonNull CLE2Request.CLE2Error cle2Error) {
                        if (cle2Error.getErrorCode() != CLE2Request.CLE2Error.CLE2ErrorCode.NONE
                                || cle2Geometries == null) {
                            Toast.makeText(m_activity, "Error : " + cle2Error.getErrorMessage(),
                                           Toast.LENGTH_LONG).show();
                            return;
                        }

                        for (CLE2Geometry geometry : cle2Geometries) {
                            CLE2PointGeometry pointGeometry = (CLE2PointGeometry) geometry;
                            m_geometryList.add(pointGeometry);
                            MapMarker mapMarker = new MapMarker(pointGeometry.getPoint());
                            m_map.addMapObject(mapMarker);
                        }
                    }
                });
            }
        });
    }

    /**
     * Upload all layer data to remote storage
     */
    private void uploadLayer() {
        if (m_geometryList.isEmpty()) {
            Toast.makeText(m_activity, "List of geometries cannot be empty.", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // get instance of the data manager
        CLE2DataManager dataManager = CLE2DataManager.getInstance();

        // create upload layer task
        CLE2Task<CLE2OperationResult> task =
                dataManager.newUploadLayerTask(m_layerEdit.getText().toString(), m_geometryList);

        // start uploading
        task.start(new CLE2Task.Callback<CLE2OperationResult>() {
            @Override
            public void onTaskFinished(@Nullable CLE2OperationResult cle2OperationResult,
                    @NonNull CLE2Request.CLE2Error cle2Error) {

                if (cle2Error.getErrorCode() != CLE2Request.CLE2Error.CLE2ErrorCode.NONE) {
                    Toast.makeText(m_activity, "Error : " + cle2Error.getErrorMessage(),
                                   Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(m_activity, "Operation completed successfully.",
                                   Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addGeometry() {
        // add some geometry to map using map center
        GeoCoordinate center = m_map.getCenter();

        // Create point geometry
        CLE2PointGeometry geometry = new CLE2PointGeometry(center);

        // Create map marker object
        MapMarker mapMarker = new MapMarker(center);

        m_geometryList.add(geometry);
        m_map.addMapObject(mapMarker);
    }

    private void clearMap() {
        m_geometryList.clear();
        m_map.removeAllMapObjects();
    }

    private void purgeLocalStorage() {
        clearMap();

        // get instance of the data manager
        CLE2DataManager dataManager = CLE2DataManager.getInstance();

        // create purge local storage task
        CLE2Task<CLE2OperationResult> task = dataManager.newPurgeLocalStorageTask();

        // start purging
        task.start(new CLE2Task.Callback<CLE2OperationResult>() {
            @Override
            public void onTaskFinished(@Nullable CLE2OperationResult cle2OperationResult,
                    @NonNull CLE2Request.CLE2Error cle2Error) {
                if (cle2Error.getErrorCode() != CLE2Request.CLE2Error.CLE2ErrorCode.NONE) {
                    Toast.makeText(m_activity, "Error : " + cle2Error.getErrorMessage(),
                                   Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(m_activity, "Operation completed successfully.",
                                   Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Demonstration of the proximity search feature.
     * Search for all geometries from the center of the map in the desired search radius.
     */
    private void proximitySearch() {
        clearMap();

        MapCircle searchCircle =
                new MapCircle(m_radiusSeekBar.getProgress() + 1, m_map.getCenter());
        searchCircle.setFillColor(Color.argb(64, 0, 0, 255));
        m_map.addMapObject(searchCircle);

        // create proximity search request using map center as center.
        CLE2ProximityRequest request =
                new CLE2ProximityRequest(m_layerEdit.getText().toString(), m_map.getCenter(),
                                         m_radiusSeekBar.getProgress() + 1);

        // set desired connectivity mode
        // if the connectivity mode is OFFLINE, the geometries will be searched in local storage,
        // otherwise in remote storage.
        request.setConnectivityMode(
                (CLE2Request.CLE2ConnectivityMode) m_connectivityModeSpinner.getSelectedItem());

        // execute the reques
        request.execute(new CLE2Request.CLE2ResultListener() {
            @Override
            public void onCompleted(@Nullable CLE2Result cle2Result, @NonNull String error) {
                if (!error.equalsIgnoreCase("none") || cle2Result == null) {
                    Toast.makeText(m_activity, "Error : " + error, Toast.LENGTH_LONG).show();
                    return;
                }

                for (CLE2Geometry geometry : cle2Result.getGeometries()) {
                    CLE2PointGeometry pointGeometry = (CLE2PointGeometry) geometry;
                    m_geometryList.add(pointGeometry);
                    MapMarker mapMarker = new MapMarker(pointGeometry.getPoint());
                    m_map.addMapObject(mapMarker);
                }
            }
        });
    }
}
