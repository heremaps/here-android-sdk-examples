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

package com.here.android.example.routing;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.RoadElement;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.DrivingDirection;
import com.here.android.mpa.routing.DynamicPenalty;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.routing.RoutingZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * This class encapsulates the properties and functionality of the Map view.A route calculation from
 * south of Berlin to the north of Berlin.
 */
public class MapFragmentView {
    private static final int ITEM_ID_SHOW_ZONES = 1;
    private static final int ITEM_ID_EXCLUDE_IN_ROUTING = 2;
    private static final int ITEM_ID_ADD_AVOIDED_AREAS = 3;
    private AndroidXMapFragment m_mapFragment;
    private Button m_createRouteButton;
    private AppCompatActivity m_activity;
    private Map m_map;
    private MapRoute m_mapRoute;
    private boolean m_isExcludeRoutingZones;
    private boolean m_addAvoidedAreas;

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        initMapFragment();
        /*
         * We use a button in this example to control the route calculation
         */
        initCreateRouteButton();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
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
                        /* get the map object */
                        m_map = m_mapFragment.getMap();

                        /*
                         * Set the map center to the south of Berlin.
                         */
                        m_map.setCenter(new GeoCoordinate(52.406425, 13.193975, 0.0),
                                Map.Animation.NONE);

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
        m_createRouteButton = (Button) m_activity.findViewById(R.id.button);

        m_createRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_map.removeMapObject(m_mapRoute);
                m_mapRoute = null;
                createRoute(Collections.<RoutingZone>emptyList());
            }
        });

    }

    private void createRoute(final List<RoutingZone> excludedRoutingZones) {
        /* Initialize a CoreRouter */
        CoreRouter coreRouter = new CoreRouter();

        /* Initialize a RoutePlan */
        RoutePlan routePlan = new RoutePlan();

        /*
         * Initialize a RouteOption. HERE Mobile SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        RouteOptions routeOptions = new RouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        /* Disable highway in this route. */
        routeOptions.setHighwaysAllowed(false);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(RouteOptions.Type.SHORTEST);
        /* Calculate 1 route. */
        routeOptions.setRouteCount(1);
        /* Exclude routing zones. */
        if (!excludedRoutingZones.isEmpty()) {
            routeOptions.excludeRoutingZones(toStringIds(excludedRoutingZones));
        }

        if (m_addAvoidedAreas) {
            DynamicPenalty dynamicPenalty = new DynamicPenalty();
            // There are two option to avoid certain areas during routing
            // 1. Add banned area using addBannedArea API
            GeoPolygon geoPolygon = new GeoPolygon();
            geoPolygon.add(Arrays.asList(new GeoCoordinate(52.631692, 13.437591),
                    new GeoCoordinate(52.631905, 13.437787),
                    new GeoCoordinate(52.632577, 13.438357)));
            // Note, the maximum supported number of banned areas is 20.
            dynamicPenalty.addBannedArea(geoPolygon);

            // 1. Add banned road link using addRoadPenalty API
            // Note, map data needs to be present to get RoadElement by the GeoCoordinate.
            RoadElement roadElement = RoadElement
                    .getRoadElement(new GeoCoordinate(52.406611, 13.194916), "MAC");
            if (roadElement != null) {
                dynamicPenalty.addRoadPenalty(roadElement, DrivingDirection.DIR_BOTH,
                        0/*new speed*/);
            }
            coreRouter.setDynamicPenalty(dynamicPenalty);
        }
        /* Finally set the route option */
        routePlan.setRouteOptions(routeOptions);

        /* Define waypoints for the route */
        /* START: South of Berlin */
        RouteWaypoint startPoint = new RouteWaypoint(new GeoCoordinate(52.406425, 13.193975));
        /* END: North of Berlin */
        RouteWaypoint destination = new RouteWaypoint(new GeoCoordinate(52.638623, 13.441998));

        /* Add both waypoints to the route plan */
        routePlan.addWaypoint(startPoint);
        routePlan.addWaypoint(destination);

        /* Trigger the route calculation,results will be called back via the listener */
        coreRouter.calculateRoute(routePlan,
                new Router.Listener<List<RouteResult>, RoutingError>() {
                    @Override
                    public void onProgress(int i) {
                        /* The calculation progress can be retrieved in this callback. */
                    }

                    @Override
                    public void onCalculateRouteFinished(List<RouteResult> routeResults,
                            RoutingError routingError) {
                        /* Calculation is done. Let's handle the result */
                        if (routingError == RoutingError.NONE) {
                            Route route = routeResults.get(0).getRoute();

                            if (m_isExcludeRoutingZones && excludedRoutingZones.isEmpty()) {
                                // Here we exclude all available routing zones in the route.
                                // Also RoutingZoneRestrictionsChecker can be used to get
                                // available routing zones for specific RoadElement.
                                createRoute(route.getRoutingZones());
                            } else {
                                /* Create a MapRoute so that it can be placed on the map */
                                m_mapRoute = new MapRoute(route);

                                /* Show the maneuver number on top of the route */
                                m_mapRoute.setManeuverNumberVisible(true);

                                /* Add the MapRoute to the map */
                                m_map.addMapObject(m_mapRoute);

                                /*
                                 * We may also want to make sure the map view is orientated properly
                                 * so the entire route can be easily seen.
                                 */
                                m_map.zoomTo(route.getBoundingBox(), Map.Animation.NONE,
                                        Map.MOVE_PRESERVE_ORIENTATION);
                            }
                        } else {
                            Toast.makeText(m_activity,
                                    "Error:route calculation returned error code: " + routingError,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        item.setChecked(!item.isChecked());
        if (item.getItemId() == ITEM_ID_SHOW_ZONES) {
            EnumSet<Map.FleetFeature> features;
            if (item.isChecked()) {
                features = EnumSet.of(Map.FleetFeature.ENVIRONMENTAL_ZONES);
            } else {
                features = EnumSet.noneOf(Map.FleetFeature.class);

            }
            m_map.setFleetFeaturesVisible(features);
        } else if (item.getItemId() == ITEM_ID_EXCLUDE_IN_ROUTING) {
            m_isExcludeRoutingZones = item.isChecked();
            if (m_mapRoute != null) {
                Toast.makeText(m_activity, "Please recalculate the route to apply this setting",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (item.getItemId() == ITEM_ID_ADD_AVOIDED_AREAS) {
            m_addAvoidedAreas = item.isChecked();
            if (m_mapRoute != null) {
                Toast.makeText(m_activity, "Please recalculate the route to apply this setting",
                        Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, ITEM_ID_SHOW_ZONES, Menu.NONE, "Show environmental zones")
                .setCheckable(true);
        menu.add(0, ITEM_ID_EXCLUDE_IN_ROUTING, Menu.NONE, "Exclude all zones in routing")
                .setCheckable(true);

        menu.add(0, ITEM_ID_ADD_AVOIDED_AREAS, Menu.NONE, "Add avoided areas")
                .setCheckable(true);

        return true;
    }

    static List<String> toStringIds(List<RoutingZone> excludedRoutingZones) {
        ArrayList<String> ids = new ArrayList<>();
        for (RoutingZone zone : excludedRoutingZones) {
            ids.add(zone.getId());
        }
        return ids;
    }
}
