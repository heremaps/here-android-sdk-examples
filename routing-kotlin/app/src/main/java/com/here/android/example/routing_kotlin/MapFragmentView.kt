package com.here.android.example.routing_kotlin

import android.content.DialogInterface
import com.here.android.mpa.routing.DynamicPenalty
import com.here.android.mpa.routing.DynamicPenalty.*
import com.here.android.example.routing_kotlin.MapFragmentView.RoadPermission.*
import android.graphics.Color
import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

class MapFragmentView(activity: AppCompatActivity) {
    private val MIN_ROUTE_WAYPOINTS_COUNT = 2
    private val MAP_POLYLINE_WIDTH = 30
    private val MAX_PERMITTED_SPEED = 50

    val activity = activity
    private val mapFragment = getAndroidXMapFragment()
    lateinit var map: Map
    private lateinit var dynamicPenalty: DynamicPenalty
    private val userWaypointsList = ArrayList<GeoCoordinate>()
    private val allowedRoadsList = HashMap<RoadElement, MapObject>()
    private val allowedPvidRoadsList = HashMap<Long, MapObject>()
    private val restrictedRoadsList = HashMap<Long, MapObject>()
    private lateinit var buttonRoute: Button
    private lateinit var buttonRestrictions: Button

    enum class RoadPermission {
        ALLOWED_ROAD_ELEMENT, ALLOWED_PVID, RESTRICTED_ROAD
    }

    private lateinit var roadPermission: RoadPermission
    var isRouteReady = false
    var isRestrictionsActive = false

    private fun MapPolyline.setLineWidthDp(width: Int) =
        setLineWidth(width * DisplayMetrics.DENSITY_DEFAULT / (activity.resources.displayMetrics.densityDpi))
    private fun Map.removeMapObjects(allowedRoadsList: HashMap<RoadElement, MapObject>) {
        val values: List<MapObject> = allowedRoadsList.values.toList()
        removeMapObjects(values)
    }
    private fun Map.removeMapPvidObjects(allowedRoadsList: HashMap<Long, MapObject>) {
        val values: List<MapObject> = allowedRoadsList.values.toList()
        removeMapObjects(values)
    }

    init {
        initViews()
        initMapFragment()
    }

    private fun getAndroidXMapFragment() =
        activity.supportFragmentManager.findFragmentById(R.id.mapfragment)
                as AndroidXMapFragment

    private fun initViews() {
        roadPermission = ALLOWED_ROAD_ELEMENT
        buttonRoute = activity.findViewById(R.id.btnRoute)
        buttonRoute.setOnClickListener {
            when (isRestrictionsActive) {
                true -> {
                    isRestrictionsActive = false
                    changeButtonsAppointment()
                }
                false, userWaypointsList.size >= 2 -> calculateRoute(userWaypointsList)
            }
        }

        buttonRestrictions = activity.findViewById(R.id.btnAllowedRoads)
        buttonRestrictions.setOnClickListener {
            when (isRestrictionsActive) {
                true -> {
                    map.removeMapObjects(allowedRoadsList)
                    map.removeMapPvidObjects(allowedPvidRoadsList)
                    map.removeMapPvidObjects(restrictedRoadsList)
                    dynamicPenalty.clearAllRoadPenalties()
                    dynamicPenalty.clearAllAllowedRoadElements()
                }
                false -> {
                    isRestrictionsActive = true
                    addOrRemoveAllowedRoad()
                }
            }
        }
    }

    private fun addAppCredentials(): ApplicationContext =
        ApplicationContext(activity).apply {
            // Set application credentials
            setAppIdCode(activity.getString(R.string.credentials_app_id), activity.getString(R.string.credentials_app_code))
            setLicenseKey(activity.getString(R.string.credentials_app_license_key))
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

                    mapFragment.mapGesture!!.addOnGestureListener(
                        createGestureListener(),
                        0,
                        false
                    )
                }

                else -> Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showTruckRestrictions(): EnumSet<Map.FleetFeature> =
        EnumSet.of(Map.FleetFeature.TRUCK_RESTRICTIONS)

    private fun createGestureListener(): MapGesture.OnGestureListener {
        return object : OnGestureListenerAdapter() {
            override fun onTapEvent(p: PointF): Boolean {
                if (isRouteReady) {
                    return false
                }

                val geoCoordinate = map.pixelToGeo(p)!!
                if (isRestrictionsActive) {
                    addRoadsStatusDialog(geoCoordinate)
                    return false
                } else {
                    addWaypointDialog(geoCoordinate)
                }
                return false
            }
        }
    }

