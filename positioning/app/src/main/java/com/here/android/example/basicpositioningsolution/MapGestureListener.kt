package com.here.android.example.basicpositioningsolution

import android.graphics.PointF
import com.here.android.mpa.common.ViewObject
import com.here.android.mpa.mapping.MapGesture

internal class MapGestureListener(
    private val userAction: (mapUserAction: MapUserAction) -> Unit
) : MapGesture.OnGestureListener {


    override fun onPanStart() {
        userAction(MapUserAction.CONTINUOUS_ACTION_STARTED)
    }

    override fun onPanEnd() {
        userAction(MapUserAction.CONTINUOUS_ACTION_FINISHED)
    }

    override fun onMultiFingerManipulationStart() {
        userAction(MapUserAction.CONTINUOUS_ACTION_STARTED)
    }

    override fun onMultiFingerManipulationEnd() {
        userAction(MapUserAction.CONTINUOUS_ACTION_FINISHED)
    }

    override fun onMapObjectsSelected(p0: MutableList<ViewObject>): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }

    override fun onTapEvent(p0: PointF): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }

    override fun onDoubleTapEvent(p0: PointF): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }

    override fun onPinchLocked() {
        userAction(MapUserAction.ONE_TIME_ACTION)
    }

    override fun onPinchZoomEvent(p0: Float, p1: PointF): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }

    override fun onRotateLocked() {
        userAction(MapUserAction.ONE_TIME_ACTION)
    }

    override fun onRotateEvent(p0: Float): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }

    override fun onTiltEvent(p0: Float): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }

    override fun onLongPressEvent(p0: PointF): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }

    override fun onLongPressRelease() {
        userAction(MapUserAction.ONE_TIME_ACTION)
    }

    override fun onTwoFingerTapEvent(p0: PointF): Boolean {
        userAction(MapUserAction.ONE_TIME_ACTION)
        return false
    }
}