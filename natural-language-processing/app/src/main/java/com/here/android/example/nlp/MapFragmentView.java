/*
 * Copyright (c) 2011-2018 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.android.example.nlp;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.nlp.Error;
import com.here.android.mpa.nlp.Intention;
import com.here.android.mpa.nlp.Nlp;
import com.here.android.mpa.nlp.Nlp.OnInitializationListener;
import com.here.android.mpa.nlp.Place;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.search.CategoryFilter;
import com.here.android.mpa.search.PlaceLink;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the properties and functionality of the Map view.It also implements the
 * functionality of the Natural Language Processing (NLP) included in HERE Android SDK.
 * As an example of NLP functionality, 3 different listeners are implemented to support
 * a search request, a clear request and show how to receive the detailed intent from NLP.
 * Volume UP key has been used as an example how NLP can start listening to utterances like
 * "Search for restaurants" or "Clear the map".
 */
public class MapFragmentView {

    private Activity m_activity;

    private Map m_map;

    private MapFragment m_mapFragment;

    private List<MapObject> m_mapObjectList = new ArrayList<>();

    private static volatile Nlp m_nlp = null;

    private static volatile boolean m_nlpInitialized = false;

    private MyASR m_myAsr = null;

    public MapFragmentView(Activity activity) {
        m_activity = activity;
        /*
         * The map fragment is not required for executing search requests. However in this example,
         * we will put some markers on the map to visualize the location of the search results.
         */
        initMapFragment();
    }