    private fun calculateRoute(geoCoordinates: ArrayList<GeoCoordinate>) {
        if (geoCoordinates.size >= MIN_ROUTE_WAYPOINTS_COUNT) {
            val routePlan = RoutePlan()

            for (waypoint in geoCoordinates) {
                routePlan.addWaypoint(RouteWaypoint(waypoint))
            }

            val routeOptions = RouteOptions()
                .setTransportMode(RouteOptions.TransportMode.TRUCK)
                .setRouteType(RouteOptions.Type.SHORTEST)
                .setTruckTrailersCount(1)
                .setTruckWidth(3F)
                .setTruckHeight(4.2F)
                .setTruckLength(6F)
                .setTruckLimitedWeight(50F)
                .setTruckWeightPerAxle(10F)

            routePlan.routeOptions = routeOptions

            CoreRouter().apply {
                setDynamicPenalty(dynamicPenalty)
                calculateRoute(routePlan, RouterListener())
            }

        } else {
            Toast.makeText(activity, "Please add at least 2 waypoints", Toast.LENGTH_SHORT)
                .show()
        }
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

    fun addWaypointDialog(geoCoordinate: GeoCoordinate) {
        val addWaypointDialog: View = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_add_waypoint, null)

        (addWaypointDialog.findViewById(R.id.waypointLatitude) as TextView).text =
            "Latitude: ${geoCoordinate.latitude}"
        (addWaypointDialog.findViewById(R.id.waypointLongitude) as TextView).text =
            "Latitude: ${geoCoordinate.longitude}"


        AlertDialog.Builder(activity)
            .setView(addWaypointDialog)
            .setTitle(R.string.caption_dialog_add_waypoint)
            .setPositiveButton("Done") { dialog, which ->
                userWaypointsList.add(geoCoordinate)
                map.addMapObject(MapMarker(geoCoordinate))
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    fun addRoadsStatusDialog(geoCoordinate: GeoCoordinate) {
        val roadElement = RoadElement.getRoadElement(geoCoordinate, "MAC")!!
        val pvidIdentifier = roadElement.permanentLinkId
        var dialogTitle = activity.getString(R.string.caption_dialog_add_allowed_road)

        val addRoadPermissionsDialog: View
        when (TruckRestrictionsChecker.getTruckRestrictions(roadElement).isEmpty()) {
            true -> {
                roadPermission = RESTRICTED_ROAD
                addRoadPermissionsDialog = LayoutInflater.from(activity)
                    .inflate(R.layout.dialog_add_waypoint, null)

                (addRoadPermissionsDialog.findViewById(R.id.waypointLatitude) as TextView).text =
                    "Latitude: ${geoCoordinate.latitude}"
                (addRoadPermissionsDialog.findViewById(R.id.waypointLongitude) as TextView).text =
                    "Latitude: ${geoCoordinate.longitude}"
            }
            false -> {
                addRoadPermissionsDialog = LayoutInflater.from(activity)
                    .inflate(R.layout.dialog_add_allowed_road, null)
                dialogTitle =
                    activity.getString(R.string.caption_dialog_add_restricted_road)

                (addRoadPermissionsDialog.findViewById(R.id.chooseWayToAddAllowedRoad) as RadioGroup)
                    .setOnCheckedChangeListener { group, checkedId ->
                        when (checkedId) {
                            R.id.roadElement -> {
                                roadPermission = ALLOWED_ROAD_ELEMENT
                                changeDialogContextVisibility(addRoadPermissionsDialog)
                            }
                            R.id.pvid -> {
                                roadPermission = ALLOWED_PVID
                                changeDialogContextVisibility(addRoadPermissionsDialog)

                                (addRoadPermissionsDialog.findViewById(R.id.valuePvid) as EditText)
                                    .setText(pvidIdentifier.toString())

                                val decimalFormat = DecimalFormat("#0.00000")
                                (addRoadPermissionsDialog.findViewById(R.id.valueCenter) as EditText)
                                    .setText(
                                        decimalFormat.format(geoCoordinate.latitude)
                                                + "," + decimalFormat.format(geoCoordinate.longitude)
                                    )
                            }
                        }
                    }
            }
        }

        val dialogButtonClickListener = DialogInterface.OnClickListener { dialog, which ->
            val pvidRoadElementIdentifier = PvidRoadElementIdentifier.create(
                pvidIdentifier,
                geoCoordinate
            )

            if (which == DialogInterface.BUTTON_POSITIVE) {
                when (roadPermission) {
                    ALLOWED_ROAD_ELEMENT -> {
                        if ((addRoadPermissionsDialog.findViewById(R.id.applyInRadius) as CheckBox).isChecked) {
                            val radiusValue =
                                (addRoadPermissionsDialog.findViewById(R.id.valueApplyInRadius) as EditText)
                                    .text.toString().toFloat()

                            val roadElementsList = allowRoadsInRadius(geoCoordinate, radiusValue)
                            if (!roadElementsList.contains(roadElement)) roadElementsList.add(
                                roadElement
                            )
                            for (element in roadElementsList) {
                                dynamicPenalty.addAllowedRoadElement(roadElement)
                            }
                        }
                        dynamicPenalty.addAllowedRoadElement(roadElement)
                    }
                    ALLOWED_PVID -> {
                        dynamicPenalty.addAllowedPvidRoadElementIdentifier(
                            pvidRoadElementIdentifier
                        )
                    }
                    RESTRICTED_ROAD -> {
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
                    ALLOWED_ROAD_ELEMENT -> {
                        if (allowedRoadsList.contains(roadElement)) {
                            map.removeMapObject(allowedRoadsList.remove(roadElement)!!)
                        }
                    }
                    ALLOWED_PVID -> {
                        if (allowedPvidRoadsList.contains(pvidIdentifier)) {
                            map.removeMapObject(allowedPvidRoadsList.remove(pvidIdentifier)!!)
                        }
                    }
                    RESTRICTED_ROAD -> {
                        if (restrictedRoadsList.contains(pvidIdentifier)) {
                            map.removeMapObject(restrictedRoadsList.remove(pvidIdentifier)!!)
                        }
                    }
                }
            }
        }

        AlertDialog.Builder(activity)
            .setView(addRoadPermissionsDialog)
            .setTitle(dialogTitle)
            .setPositiveButton("Done", dialogButtonClickListener)
            .setNegativeButton("Remove", dialogButtonClickListener)
            .create()
            .show()
    }

    private fun showChangedRoadStatusOnMap(pvidIdentifier: Long, roadElement: RoadElement) {
        val mapPolyline: MapPolyline = MapPolyline(GeoPolyline(roadElement.geometry))
            .setLineWidthDp(MAP_POLYLINE_WIDTH)

        when (roadPermission) {
            ALLOWED_ROAD_ELEMENT -> {
                mapPolyline.lineColor = Color.GREEN
                allowedRoadsList[roadElement] = mapPolyline
            }
            ALLOWED_PVID -> {
                mapPolyline.lineColor = Color.GREEN
                allowedPvidRoadsList[pvidIdentifier] = mapPolyline
            }
            RESTRICTED_ROAD -> {
                restrictedRoadsList[pvidIdentifier] = mapPolyline
                mapPolyline.lineColor = Color.RED
            }
        }
        map.addMapObject(mapPolyline)
    }

    private fun allowRoadsInRadius(geoCoordinate: GeoCoordinate, radius: Float): ArrayList<RoadElement> {
        val geoBoundingBox = GeoBoundingBox(geoCoordinate, radius * 2, radius * 2)
        return RoadElement.getRoadElements(geoBoundingBox, "MAC") as ArrayList<RoadElement>
    }

    private fun changeDialogContextVisibility(view: View) {
        val roadElementLayout: LinearLayout =
            view.findViewById(R.id.roadElementLayout)
        val pvidIdLayout: LinearLayout =
            view.findViewById(R.id.pvidIdLayout)
        val pvidCenterLayout: LinearLayout =
            view.findViewById(R.id.pvidCenterLayout)

        when (roadPermission) {
            ALLOWED_ROAD_ELEMENT -> {
                roadElementLayout.visibility = VISIBLE
                pvidIdLayout.visibility = GONE
                pvidCenterLayout.visibility = GONE
            }
            ALLOWED_PVID -> {
                roadElementLayout.visibility = GONE
                pvidIdLayout.visibility = VISIBLE
                pvidCenterLayout.visibility = VISIBLE
            }
            else -> {}
        }
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clearMap -> {
                map.removeAllMapObjects()
                userWaypointsList.clear()
                isRouteReady = false
                true
            }
            else -> false
        }
    }

    private fun addOrRemoveAllowedRoad() {
        isRestrictionsActive = true

        changeButtonsAppointment()
    }

    private fun changeButtonsAppointment() {
        if (isRestrictionsActive) {
            buttonRoute.text = activity.getString(R.string.btn_caption_backToRoute)
            buttonRestrictions.text = activity.getString(R.string.btn_caption_removeAllChangedRoads)
        } else {
            buttonRoute.text = activity.getString(R.string.btn_caption_calculateRoute)
            buttonRestrictions.text = activity.getString(R.string.btn_caption_addChangedRoad)
        }
    }

}