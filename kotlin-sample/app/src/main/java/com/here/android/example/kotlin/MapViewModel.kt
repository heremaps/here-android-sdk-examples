package com.here.android.example.kotlin

import android.graphics.PointF
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.guidance.NavigationManager
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapMarker
import com.here.android.mpa.mapping.MapRoute
import com.here.android.mpa.routing.*

sealed class ButtonStatus {
    object CreateRoute: ButtonStatus()
    object SimulateRouting: ButtonStatus()
    object FinishRoutingSimulation: ButtonStatus()
}

sealed class RouteCalculationStatus {
    class Error(val error: RoutingError?): RouteCalculationStatus()
    object Successful: RouteCalculationStatus()
}

class MapViewModel : ViewModel() {
    private val listOfGeoCoordinates: ArrayList<GeoCoordinate> = ArrayList()
    private var map: Map? = null
    private var route: Route? = null
    private var navigationManager: NavigationManager? = null

    val buttonStatus: MutableLiveData<ButtonStatus> by lazy {
        MutableLiveData<ButtonStatus>(ButtonStatus.CreateRoute)
    }
    
    val routeCalculationResultMessage: MutableLiveData<RouteCalculationStatus> by lazy {
        MutableLiveData<RouteCalculationStatus>()
    }

    fun getCurrentMap(): Map {
        if (map == null) map = Map()
        return map!!
    }

    private fun setupInitialMap() {
        map?.run {
            // Set the map center to the Berlin region (no animation)
            setCenter(GeoCoordinate(52.500556, 13.398889, 0.0), Map.Animation.NONE)
            // Set the zoom level to the average between min and max zoom level.
            zoomLevel = (maxZoomLevel + minZoomLevel) / 2;
            tilt = 0.0f
        }
    }

    fun addMapObj(point: PointF) {
        if (route == null) {
            map?.pixelToGeo(point)?.let {
                listOfGeoCoordinates.add(it)
                addMapMarker(it)
            }
        }
    }

    fun addMapMarker(geoCoordinate: GeoCoordinate) {
        MapMarker(geoCoordinate).also {
            map?.addMapObject(it)
        }
    }

    fun getGeoCoordinatesList() : ArrayList<GeoCoordinate> {
        return listOfGeoCoordinates
    }

    fun changeButtonStatus(canCreateRoute: Boolean) {
        buttonStatus.value = when (buttonStatus.value) {
            is ButtonStatus.CreateRoute ->
                if (canCreateRoute)
                    ButtonStatus.SimulateRouting
                else ButtonStatus.CreateRoute
            is ButtonStatus.SimulateRouting -> ButtonStatus.FinishRoutingSimulation
            is ButtonStatus.FinishRoutingSimulation -> ButtonStatus.CreateRoute
            else -> ButtonStatus.CreateRoute
        }
    }

    fun createRoute() {
        val routePlan = RoutePlan().apply {
            routeOptions = RouteOptions().apply {
                transportMode = RouteOptions.TransportMode.CAR
                routeType = RouteOptions.Type.FASTEST
                routeCount = 1
            }
            for (waypoint in listOfGeoCoordinates) addWaypoint(RouteWaypoint(waypoint))
        }

        CoreRouter().calculateRoute(
            routePlan,
            object : Router.Listener<List<RouteResult?>?, RoutingError?> {
                override fun onProgress(p0: Int) {
//                        The calculation progress can be retrieved in this callback.
                }

                override fun onCalculateRouteFinished(
                    routeResults: List<RouteResult?>,
                    routingError: RoutingError
                ) {
                    if (routingError == RoutingError.NONE) {
                        route = routeResults[0]?.route.also { route ->
                            route?.let {
                                map?.apply {
                                    addMapObject(MapRoute(route))
                                    zoomTo(route.boundingBox!!, Map.Animation.NONE,
                                        Map.MOVE_PRESERVE_ORIENTATION)
                                }

                                routeCalculationResultMessage.value = RouteCalculationStatus.Successful
                            } ?: kotlin.run {
                                routeCalculationResultMessage.value = RouteCalculationStatus.Error(null)
                            }
                        }
                    } else {
                        routeCalculationResultMessage.value = RouteCalculationStatus.Error(routingError)
                    }
                }
            })
    }

    fun getRoute() = route

    fun getNavigationManager() : NavigationManager? {
        return navigationManager
    }

    fun startSimulation() {
        map?.apply {
            positionIndicator.isVisible = true
            tilt = 30f
            zoomLevel = this.maxZoomLevel / 1.1
        }

        if (navigationManager == null) navigationManager = NavigationManager.getInstance()
        navigationManager?.also { navigationManager ->
            navigationManager.setMap(map)

            if (navigationManager.mapUpdateMode == NavigationManager.MapUpdateMode.NONE) {
                navigationManager.mapUpdateMode = NavigationManager.MapUpdateMode.ROADVIEW_NOZOOM
                route?.let { navigationManager.simulate(it, 40) }
            } else {
                navigationManager.mapUpdateMode = navigationManager.mapUpdateMode
                navigationManager.resume()
            }
        }
    }

    fun clearMapData() {
        map?.apply {
            positionIndicator.isVisible = false
            removeAllMapObjects()
        }

        navigationManager?.stop()
        navigationManager = null
        route = null

        listOfGeoCoordinates.clear()

        setupInitialMap()
    }
}