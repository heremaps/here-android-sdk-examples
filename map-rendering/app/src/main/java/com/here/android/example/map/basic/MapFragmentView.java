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

package com.here.android.example.map.basic;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.AndroidXMapFragment;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * This class encapsulates the properties and functionality of the Map view.
 */
public class MapFragmentView {
    private AndroidXMapFragment m_mapFragment;
    private AppCompatActivity m_activity;
    private Map m_map;

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        initMapFragment();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
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
                         * It also can be set to the current location ,which needs to be delivered by the PositioningManager.
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
}
