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

package com.here.android.example.ftcr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.kevinsawicki.http.HttpRequest;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolyline;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.ftcr.FTCRRoute;
import com.here.android.mpa.ftcr.FTCRRouteOptions;
import com.here.android.mpa.ftcr.FTCRRoutePlan;
import com.here.android.mpa.ftcr.FTCRRouter;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.FTCRMapRoute;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapPolyline;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.graphics.Color.argb;

/**
 * This class encapsulates the properties and functionality of the Map view.A route calculation from
 * HERE Burnaby office to Langley BC is also being handled in this class
 */
public class MapFragmentView {
    private static final String TAG = MapFragmentView.class.getName();
    private AndroidXMapFragment m_mapFragment;
    private Button m_calculateRouteButton;
    private AppCompatActivity m_activity;
    private Map m_map;
    private FTCRMapRoute m_mapRoute;

    private GeoCoordinate[] m_fakeRoute;
    private GeoCoordinate m_startPoint;
    private GeoCoordinate m_endPoint;

    private static final String URL_UPLOAD_ROUTE =
            "https://fleet.api.here.com/2/overlays/upload.json";
    public static final String OVERLAY_NAME = "OVERLAY-EXAMPLE";

    private String m_appId;
    private String m_appToken;
    private String m_overlayName = OVERLAY_NAME;
    private FTCRRouter m_router;
    private FTCRRouter.CancellableTask m_routeTask;

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        try {
            ApplicationInfo app = m_activity.getPackageManager()
                    .getApplicationInfo(m_activity.getPackageName(), PackageManager.GET_META_DATA);
            m_appId = app.metaData.getString("com.here.android.maps.appid");
            m_appToken = app.metaData.getString("com.here.android.maps.apptoken");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            m_activity.finish();
        }
        initMapFragment();
        /*
         * We use a button in this example to control the route calculation
         */
        initCreateRouteButton();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager()
                .findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = getMapFragment();

        if (m_mapFragment != null) {
            /* Initialize the AndroidXMapFragment, results will be given via the called back. */
            m_mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {

                    if (error == Error.NONE) {
                        /* Initialize a FTCRRouter */
                        m_router = new FTCRRouter();

                        /* get the map object */
                        m_map = m_mapFragment.getMap();

                        addFakeRoute();

                        m_map.setCenter(m_startPoint, Map.Animation.NONE);

                        /* Set the zoom level to the average between min and max zoom level. */
                        m_map.setZoomLevel((m_map.getMaxZoomLevel() + m_map.getMinZoomLevel()) / 2);
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

    private void initCreateRouteButton() {
        m_calculateRouteButton = (Button) m_activity.findViewById(R.id.button);

        m_calculateRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Clear map if previous results are still on map, otherwise proceed to creating
                 * route
                 */
                if (m_map != null && m_mapRoute != null) {
                    m_map.removeMapObject(m_mapRoute);
                    m_mapRoute = null;
                } else {
                    /*
                     * The route calculation requires local map data.Unless there is pre-downloaded
                     * map data on device by utilizing MapLoader APIs, it's not recommended to
                     * trigger the route calculation immediately after the MapEngine is
                     * initialized.The INSUFFICIENT_MAP_DATA error code may be returned by
                     * CoreRouter in this case.
                     *
                     */
                    calculateRoute();
                }
            }
        });

    }

