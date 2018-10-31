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
