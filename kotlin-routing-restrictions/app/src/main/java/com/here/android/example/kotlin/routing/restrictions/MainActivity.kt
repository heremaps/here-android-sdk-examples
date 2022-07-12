package com.here.android.example.kotlin.routing.restrictions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.routing.*
import com.here.android.mpa.mapping.Map

class MainActivity : FragmentActivity() {

    private val REQUEST_CODE_ASK_PERMISSIONS = 1

    private val RUNTIME_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    private var m_map: Map? = null
    private lateinit var m_dynamicPenalty: DynamicPenalty
    private lateinit var m_coreRouter: CoreRouter
    private lateinit var m_routePlan: RoutePlan
    private var m_mapRoute: MapRoute? = null
    private lateinit var m_mapPolyline: List<MapPolyline>

    private lateinit var startMarker: MapMarker
    private lateinit var endMarker: MapMarker

    private lateinit var startGeoCoordinate: GeoCoordinate
    private lateinit var endGeoCoordinate: GeoCoordinate

    private lateinit var allowedPVID: DynamicPenalty.PvidRoadElementIdentifier
    private lateinit var restrictedPVID: DynamicPenalty.PvidRoadElementIdentifier
    private lateinit var allowedRoadElement: RoadElement

    private var isFirstRouteCalculation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
    }

    private fun requestPermissions() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS
            )
        } else {
            initMapFragmentView()
        }
    }

    // Checks whether permission represented by this string is granted
    private fun String.permissionGranted(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, this) == PackageManager.PERMISSION_GRANTED

    private fun hasPermissions(): Boolean {
        /**
         * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
         * needs when the app is running.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        return RUNTIME_PERMISSIONS.count { !it.permissionGranted(this) } == 0
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                for (index in RUNTIME_PERMISSIONS.indices) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        var notGrantedMessage =
                            "Required permission ${permissions[index]} not granted."

                        /**
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permissions[index]
                            )
                        ) {
                            notGrantedMessage += "Please go to settings and turn on for sample app."
                        }

                        Toast.makeText(this, notGrantedMessage, Toast.LENGTH_LONG).show();
                    }
                }

                /**
                 * All permission requests are being handled. Create map fragment view. Please note
                 * the HERE Mobile SDK requires all permissions defined above to operate properly.
                 */
                initMapFragmentView()
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun initMapFragmentView() {
        val mapFragment: AndroidXMapFragment =
            supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment

        val context = ApplicationContext(this).apply {
            // Set application credentials
            setAppIdCode("{YOUR_APP_ID}", "{YOUR_APP_CODE}")
            setLicenseKey("{YOUR_LICENSE_KEY}")
        }

        mapFragment.let { fragment ->
            fragment.init(context) { error ->
                when (error) {
                    OnEngineInitListener.Error.NONE -> {
                        m_map = fragment.map

                        m_map?.run {
                            // Set the map center to the Berlin region (no animation)
                            setCenter(GeoCoordinate(52.510756348557756, 13.38113432397731), Map.Animation.NONE)
                            // Set the zoom level to the average between min and max zoom level.
                            zoomLevel = 14.0

                            setupRoute()
                            initCalculateRouteButton()
                        }
                    }
                    else -> {
                        val errorMessage =
                            "Error: ${error}, SDK Version: ${Version.getSdkVersion()}"

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private fun setupRoadRestrictions() {
        val pvidAllowedCoordinate = GeoCoordinate(52.50976757472477, 13.376763138824241)
        val roadElementAllowedCoordinate = GeoCoordinate(52.50967105768706, 13.37891280127972)
        val pvidRestrictedCoordinate = GeoCoordinate(52.511488759932575, 13.383899313444406)

        allowedPVID = DynamicPenalty.PvidRoadElementIdentifier
            .create(
                (RoadElement.getRoadElement(
                    pvidAllowedCoordinate,
                    "MAC"
                ))!!.permanentDirectedLinkId, pvidAllowedCoordinate
            )

        allowedRoadElement = RoadElement.getRoadElement(roadElementAllowedCoordinate, "MAC")!!

        restrictedPVID = DynamicPenalty.PvidRoadElementIdentifier
            .create(
                (RoadElement.getRoadElement(
                    pvidRestrictedCoordinate,
                    "MAC"
                ))!!.permanentDirectedLinkId, pvidRestrictedCoordinate
            )

        m_mapPolyline = createMapPolylineList(
            pvidAllowedCoordinate,
            roadElementAllowedCoordinate,
            pvidRestrictedCoordinate
        )

        for ((i, polyLine) in m_mapPolyline.withIndex()) {
            polyLine.lineWidth = 12

            changeMapPolylineColor(i, i == 2)
        }
    }

    private fun createMapPolylineList(vararg geoCoordinates: GeoCoordinate) = geoCoordinates.map {
        MapPolyline(GeoPolyline((RoadElement.getRoadElement(it, "MAC"))!!.geometry))
    }

    private fun initCalculateRouteButton() {
        val calculateRouteButton: Button = findViewById(R.id.calculateRoute)

        calculateRouteButton.visibility = View.VISIBLE
        calculateRouteButton.setOnClickListener {
            if (m_mapRoute != null) {
                m_map!!.removeMapObject(m_mapRoute!!)
                m_map!!.removeMapObject(startMarker)
                m_map!!.removeMapObject(endMarker)
            }

            calculateRoute(
                m_coreRouter,
                m_routePlan,
                startGeoCoordinate,
                endGeoCoordinate
            )
        }
    }

    private fun setupRoute() {
        m_coreRouter = CoreRouter()
        m_routePlan = RoutePlan()
        val routeOptions = RouteOptions()

        m_dynamicPenalty = DynamicPenalty()

        routeOptions.transportMode = RouteOptions.TransportMode.CAR
        routeOptions.routeType = RouteOptions.Type.FASTEST
        routeOptions.routeCount = 1

        m_routePlan.routeOptions = routeOptions

        startGeoCoordinate = GeoCoordinate(52.50840783539561, 13.376705207378569)
        endGeoCoordinate = GeoCoordinate(52.51278601743045, 13.385974921776945)

        val startPoint = RouteWaypoint(startGeoCoordinate)
        val destination = RouteWaypoint(endGeoCoordinate)

        m_routePlan.addWaypoint(startPoint)
        m_routePlan.addWaypoint(destination)
    }

    private fun calculateRoute(
        coreRouter: CoreRouter,
        routePlan: RoutePlan,
        startGeoCoordinate: GeoCoordinate,
        endGeoCoordinate: GeoCoordinate
    ) {
        coreRouter.calculateRoute(routePlan,
            object : Router.Listener<List<RouteResult>, RoutingError> {
                override fun onProgress(i: Int) {}

                override fun onCalculateRouteFinished(
                    routeResults: List<RouteResult>,
                    routingError: RoutingError
                ) {
                    if (routingError == RoutingError.NONE) {
                        val route = routeResults.get(0).route
                        m_mapRoute = MapRoute(route)

                        startMarker = MapMarker(startGeoCoordinate)
                        endMarker = MapMarker(endGeoCoordinate)

                        m_map?.let {
                            it.addMapObject(startMarker)
                            it.addMapObject(endMarker)
                            it.addMapObject(m_mapRoute!!)

                            it.zoomTo(
                                route.boundingBox!!,
                                Map.Animation.NONE,
                                Map.MOVE_PRESERVE_ORIENTATION
                            )
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Error = ${routingError.name}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    private fun changeDefaultRestrictions(
        enable: Boolean, roadElement: RoadElement? = null,
        pvidRoadElementIdentifier: DynamicPenalty.PvidRoadElementIdentifier? = null
    ) {
        when {
            roadElement == null && pvidRoadElementIdentifier == null -> {
                m_coreRouter.dynamicPenalty.addRoadPenalty(
                    allowedPVID,
                    DrivingDirection.DIR_BOTH,
                    0
                )
                m_coreRouter.dynamicPenalty.addRoadPenalty(
                    allowedRoadElement,
                    DrivingDirection.DIR_BOTH,
                    0
                )
            }
            roadElement != null -> {
                if (enable) m_coreRouter.dynamicPenalty.removeRoadPenalty(roadElement)
                else m_coreRouter.dynamicPenalty.addRoadPenalty(
                    roadElement,
                    DrivingDirection.DIR_BOTH, 0
                )
            }
            pvidRoadElementIdentifier != null -> {
                if (enable) m_coreRouter.dynamicPenalty.removeRoadPenalty(pvidRoadElementIdentifier)
                else m_coreRouter.dynamicPenalty.addRoadPenalty(
                    pvidRoadElementIdentifier,
                    DrivingDirection.DIR_BOTH, 0
                )
            }
        }
    }

    private fun changeMapPolylineColor(idInGeoCoordinatesList: Int, isAllowed: Boolean) {
        if (m_map!!.allMapObjects.contains(m_mapPolyline[idInGeoCoordinatesList])) {
            m_map!!.removeMapObject(m_mapPolyline[idInGeoCoordinatesList])
        }

        if (isAllowed) {
            m_mapPolyline[idInGeoCoordinatesList].lineColor = Color.GREEN
        } else {
            m_mapPolyline[idInGeoCoordinatesList].lineColor = Color.RED
        }
        m_map!!.addMapObject(m_mapPolyline[idInGeoCoordinatesList])
    }

    private var optionsMenu: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        optionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        item.isChecked = !item.isChecked

        when (item.itemId) {
            R.id.addDefaultPenalties -> {
                if (isFirstRouteCalculation) {
                    setupRoadRestrictions()
                    changeDefaultRestrictions(true)

                    isFirstRouteCalculation = false
                    item.isVisible = false

                    optionsMenu?.let {
                        it.findItem(R.id.allowPVIDRoadElement).isVisible = true
                        it.findItem(R.id.allowRoadElement).isVisible = true
                        it.findItem(R.id.restrictPVIDRoadElement).isVisible = true
                    }
                }
            }
            R.id.allowPVIDRoadElement -> {
                changeDefaultRestrictions(item.isChecked, pvidRoadElementIdentifier = allowedPVID)

                if (item.isChecked)
                    m_coreRouter.dynamicPenalty.addAllowedPvidRoadElementIdentifier(allowedPVID)
                else
                    m_coreRouter.dynamicPenalty.removeAllowedPvidRoadElementIdentifier(
                        allowedPVID
                    )

                changeMapPolylineColor(item.order, item.isChecked)
            }
            R.id.allowRoadElement -> {
                changeDefaultRestrictions(item.isChecked, allowedRoadElement)

                if (item.isChecked)
                    m_coreRouter.dynamicPenalty.addAllowedRoadElement(allowedRoadElement)
                else
                    m_coreRouter.dynamicPenalty.removeAllowedRoadElement(allowedRoadElement)

                changeMapPolylineColor(item.order, item.isChecked)
            }
            R.id.restrictPVIDRoadElement -> {
                if (item.isChecked)
                    m_coreRouter.dynamicPenalty.addRoadPenalty(
                        restrictedPVID,
                        DrivingDirection.DIR_BOTH,
                        0
                    )
                else m_coreRouter.dynamicPenalty.removeRoadPenalty(restrictedPVID)

                changeMapPolylineColor(item.order, !item.isChecked)
            }
        }

        Toast.makeText(
            this,
            "Please recalculate the route to apply this setting",
            Toast.LENGTH_LONG
        ).show()
        return true
    }
}