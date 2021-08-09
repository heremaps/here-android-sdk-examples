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

package com.here.android.example.map.data.prefetcher;

import android.graphics.PointF;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.prefetcher.MapDataPrefetcher;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Besides the turn-by-turn navigation example app, This app covers 2 other common use cases:
 * - usage of MapUpdateMode#RoadView during navigation and its interactions with user gestures.
 * - using a MapMarker as position indicator and how to make the movements smooth and
 * synchronized with map movements.
 */
public class MapFragmentView {
    private static final long CACHE_SIZE = 2000;

    private final AppCompatActivity m_activity;

    private AndroidXMapFragment m_mapFragment;
    private Map m_map;

    private MapDataPrefetcher m_mapDataPrefetcher;
    private MapDataPrefetcher.Request prefetchRequest;
    private GeoCoordinate startNavPoint, endNavPoint;
    private GeoBoundingBox geoBoundingBox;
    private RoutePlan m_routePlan;
    private Route m_route;
    private NavigationManager navigationManager;

    private ProgressBar progressBar;
    private View mapFragment;
    private Button btnChangeMapState;

    private MapType mapType;
    private MapState mapState;
    public boolean isRouteSimulationFinished = true;

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;

        initViews();
        initMapFragment();
    }

    private void loadMap() {
        showProgressBar(true);
        changeMapState(MapState.DEFAULT);

        if (m_mapDataPrefetcher != null) {
            m_map.removeAllMapObjects();
            m_mapFragment.getPositionIndicator().setVisible(false);

            MapEngine.setOnline(true);
        }

        if (mapType == MapType.MAP_BOUNDING_BOX) {
            showMapBoundingBox();
        } else {
            showRoute();
        }
    }

    public void setMapType(MapType mapType) {
        this.mapType = mapType;

        loadMap();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initViews() {
        progressBar = m_activity.findViewById(R.id.progress_circular);
        mapFragment = m_activity.findViewById(R.id.mapfragment);

        showProgressBar(true);

        btnChangeMapState = m_activity.findViewById(R.id.btn_changeMapState);
        btnChangeMapState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mapState) {
                    case PREFETCH_MAP:
                        showProgressBar(true);
                        prefetchMap(mapType);
                        break;
                    case CALCULATE_ROUTE:
                        startNavPoint = new GeoCoordinate(53.33201, -6.27858);
                        endNavPoint = new GeoCoordinate(53.34220, -6.28677);

                        calculateRoute(startNavPoint, endNavPoint);
                        break;
                    case SIMULATE_NAVIGATION:
                        startNavigationSimulation(startNavPoint);
                        break;
                    case STOP_NAVIGATION:
                        navigationManager.stop();

                        m_map.setTilt(0);

                        isRouteSimulationFinished = true;

                        loadMap();
                }
            }
        });
    }

    private void showProgressBar(Boolean showProgress) {
        if (showProgress) {
            progressBar.setVisibility(View.VISIBLE);
            mapFragment.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            mapFragment.setVisibility(View.VISIBLE);
        }
    }

    private void initMapFragment() {
        m_mapFragment = getMapFragment();

        // This will use external storage to save map cache data, it is also possible to set
        // private app's path
        String path = new File(m_activity.getExternalFilesDir(null), ".here-map-data")
                .getAbsolutePath();
        // This method will throw IllegalArgumentException if provided path is not writable
        com.here.android.mpa.common.MapSettings.setDiskCacheRootPath(path);

        if (m_mapFragment != null) {
            ApplicationContext applicationContext = new ApplicationContext(m_activity);
            applicationContext.setDiskCacheSize(CACHE_SIZE);

            /* Initialize the AndroidXMapFragment, results will be given via the called back. */
            m_mapFragment.init(applicationContext, new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        m_map = m_mapFragment.getMap();

                        showProgressBar(false);
                    } else {
                        Toast.makeText(m_activity,
                                "Error occurred: " + error.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private class PrefetchMapDataListener extends MapDataPrefetcher.Adapter {
        @Override
        public void onDataSizeEstimated(int requestId, boolean success, long dataSizeKB) {
            super.onDataSizeEstimated(requestId, success, dataSizeKB);

            /**
             * {@link CACHE_SIZE} set in MB,
             * while {@link l} set in KB
             * So, we divide l by 1000 to get estimated size in MB
             */
            if (success) {
                if ((dataSizeKB / 1000) > CACHE_SIZE) {
                    Toast.makeText(m_activity,
                            "Error occurred: map data size is bigger than expected",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (mapType == MapType.ROAD_RADIUS) {
                        Toast.makeText(m_activity, "Prefetching map", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        @Override
        public void onStatus(int requestId, PrefetchStatus status) {
            super.onStatus(requestId, status);
            if (status == PrefetchStatus.PREFETCH_SUCCESS) {
                if (mapType == MapType.MAP_BOUNDING_BOX) {
                    showProgressBar(false);
                    changeMapState(MapState.CALCULATE_ROUTE);

                    m_map.zoomTo(geoBoundingBox, Map.Animation.NONE, 0);
                } else {
                    showProgressBar(false);
                    changeMapState(MapState.SIMULATE_NAVIGATION);

                    m_map.zoomTo(geoBoundingBox, m_map.getWidth() - 200, m_map.getHeight() - 200,
                            Map.Animation.NONE, 0);
                }

                MapEngine.setOnline(false);
            }
        }
    }

    private void changeMapState(MapState mapState) {
        this.mapState = mapState;
        btnChangeMapState.setVisibility(View.VISIBLE);

        switch (mapState) {
            case DEFAULT:
                btnChangeMapState.setVisibility(View.GONE);
                break;
            case PREFETCH_MAP:
                btnChangeMapState.setText(R.string.prefetch_map);
                break;
            case CALCULATE_ROUTE:
                btnChangeMapState.setText(R.string.calculate_route);
                break;
            case SIMULATE_NAVIGATION:
                btnChangeMapState.setText(R.string.simulate_navigation);
                break;
            case STOP_NAVIGATION:
                btnChangeMapState.setText(R.string.stop_navigation);
                break;
        }
    }

    private void showMapBoundingBox() {
        GeoCoordinate m_geoCoordinate = new GeoCoordinate(53.34187, -6.28635);
        geoBoundingBox = new GeoBoundingBox(m_geoCoordinate, 2000, 2000);

        prefetchMap(mapType);
    }

    private void showRoute() {
        startNavPoint = new GeoCoordinate(55.93176, -3.22734);
        endNavPoint = new GeoCoordinate(55.98084, -3.17778);

        calculateRoute(startNavPoint, endNavPoint);

        List<GeoCoordinate> geoCoordinateList = new ArrayList<>();
        geoCoordinateList.add(startNavPoint);
        geoCoordinateList.add(endNavPoint);

        geoBoundingBox = GeoBoundingBox
                .getBoundingBoxContainingGeoCoordinates(geoCoordinateList);
        m_map.zoomTo(geoBoundingBox, m_map.getWidth() - 200, m_map.getHeight() - 200,
                Map.Animation.NONE, 0);

        changeMapState(MapState.PREFETCH_MAP);
    }

    private void prefetchMap(MapType mapType) {
        m_mapDataPrefetcher = MapDataPrefetcher.getInstance();
        m_mapDataPrefetcher.addListener(new PrefetchMapDataListener());

        if (mapType == MapType.MAP_BOUNDING_BOX) {
            m_mapDataPrefetcher.estimateMapDataSize(geoBoundingBox);
            prefetchRequest = m_mapDataPrefetcher.fetchMapData(geoBoundingBox);
        } else {
            changeMapState(MapState.DEFAULT);

            m_mapDataPrefetcher.estimateMapDataSize(m_route, 1000);
            prefetchRequest = m_mapDataPrefetcher.fetchMapData(m_route, 1000);
        }
    }

    public class RouterListener implements CoreRouter.Listener {
        private AppCompatActivity activity;

        public RouterListener(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onProgress(int i) {
        }

        @Override
        public void onCalculateRouteFinished(@NonNull List<RouteResult> list, @NonNull RoutingError routingError) {
            if (routingError == RoutingError.NONE) {
                m_route = list.get(0).getRoute();

                m_map.addMapObject(new MapRoute(m_route));

                if (mapType == MapType.MAP_BOUNDING_BOX) {
                    changeMapState(MapState.SIMULATE_NAVIGATION);
                } else {
                    showProgressBar(false);
                }
            } else {
                Toast.makeText(activity,
                        "Error occurred: " + routingError.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void calculateRoute(GeoCoordinate startNavPoint, GeoCoordinate endNavPoint) {
        m_map.addMapObject(new MapMarker(startNavPoint));
        m_map.addMapObject(new MapMarker(endNavPoint));

        m_routePlan = new RoutePlan();
        m_routePlan.addWaypoint(new RouteWaypoint(startNavPoint));
        m_routePlan.addWaypoint(new RouteWaypoint(endNavPoint));

        RouteOptions m_routeOptions = new RouteOptions();
        m_routeOptions.setTransportMode(RouteOptions.TransportMode.CAR)
                .setRouteType(RouteOptions.Type.SHORTEST);

        m_routePlan.setRouteOptions(m_routeOptions);

        CoreRouter m_router = new CoreRouter();
        m_router.calculateRoute(m_routePlan, new RouterListener(m_activity));
    }

//    public class PositionEventListener extends NavigationManager.PositionListener {
//        @Override
//        public void onPositionUpdated(@NonNull GeoPosition geoPosition) {
//            super.onPositionUpdated(geoPosition);
//
//            double speed = geoPosition.getSpeed();
//
//            if (geoPosition.getSpeed() < 10) {
//                m_map.setTilt(0);
//
//                isRouteSimulationFinished = true;
//                Toast.makeText(m_activity, "You reached destination", Toast.LENGTH_LONG).show();
//            }
//        }
//    }

    private void startNavigationSimulation(GeoCoordinate startPosition) {
        changeMapState(MapState.DEFAULT);

        if (m_route != null) {
            m_map.setCenter(m_routePlan.getWaypoint(0).getNavigablePosition(), Map.Animation.NONE);

            navigationManager = NavigationManager.getInstance();
            navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW_NOZOOM);
//            navigationManager.addPositionListener(
//                    new WeakReference<>(new PositionEventListener()));

            PointF transformCenter = new PointF((float) startPosition.getLatitude(),
                    (float) startPosition.getLongitude());
            m_map.setTransformCenter(transformCenter);
            m_map.setZoomLevel(m_map.getMaxZoomLevel());
            m_map.setTilt(60);

            MapMarker mapMarker = new MapMarker();
            mapMarker.setVisible(true);
            mapMarker.setCoordinate(m_map.getCenter());

            m_map.addMapObject(mapMarker);

            m_mapFragment.getPositionIndicator().setVisible(true);

            m_map.setMapScheme(Map.Scheme.CARNAV_DAY);
            navigationManager.setMap(m_map);

            changeMapState(MapState.DEFAULT);

            isRouteSimulationFinished = false;

            NavigationManager.Error simulationError = navigationManager.simulate(m_route, 24);

            if (simulationError != NavigationManager.Error.NONE) {
                Toast.makeText(m_activity, "Simulation error: " + simulationError.toString(),
                        Toast.LENGTH_LONG).show();
            } else {
                changeMapState(MapState.STOP_NAVIGATION);
            }
        } else {
            Toast.makeText(m_activity,
                    "Error occurred: Route is null", Toast.LENGTH_LONG).show();
        }
    }

    void onDestroy() {
        m_mapDataPrefetcher.clearMapDataCache();
    }
}