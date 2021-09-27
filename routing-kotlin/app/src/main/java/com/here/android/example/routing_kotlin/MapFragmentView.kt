package com.here.android.example.routing_kotlin

import android.annotation.SuppressLint
import android.content.DialogInterface
import com.here.android.mpa.routing.DynamicPenalty
import com.here.android.mpa.routing.DynamicPenalty.*
import android.graphics.Color
import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.here.android.mpa.common.*
import com.here.android.mpa.guidance.TruckRestrictionsChecker
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapGesture.OnGestureListener.OnGestureListenerAdapter
import com.here.android.mpa.routing.*
import java.io.File
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MapFragmentView(private val activity: AppCompatActivity) {
    companion object {
        private const val MIN_ROUTE_WAYPOINTS_COUNT = 2
        private const val MAP_POLYLINE_WIDTH = 30
        private const val MAX_PERMITTED_SPEED = 50
    }

    enum class RoadPermission {
        ALLOWED_ROAD_ELEMENT, ALLOWED_PVID, RESTRICTED_ROAD
    }

    private val mapFragment by lazy {
        activity.supportFragmentManager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment
    }

    private lateinit var map: Map
    private lateinit var dynamicPenalty: DynamicPenalty

    private val userWaypointsList = ArrayList<GeoCoordinate>()
    private val allowedRoadsList = HashMap<RoadElement, MapObject>()
    private val allowedPvidRoadsList = HashMap<Long, MapObject>()
    private val restrictedRoadsList = HashMap<Long, MapObject>()

    private val buttonCalculateRoute by lazy {
        activity.findViewById<FloatingActionButton>(R.id.fab_calculate_route)
    }

    private var roadPermission = RoadPermission.ALLOWED_ROAD_ELEMENT

    private var isRouteReady = false

    init {
        initViews()
        initMapFragment()
    }

    private fun initViews() {
        buttonCalculateRoute.setOnClickListener {
            if (userWaypointsList.size >= MIN_ROUTE_WAYPOINTS_COUNT) {
                calculateRoute(userWaypointsList)
            } else {
                Toast.makeText(
                    activity, activity.getString(R.string.calculate_route_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addAppCredentials(): ApplicationContext =
        ApplicationContext(activity).apply {
            // Set application credentials
            setAppIdCode("{YOUR_APP_ID}", "{YOUR_APP_CODE}")
            setLicenseKey("{YOUR_LICENSE_KEY}")
        }

    private fun initMapFragment() {
        // This will use external storage to save map cache data, it is also possible to set
        // private app's path
        val path = File(activity.getExternalFilesDir(null), ".here-map-data")
            .absolutePath
        // This method will throw IllegalArgumentException if provided path is not writable
        MapSettings.setDiskCacheRootPath(path)

        mapFragment.init(addAppCredentials()) { error ->
            when (error) {
                OnEngineInitListener.Error.NONE -> {
                    map = mapFragment.map!!

                    map.fleetFeaturesVisible = showTruckRestrictions()
                    dynamicPenalty = DynamicPenalty()

                    mapFragment.mapGesture?.addOnGestureListener(
                        createGestureListener(),
                        0,
                        false
                    )
                }
                else -> Toast.makeText(
                    activity, activity.getString(R.string.error) +
                            error.toString(), Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showTruckRestrictions(): EnumSet<Map.FleetFeature> =
        EnumSet.of(Map.FleetFeature.TRUCK_RESTRICTIONS)

    private fun createGestureListener(): MapGesture.OnGestureListener {
        return object : OnGestureListenerAdapter() {
            override fun onTapEvent(pointF: PointF): Boolean {
                if (isRouteReady) return false

                map.pixelToGeo(pointF)?.let { addWaypointDialog(it) }
                return true
            }

            override fun onLongPressEvent(pointF: PointF): Boolean {
                if (isRouteReady) return false

                map.pixelToGeo(pointF)?.let { addRoadsStatusDialog(it) }
                return true
            }
        }
    }

    private fun calculateRoute(geoCoordinates: ArrayList<GeoCoordinate>) {
        val routePlan = RoutePlan()

        for (waypoint in geoCoordinates) {
            routePlan.addWaypoint(RouteWaypoint(waypoint))
        }

        routePlan.routeOptions = getTruckRouteOptions()

        CoreRouter().apply {
            setDynamicPenalty(dynamicPenalty)
            calculateRoute(routePlan, RouterListener())
        }
    }

    private fun getTruckRouteOptions(): RouteOptions {
        return RouteOptions()
            .setTransportMode(RouteOptions.TransportMode.TRUCK)
            .setRouteType(RouteOptions.Type.SHORTEST)
            .setTruckTrailersCount(1)
            .setTruckWidth(3F)
            .setTruckHeight(4.2F)
            .setTruckLength(6F)
            .setTruckLimitedWeight(50F)
            .setTruckWeightPerAxle(10F)
    }

    inner class RouterListener : CoreRouter.Listener {
        override fun onProgress(p0: Int) {}

        override fun onCalculateRouteFinished(
            routeCalculationResult: MutableList<RouteResult>,
            routingError: RoutingError
        ) {
            if (routingError == RoutingError.NONE) {
                map.addMapObject(MapRoute(routeCalculationResult[0].route))

                isRouteReady = true
            } else {
                Toast.makeText(
                    activity, activity.getString(R.string.error) +
                            routingError.toString(), Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @SuppressLint("InflateParams")
    fun addWaypointDialog(geoCoordinate: GeoCoordinate) {
        val addWaypointDialog: View = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_add_waypoint, null)

        setGeoCoordinatesDialog(addWaypointDialog, geoCoordinate)

        val dialogButtonClickListener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                userWaypointsList.add(geoCoordinate)
                map.addMapObject(MapMarker(geoCoordinate))
            }
        }

        createAlertDialog(
            addWaypointDialog,
            R.string.caption_dialog_add_waypoint,
            R.string.button_done,
            R.string.button_cancel,
            dialogButtonClickListener
        )
    }

    @SuppressLint("InflateParams")
    fun addRoadsStatusDialog(geoCoordinate: GeoCoordinate) {
        val roadElement = RoadElement.getRoadElement(geoCoordinate, "MAC")!!
        val pvidIdentifier = roadElement.permanentLinkId
        var dialogTitle = R.string.caption_dialog_add_allowed_road

        val addRoadPermissionsDialog: View
        if (TruckRestrictionsChecker.getTruckRestrictions(roadElement, getTruckRouteOptions())
                .isEmpty()
        ) {
            roadPermission = RoadPermission.RESTRICTED_ROAD
            addRoadPermissionsDialog = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_add_waypoint, null)

            setGeoCoordinatesDialog(addRoadPermissionsDialog, geoCoordinate)
        } else {
            addRoadPermissionsDialog = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_add_allowed_road, null)

            dialogTitle = R.string.caption_dialog_add_restricted_road

            (addRoadPermissionsDialog.findViewById(R.id.chooseWayToAddAllowedRoad) as RadioGroup)
                .setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.roadElement -> {
                            roadPermission = RoadPermission.ALLOWED_ROAD_ELEMENT
                            changeDialogContextVisibility(addRoadPermissionsDialog)
                        }
                        R.id.pvid -> {
                            roadPermission = RoadPermission.ALLOWED_PVID
                            changeDialogContextVisibility(addRoadPermissionsDialog)

                            (addRoadPermissionsDialog.findViewById(R.id.valuePvid) as EditText)
                                .setText(pvidIdentifier.toString())

                            val decimalFormat = DecimalFormat("#0.00000")
                            val coordinates = "${decimalFormat.format(geoCoordinate.latitude)}, ${
                                decimalFormat.format(geoCoordinate.longitude)
                            }"

                            (addRoadPermissionsDialog.findViewById(R.id.valueCenter) as EditText)
                                .setText(coordinates)
                        }
                    }
                }
        }

        val dialogButtonClickListener = DialogInterface.OnClickListener { _, which ->
            val pvidRoadElementIdentifier = PvidRoadElementIdentifier.create(
                pvidIdentifier,
                geoCoordinate
            )

            if (which == DialogInterface.BUTTON_POSITIVE) {
                when (roadPermission) {
                    RoadPermission.ALLOWED_ROAD_ELEMENT -> {
                        if ((addRoadPermissionsDialog.findViewById(R.id.applyInRadius) as CheckBox)
                                .isChecked
                        ) {
                            val radiusValue =
                                (addRoadPermissionsDialog.findViewById(R.id.valueApplyInRadius)
                                        as EditText)
                                    .text.toString().toFloat()

                            val roadElementsList = allowRoadsInRadius(geoCoordinate, radiusValue)

                            if (!roadElementsList.contains(roadElement)) {
                                roadElementsList.add(roadElement)
                            }

                            for (element in roadElementsList) {
                                dynamicPenalty.addAllowedRoadElement(roadElement)
                            }
                        }
                        dynamicPenalty.addAllowedRoadElement(roadElement)
                    }
                    RoadPermission.ALLOWED_PVID -> {
                        dynamicPenalty.addAllowedPvidRoadElementIdentifier(
                            pvidRoadElementIdentifier
                        )
                    }
                    RoadPermission.RESTRICTED_ROAD -> {
                        dynamicPenalty.addRoadPenalty(
                            PvidRoadElementIdentifier.create(
                                pvidIdentifier,
                                geoCoordinate
                            ),
                            DrivingDirection.DIR_BOTH,
                            MAX_PERMITTED_SPEED
                        )
                    }
                }

                showChangedRoadStatusOnMap(pvidIdentifier, roadElement)
            } else {
                when (roadPermission) {
                    RoadPermission.ALLOWED_ROAD_ELEMENT -> {
                        if (allowedRoadsList.contains(roadElement)) {
                            map.removeMapObject(allowedRoadsList.remove(roadElement)!!)
                        }
                    }
                    RoadPermission.ALLOWED_PVID -> {
                        if (allowedPvidRoadsList.contains(pvidIdentifier)) {
                            map.removeMapObject(allowedPvidRoadsList.remove(pvidIdentifier)!!)
                        }
                    }
                    RoadPermission.RESTRICTED_ROAD -> {
                        if (restrictedRoadsList.contains(pvidIdentifier)) {
                            map.removeMapObject(restrictedRoadsList.remove(pvidIdentifier)!!)
                        }
                    }
                }
            }
        }

        createAlertDialog(
            addRoadPermissionsDialog,
            dialogTitle,
            R.string.button_done,
            R.string.button_remove,
            dialogButtonClickListener
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setGeoCoordinatesDialog(view: View, geoCoordinate: GeoCoordinate) {
        (view.findViewById(R.id.waypointLatitude) as TextView).text =
            "Latitude: ${geoCoordinate.latitude}"
        (view.findViewById(R.id.waypointLongitude) as TextView).text =
            "Longitude: ${geoCoordinate.longitude}"
    }

    private fun createAlertDialog(
        dialogView: View, dialogTitle: Int, positiveDialogButtonTitle: Int,
        negativeDialogButtonTitle: Int, dialogButtonListener: DialogInterface.OnClickListener
    ) {
        AlertDialog.Builder(activity)
            .setView(dialogView)
            .setTitle(dialogTitle)
            .setPositiveButton(positiveDialogButtonTitle, dialogButtonListener)
            .setNegativeButton(negativeDialogButtonTitle, dialogButtonListener)
            .create()
            .show()
    }

    private fun showChangedRoadStatusOnMap(pvidIdentifier: Long, roadElement: RoadElement) {
        val mapPolyline: MapPolyline = MapPolyline(GeoPolyline(roadElement.geometry))
            .setLineWidthDp(MAP_POLYLINE_WIDTH)

        when (roadPermission) {
            RoadPermission.ALLOWED_ROAD_ELEMENT -> {
                mapPolyline.lineColor = Color.GREEN
                allowedRoadsList[roadElement] = mapPolyline
            }
            RoadPermission.ALLOWED_PVID -> {
                mapPolyline.lineColor = Color.GREEN
                allowedPvidRoadsList[pvidIdentifier] = mapPolyline
            }
            RoadPermission.RESTRICTED_ROAD -> {
                mapPolyline.lineColor = Color.RED
                restrictedRoadsList[pvidIdentifier] = mapPolyline
            }
        }

        map.addMapObject(mapPolyline)
    }

    private fun allowRoadsInRadius(
        geoCoordinate: GeoCoordinate,
        radius: Float
    ): ArrayList<RoadElement> {
        val geoBoundingBox = GeoBoundingBox(geoCoordinate, radius * 2, radius * 2)

        return ArrayList(RoadElement.getRoadElements(geoBoundingBox, "MAC"))
    }

    private fun changeDialogContextVisibility(view: View) {
        val roadElementLayout: LinearLayout = view.findViewById(R.id.roadElementLayout)
        val pvidIdLayout: LinearLayout = view.findViewById(R.id.pvidIdLayout)
        val pvidCenterLayout: LinearLayout = view.findViewById(R.id.pvidCenterLayout)

        when (roadPermission) {
            RoadPermission.ALLOWED_ROAD_ELEMENT -> {
                roadElementLayout.visibility = VISIBLE
                pvidIdLayout.visibility = GONE
                pvidCenterLayout.visibility = GONE
            }
            RoadPermission.ALLOWED_PVID -> {
                roadElementLayout.visibility = GONE
                pvidIdLayout.visibility = VISIBLE
                pvidCenterLayout.visibility = VISIBLE
            }
            else -> {
            }
        }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.clearMap) {
            map.removeMapObjects(allowedRoadsList)
            map.removeMapPvidObjects(allowedPvidRoadsList)
            map.removeMapPvidObjects(restrictedRoadsList)

            dynamicPenalty.clearAllRoadPenalties()
            dynamicPenalty.clearAllAllowedRoadElements()

            userWaypointsList.clear()

            map.removeAllMapObjects()
            isRouteReady = false

            true
        } else false
    }

    private fun MapPolyline.setLineWidthDp(width: Int): MapPolyline {
        lineWidth =
            width * DisplayMetrics.DENSITY_DEFAULT / (activity.resources.displayMetrics.densityDpi)

        return this
    }

    private fun Map.removeMapObjects(allowedRoadsList: HashMap<RoadElement, MapObject>) {
        val values: List<MapObject> = allowedRoadsList.values.toList()

        removeMapObjects(values)
    }

    private fun Map.removeMapPvidObjects(allowedRoadsList: HashMap<Long, MapObject>) {
        val values: List<MapObject> = allowedRoadsList.values.toList()

        removeMapObjects(values)
    }
}

