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

package com.here.android.example.map.data.prefetcher;

import android.graphics.PointF;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
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
import java.util.ArrayList;
import java.util.List;

/**
 * This app covers 2 use cases of MapDataPrefetcher:
 * - downloading map data for an arbitrary bounding box.
 * - downloading map data for an area along the route.
 */
public class MapFragmentView {
    private static final long CACHE_SIZE = 2000;
    private static final long CAR_SPEED = 24;
    private static final int BOUNDING_BOX_HEIGHT = 2000;
    private static final int BOUNDING_BOX_WIDTH = 2000;
    private static final int ROUTE_RADIUS = 1000;
    private static final int ZOOM_HEIGHT = 200;
    private static final int ZOOM_WIDTH = 200;

    private final AppCompatActivity m_activity;

    private AndroidXMapFragment m_mapFragment;
    private Map m_map;

    private MapDataPrefetcher m_mapDataPrefetcher;
    private GeoCoordinate m_startNavPoint, m_endNavPoint;
    private GeoBoundingBox m_geoBoundingBox;
    private RoutePlan m_routePlan;
    private Route m_route;
    private NavigationManager m_navigationManager;

    private ProgressBar m_progressBar;
    private Button m_btnChangeState;

    private OptionType m_selectedOption;
    private State m_state;
    public boolean m_isRouteSimulationFinished = true;

    private enum OptionType {
        MAP_BOUNDING_BOX,
        ROAD_RADIUS
    }

    private enum State {
        DEFAULT,
        PREFETCH_MAP,
        CALCULATE_ROUTE,
        SIMULATE_NAVIGATION,
        STOP_NAVIGATION
    }

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;

