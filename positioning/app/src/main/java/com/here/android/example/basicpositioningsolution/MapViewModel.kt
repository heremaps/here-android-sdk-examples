package com.here.android.example.basicpositioningsolution

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.here.android.mpa.mapping.Map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MapViewModel : ViewModel() {

    private lateinit var arrowRotationCallback: (ArrowState) -> Unit
    private var map: Map? = null
    private val mapFlow: MutableSharedFlow<Map> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val userAction: MutableSharedFlow<MapUserAction> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _heading: MutableSharedFlow<Double> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val heading: Flow<Float> by lazy {
        _heading
            .map { it.roundToInt().toFloat() }
            .conflate()
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .shareIn(scope = viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())
    }

    private val hasUserAction by lazy {
        userAction
            .conflate()
            .flowOn(Dispatchers.Default)
            .shareIn(scope = viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())
    }

    private val mapTransformationFlow by lazy {
        mapFlow.flatMapLatest { it.mapTransformationStateFlow }
            .conflate()
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .shareIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(1000))
    }
    private val arrowState: Flow<ArrowState> by lazy {
        combine(
            hasUserAction,
            heading,
            mapTransformationFlow
        ) { _isUserJustMadeActionTrigger, heading, mapTransformationTriggerEvent ->
            if (map != null) {
                Log.d(
                    "ArrowState",
                    "map != null, heading " + heading + "map!!.orientation " + map!!.orientation
                )
                val arrowRotationAngle = heading - map!!.orientation
                ArrowState(
                    arrowRotationAngle.roundToInt().toFloat(),
                    mapTransformationTriggerEvent.mapState?.tilt ?: map!!.tilt
                )
            } else {
                Log.d("ArrowState", "map == null")
                null
            }

        }.filterNotNull()
            .conflate()
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .shareIn(scope = viewModelScope, replay = 1, started = SharingStarted.WhileSubscribed())
    }

    init {
        viewModelScope.launch(Dispatchers.Main) {
            arrowState
                .collectLatest {
                    arrowRotationCallback?.invoke(it)
                }
        }
    }

    fun onUserAction(mapUserAction: MapUserAction) {
        userAction.tryEmit(mapUserAction)
    }

    fun onHeading(heading: Double) {
        _heading.tryEmit(heading)
    }

    fun addMap(map: Map) {
        this.map = map
        mapFlow.tryEmit(map)
    }

    fun setArrowCallback(callback: (ArrowState) -> Unit) {
        this.arrowRotationCallback = callback
    }

}