/*
 * Copyright © 2011-2017 HERE Europe B.V.
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

package com.here.android.example.map.gesture;

import java.io.IOException;
import java.util.List;

import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapScreenMarker;

import android.app.Activity;
import android.graphics.PointF;
import android.os.Handler;
import android.widget.Toast;

/**
 * This class encapsulates the properties and functionality of the Map view.
 */
public class MapFragmentView {
    private MapFragment m_mapFragment;
    private Activity m_activity;
    private Map m_map;

    private static Image m_marker_image;
    private MapScreenMarker m_tap_marker;

    public MapFragmentView(Activity activity) {
        m_activity = activity;
        initMapFragment();
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = (MapFragment) m_activity.getFragmentManager()
                .findFragmentById(R.id.mapfragment);

        if (m_mapFragment != null) {
            /* Initialize the MapFragment, results will be given via the called back. */
            m_mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {

                    if (error == Error.NONE) {
                        /*
                         * If no error returned from map fragment initialization, the map will be
                         * rendered on screen at this moment.Further actions on map can be provided
                         * by calling Map APIs.
                         */
                        m_map = m_mapFragment.getMap();
                        /* create an image to mark coordinate when tap event happens */
                        m_marker_image = new Image();

                        try {
                            m_marker_image.setImageResource(R.drawable.marker);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        m_mapFragment.getMapGesture()
                                .addOnGestureListener(new MapGesture.OnGestureListener() {
                                    @Override
                                    public void onPanStart() {
                                        showMsg("onPanStart");
                                    }

                                    @Override
                                    public void onPanEnd() {
                                        /* show toast message for onPanEnd gesture callback */
                                        showMsg("onPanEnd");
                                    }

                                    @Override
                                    public void onMultiFingerManipulationStart() {

                                    }

                                    @Override
                                    public void onMultiFingerManipulationEnd() {

                                    }

                                    @Override
                                    public boolean onMapObjectsSelected(List<ViewObject> list) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onTapEvent(PointF pointF) {
                                        /* show toast message for onPanEnd gesture callback */
                                        showMsg("onTapEvent");
                                        /*
                                         * add map screen marker at coordinates of gesture. if map
                                         * screen marker already exists, change to new coordinate
                                         */
                                        if (m_tap_marker == null) {
                                            m_tap_marker = new MapScreenMarker(pointF,
                                                    m_marker_image);
                                            m_map.addMapObject(m_tap_marker);

                                        } else {
                                            m_tap_marker.setScreenCoordinate(pointF);
                                        }

                                        return false;
                                    }

                                    @Override
                                    public boolean onDoubleTapEvent(PointF pointF) {
                                        return false;
                                    }

                                    @Override
                                    public void onPinchLocked() {

                                    }

                                    @Override
                                    public boolean onPinchZoomEvent(float v, PointF pointF) {
                                        return false;
                                    }

                                    @Override
                                    public void onRotateLocked() {

                                    }

                                    @Override
                                    public boolean onRotateEvent(float v) {
                                        /* show toast message for onRotateEvent gesture callback */
                                        showMsg("onRotateEvent");
                                        return false;
                                    }

                                    @Override
                                    public boolean onTiltEvent(float v) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onLongPressEvent(PointF pointF) {
                                        return false;
                                    }

                                    @Override
                                    public void onLongPressRelease() {

                                    }

                                    @Override
                                    public boolean onTwoFingerTapEvent(PointF pointF) {
                                        return false;
                                    }
                                });
                    } else {
                        Toast.makeText(m_activity,
                                "ERROR: Cannot initialize Map with error " + error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }

    /**
     * This utility function is intended to show message for gestures callback
     */
    private void showMsg(String msg) {
        final Toast msgToast = Toast.makeText(m_activity, msg, Toast.LENGTH_SHORT);

        msgToast.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                msgToast.cancel();
            }
        }, 1000);

    }
}
