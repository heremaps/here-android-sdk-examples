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

package com.here.android.example.navigation.ftcr;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.ftcr.FTCRNavigationManager;
import com.here.android.mpa.ftcr.FTCRRoute;
import com.here.android.mpa.ftcr.FTCRRouteOptions;
import com.here.android.mpa.ftcr.FTCRRoutePlan;
import com.here.android.mpa.ftcr.FTCRRouter;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.NavigationManager.NavigationManagerEventListener;
import com.here.android.mpa.guidance.NavigationManager.NavigationState;
import com.here.android.mpa.guidance.NavigationManager.RerouteListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapView;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RouteWaypoint.Type;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NavigationController {
    private static final String TAG = "MY_TAG";

    // Used to sparse FTCR route geometry and create waypoints for deafult route calculation.
    // Too little value will cause higher number of waypoints,
    // more than 2000 waypoints can cause route calculation failure.
    private static final int MIN_DISTANCE_BETWEEN_WAYPOINTS = 300;// meters

    // Distance to compare FTCR route geomerty coordinate and waypoint stopover coordinate
    // to understand whether geo point can be considered as waypoint stopover.
    private static final int THRESHOLD_DST_TO_STOPOVER = 2;// meters

    private FTCRRouter ftcrRouter;
    private FTCRRouteOptions ftcrRouteOptions;
    private FTCRRouter.CancellableTask ftcrRoutingTask;
    private FTCRNavigationManager ftcrNavigationManager;

    private MapRoute defaultMapRoute;
    private RouteOptions defaultRouteOptions;
    private CoreRouter defeultCoreRouter;
    private NavigationManager defaultNavigationManager;
    private RerouteListener defaultRerouteListener;
    private NavigationManagerEventListener defaultNavEventListener;

    private List<RouteWaypoint> originalWaypointList;
    private int currentReachedStopoverIndex;

    private final MapView mapView;
    private final Activity activity;
    private Map map;

    public NavigationController(Activity activity) {
        this.activity = activity;
        this.mapView = activity.findViewById(R.id.map_view);
        initMapEngine();
    }

    void onResume() {
        mapView.onResume();
    }

    void onPause() {
        mapView.onPause();
    }

    void onDestroy() {
        freeResources();
    }

    private void initMapEngine() {
        MapEngine.getInstance().init(new ApplicationContext(activity.getApplicationContext()),
                new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(Error error) {
                        if (error == Error.NONE) {
                            onEgineInited();
                        } else {
                            Log.e(TAG, "onEngineInitializationCompleted with error " + error);
                        }
                    }
                });
    }

    private void onEgineInited() {
        map = new Map();
        mapView.setMap(map);

        ftcrRouter = new FTCRRouter();
        ftcrNavigationManager = new FTCRNavigationManager();

        defeultCoreRouter = new CoreRouter();
        defaultNavigationManager = NavigationManager.getInstance();

        createRerouteListener();
        createdefaultNavEventListener();

        // Use STOP waypoint type in originalWaypointList.
        // Stopover is waypoint that must be visited, it is used in SDK calbacks and during voice guidance.
        // Via waypoint is only used in initial route calculation, during recalculation all via point will be removed.
        originalWaypointList = Collections.unmodifiableList(Arrays.asList(
                new RouteWaypoint(new GeoCoordinate(52.516224, 13.376820), Type.STOP_WAYPOINT),
                new RouteWaypoint(new GeoCoordinate(52.513934, 13.382110), Type.STOP_WAYPOINT),
                new RouteWaypoint(new GeoCoordinate(52.512247, 13.391867), Type.STOP_WAYPOINT)));

        buildRouteOptions();
        calculateFTCRRoute(originalWaypointList);

        showWaypointsOnMap();
    }

    void showWaypointsOnMap() {
        map.setCenter(originalWaypointList.get(0).getOriginalPosition(),
                Map.Animation.NONE, 18.0d, 0, 0);

        for (RouteWaypoint routeWaypoint : originalWaypointList) {
            map.addMapObject(new MapMarker(routeWaypoint.getNavigablePosition()));
        }
    }

    private void createdefaultNavEventListener() {
        defaultNavEventListener = new NavigationManagerEventListener() {
            @Override
            public void onStopoverReached(int index) {
                Log.d(TAG, "onStopoverReached() called with: index = [" + index + "]");
                currentReachedStopoverIndex++;
            }
        };
    }

    private void createRerouteListener() {
        defaultRerouteListener = new RerouteListener() {
            @Override
            public void onRerouteBegin() {
                Log.d(TAG, "onRerouteBegin() called");
                defaultNavigationManager.pause();

                ArrayList<RouteWaypoint> newWaypoints = new ArrayList<>(originalWaypointList
                        .subList(currentReachedStopoverIndex + 1, originalWaypointList.size()));

                // add current position as start waypoint, also use car heading
                GeoPosition geoPosition = PositioningManager.getInstance().getLastKnownPosition();
                RouteWaypoint startWaypoint = new RouteWaypoint(geoPosition.getCoordinate(), Type.STOP_WAYPOINT);
                startWaypoint.setCourse(geoPosition.getHeading());
                newWaypoints.add(0, startWaypoint);

                calculateFTCRRoute(newWaypoints);
            }

        };
    }

    private void calculateFTCRRoute(final List<RouteWaypoint> waypointList) {
        FTCRRoutePlan ftcrRoutePlan = new FTCRRoutePlan(waypointList, ftcrRouteOptions);
        ftcrRoutingTask = ftcrRouter.calculateRoute(ftcrRoutePlan, new FTCRRouter.Listener() {
            @Override
            public void onCalculateRouteFinished(@NonNull final List<FTCRRoute> routes,
                                                 @NonNull FTCRRouter.ErrorResponse error) {
                if (error.getErrorCode() != RoutingError.NONE || routes.isEmpty()) {
                    Log.e(TAG, "onFTCRCalculateRouteFinished with error "
                            + error.getMessage() + ", " + error.getErrorCode());
                    return;
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onFTCRRouteCalculated(routes.get(0), waypointList);
                    }
                });
            }
        });
    }

    private void calculateDefaultRoute(List<RouteWaypoint> waypointList) {
        Log.d(TAG, "calculateDefaultRoute() called ");
        RoutePlan routePlan = new RoutePlan();
        routePlan.setRouteOptions(defaultRouteOptions);
        for (RouteWaypoint routeWaypoint : waypointList) {
            routePlan.addWaypoint(routeWaypoint);
        }

        defeultCoreRouter.calculateRoute(routePlan,
                new Router.Listener<List<RouteResult>, RoutingError>() {
                    @Override
                    public void onProgress(int percentage) {
                        // callback is used only for offline calculation
                    }

                    @Override
                    public void onCalculateRouteFinished(@NonNull final List<RouteResult> response,
                                                         @NonNull RoutingError error) {
                        if (error != RoutingError.NONE || response.isEmpty()) {
                            Log.e(TAG, "onDefaultCalculateRouteFinished with error " + error);
                            return;
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onDefaultRouteCalculated(response.get(0).getRoute());
                            }
                        });
                    }
                });

    }

    private void onFTCRRouteCalculated(FTCRRoute route, List<RouteWaypoint> stopovers) {
        List<RouteWaypoint> waypoints = createWaypointsFromFTCRGeometry(route.getGeometry(), stopovers);
        calculateDefaultRoute(waypoints);
    }

    private void onDefaultRouteCalculated(Route route) {
        if (defaultNavigationManager.getRunningState() == NavigationState.PAUSED) {
            resumeNavigationManagerWithNewRoute(route);
        } else {
            startDefaultNavigation(route);
        }
    }

    private void buildRouteOptions() {
        defaultRouteOptions = new RouteOptions();
        ftcrRouteOptions = new FTCRRouteOptions();

        defaultRouteOptions.setTransportMode(RouteOptions.TransportMode.TRUCK);
        ftcrRouteOptions.setTransportMode(FTCRRouteOptions.TransportMode.TRUCK);

        defaultRouteOptions.setTruckHeight(3.0f);
        ftcrRouteOptions.setVehicleHeight(3.0f);

        // ... and so on
    }


    private void startDefaultNavigation(Route route) {
        Log.d(TAG, "startDefaultNavigation");
        defaultMapRoute = new MapRoute(route);
        map.addMapObject(defaultMapRoute);
        map.getPositionIndicator().setVisible(true);

        defaultNavigationManager.setMap(map);
        defaultNavigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);
        // Use one of the gps simulation app(e.g. Lockito) with following coordinates from
        // GPX file https://pastebin.pl/view/0be4bad7
        defaultNavigationManager.startNavigation(route);
        defaultNavigationManager.addRerouteListener(
                new WeakReference<RerouteListener>(defaultRerouteListener));
        defaultNavigationManager.addNavigationManagerEventListener(
                new WeakReference<NavigationManagerEventListener>(defaultNavEventListener));
    }

    private void resumeNavigationManagerWithNewRoute(Route route) {
        map.removeMapObject(defaultMapRoute);
        defaultMapRoute = new MapRoute(route);
        map.addMapObject(defaultMapRoute);
        defaultNavigationManager.setRoute(route);
        defaultNavigationManager.resume();
    }

    private void freeResources() {
        if (ftcrRoutingTask != null) {
            ftcrRoutingTask.cancel();
        }
        if (defeultCoreRouter != null && defeultCoreRouter.isBusy()) {
            defeultCoreRouter.cancel();
        }
        if (defaultNavigationManager != null) {
            defaultNavigationManager.removeRerouteListener(defaultRerouteListener);
            defaultNavigationManager.removeNavigationManagerEventListener(defaultNavEventListener);
            defaultNavigationManager.stop();
        }
    }

    private List<RouteWaypoint> createWaypointsFromFTCRGeometry(List<GeoCoordinate> ftcrRouteGeometry,
                                                                List<RouteWaypoint> stopoves) {
        ArrayList<RouteWaypoint> sparseFTCRGeometryAsWaypoints = new ArrayList<>();
        int lastAddedStopoverIndex = 0;
        RouteWaypoint lastAddedStopover = stopoves.get(lastAddedStopoverIndex);
        RouteWaypoint nextStopover = stopoves.get(lastAddedStopoverIndex + 1);

        sparseFTCRGeometryAsWaypoints.add(lastAddedStopover);

        for (int i = 0; i < ftcrRouteGeometry.size(); i++) {
            GeoCoordinate curr = ftcrRouteGeometry.get(i);
            if (curr.distanceTo(lastAddedStopover.getNavigablePosition()) >= MIN_DISTANCE_BETWEEN_WAYPOINTS) {
                // use VIA waypoint just to advice SDK to calculate route through this geo point
                // no any event will be triggered on reaching this type of waypoint
                lastAddedStopover = new RouteWaypoint(curr, Type.VIA_WAYPOINT);
                sparseFTCRGeometryAsWaypoints.add(lastAddedStopover);
            }

            // also find and add stopover waupoints
            if (isFTCRGeometryPointCloseToStopover(nextStopover, curr)) {
                lastAddedStopover = nextStopover;
                lastAddedStopoverIndex = lastAddedStopoverIndex + 1;
                // lastAddedStopover has STOP type, so SDK will trigger needed events in guidance
                sparseFTCRGeometryAsWaypoints.add(lastAddedStopover);

                if (lastAddedStopoverIndex + 1 < stopoves.size()) {
                    nextStopover = stopoves.get(lastAddedStopoverIndex + 1);
                } else {
                    nextStopover = null;
                }
            }
        }

        return sparseFTCRGeometryAsWaypoints;
    }

    private boolean isFTCRGeometryPointCloseToStopover(RouteWaypoint stopover,
                                                       GeoCoordinate ftcrGeometryPoint) {
        return stopover != null
                && stopover.getNavigablePosition().distanceTo(ftcrGeometryPoint) <= THRESHOLD_DST_TO_STOPOVER;
    }

}