/*
 * Copyright (c) 2011-2018 HERE Europe B.V.
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

package com.here.android.example.geocoding;

import java.io.File;
import java.util.List;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest2;
import com.here.android.mpa.search.GeocodeResult;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.ReverseGeocodeRequest2;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class demonstrates the usage of Geocoding and Reverse Geocoding request APIs
 */
public class MainView {
    private Activity m_activity;
    private TextView m_resultTextView;

    public MainView(Activity activity) {
        m_activity = activity;
        initMapEngine();
        initUIElements();
    }

    private void initMapEngine() {
        // Set path of isolated disk cache
        String diskCacheRoot = Environment.getExternalStorageDirectory().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = m_activity.getPackageManager().getApplicationInfo(m_activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(), "Failed to find intent name, NameNotFound: " + e.getMessage());
        }

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot, intentName);
        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            /*
             * Even though we don't display a map view in this application, in order to access any
             * services that HERE Android SDK provides, the MapEngine must be initialized as the
             * prerequisite.
             */
            MapEngine.getInstance().init(new ApplicationContext(m_activity), new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(Error error) {
                    Toast.makeText(m_activity, "Map Engine initialized with error code:" + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initUIElements() {
        m_resultTextView = (TextView) m_activity.findViewById(R.id.resultTextView);
        Button geocodeButton = (Button) m_activity.findViewById(R.id.geocodeRequestBtn);
        Button revGeocodeButton = (Button) m_activity.findViewById(R.id.revgeocodeRequestBtn);

        geocodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                triggerGeocodeRequest();
            }
        });

        revGeocodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                triggerRevGeocodeRequest();
            }
        });

    }

    private void triggerGeocodeRequest() {
        m_resultTextView.setText("");
        /*
         * Create a GeocodeRequest object with the desired query string, then set the search area by
         * providing a GeoCoordinate and radius before executing the request.
         */
        String query = "4350 Still Creek Dr,Burnaby";
        GeocodeRequest2 geocodeRequest = new GeocodeRequest2(query);
        GeoCoordinate coordinate = new GeoCoordinate(49.266787, -123.056640);
        geocodeRequest.setSearchArea(coordinate, 5000);
        geocodeRequest.execute(new ResultListener<List<GeocodeResult>>() {
            @Override
            public void onCompleted(List<GeocodeResult> results, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    /*
                     * From the result object, we retrieve the location and its coordinate and
                     * display to the screen. Please refer to HERE Android SDK doc for other
                     * supported APIs.
                     */
                    StringBuilder sb = new StringBuilder();
                    for (GeocodeResult result : results) {
                        sb.append(result.getLocation().getCoordinate().toString());
                        sb.append("\n");
                    }
                    updateTextView(sb.toString());
                } else {
                    updateTextView("ERROR:Geocode Request returned error code:" + errorCode);
                }
            }
        });
    }

    private void triggerRevGeocodeRequest() {
        m_resultTextView.setText("");
        /* Create a ReverseGeocodeRequest object with a GeoCoordinate. */
        GeoCoordinate coordinate = new GeoCoordinate(49.25914, -123.00777);
        ReverseGeocodeRequest2 revGecodeRequest = new ReverseGeocodeRequest2(coordinate);
        revGecodeRequest.execute(new ResultListener<Location>() {
            @Override
            public void onCompleted(Location location, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    /*
                     * From the location object, we retrieve the address and display to the screen.
                     * Please refer to HERE Android SDK doc for other supported APIs.
                     */
                    updateTextView(location.getAddress().toString());
                } else {
                    updateTextView("ERROR:RevGeocode Request returned error code:" + errorCode);
                }
            }
        });
    }

    private void updateTextView(final String txt) {
        m_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_resultTextView.setText(txt);
            }
        });
    }
}