    // Google has deprecated android.app.Fragment class. It is used in current SDK implementation.
    // Will be fixed in future SDK version.
    @SuppressWarnings("deprecation")
    private MapFragment getMapFragment() {
        return (MapFragment) m_activity.getFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        // Locate the mapFragment UI element
        m_mapFragment = getMapFragment();

        // Set path of isolated disk cache
        String diskCacheRoot = Environment.getExternalStorageDirectory().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = m_activity.getPackageManager().getApplicationInfo(m_activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(), "Failed to find intent name, NameNotFound: " + e.getMessage());
        }

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot, intentName);
        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            if (m_mapFragment != null) {
                // Initialize the MapFragment, results will be given via the called back.
                m_mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                        if (error == Error.NONE) {
                            m_map = m_mapFragment.getMap();
                            m_map.setCenter(new GeoCoordinate(49.259149, -123.008555),
                                    Map.Animation.NONE);
                            m_map.setZoomLevel(13.2);

                            // Show position indicator and accuracy aura
                            m_mapFragment.getPositionIndicator()
                                    .setVisible(true)
                                    .setAccuracyIndicatorVisible(true);

                            // Start the positioning manager
                            PositioningManager.getInstance().
                                    start(PositioningManager.LocationMethod.GPS_NETWORK);

                            // Create Map NLP object to control voice operations
                            // Pass Activity as a Context!!!
                            m_nlp = Nlp.getInstance();
                            m_myAsr = new MyASR(m_activity.getApplicationContext());
                            m_nlp.init(m_activity, m_mapFragment, null, m_myAsr, m_nlpListener);
                            m_nlp.addListener(m_routeListener);
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

    /**
     * Represents a listener to provide notification of the Nlp status
     * upon completion of initialization.
     */
    private OnInitializationListener m_nlpListener = new OnInitializationListener() {
        @Override
        public void onComplete(Error error) {
            if (error == Error.NONE) {
                // NLP is initialized
                m_nlpInitialized = true;

                m_myAsr.setNlp(m_nlp);

                // Set speech volume percentage
                m_nlp.setSpeechVolume(25);

                // Listen to intent, search and clear callbacks
                m_nlp.addListener(m_intentListener);
                m_nlp.addListener(m_searchListener);
                m_nlp.addListener(m_clearListener);
            } else {
                Toast.makeText(m_activity,
                        "ERROR: Cannot initialize Nlp: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    // Public helpers

    public void pauseNlp() {
        if (nlpIsInitialized()) {
            m_nlp.pause();
        }
    }

    public void removeNlpListeners() {
        if (nlpIsInitialized()) {
            m_nlp.removeListener(m_intentListener);
            m_nlp.removeListener(m_searchListener);
            m_nlp.removeListener(m_clearListener);
        }
    }

    public void resumeNlp() {
        if (nlpIsInitialized()) {
            m_nlp.resume(m_activity);
        }
    }

    public boolean startNlpListening() {
        boolean nlpIsInitialized = nlpIsInitialized();
        if (nlpIsInitialized) {
            m_nlp.startListening();
        }
        return nlpIsInitialized;
    }

    private boolean nlpIsInitialized() {
        return (m_nlp != null) && m_nlpInitialized;
    }


    // Place markers handling helpers

    private void addMarkerAtPlace(PlaceLink placeLink) {
        Image img = new Image();
        try {
            img.setImageResource(R.drawable.marker);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MapMarker mapMarker = new MapMarker();
        mapMarker.setIcon(img);
        mapMarker.setCoordinate(new GeoCoordinate(placeLink.getPosition()));

        Map map = m_mapFragment.getMap();
        map.addMapObject(mapMarker);
        m_mapObjectList.add(mapMarker);
    }

    private void cleanMap() {
        Map map = m_mapFragment.getMap();
        if (!m_mapObjectList.isEmpty()) {
            map.removeMapObjects(m_mapObjectList);
            m_mapObjectList.clear();
        }
    }

    // NLP Listeners

    /**
     * On Clear callbacks.
     */
    private Nlp.OnClearListener m_clearListener = new Nlp.OnClearListener() {
        /**
         * A callback for clearing the current route.
         * e.g. "Clear current route"
         *
         * @param route
         *            The Route object to clear from the map.
         */
        @Override
        public void onClear(final Route route) {
            cleanMap();
        }

        /**
         * A callback for clearing the current search results.
         * e.g. "Clear search"
         *
         * @param places
         *            The search result PlaceLink objects to clear from the map.
         */
        @Override
        public void onClear(final List<PlaceLink> places) {
            cleanMap();
        }

        /**
         * A callback for a generic clear request without specifying specific
         * target. e.g. "Clear everything"
         */
        @Override
        public void onClear() {
            cleanMap();
        }
    };

    /**
     * On Intention ready callback.
     */
    private Nlp.OnIntentListener m_intentListener = new Nlp.OnIntentListener() {
        @Override
        public Nlp.Reply onIntent(Intention intention) {
            Toast.makeText(m_activity,
                    "onIntent:" + intention.getOriginalText(),
                    Toast.LENGTH_SHORT).show();

            // Example of handling intention. See {@link Intention.Function}
            if (intention.has(Intention.Function.WEB_SEARCH)) {
                final List<String> targets = intention.getFieldValues(Intention.Field.TARGET);
                if (targets != null && !targets.isEmpty()) {
                    String target = targets.toString();
                    // remove brackets around the String
                    target = target.substring(1, target.length() - 1);

                    // start Google Search with "target" for example

                    // Indicate that the intention was consumed and no further
                    // processing is required by Nlp
                    return Nlp.Reply.CONSUMED;
                }
            }
            return Nlp.Reply.PROCEED;
        }
    };

    /**
     * On Search callbacks.
     */
    private Nlp.OnSearchListener m_searchListener = new Nlp.OnSearchListener() {
        /**
         * A callback with the requested search string for the specified bounding
         * box area. e.g. "Find a coffee shop"
         */
        @Override
        public void onStart(final String subject, final GeoBoundingBox box) {
        }

        /**
         * A callback with the requested category filter for the specified
         * bounding box area
         */
        @Override
        public void onStart(final CategoryFilter filter, final GeoBoundingBox box) {
        }

        /**
         * A callback with requested reverse geocoding search location
         * {@link GeoCoordinate}. e.g. "Where am I"
         */
        @Override
        public void onStart(final GeoCoordinate center) {
        }

        /**
         * A callback with the resulting list of {@link PlaceLink} when a particular
         * search request is complete. e.g. "Find a coffee shop"
         * When the callback is received, the application is responsible for displaying
         * the search results, if desired.
         */
        @Override
        public void onComplete(final Error error,
                               final String searchString,
                               final String whereString,
                               final String nearString,
                               List<PlaceLink> placeLinks) {
            if (error == Error.NONE && placeLinks != null) {
                // Show all place results on the map.
                for (PlaceLink place : placeLinks) {
                    addMarkerAtPlace(place);
                }
            }
        }
    };

    /**
     * Route callbacks
     */
    private Nlp.OnRouteListener m_routeListener = new Nlp.OnRouteListener() {
        @Override
        public void onStart() {
            cleanMap();
        }

        @Override
        public void onComplete(Error error, Route route, List<Place> list, String s, List<Route> list1) {
            if (error == Error.NONE) {
                MapRoute mapRoute = new MapRoute(route);
                m_mapObjectList.add(mapRoute);
                m_map.addMapObject(mapRoute);
            }
        }
    };
}
