package com.here.android.example.kotlin

import android.graphics.PointF
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.here.android.mpa.common.*
import com.here.android.mpa.guidance.NavigationManager
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapGesture.OnGestureListener.OnGestureListenerAdapter
import com.here.android.mpa.routing.*
import kotlin.collections.ArrayList
import kotlin.collections.List

enum class ButtonTitles(val text: String) {
    CREATE_ROUTE("Create route"),
    SIMULATE("Simulate"),
    FINISH_SIMULATION("Finish simulation")
}

class AppMapView : AppCompatActivity() {

    var mapView: MapView? = null
    private lateinit var map: Map
    private var route: Route? = null
    private lateinit var geoBoundingBox: GeoBoundingBox
    private var navigationManager: NavigationManager? = null

    var buttonTitles = ButtonTitles.CREATE_ROUTE
    private var listOfGeoCoordinates: ArrayList<GeoCoordinate> = ArrayList()
    private val listOfMapMarkers: ArrayList<MapMarker> = ArrayList()
    private var isRouteCalculated = true

    private val mapViewModel by lazy { ViewModelProvider(this)[MapViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (mapViewModel.getGeoCoordinatesList().size > 0) {
            listOfGeoCoordinates = mapViewModel.getGeoCoordinatesList()
        }

        setContent {
            ShowMap(this)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun setupInitialMap() {
        map.run {
            // Set the map center to the Berlin region (no animation)
            setCenter(GeoCoordinate(52.500556, 13.398889, 0.0), Map.Animation.NONE)
            // Set the zoom level to the average between min and max zoom level.
            zoomLevel = (maxZoomLevel + minZoomLevel) / 2;
            tilt = 0.0f
        }
    }

    private fun addMapMarker(geoCoordinate: GeoCoordinate) {
        MapMarker(geoCoordinate).also {
            listOfMapMarkers.add(it)
            map.addMapObject(it)
        }

    }

    fun initMap(mapView: MapView) {
        this.mapView = mapView
        val context = ApplicationContext(this).apply {
            // Set application credentials
            setAppIdCode("{YOUR_APP_ID}", "{YOUR_APP_CODE}")
            setLicenseKey("{YOUR_LICENSE_KEY}")
        }

        MapEngine.getInstance().init(context) { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                /* get the map object */
                map = Map()
                mapView.map = map

                mapView.mapGesture?.addOnGestureListener(mapGestureListener, 0, false)
                if (listOfGeoCoordinates.size > 0) {
                    for (geoCoordinate in listOfGeoCoordinates) {
                        addMapMarker(geoCoordinate)
                    }
                }

                if (mapViewModel.getNavigationManager() != null) {
                    startSimulation()
                }

                mapViewModel.getRoute()?.let {
                    route = it
                    map.addMapObject(MapRoute(route!!))
                }
            } else {
                Toast.makeText(this, "Error: ${error!!}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private val mapGestureListener: MapGesture.OnGestureListener =
        object : OnGestureListenerAdapter() {
            override fun onLongPressEvent(point: PointF): Boolean {
                if (isRouteCalculated) {
                    val geoCoordinate = map.pixelToGeo(point)
                    geoCoordinate?.let {
                        listOfGeoCoordinates.add(it)
                        mapViewModel.addGeoCoordinate(it)
                        addMapMarker(it)
                    }
                }

                return true
            }
        }

    private fun createRoute(): Boolean {
        if (listOfGeoCoordinates.size >= 2) {
            val coreRouter = CoreRouter()
            val routePlan = RoutePlan()
            var routeOptions = RouteOptions().apply {
                transportMode = RouteOptions.TransportMode.CAR
                routeType = RouteOptions.Type.FASTEST
                routeCount = 1
            }

            routePlan.apply {
                routeOptions = routeOptions
                for (waypoint in listOfGeoCoordinates) addWaypoint(RouteWaypoint(waypoint))
            }


            coreRouter.calculateRoute(
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
                            route = routeResults[0]?.route.also {
                                it?.let {
                                    mapViewModel.setRoute(it)
                                    val mapRoute = MapRoute(it)

                                    map.addMapObject(mapRoute)

                                    geoBoundingBox = it.boundingBox!!
                                    map.zoomTo(
                                        geoBoundingBox, Map.Animation.NONE,
                                        Map.MOVE_PRESERVE_ORIENTATION
                                    )
                                } ?: kotlin.run {
                                    Toast.makeText(
                                        this@AppMapView,
                                        "Error:route results returned is not valid",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@AppMapView,
                                "Error: route calculation returned error code: $routingError",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            return true
        } else {
            Toast.makeText(this, "Please choose minimum 2 waypoints", Toast.LENGTH_LONG)
                .show()
            return false
        }
    }

    private fun startSimulation() {
        map.positionIndicator.isVisible = true

        navigationManager =
            if (mapViewModel.getNavigationManager() != null) {
                mapViewModel.getNavigationManager().also {navigationManager ->
                    navigationManager!!.setMap(map)
                    navigationManager.resume()
                }
            }
            else {
                NavigationManager.getInstance().also {navigationManager ->
                    mapViewModel.setNavigationManager(navigationManager)
                    navigationManager.setMap(map)
                    navigationManager.mapUpdateMode = NavigationManager.MapUpdateMode.ROADVIEW_NOZOOM
                    route?.let { navigationManager.simulate(it, 40) }
                }
            }

        map.tilt = 30f
        map.zoomLevel = map.maxZoomLevel / 1.1
    }

    fun onClick(title: String): String {
        if (buttonTitles.text != title) {
            buttonTitles = when(title) {
                ButtonTitles.CREATE_ROUTE.text -> ButtonTitles.CREATE_ROUTE
                ButtonTitles.SIMULATE.text -> ButtonTitles.SIMULATE
                ButtonTitles.FINISH_SIMULATION.text -> ButtonTitles.FINISH_SIMULATION
                else -> ButtonTitles.CREATE_ROUTE
            }
        }
        buttonTitles = when (buttonTitles) {
            ButtonTitles.CREATE_ROUTE -> {
                if (createRoute()) ButtonTitles.SIMULATE else ButtonTitles.CREATE_ROUTE
            }
            ButtonTitles.SIMULATE -> {
                startSimulation()
                ButtonTitles.FINISH_SIMULATION
            }
            ButtonTitles.FINISH_SIMULATION -> {
                clearMapData()
                ButtonTitles.CREATE_ROUTE
            }
        }

        isRouteCalculated = buttonTitles == ButtonTitles.CREATE_ROUTE

        return buttonTitles.text
    }

    private fun clearMapData() {
        map.removeAllMapObjects()
        map.positionIndicator.isVisible = false

        navigationManager!!.stop()
        mapViewModel.also {
            it.clearGeoCoordinatesList()
            it.setRoute(null)
            it.setNavigationManager(null)
        }

        setupInitialMap()

        listOfGeoCoordinates.clear()
        listOfMapMarkers.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        navigationManager!!.pause()
    }
}

@Composable
fun ShowMap(activity: AppMapView) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (map, button) = createRefs()

        AndroidView(factory = { MapView(activity.applicationContext, activity.findViewById(R.id.compose_view)) }, modifier = Modifier.constrainAs(map) {
            start.linkTo(parent.start)
            top.linkTo(parent.top)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        }) {
            activity.initMap(it)
        }

        var title by rememberSaveable {
            mutableStateOf(activity.buttonTitles.text)
        }

        Button(onClick = { title = activity.onClick(title) },
            modifier = Modifier
                .padding(0.dp, 0.dp, 10.dp, 10.dp)
                .constrainAs(button) {
                    end.linkTo(map.end)
                    bottom.linkTo(map.bottom)
                }) {
            Text(text = title)
        }
    }
}