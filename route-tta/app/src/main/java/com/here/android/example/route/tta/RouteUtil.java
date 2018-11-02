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

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;

public class RouteUtil {
    public static RoutePlan createRoute() {
        /* Initialize a RoutePlan */
        final RoutePlan routePlan = new RoutePlan();

        /*
         * Initialize a RouteOption.HERE SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        final RouteOptions routeOptions = new RouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        /* Disable highway in this route. */
        routeOptions.setHighwaysAllowed(false);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        /* Calculate 1 route. */
        routeOptions.setRouteCount(1);
        /* Finally set the route option */
        routePlan.setRouteOptions(routeOptions);

        /* Define waypoints for the route */
        /* START: Holländerstraße, Wedding, 13407 Berlin */
        RouteWaypoint startPoint = new RouteWaypoint(
                new GeoCoordinate(52.562755700200796, 13.34599438123405));

        /* MIDDLE: Lynarstraße 3 */
        RouteWaypoint middlePoint = new RouteWaypoint(
                new GeoCoordinate(52.54172, 13.36354));

        /* END: Agricolastraße 29, 10555 Berlin */
        RouteWaypoint destination = new RouteWaypoint(
                new GeoCoordinate(52.520720371976495, 13.332345457747579));

        /* Add both waypoints to the route plan */
        routePlan.addWaypoint(startPoint);
        routePlan.addWaypoint(middlePoint);
        routePlan.addWaypoint(destination);

        return routePlan;
    }

    static abstract class RouteListener<T, U extends Enum<?>> implements Router.Listener<T, U> {
        @Override
        public void onProgress(int i) {
            /* The calculation progress can be retrieved in this callback. */
        }
    }
}