        initViews();
        initMapFragment();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (m_isRouteSimulationFinished) {

            if (!item.isChecked()) {
                item.setChecked(true);

                changeState(State.DEFAULT);

                switch (item.getItemId()) {
                    case R.id.mapBoundingBox:
                        setOptionType(OptionType.MAP_BOUNDING_BOX);
                        return true;
                    case R.id.mapRoute:
                        changeState(State.DEFAULT);

                        setOptionType(OptionType.ROAD_RADIUS);
                        return true;
                }
            }
        } else {
            Toast.makeText(m_activity, m_activity.getText(R.string.change_map_error),
                    Toast.LENGTH_LONG).show();
        }
        return true;
    }


    private void showSelectedMap() {
        if (m_mapDataPrefetcher != null) {
            m_map.removeAllMapObjects();
            m_mapFragment.getPositionIndicator().setVisible(false);

            MapEngine.setOnline(true);
        }

        if (m_selectedOption == OptionType.MAP_BOUNDING_BOX) {
            if (m_progressBar.getVisibility() == View.VISIBLE) {
                boolean b = true;
            }

            m_map.setZoomLevel(11.73);
            m_map.setCenter(new GeoCoordinate(52.531003, 13.384783), Map.Animation.NONE);

            changeState(State.PREFETCH_MAP);
        } else {
            showRoute();
        }
    }

    private void setOptionType(OptionType selectedOption) {
        m_selectedOption = selectedOption;

        showSelectedMap();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initViews() {
        m_progressBar = m_activity.findViewById(R.id.progress_circular);

        m_btnChangeState = m_activity.findViewById(R.id.btn_changeState);
        m_btnChangeState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (m_state) {
                    case PREFETCH_MAP:
                        showProgressBar(true);
                        prefetchMap(m_selectedOption);
                        break;
                    case CALCULATE_ROUTE:
                        m_startNavPoint = new GeoCoordinate(53.33201, -6.27858);
                        m_endNavPoint = new GeoCoordinate(53.34220, -6.28677);

                        calculateRoute(m_startNavPoint, m_endNavPoint);
                        break;
                    case SIMULATE_NAVIGATION:
                        startNavigationSimulation(m_startNavPoint);
                        break;
                    case STOP_NAVIGATION:
                        m_navigationManager.stop();

                        m_map.setTilt(0);

                        m_isRouteSimulationFinished = true;

                        showSelectedMap();
                }
            }
        });
    }

    private void showProgressBar(Boolean showProgress) {
        if (showProgress) {
            m_progressBar.setVisibility(View.VISIBLE);
        } else {
            m_progressBar.setVisibility(View.GONE);
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
                public void onEngineInitializationCompleted(Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        m_map = m_mapFragment.getMap();

                        if (m_progressBar.getVisibility() == View.VISIBLE) {
                            boolean b = true;
                        }
                        setOptionType(OptionType.MAP_BOUNDING_BOX);
                    } else {
                        Toast.makeText(m_activity, m_activity.getText(R.string.error_occurred) +
                                error.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private class PrefetchMapDataListener extends MapDataPrefetcher.Adapter {
        @Override
        public void onDataSizeEstimated(int requestId, boolean success, long dataSizeKB) {
            super.onDataSizeEstimated(requestId, success, dataSizeKB);

//            CACHE_SIZE set in MB, while dataSizeKB set in KB
//            So, we divide dataSizeKB by 1000 to get estimated size in MB
            if (success) {
                if ((dataSizeKB / 1000) > CACHE_SIZE) {
                    Toast.makeText(m_activity, m_activity.getText(R.string.error_map_size),
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (m_selectedOption == OptionType.ROAD_RADIUS) {
                        Toast.makeText(m_activity, m_activity.getText(R.string.prefetching_map),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        @Override
        public void onStatus(int requestId, PrefetchStatus status) {
            super.onStatus(requestId, status);
            if (status == PrefetchStatus.PREFETCH_SUCCESS) {
                showProgressBar(false);

                if (m_selectedOption == OptionType.MAP_BOUNDING_BOX) {
                    changeState(State.CALCULATE_ROUTE);

                    m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE, 0);
                } else {
                    changeState(State.SIMULATE_NAVIGATION);

                    m_map.zoomTo(m_geoBoundingBox, m_map.getWidth() - ZOOM_WIDTH,
                            m_map.getHeight() - ZOOM_HEIGHT, Map.Animation.NONE, 0);
                }

                MapEngine.setOnline(false);
            }
        }
    }

    private void changeState(State state) {
        this.m_state = state;
        m_btnChangeState.setVisibility(View.VISIBLE);

        switch (state) {
            case DEFAULT:
                m_btnChangeState.setVisibility(View.GONE);
                break;
            case PREFETCH_MAP:
                if (m_selectedOption == OptionType.MAP_BOUNDING_BOX) {
                    m_btnChangeState.setText(R.string.prefetch_geo_box);
                } else {
                    m_btnChangeState.setText(R.string.prefetch_route);
                }
                break;
            case CALCULATE_ROUTE:
                m_btnChangeState.setText(R.string.calculate_route);
                break;
            case SIMULATE_NAVIGATION:
                m_btnChangeState.setText(R.string.simulate_navigation);
                break;
            case STOP_NAVIGATION:
                m_btnChangeState.setText(R.string.stop_navigation);
                break;
        }
    }

    private void showRoute() {
        m_startNavPoint = new GeoCoordinate(55.93176, -3.22734);
        m_endNavPoint = new GeoCoordinate(55.98084, -3.17778);

        calculateRoute(m_startNavPoint, m_endNavPoint);

        List<GeoCoordinate> geoCoordinateList = new ArrayList<>();
        geoCoordinateList.add(m_startNavPoint);
        geoCoordinateList.add(m_endNavPoint);

        m_geoBoundingBox = GeoBoundingBox
                .getBoundingBoxContainingGeoCoordinates(geoCoordinateList);
        m_map.zoomTo(m_geoBoundingBox, m_map.getWidth() - ZOOM_WIDTH,
                m_map.getHeight() - ZOOM_HEIGHT, Map.Animation.NONE, 0);

        changeState(State.PREFETCH_MAP);
    }

    private void prefetchMap(OptionType selectedOption) {
        m_mapDataPrefetcher = MapDataPrefetcher.getInstance();
        m_mapDataPrefetcher.addListener(new PrefetchMapDataListener());

        changeState(State.DEFAULT);

        MapDataPrefetcher.Request prefetchRequest;
        if (selectedOption == OptionType.MAP_BOUNDING_BOX) {
            GeoCoordinate m_geoCoordinate = new GeoCoordinate(53.34187, -6.28635);
            m_geoBoundingBox = new GeoBoundingBox(m_geoCoordinate, BOUNDING_BOX_HEIGHT, BOUNDING_BOX_WIDTH);

            m_mapDataPrefetcher.estimateMapDataSize(m_geoBoundingBox);
            prefetchRequest = m_mapDataPrefetcher.fetchMapData(m_geoBoundingBox);
        } else {
            m_mapDataPrefetcher.estimateMapDataSize(m_route, ROUTE_RADIUS);
            prefetchRequest = m_mapDataPrefetcher.fetchMapData(m_route, ROUTE_RADIUS);
        }
    }

    public class RouterListener implements CoreRouter.Listener {
        private final AppCompatActivity activity;

        public RouterListener(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onProgress(int progress) {
        }

        @Override
        public void onCalculateRouteFinished(@NonNull List<RouteResult> result, @NonNull RoutingError errorCode) {
            if (errorCode == RoutingError.NONE) {
                m_route = result.get(0).getRoute();

                m_map.addMapObject(new MapRoute(m_route));

                if (m_selectedOption == OptionType.MAP_BOUNDING_BOX) {
                    changeState(State.SIMULATE_NAVIGATION);
                }
            } else {
                Toast.makeText(activity, m_activity.getText(R.string.error_occurred) +
                        errorCode.toString(), Toast.LENGTH_LONG).show();
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


    private void startNavigationSimulation(GeoCoordinate startPosition) {
        changeState(State.DEFAULT);

        if (m_route != null) {
            m_map.setCenter(m_routePlan.getWaypoint(0).getNavigablePosition(), Map.Animation.NONE);

            m_navigationManager = NavigationManager.getInstance();
            m_navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW_NOZOOM);

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
            m_navigationManager.setMap(m_map);

            changeState(State.DEFAULT);

            m_isRouteSimulationFinished = false;

            NavigationManager.Error simulationError = m_navigationManager.simulate(m_route, CAR_SPEED);

            if (simulationError != NavigationManager.Error.NONE) {
                Toast.makeText(m_activity, m_activity.getText(R.string.simulation_error) +
                        simulationError.toString(), Toast.LENGTH_LONG).show();
            } else {
                changeState(State.STOP_NAVIGATION);
            }
        } else {
            Toast.makeText(m_activity,
                    m_activity.getText(R.string.error_null_route), Toast.LENGTH_LONG).show();
        }
    }

    void onDestroy() {
        m_mapDataPrefetcher.clearMapDataCache();
    }
}