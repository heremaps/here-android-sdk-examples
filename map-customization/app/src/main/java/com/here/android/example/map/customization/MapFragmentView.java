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

package com.here.android.example.map.customization;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.customization.CustomizableScheme;
import com.here.android.mpa.mapping.customization.CustomizableVariables;
import com.here.android.mpa.mapping.customization.ZoomRange;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MapFragmentView {

    private AndroidXMapFragment m_mapFragment;
    private AppCompatActivity m_activity;
    private Map m_map;
    private CustomizableScheme m_colorScheme;
    private CustomizableScheme m_floatScheme;
    private Button m_colorBtn;
    private Button m_floatBtn;

    private final String m_colorSchemeName = "colorScheme";
    private final String m_floatSchemeName = "floatScheme";

    /**
     * Initial UI button on map fragment view. It includes "change color property button" and
     * "change float property button" to add/change customizable scheme
     * 
     * @param activity
     */
    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        initMapFragment();
        initColorPropertyButton();
        initFloatPropertyButton();
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
                         * Set the map center to Berlin Germany.
                         */
                        m_map.setCenter(new GeoCoordinate(52.500556, 13.398889, 0.0),
                                Map.Animation.NONE);

                        /* Set the zoom level. */
                        m_map.setZoomLevel(8);
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
     * Initialize change float property button to add customizable scheme which customizes country
     * boundary 's width.
     */
    private void initFloatPropertyButton() {
        m_floatBtn = (Button) m_activity.findViewById(R.id.float_button);

        m_floatBtn.setOnClickListener(new View.OnClickListener() {
            // if customizable scheme named "floatScheme" does not exist yet, create one
            // and set customizable scheme 's country boundary width to be 10.0f for specific range
            // and set it to be current map scheme.
            @Override
            public void onClick(View v) {
                // if customizable scheme named with floatScheme does not exist, create one
                if (m_map != null && m_map.getCustomizableScheme(m_floatSchemeName) == null) {
                    m_map.createCustomizableScheme(m_floatSchemeName, m_map.getMapScheme());
                    m_floatScheme = m_map.getCustomizableScheme(m_floatSchemeName);
                }
                // create zoom range
                ZoomRange range = new ZoomRange(m_map.getMinZoomLevel(), m_map.getMaxZoomLevel());
                // set country property'width to be 10.0f
                m_floatScheme.setVariableValue(CustomizableVariables.CountryBoundary.WIDTH, 10.0f,
                        range);
                // set customizable scheme to be current map scheme.
                m_map.setMapScheme(m_floatScheme);
                // set current center and zoom level to have a better view.
                m_map.setCenter(new GeoCoordinate(52.500556, 13.398889, 0.0), Map.Animation.NONE);
                m_map.setZoomLevel(5);
            }
        });
    }

    /**
     * Initialize change color property button to add customizable scheme which customizes airport
     * area's color.
     */
    private void initColorPropertyButton() {
        m_colorBtn = (Button) m_activity.findViewById(R.id.color_button);

        m_colorBtn.setOnClickListener(new View.OnClickListener() {
            // if customizable scheme named "colorScheme" does not exist yet, create one
            // and set customizable scheme 's airport area color to be red for specific range
            // and set it to be current map scheme.
            @Override
            public void onClick(View v) {
                if (m_map != null && m_map.getCustomizableScheme(m_colorSchemeName) == null) {
                    m_map.createCustomizableScheme(m_colorSchemeName, Map.Scheme.NORMAL_DAY);
                    m_colorScheme = m_map.getCustomizableScheme(m_colorSchemeName);
                }
                ZoomRange range = new ZoomRange(0.0, 20.0);
                m_colorScheme.setVariableValue(CustomizableVariables.AirportArea.COLOR, Color.RED,
                        range);
                m_map.setMapScheme(m_colorScheme);
                // set current center and zoom level to have a better view.
                m_map.setCenter(new GeoCoordinate(52.5588642, 13.2850454, 0.0), Map.Animation.NONE);
                m_map.setZoomLevel(12);
            }
        });
    }
}
