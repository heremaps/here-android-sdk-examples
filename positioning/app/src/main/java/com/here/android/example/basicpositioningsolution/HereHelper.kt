package com.here.android.example.basicpositioningsolution

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.here.android.mpa.common.Image
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

fun Context.getBitmapFromVectorDrawable(@DrawableRes drawableId: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(this, drawableId)
    val bitmap = Bitmap.createBitmap(
        drawable!!.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun Image.rotateAndSkewImage(srcBitmap: Bitmap, angle: Float, tilt: Float) {
    val old = bitmap
    setBitmap(srcBitmap.rotateAndSkewBitmap(angle, tilt))
    old?.recycle()
}

fun Bitmap.rotateAndSkewBitmap(angle: Float, tilt: Float): Bitmap {
    val transformationCamera = Camera()
    val matrix = Matrix()
    transformationCamera.rotateX(-tilt);
    transformationCamera.rotateY(0f);
    transformationCamera.rotateZ(0f);
    transformationCamera.getMatrix(matrix);

    val centerX = this.width / 2f;
    val centerY = this.height / 2f;
    matrix.preRotate(angle)
    matrix.preTranslate(-centerX, -centerY)
    matrix.postTranslate(centerX, centerY)

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun getArrowBitmap(context: Context):Bitmap {
    return context.getBitmapFromVectorDrawable(R.drawable.ic_map_arrow)
}

val Map.mapTransformationStateFlow: Flow<MapTransformationState>
    get() = trackMapTransform()
        .let { combine(it, it.scanCountActiveTransform(), ::Pair) }
        .conflate()
        .map { (event, count) ->
            when (event) {
                MapTransformEvent.MapTransformStart ->
                    MapTransformationState(
                        activeTransformationCount = count.toInt(),
                        mapState = null
                    )
                is MapTransformEvent.MapTransformEnd -> {
                    MapTransformationState(
                        activeTransformationCount = count.toInt(),
                        mapState = event.mapState
                    )
                }
            }
        }

data class MapTransformationState(
    val activeTransformationCount: Int,
    val mapState: MapState? = null
)

sealed class MapTransformEvent {
    object MapTransformStart : MapTransformEvent()
    data class MapTransformEnd(val mapState: MapState) : MapTransformEvent()
}


fun Map.trackMapTransform(): Flow<MapTransformEvent> =
    callbackFlow {
        val listener = object : Map.OnTransformListener {
            override fun onMapTransformStart() {
                trySend(MapTransformEvent.MapTransformStart)
            }

            override fun onMapTransformEnd(mapState: MapState) {
                trySend(MapTransformEvent.MapTransformEnd(mapState))
            }
        }
        addTransformListener(listener)
        awaitClose { removeTransformListener(listener) }
    }

fun Flow<MapTransformEvent>.scanCountActiveTransform(): Flow<Long> =
    scan(0L) { acc, event ->
        return@scan when (event) {
            is MapTransformEvent.MapTransformEnd -> (acc - 1).coerceAtLeast(0)
            MapTransformEvent.MapTransformStart -> acc + 1
        }
    }.conflate()