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

package com.here.android.example.route.tta;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.guidance.TrafficUpdater;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapView;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.DynamicPenalty;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteTta;
import com.here.android.mpa.routing.RoutingError;

import java.io.File;
import java.util.List;

/**
 * Main activity which launches map view and handles Android run-time requesting permission.
 */
public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 0x754;

    private MapView m_mapView;
    private Map m_map;
    private Route m_route;
    private TrafficUpdater.RequestInfo m_requestInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_mapView = findViewById(R.id.mapView);
        requestPermissions();
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private void requestPermissions() {
        final int result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (result != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            initMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        final boolean permissionGranted = grantResults.length > 0
                && (requestCode == REQUEST_CODE_ASK_PERMISSIONS
                && grantResults[0] == PackageManager.PERMISSION_GRANTED);

        if (permissionGranted) {
            initMap();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (MapEngine.isInitialized()) {
            TrafficUpdater.getInstance().enableUpdate(false);
        }

        if (m_requestInfo != null) {
            /*  Cancel request by request Id */
            TrafficUpdater.getInstance().cancelRequest(m_requestInfo.getRequestId());
        }
    }

    /**
     * Initialize m_map engine.
     * After initialization set m_map object for {@link MapView}.
     * Also in callback check if initialization completed successfully
     */
    private void initMap() {
        // Set path of isolated disk cache
        String diskCacheRoot = Environment.getExternalStorageDirectory().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
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
            return;
        }

        MapEngine.getInstance().init(new ApplicationContext(getApplicationContext()),
                new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(Error error) {
                        if (error == Error.NONE) {
                            /* get the map object */
                            m_map = new Map();
                            m_mapView.setMap(m_map);

                            /* Start calculating m_route */
                            calculateRoute();
                        }
                    }
                });
    }

    private void calculateTta() {
        /*
         * Receive arrival time for the whole m_route, if you want to get time only for part of
         * m_route pass parameter in bounds 0 <= m_route.getSublegCount()
         */
        final RouteTta ttaExcluding = m_route.getTtaExcludingTraffic(Route.WHOLE_ROUTE);
        final RouteTta ttaIncluding = m_route.getTtaIncludingTraffic(Route.WHOLE_ROUTE);

        final TextView tvInclude = findViewById(R.id.tvTtaInclude);
        tvInclude.setText("Tta included: " + String.valueOf(ttaIncluding.getDuration()));

        final TextView tvExclude = findViewById(R.id.tvTtaExclude);
        tvExclude.setText("Tta excluded: " + String.valueOf(ttaExcluding.getDuration()));
    }

    private void calculateTtaUsingDownloadedTraffic() {
        /* Turn on traffic updates */
        TrafficUpdater.getInstance().enableUpdate(true);

        m_requestInfo = TrafficUpdater.getInstance().request(
                m_route, new TrafficUpdater.Listener() {
                    @Override
                    public void onStatusChanged(TrafficUpdater.RequestState requestState) {
                        final RouteTta ttaDownloaded = m_route.getTtaUsingDownloadedTraffic(
                                Route.WHOLE_ROUTE);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView tvDownload = findViewById(R.id.tvTtaDowload);

                                if (tvDownload != null) {
                                    tvDownload.setText("Tta downloaded: " +
                                            String.valueOf(ttaDownloaded.getDuration()));
                                }
                            }
                        });
                    }
                });
    }

    private void calculateRoute() {
        /* Initialize a CoreRouter */
        CoreRouter coreRouter = new CoreRouter();

        /* For calculating traffic on the m_route */
        DynamicPenalty dynamicPenalty = new DynamicPenalty();
        dynamicPenalty.setTrafficPenaltyMode(Route.TrafficPenaltyMode.OPTIMAL);
        coreRouter.setDynamicPenalty(dynamicPenalty);

        final RoutePlan routePlan = RouteUtil.createRoute();

        coreRouter.calculateRoute(routePlan,
                new RouteUtil.RouteListener<List<RouteResult>, RoutingError>() {
                    @Override
                    public void onCalculateRouteFinished(List<RouteResult> routeResults,
                                                         RoutingError routingError) {
                        /* Calculation is done. Let's handle the result */
                        if (routingError == RoutingError.NONE) {
                            /* Get route fro results */
                            m_route = routeResults.get(0).getRoute();

                            /* Create a MapRoute so that it can be placed on the map */
                            final MapRoute map_route = new MapRoute(routeResults.get(0).getRoute());

                            /* Add the MapRoute to the map */
                            m_map.addMapObject(map_route);

                            /*
                             * We may also want to make sure the map view is orientated properly so
                             * the entire route can be easily seen.
                             */
                            m_map.zoomTo(m_route.getBoundingBox(), Map.Animation.NONE, 15);

                            /* Get TTA */
                            calculateTta();
                            calculateTtaUsingDownloadedTraffic();
                        }
                    }
                });
    }
}
