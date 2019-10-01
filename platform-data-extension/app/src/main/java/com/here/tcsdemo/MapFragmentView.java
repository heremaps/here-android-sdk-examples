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

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolyline;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapPolyline;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.pde.PlatformDataItem;
import com.here.android.mpa.pde.PlatformDataItemCollection;
import com.here.android.mpa.pde.PlatformDataRequest;
import com.here.android.mpa.pde.PlatformDataResult;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class MapFragmentView {

    private static final String TAG = MapFragmentView.class.getName();
    private final static String ADAS_LAYER = "ADAS_ATTRIB_FC5";
    private final static String ROAD_GEOM_LAYER = "ROAD_GEOM_FC5";
    private final static String CITY_POI_LAYER = "CITY_POI_0";
    private final static int HIGH_COLOR = Color.GREEN;
    private final static int LOW_COLOR = Color.RED;
    private final Set<Long> pvids = new HashSet<>();
    private AndroidXMapFragment mapFragment;
    private AppCompatActivity activity;
    private Map map;

    /**
     * Initialization of UI buttons on map fragment view.
     *
     * @param activity
     */
    public MapFragmentView(AppCompatActivity activity) {
        this.activity = activity;
        initMapFragment();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) activity
                .getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /*
         * Locate the mapFragment UI element
         */
        mapFragment = getMapFragment();

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                activity.getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                "{YOUR_INTENT_NAME}");

        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            if (mapFragment != null) {
                /*
                 * Initialize the MapFragment, results will be given via the called back.
                 */
                mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                        if (error == Error.NONE) {
                            /*
                             * If no error returned from map fragment initialization, the map will be
                             * rendered on screen at this moment.Further actions on map can be provided
                             * by calling Map APIs.
                             */
                            map = mapFragment.getMap();
                            /*
                             * Set the map center to Berlin Germany.
                             */
                            map.setCenter(new GeoCoordinate(52.517031, 13.389015),
                                    Map.Animation.NONE);

                            /*
                             * Set the zoom level.
                             */
                            map.setZoomLevel(3.95);

                            initCapitalsButton();
                            initRoadSlopesButton();
                        } else {
                            /*
                             * Process errors during initialization.
                             */
                            Log.e(TAG, "Error on map fragment initialization: " + error);
                            Log.e(TAG, error.getDetails());
                            Log.e(TAG, error.getStackTrace());
                        }
                    }
                });
            }
        }
    }

    /**
     * Method contains logic for extracting data about capitals and placing it on a map
     */
    private void showCapitals() {
        /*
         * Create an image for marker
         */
        final int[] colorArray = new int[25 * 25];
        for (int i = 0; i < colorArray.length; i++) {
            colorArray[i] = Color.GREEN;
        }
        Bitmap bitmap = Bitmap.createBitmap(colorArray, 25, 25, Bitmap.Config.ARGB_8888);
        final Image image = new Image();
        image.setBitmap(bitmap);

        /*
         * Create list of PDE layers to extract
         */
        Set<String> layers = new HashSet<>(Arrays.asList(CITY_POI_LAYER));
        GeoBoundingBox bbox = map.getBoundingBox();

        /*
         * Check that bounding box is valid
         */
        if (bbox == null || bbox.isEmpty()) {
            Log.e(TAG, "PDE bbox is null or empty!");
            Toast.makeText(this.activity.getApplicationContext(),
                    "Current zoom level is too low. Please zoom closer.",
                    Toast.LENGTH_LONG).show();
        } else {
            /*
             * Create and send PDE request
             */
            final PlatformDataRequest request = PlatformDataRequest.createBoundingBoxRequest(layers, bbox);
            request.execute(new PlatformDataRequest.Listener<PlatformDataResult>() {
                @Override
                public void onCompleted(PlatformDataResult platformDataResult, PlatformDataRequest.Error error) {
                    if (error == null) {
                        /*
                         * Process PDE request response
                         */
                        PlatformDataItemCollection result = platformDataResult.get(CITY_POI_LAYER);
                        List<MapObject> markers = new ArrayList<>();
                        for (java.util.Map<String, String> entry : result.extract()) {
                            double lat = Double.parseDouble(entry.get("LAT")) / 100_000;
                            double lon = Double.parseDouble(entry.get("LON")) / 100_000;
                            MapMarker marker = new MapMarker();
                            marker.setCoordinate(new GeoCoordinate(lat, lon));
                            marker.setIcon(image);
                            markers.add(marker);
                        }
                        /*
                         * Add list of map markers on map
                         */
                        map.addMapObjects(markers);
                        /*
                         * Set the zoom level.
                         */
                        map.setZoomLevel(3.95);
                    } else {
                        /*
                         * Process PDE request error
                         */
                        Log.i(TAG, "PDE error: " + error.getFaultCode());
                        Log.i(TAG, "PDE error: " + error.getMessage());
                        Log.i(TAG, "PDE error: " + error.getResponseCode());
                        Log.i(TAG, "PDE error: " + error.getType().toString());
                    }
                }
            });
        }
    }

    /**
     * Method contains logic for extracting data about slopes and showing it on a route
     */
    private void roadSlopes() {
        final java.util.Map<String, Integer> slopesMap = new HashMap<>();
        final java.util.Map<String, List<GeoCoordinate>> geometryMap = new HashMap<>();

        /*
         * Create a route plan for route calculation
         */
        RoutePlan rp = new RoutePlan();
        rp.addWaypoint(new RouteWaypoint(new GeoCoordinate(37.79513, -122.47603)));
        rp.addWaypoint(new RouteWaypoint(new GeoCoordinate(37.78166, -122.44450)));

        CoreRouter router = new CoreRouter();
        /*
         * For getting list of Link IDs routing should be forced to work online
         */
        router.setConnectivity(CoreRouter.Connectivity.ONLINE);
        router.calculateRoute(rp, new CoreRouter.Listener() {
            @Override
            public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
                if (routingError == RoutingError.NONE) {
                    Route route = list.get(0).getRoute();

                    /*
                     * Show route on the map and zoom to the route
                     */
                    GeoBoundingBox bbox = route.getBoundingBox();
                    map.addMapObject(new MapRoute(route));
                    map.zoomTo(bbox, Map.Animation.NONE, 0);

                    /*
                     * Get list of Link IDs for the route
                     */
                    final List<Long> ids = route.getPermanentLinkIds();
                    for (Long id : ids) {
                        pvids.add(id);
                    }

                    Set<String> layers = new HashSet<>(Arrays.asList(ADAS_LAYER, ROAD_GEOM_LAYER));
                    PlatformDataRequest request = PlatformDataRequest.createBoundingBoxRequest(layers, bbox);
                    request.execute(new PlatformDataRequest.Listener<PlatformDataResult>() {
                        @Override
                        public void onCompleted(PlatformDataResult data, PlatformDataRequest.Error error) {
                            if (error == null) {
                                /*
                                 * Process route geometry from PDE
                                 */
                                PlatformDataItemCollection roadDataCollection = data.get(ROAD_GEOM_LAYER);
                                for (PlatformDataItem item : roadDataCollection) {
                                    geometryMap.put(item.getLinkId(), item.getCoordinates());
                                }

                                /*
                                 * Process ADAS data from PDE
                                 */
                                PlatformDataItemCollection adasDataCollection = data.get(ADAS_LAYER);
                                for (PlatformDataItem item : adasDataCollection) {
                                    List<String> values = new ArrayList<>();
                                    /*
                                     * Split slopes data
                                     */
                                    StringTokenizer tokenizer = new StringTokenizer(item.get("SLOPES"));
                                    while (tokenizer.hasMoreTokens()) {
                                        String token = tokenizer.nextToken(",");
                                        /*
                                         * Filter out invalid data
                                         */
                                        if (!token.equals("NULL") && !token.equals("1000000000")) {
                                            values.add(token);
                                        }
                                    }

                                    /*
                                     * Mark slopes data if it contains either high or low value
                                     */
                                    int max = 0;
                                    int min = 0;
                                    for (String str : values) {
                                        int temp = Integer.valueOf(str);
                                        if (temp > max) max = temp;
                                        if (temp < min) min = temp;
                                    }
                                    if ((min * -1) > max && min <= -5_000)
                                        slopesMap.put(item.getLinkId(), LOW_COLOR);
                                    else if ((min * -1) < max && max >= 5_000)
                                        slopesMap.put(item.getLinkId(), HIGH_COLOR);
                                }

                                /*
                                 * Process list of geometry
                                 * find route segment with high or low slopes value
                                 * and add this geometry to the list
                                 */
                                List<MapObject> polylines = new ArrayList<>();
                                for (java.util.Map.Entry<String, List<GeoCoordinate>> entry : geometryMap.entrySet()) {
                                    if (pvids.contains(Long.parseLong(entry.getKey()))) {
                                        GeoPolyline polyline = new GeoPolyline();
                                        polyline.add(entry.getValue());
                                        MapPolyline line = new MapPolyline(polyline);
                                        if (slopesMap.containsKey(entry.getKey())) {
                                            line.setLineColor(slopesMap.get(entry.getKey()));
                                            line.setLineWidth(15);
                                            polylines.add(line);
                                        }
                                    }
                                }

                                /*
                                 * Show a list of slopes geometry on the map
                                 */
                                map.addMapObjects(polylines);
                            } else {
                                /*
                                 * Process PDE request error
                                 */
                                Log.i(TAG, "PDE error: " + error.getFaultCode());
                                Log.i(TAG, "PDE error: " + error.getMessage());
                                Log.i(TAG, "PDE error: " + error.getResponseCode());
                                Log.i(TAG, "PDE error: " + error.getType().toString());
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "Routing error: " + routingError);
                }
            }

            @Override
            public void onProgress(int i) {
                Log.i(TAG, String.format("Route calculation progress: %d%%", i));
            }
        });

    }

    private void initCapitalsButton() {
        Button capitalsButton = (Button) activity.findViewById(R.id.pde_first_button);
        capitalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCapitals();
            }
        });
    }

    private void initRoadSlopesButton() {
        Button roadSlopesButton = (Button) activity.findViewById(R.id.pde_second_button);
        roadSlopesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roadSlopes();
            }
        });
    }

}
