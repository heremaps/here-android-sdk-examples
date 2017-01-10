/*
 * Copyright © 2011-2016 HERE Europe B.V.
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

package com.here.android.example.map.basic;

import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;

import android.app.Activity;
import android.widget.Toast;

/**
 * This class encapsulates the properties and functionality of the Map view.
 */
public class MapFragmentView {
    private MapFragment m_mapFragment;
    private Activity m_activity;
    private Map m_map;

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
                    } else {
                        Toast.makeText(m_activity,
                                "ERROR: Cannot initialize Map with error " + error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }
}