    private void uploadRouteGeometryOnServer() {

        makeRequest(m_fakeRoute, new Callback() {
            @Override
            public void onResponse(final String message) {
                Log.i(TAG, "Response: \n" + message);
                final String userMessage;
                if (message.contains("error")) {
                    userMessage = "Could not upload layer on the server\n" + message;
                } else {
                    userMessage = "Layer successfully uploaded on the server";
                }

                m_activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(m_activity, userMessage, Toast.LENGTH_LONG).show();
                        m_calculateRouteButton.setEnabled(true);
                    }
                });
            }
        });
    }

    private void calculateRoute() {
        /*
         * Initialize a RouteOption. HERE Mobile SDK allow users to define their own parameters
         * for the route calculation, including transport modes, route types and
         * route restrictions etc. Please refer to API doc for full list of APIs
         */
        FTCRRouteOptions routeOptions = new FTCRRouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(FTCRRouteOptions.TransportMode.CAR);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(FTCRRouteOptions.Type.SHORTEST);
        /* Define waypoints for the route */
        RouteWaypoint startPoint = new RouteWaypoint(m_startPoint);

        RouteWaypoint destination = new RouteWaypoint(m_endPoint);

        /* Initialize a RoutePlan */
        List<RouteWaypoint> routePoints = new ArrayList<>();
        routePoints.add(startPoint);
        routePoints.add(destination);
        FTCRRoutePlan routePlan = new FTCRRoutePlan(routePoints, routeOptions);
        /*
          Set the name of the map overlay. It has to be the same that is used for uploading
          the custom roads to the fleet telematics server.
         */
        routePlan.setOverlay(OVERLAY_NAME);
        if (m_routeTask != null) {
            m_routeTask.cancel();
        }

        m_routeTask = m_router.calculateRoute(routePlan, new FTCRRouter.Listener() {
            @Override
            public void onCalculateRouteFinished(@NonNull List<FTCRRoute> routeResults,
                                                 @NonNull FTCRRouter.ErrorResponse errorResponse) {
                /* Calculation is done. Let's handle the result */
                if (errorResponse.getErrorCode() == RoutingError.NONE) {
                    if (routeResults.get(0) != null) {
                        /* Create a FTCRMapRoute so that it can be placed on the map */
                        m_mapRoute = new FTCRMapRoute(routeResults.get(0));

                        /* Add the FTCRMapRoute to the map */
                        m_map.addMapObject(m_mapRoute);

                        /*
                         * We may also want to make sure the map view is orientated properly
                         * so the entire route can be easily seen.
                         */
                        GeoBoundingBox gbb = routeResults.get(0).getBoundingBox();
                        m_map.zoomTo(gbb, Map.Animation.NONE,
                                Map.MOVE_PRESERVE_ORIENTATION);
                    } else {
                        Toast.makeText(m_activity,
                                "Error:route results returned is not valid",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(m_activity,
                            "Error:route calculation returned error code: "
                                    + errorResponse.getErrorCode()
                                    + ",\nmessage: " + errorResponse.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Sends the fake route into the server and displays on the map.
    // Note, layer uploading functionality is not part of the Mobile SDK 3.X, this was added
    // added for demo purposes.
    // Refer to the  official fleet telematics documentation to properly implement uploading
    // layer on the server.
    private void addFakeRoute() {
        m_fakeRoute = new GeoCoordinate[]
                {
                        new GeoCoordinate(52.51517456956208, 13.35019220598042),
                        new GeoCoordinate(52.51693326048553, 13.350012330338359),
                        new GeoCoordinate(52.51796909607947, 13.350633010268211),
                        new GeoCoordinate(52.51875473186374, 13.351499531418085),
                        new GeoCoordinate(52.519415309652686, 13.350413320586085),
                        new GeoCoordinate(52.51992283388972, 13.351393081247807)
                };

        m_startPoint = new GeoCoordinate(52.50920455902815, 13.351180600002408);
        m_endPoint = new GeoCoordinate(52.521314984187484, 13.350122384727001);
        uploadRouteGeometryOnServer();
        GeoPolyline fakeRoutePolyLine = new GeoPolyline();
        fakeRoutePolyLine.add(Arrays.asList(m_fakeRoute));
        MapPolyline mapFakeRoutePolyline = new MapPolyline(fakeRoutePolyLine);
        mapFakeRoutePolyline.setLineWidth(15);
        mapFakeRoutePolyline.setLineColor(
                argb(255, 185, 63, 2));
        mapFakeRoutePolyline.setPatternStyle(MapPolyline.PatternStyle.DASH_PATTERN);
        m_map.addMapObject(mapFakeRoutePolyline);

        MapMarker startMapMarker = new MapMarker(m_startPoint);
        MapMarker endMapMarker = new MapMarker(m_endPoint);
        m_map.addMapObject(startMapMarker);
        m_map.addMapObject(endMapMarker);
    }

    public interface Callback {
        void onResponse(String message);
    }

    void makeRequest(GeoCoordinate[] list, final Callback callback) {
        final HttpRequest httpRequest = HttpRequest.post(
                URL_UPLOAD_ROUTE
                        + "?app_id=" + m_appId
                        + "&app_code=" + m_appToken
                        + "&map_name=" + m_overlayName
                        + "&storage=readonly");

        final StringBuilder overlaySpec = new StringBuilder();
        overlaySpec.append("[");
        StringBuilder pathStr = new StringBuilder();
        pathStr.append("[");
        for (GeoCoordinate geoCoordinate : list) {
            pathStr.append("[")
                    .append(geoCoordinate.getLatitude())
                    .append(",")
                    .append(geoCoordinate.getLongitude())
                    .append("]")
                    .append(",");
        }
        pathStr.deleteCharAt(pathStr.length() - 1);
        pathStr.append("]");
        String shape = "{\"op\":\"create\",\"shape\":" + pathStr + "}";
        overlaySpec.append(shape).append(",");
        overlaySpec.deleteCharAt(overlaySpec.length() - 1);
        overlaySpec.append("]");

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (MapFragmentView.this.m_activity.isDestroyed()) {
                    return;
                }

                httpRequest.part("overlay_spec", overlaySpec.toString());
                if (httpRequest.created()) {
                    callback.onResponse(httpRequest.body());
                } else {
                    String message;
                    try {
                        message = "Code:" + httpRequest.code()
                                + ", " + httpRequest.body();
                    } catch (HttpRequest.HttpRequestException e) {
                        message = e.getMessage();
                    }
                    callback.onResponse(message);

                }
            }
        }).start();
    }
}
