package com.here.android.example.kotlin

import androidx.lifecycle.ViewModel
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.guidance.NavigationManager
import com.here.android.mpa.routing.Route

class MapViewModel : ViewModel() {
    private val listOfGeoCoordinates: ArrayList<GeoCoordinate> = ArrayList()
    private var route: Route? = null
    private var navigationManager: NavigationManager? = null

    fun addGeoCoordinate(geoCoordinate: GeoCoordinate) {
        listOfGeoCoordinates.add(geoCoordinate)
    }

    fun getGeoCoordinatesList() : ArrayList<GeoCoordinate> {
        return listOfGeoCoordinates
    }

    fun clearGeoCoordinatesList() {
        listOfGeoCoordinates.clear()
    }

    fun setRoute(route: Route?) {
        this.route = route
    }

    fun getRoute() = route

    fun setNavigationManager(navigationManager: NavigationManager?) {
        this.navigationManager = navigationManager
    }

    fun getNavigationManager() = navigationManager
}