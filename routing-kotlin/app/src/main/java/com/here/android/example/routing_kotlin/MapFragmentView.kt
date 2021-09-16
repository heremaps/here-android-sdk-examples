package com.here.android.example.routing_kotlin

import android.content.DialogInterface
import android.content.Intent
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
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapGesture.OnGestureListener.OnGestureListenerAdapter
import com.here.android.mpa.routing.*
import java.io.File
import java.io.Serializable
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class MapFragmentView(activity: AppCompatActivity) {
    val activity = activity
    val m_mapFragment = getAndroidXMapFragment()
    lateinit var m_map: Map
    lateinit var m_dynamicPenalty: DynamicPenalty
    val userWaypoints = ArrayList<GeoCoordinate>()
    lateinit var buttonRoute: Button
    lateinit var buttonRestrictions: Button

    lateinit var roadPermission: RoadPermission
    var isRouteReady = false
    var isRestrictionsActive = false

    fun MapPolyline.setLineWidthDp(width: Int) =
        setLineWidth(width * DisplayMetrics.DENSITY_DEFAULT/ (activity.resources.displayMetrics.densityDpi))
    
    init {
        initViews()
        initMapFragment()
    }

    private fun getAndroidXMapFragment() =
        activity.supportFragmentManager.findFragmentById(R.id.mapfragment)
                as AndroidXMapFragment

    private fun initViews() {
        roadPermission = RoadPermission.ROAD_ELEMENT
        buttonRoute = activity.findViewById(R.id.btnRoute)
        buttonRoute.setOnClickListener {
            when (isRestrictionsActive) {
                true -> {
                    isRestrictionsActive = false
                    changeButtonsAppointment()
                }
                false, userWaypoints.size >= 2 -> calculateRoute(userWaypoints)
            }
        }

        buttonRestrictions = activity.findViewById(R.id.btnAllowedRoads)
        buttonRestrictions.setOnClickListener {
            when (isRestrictionsActive) {
                true -> m_dynamicPenalty.clearAllAllowedRoadElements()
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
            setAppIdCode("3BB1TRFCHg8KnzBw6yCs", "GUkhoasO18lh5ZUHMIaxOg")
            setLicenseKey("NKjz8taBG2qvviobojfLgORLfGSO3MUamOK6cuaj+9HRlDSuH7I4QhlFGwD5kCRzwKbwaqcTUss/lACy5bk+e92oNIt/kLaKRfNw5XPNvRcIfEELiYoj3woDYvcHTjwAa7sCEGRsYamD/XOaeTYzx77Genesuo1iXyhY7+yKPqVfkX44kILZJYxJrh4THZdUtZUQJ3NXua8V0ZHAmeQa7ZzrM8Ry5OE5fHlOMYHSpltGzYiuuJP1SE8Y62mahIj4gZuD5zidyecrni8+toN5PxyIF1s86Dey2WJB6dPdS4L+YeqfXvsuIj+BqeSS78KpRLefcwE6PX0cO5bUpM3AvB3mmfCk6quxOy/tL1L6XV1uo0HnhPyYPwo/H5aB+XXMjmVSDmTV0C40A5VlyEM4267vRGpgQQ4pjew3z2c3jn02094GQrcjuOnFdgTDt2uNgRFnWUxGHKVEneRsDDuBnx+0BPEJcuOIKQAyG6SlFYJ9Vc25T5Qg6srZNmL8bizGqt/LOa1IZSXsggizoPCh4hvXKnSxV88zLrTxNP+PRdh6LDw6Fs0eG9JzJe7Qt/V35VylfY3ZRrmabMx9latVc+aOVdZ7eBp+B5G/BfY3bOiwvORq/4OpBm7UqZFaYkMq/qUJ5uc7jeo7eUY6iBJkIiEhxhJWRPCsPzMMkosXFs8=")
        }

    private fun initMapFragment() {
        // This will use external storage to save map cache data, it is also possible to set
        // private app's path
        val path = File(activity.getExternalFilesDir(null), ".here-map-data")
            .absolutePath
        // This method will throw IllegalArgumentException if provided path is not writable
        com.here.android.mpa.common.MapSettings.setDiskCacheRootPath(path)

        val applicationContext: ApplicationContext = ApplicationContext(activity)

        m_mapFragment.init(addAppCredentials()) { error ->
            when (error) {
                OnEngineInitListener.Error.NONE -> {
                    m_map = m_mapFragment.map!!

                    m_map.fleetFeaturesVisible = showTruckRestrictions()
                    m_dynamicPenalty = DynamicPenalty()

                    m_mapFragment.mapGesture!!.addOnGestureListener(
                        createGestureListener(),
                        0,
                        false
                    )
                }

                else -> Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun showTruckRestrictions(): EnumSet<Map.FleetFeature> = EnumSet.of(Map.FleetFeature.TRUCK_RESTRICTIONS)

    private fun createGestureListener(): MapGesture.OnGestureListener {
        return object : OnGestureListenerAdapter() {
            override fun onTapEvent(p: PointF): Boolean {
                if (isRouteReady) {
                    return false
                }

                val geoCoordinate = m_map.pixelToGeo(p)!!
                if (isRestrictionsActive) {
                    addOrRemoveTruckRestrictionsDialog(geoCoordinate)
                    return false
                } else {
                    addWaypointDialog(geoCoordinate)
                }
                return false
            }
        }
    }

    fun calculateRoute(geoCoordinates: ArrayList<GeoCoordinate>) {
        if (geoCoordinates.size >= 2) {
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
                setDynamicPenalty(m_dynamicPenalty)
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
                m_map.addMapObject(MapRoute(routeCalculationResult[0].route))

                isRouteReady = true
            } else {
                Toast.makeText(
                    activity, "Error occured: " +
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
                userWaypoints.add(geoCoordinate)
                m_map.addMapObject(MapMarker(geoCoordinate))
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    fun addOrRemoveTruckRestrictionsDialog(geoCoordinate: GeoCoordinate) {
        val addRestrictionsDialog: View = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_add_or_remove_restrictions, null)

        val roadElement = RoadElement.getRoadElement(geoCoordinate, "MAC")!!

        (addRestrictionsDialog.findViewById(R.id.chooseWayToAddAllowedRoad) as RadioGroup)
            .setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.roadElement -> {
                        roadPermission = RoadPermission.ROAD_ELEMENT
                        changeDialogContextVisibility(addRestrictionsDialog)
                    }
                    R.id.pvid -> {
                        roadPermission = RoadPermission.PVID
                        changeDialogContextVisibility(addRestrictionsDialog)

                        (addRestrictionsDialog.findViewById(R.id.valuePvid) as EditText)
                            .setText(roadElement.permanentLinkId.toString())

                        val decimalFormat = DecimalFormat("#0.00000")
                        (addRestrictionsDialog.findViewById(R.id.valueCenter) as EditText)
                            .setText(
                                decimalFormat.format(geoCoordinate.latitude)
                                        + "," + decimalFormat.format(geoCoordinate.longitude)
                            )
                    }
                }
            }

        val positiveButtonClick = DialogInterface.OnClickListener { dialog, which ->
            when (roadPermission) {
                RoadPermission.ROAD_ELEMENT -> {
                    val roadElementsList: ArrayList<RoadElement>
                    if ((addRestrictionsDialog.findViewById(R.id.applyInRadius) as CheckBox).isChecked) {
                        val radiusValue =
                            (addRestrictionsDialog.findViewById(R.id.valueApplyInRadius) as EditText)
                                .text.toString().toFloat()

                        roadElementsList = allowRoadsInRadius(geoCoordinate, radiusValue)
                        if (!roadElementsList.contains(roadElement)) roadElementsList.add(
                            roadElement
                        )
                        for (element in roadElementsList) {
                            m_dynamicPenalty.addAllowedRoadElement(roadElement)
                        }
                    }
                }
                RoadPermission.PVID -> ""
            }
            showAllowedRoadOnMap(roadElement)
//            if (which == DialogInterface.BUTTON_POSITIVE) {
//                m_dynamicPenalty.addRoadPenalty(
//                    DynamicPenalty.PvidRoadElementIdentifier.create(
//                        roadElement.permanentLinkId,
//                        geoCoordinate
//                    ),
//                    DrivingDirection.DIR_BOTH,
//                    50
//                )
//            }
        }

        AlertDialog.Builder(activity)
            .setView(addRestrictionsDialog)
            .setTitle(R.string.caption_dialog_add_allowed_road)
            .setPositiveButton("Done", positiveButtonClick)
            .setNegativeButton("Remove", null)
            .create()
            .show()
    }

    fun showAllowedRoadOnMap(roadElement: RoadElement) {
        when (roadPermission) {
            RoadPermission.ROAD_ELEMENT -> {
                /**
                 *
                 */
            }
            RoadPermission.PVID -> {
                /**
                 *
                 */
            }
        }
        val mapPolyline: MapPolyline = MapPolyline(GeoPolyline(roadElement.geometry))
            .setLineColor(Color.GREEN)
        mapPolyline.setLineWidthDp(30)
        m_map.addMapObject(mapPolyline)
    }

    fun allowRoadsInRadius(geoCoordinate: GeoCoordinate, radius: Float): ArrayList<RoadElement> {
        val geoBoundingBox = GeoBoundingBox(geoCoordinate, radius * 2, radius * 2)
        return RoadElement.getRoadElements(geoBoundingBox, "MAC") as ArrayList<RoadElement>
    }

    fun changeDialogContextVisibility(view: View) {
        val roadElementLayout: LinearLayout =
            view.findViewById(R.id.roadElementLayout)
        val pvidIdLayout: LinearLayout =
            view.findViewById(R.id.pvidIdLayout)
        val pvidCenterLayout: LinearLayout =
            view.findViewById(R.id.pvidCenterLayout)

        when (roadPermission) {
            RoadPermission.ROAD_ELEMENT -> {
                roadElementLayout.visibility = VISIBLE
                pvidIdLayout.visibility = GONE
                pvidCenterLayout.visibility = GONE
            }
            RoadPermission.PVID -> {
                roadElementLayout.visibility = GONE
                pvidIdLayout.visibility = VISIBLE
                pvidCenterLayout.visibility = VISIBLE
            }
        }
    }

    fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = activity.menuInflater
        inflater.inflate(R.menu.routing_menu, menu)
        return true
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clearMap -> {
                m_map.removeAllMapObjects()
                userWaypoints.clear()
                isRouteReady = false
                true
            }
            else -> false
        }
    }

    fun addOrRemoveAllowedRoad() {
        isRestrictionsActive = true

        changeButtonsAppointment()
    }

    fun changeButtonsAppointment() {
        if (isRestrictionsActive) {
            buttonRoute.text = activity.getString(R.string.btn_caption_backToRoute)
            buttonRestrictions.text = activity.getString(R.string.btn_caption_removeAllAllowedRoad)
        } else {
            buttonRoute.text = activity.getString(R.string.btn_caption_calculateRoute)
            buttonRestrictions.text = activity.getString(R.string.btn_caption_addOrRemAllowedRoad)
        }
    }

}

enum class RoadPermission {
    ROAD_ELEMENT, PVID
}