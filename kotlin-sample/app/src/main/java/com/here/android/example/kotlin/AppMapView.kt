package com.here.android.example.kotlin

import android.graphics.PointF
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.MapGesture.OnGestureListener.OnGestureListenerAdapter

class AppMapView : AppCompatActivity() {
    val mapViewModel by lazy { ViewModelProvider(this)[MapViewModel::class.java] }
    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShowMap(this)
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
                mapViewModel.getCurrentMap().also { mapView.map = it }

                mapView.mapGesture?.addOnGestureListener(mapGestureListener, 0, false)
                if (mapViewModel.getGeoCoordinatesList().size > 0) {
                    for (geoCoordinate in mapViewModel.getGeoCoordinatesList()) {
                        mapViewModel.addMapMarker(geoCoordinate)
                    }
                }

                mapView.onResume()

                mapViewModel.getRoute()?.let {
                    mapViewModel.getCurrentMap().addMapObject(MapRoute(it))
                }

                if (mapViewModel.getNavigationManager() != null) {
                    mapViewModel.startSimulation()
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
                mapViewModel.addMapObj(point)

                return true
            }
        }

    private fun createRoute(): Boolean {
        return if (mapViewModel.getGeoCoordinatesList().size >= 2) {
            val routeCalculationResultObserver = Observer<RouteCalculationStatus> { result ->
                val message = when (result) {
                    is RouteCalculationStatus.Successful -> getString(R.string.route_calc_success)
                    is RouteCalculationStatus.Error -> {
                        if (result.error == null)
                            getString(R.string.route_calc_invalid)
                        else
                            getString(R.string.route_calc_error) + result.error
                    }
                }

                Toast.makeText(this, message, Toast.LENGTH_LONG)
                    .show()
            }
            mapViewModel.routeCalculationResultMessage.observe(this, routeCalculationResultObserver)

            mapViewModel.createRoute()

            true
        } else {
            Toast.makeText(this, getString(R.string.min_points_count), Toast.LENGTH_LONG)
                .show()
            false
        }
    }

    fun onClick(buttonStatus: ButtonStatus?): String {
        var canCreateRoute = false

        val buttonTitle = when (buttonStatus) {
            is ButtonStatus.CreateRoute -> {
                canCreateRoute = createRoute()
                if (canCreateRoute) {
                    getString(R.string.simulate)
                } else
                    getString(R.string.create_route)
            }
            is ButtonStatus.SimulateRouting -> {
                mapViewModel.startSimulation()
                getString(R.string.finish_simulation)
            }
            is ButtonStatus.FinishRoutingSimulation -> {
                mapViewModel.clearMapData()
                getString(R.string.create_route)
            }
            else -> getString(R.string.create_route)
        }

        mapViewModel.changeButtonStatus(canCreateRoute)
        return buttonTitle
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mapViewModel.getCurrentMap().allMapObjects.size != 0) {
            mapViewModel.getNavigationManager()?.pause()
        }
    }
}

@Composable
fun ShowMap(activity: AppMapView) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (map, button) = createRefs()

        AndroidView(factory = {
            MapView(
                activity.applicationContext,
                activity.findViewById(R.id.compose_view)
            )
        }, modifier = Modifier.constrainAs(map) {
            start.linkTo(parent.start)
            top.linkTo(parent.top)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        }) {
            activity.initMap(it)
        }

        var title by rememberSaveable {
            mutableStateOf(activity.getString(R.string.create_route))
        }

        val buttonStatusObserver by
        activity.mapViewModel.buttonStatus.observeAsState(initial = ButtonStatus.CreateRoute)

        Button(onClick = { title = activity.onClick(buttonStatusObserver) },
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