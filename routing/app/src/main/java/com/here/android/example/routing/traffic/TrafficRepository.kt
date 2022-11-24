package com.here.android.example.routing.traffic

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.here.android.mpa.common.Identifier
import com.here.android.mpa.common.RoadElement
import com.here.android.mpa.guidance.TrafficUpdater
import com.here.android.mpa.mapping.TrafficEvent
import com.here.android.mpa.mapping.TrafficEvent.Severity
import com.here.android.mpa.routing.Route
import com.here.android.mpa.routing.RouteElement
import com.here.android.mpa.routing.RouteElements
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToLong

data class RouteTrafficInfo(
    val startPercent: Float,
    val endPercent: Float,
    val trafficSeverity: Severity
)

data class RelativeTrafficInfoMeters(
    val roadElement: RoadElement,
    val start: Long,
    val length: Long,
    val trafficSeverity: Severity,
)

object TrafficRepository {


    private val trafficUpdater by lazy { TrafficUpdater.getInstance() }
    private var job: Job? = null

    fun loadTrafficForRoute(activity: AppCompatActivity, route: Route, block: (List<RouteTrafficInfo>) -> Unit) {
        job?.cancel()
        job = activity.lifecycleScope.launch {
            var routeTrafficInfoList = getTrafficForRoute(route, false)

            withContext(Dispatchers.Main) {
                block(routeTrafficInfoList)
            }

            while (isActive) {
                delay(15_000L)
                routeTrafficInfoList = getTrafficForRoute(route, true)

                withContext(Dispatchers.Main) {
                    block(routeTrafficInfoList)
                }
            }
        }
    }

    fun cancelRequest() {
        job?.cancel()
    }

    private suspend fun getTrafficForRoute(
        route: Route,
        forceRequest: Boolean
    ): List<RouteTrafficInfo> = try {
        if (forceRequest) {
            route.routeElements?.let {
                requestTrafficUpdate(it)
            }
        }
        val trafficEventList = fetchTrafficEventList(route)
        val routeElements = route.routeElements
        if (routeElements != null ) {
            val affectedRouteElements = routeElements.elements
                .getRouteRangesTraffic()
                .applyTrafficEvents(trafficEventList.filter { it.isOnRoute(route) })

            val packedTrafficInfo = affectedRouteElements.values.toList()
                .pack()
                .toInterval(route.length.toLong())

            packedTrafficInfo
        } else {
            emptyList()
        }
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        Log.e(this.javaClass.name, "getTrafficForRoute", ex)
        emptyList()
    }

    private suspend fun fetchTrafficEventList(route: Route): List<TrafficEvent> =
        suspendCoroutine { continuation ->
            val listener = TrafficUpdater.GetEventsListener { eventsList, error ->
                if (error != TrafficUpdater.Error.NONE) {
                    Log.e(this.javaClass.name, "fetchTrafficEventList $error")
                    continuation.resumeWith(Result.success(emptyList()))
                } else {
                    Log.v(this.javaClass.name, "fetchTrafficEventList ${eventsList.filter { it.severity != TrafficEvent.Severity.NORMAL }}")
                    continuation.resumeWith(Result.success(eventsList))
                }
            }
            trafficUpdater.getEvents(route, listener)
        }

    private suspend fun requestTrafficUpdate(routeElements: RouteElements): TrafficUpdater.RequestState =
        suspendCancellableCoroutine { continuation ->
            val listener = TrafficUpdater.Listener { requestState ->
                continuation.resume(requestState)
            }

            val requestInfo = trafficUpdater.request(routeElements, listener)

            if (requestInfo.error != TrafficUpdater.Error.NONE)
                continuation.resumeWithException(
                    IllegalStateException("an error occurred while requesting ${requestInfo.error}}")
                )
            continuation.invokeOnCancellation {
                trafficUpdater.cancelRequest(requestInfo.requestId)
            }
        }

    /**
     * Maps ALL current route RoadElements to RelativeTrafficInfoMeters
     */
    private fun List<RouteElement>.getRouteRangesTraffic(): MutableMap<Identifier, RelativeTrafficInfoMeters> {
        val internalRoadElements = this.mapNotNull { it.roadElement }

        val map: MutableMap<Identifier, RelativeTrafficInfoMeters> = HashMap()
        var lastFinish = 0L
        internalRoadElements
            .map { Triple(it, it.identifier, it.geometryLength.roundToLong()) }
            .forEach { (routeElement, id, length) ->
                id?.let { itemId ->
                    map[itemId] =
                        RelativeTrafficInfoMeters(
                            routeElement,
                            lastFinish,
                            length,
                            TrafficEvent.Severity.NORMAL
                        )
                    lastFinish += length
                }
            }
        return map
    }

    /**
     * Applies traffic severity info to route road elements
     */
    private fun MutableMap<Identifier, RelativeTrafficInfoMeters>.applyTrafficEvents(eventsList: List<TrafficEvent>): Map<Identifier, RelativeTrafficInfoMeters> {
        eventsList.filter { it.severity != TrafficEvent.Severity.NORMAL }
            .flatMap { event ->
                event.affectedRoadElements.map { roadElement ->
                    Pair(
                        roadElement.identifier,
                        event.severity
                    )
                }
            }
            .forEach { (id, severity) ->
                id?.let { itemId ->
                    this[itemId]?.copy(trafficSeverity = severity)?.let {
                        this[itemId] = it
                    }
                }
            }
        return this.toMap()
    }

    /**
     * Merge sibling [RelativeTrafficInfoMeters] elements anc calculate merged length.
     */
    private fun List<RelativeTrafficInfoMeters>.pack(): List<RelativeTrafficInfoMeters> {
        val packedTrafficList = emptyList<RelativeTrafficInfoMeters>().toMutableList()
        this.sortedBy { it.start }
            .forEach {
                if (packedTrafficList.isEmpty())
                    packedTrafficList.add(it)
                else {
                    val last = packedTrafficList.last()
                    if (last.trafficSeverity == it.trafficSeverity) {
                        packedTrafficList.removeLast()
                        // merge lengths
                        packedTrafficList.add(last.copy(length = last.length + it.length))
                    } else {
                        packedTrafficList.add(it)
                    }
                }
            }

        return packedTrafficList
    }

    private fun List<RelativeTrafficInfoMeters>.toInterval(routeLength: Long): List<RouteTrafficInfo> {
        return this.map {
            RouteTrafficInfo(
                it.start.fractionForRange(0, routeLength),
                (it.start + it.length).fractionForRange(0, routeLength),
                it.trafficSeverity
            )
        }
    }

    fun Long.fractionForRange(start: Long, end: Long): Float =
        coerceIn(start, end).let { (it - start) / (end - start).toFloat() }
}

