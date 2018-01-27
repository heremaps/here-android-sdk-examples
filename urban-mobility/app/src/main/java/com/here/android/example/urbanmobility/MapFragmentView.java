/*
 * Copyright (c) 2011-2017 HERE Europe B.V.
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

package com.here.android.example.urbanmobility;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.urbanmobility.City;
import com.here.android.mpa.urbanmobility.CitySearchRequest;
import com.here.android.mpa.urbanmobility.CitySearchResult;
import com.here.android.mpa.urbanmobility.Departure;
import com.here.android.mpa.urbanmobility.DepartureBoard;
import com.here.android.mpa.urbanmobility.DepartureBoardRequest;
import com.here.android.mpa.urbanmobility.ErrorCode;
import com.here.android.mpa.urbanmobility.ExploredCoverage;
import com.here.android.mpa.urbanmobility.NearbyCoverageRequest;
import com.here.android.mpa.urbanmobility.NearbyCoverageResult;
import com.here.android.mpa.urbanmobility.RequestManager;
import com.here.android.mpa.urbanmobility.RequestManager.ResponseListener;
import com.here.android.mpa.urbanmobility.Station;
import com.here.android.mpa.urbanmobility.StationSearchRequest;
import com.here.android.mpa.urbanmobility.StationSearchResult;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class encapsulates the properties and functionality of the Map view.It also implements 4
 * types of Urban Mobility requests that HERE Android SDK provides as example.
 */
public class MapFragmentView {

    public static List<Departure> s_ResultList;

    private MapFragment m_mapFragment;
    private Activity m_activity;
    private Map m_map;

    private LinearLayout m_cityDetailLayout;
    private TextView m_cityName;
    private TextView m_cityLinesCount;
    private TextView m_cityStopsCount;
    private TextView m_cityCoverageQuality;
    private Button m_detailButton;

    private List<MapObject> m_mapObjectList = new ArrayList<>();
    private List<Station> stationList;
    private List<City> cityList;

    private DecimalFormat qualityFormatter = new DecimalFormat("0.00");

    public MapFragmentView(Activity activity) {
        m_activity = activity;
        /*
         * The map fragment is not required for Urban Mobility requests. However in this example,
         * adding some map markers on the map to visualize the location of the results.
         */
        initMapFragment();
        /* Use buttons to trigger the Urban Mobility requests */
        initUMRequestButtons();
        /* Use a listView to present the departure results in text format */
        initResultListButton();
        /* Use an overlay layout to present the city results in text format */
        initCityDetailLayout();
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = (MapFragment) m_activity.getFragmentManager()
                .findFragmentById(R.id.mapfragment);

        // Set path of isolated disk cache
        String diskCacheRoot = Environment.getExternalStorageDirectory().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = m_activity.getPackageManager().getApplicationInfo(m_activity.getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(), "Failed to find intent name, NameNotFound: " + e.getMessage());
        }

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot,
                intentName);
        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            if (m_mapFragment != null) {
            /* Initialize the MapFragment, results will be given via the called back. */
                m_mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                        if (error == Error.NONE) {
                            m_map = m_mapFragment.getMap();
                        /* Set the map center to HERE Burnaby office, no animation */
                            m_map.setCenter(new GeoCoordinate(49.2591502, -123.0091942),
                                    Map.Animation.NONE);
                            m_map.setZoomLevel(15);
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

    private void initResultListButton() {
        m_detailButton = (Button) m_activity.findViewById(R.id.resultListBtn);
        m_detailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Open the ResultListActivity */
                Intent intent = new Intent(m_activity, ResultListActivity.class);
                m_activity.startActivity(intent);
            }
        });
    }

    private void initCityDetailLayout() {
        m_cityDetailLayout = (LinearLayout) m_activity.findViewById(R.id.cityDetailLayout);
        m_cityDetailLayout.setVisibility(View.GONE);

        m_cityName = (TextView) m_activity.findViewById(R.id.cityName);
        m_cityLinesCount = (TextView) m_activity.findViewById(R.id.cityLinesCount);
        m_cityStopsCount = (TextView) m_activity.findViewById(R.id.cityStopsCount);
        m_cityCoverageQuality = (TextView) m_activity.findViewById(R.id.cityCoverageQuality);

        Button closeCityDetailButton = (Button) m_activity.findViewById(R.id.closeLayoutButton);
        closeCityDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_cityDetailLayout.getVisibility() == View.VISIBLE) {
                    m_cityDetailLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    private void initUMRequestButtons() {

        Button stationRequestButton = (Button) m_activity.findViewById(R.id.stationRequestBtn);
        stationRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanMap();
                /* Setup StationSearchRequest listener */
                ResponseListener<StationSearchResult> stationSearchListener = new ResponseListener<StationSearchResult>() {
                    @Override
                    public void onError(ErrorCode errorCode, String errorMessage) {
                        Toast.makeText(m_activity,
                                "ERROR: stationRequest returned error code: " + errorCode,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(StationSearchResult result) {
                        stationList = result.getStations();
                        if (stationList.size() > 0) {
                            for (int i = 0; i < stationList.size(); i++) {
                                Station stationResult = stationList.get(i);
                                /*
                                 * Present the station results with map markers. A listView can be
                                 * added to present the station results in text format
                                 */
                                addMarkerAtResult(stationResult.getAddress().getCoordinate());
                            }
                        } else {
                            Toast.makeText(m_activity, "No station information yet",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                /*
                 * StationSearchRequest discovers transit stations by searching around a given
                 * location, filtered by station name, maximum results, search radius, and so on
                 */
                GeoCoordinate searchCoordinate = new GeoCoordinate(49.2852595, -123.1132904);
                String stationName = null;
                StationSearchRequest stationRequest = new RequestManager()
                        .createStationSearchRequest(searchCoordinate, stationName,
                                stationSearchListener);
                stationRequest.setMaximumResults(15);
                stationRequest.setRadius(5000);
                stationRequest.setRequestStationDetailsEnabled(true);
                stationRequest.execute();
                /* Set map center to the search location to present the station results nearby */
                m_map.setCenter(searchCoordinate, Map.Animation.NONE);
                m_map.setZoomLevel(16.7);
            }
        });

        Button citySearchRequestButton = (Button) m_activity.findViewById(R.id.cityRequestBtn);
        citySearchRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cleanMap();

                /* Setup CitySearchRequest listener */
                ResponseListener<CitySearchResult> citySearchListener = new ResponseListener<CitySearchResult>() {
                    @Override
                    public void onError(ErrorCode errorCode, String errorMessage) {
                        Toast.makeText(m_activity,
                                "ERROR: citySearchRequest returned error code: " + errorCode,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(CitySearchResult result) {
                        cityList = result.getCities();
                        if (cityList.size() > 0) {
                            GeoCoordinate cityCoordinate = new GeoCoordinate(
                                    cityList.get(0).getLocation());
                            /* Add map marker to the city */
                            addMarkerAtResult(cityCoordinate);
                            /* Set map center to the city */
                            m_map.setCenter(cityCoordinate, Map.Animation.NONE);
                            m_map.setZoomLevel(13);
                            /*
                             * enable the overlay to present the city transit results in text format
                             */
                            m_cityDetailLayout.setVisibility(View.VISIBLE);
                            m_cityName.setText(cityList.get(0).getDisplayName());
                            m_cityLinesCount.setText("City Lines: "
                                    + Integer.toString(cityList.get(0).getTransportsCount()));
                            m_cityStopsCount.setText("City Stops: "
                                    + Integer.toString(cityList.get(0).getStopsCount()));
                            m_cityCoverageQuality.setText("City Coverage Quality: "
                                    + qualityFormatter.format(cityList.get(0).getQuality()));
                        } else {
                            Toast.makeText(m_activity, "No city information yet",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                /*
                 * CitySearchRequest discovers city coverage information by searching cities with
                 * names that begin with the query string filtered by maximum results, and so on
                 */
                String cityName = "Rome";
                CitySearchRequest citySearchRequest = new RequestManager()
                        .createCitySearchRequest(cityName, citySearchListener);
                citySearchRequest.setMaximumResults(3);
                citySearchRequest.setRequestCityDetailsEnabled(true);
                citySearchRequest.execute();
            }
        });

        Button nearbyCoverageRequestButton = (Button) m_activity
                .findViewById(R.id.nearbyCoverageRequestBtn);
        nearbyCoverageRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cleanMap();

                /* Setup NearbyCoverageRequest listener */
                ResponseListener<NearbyCoverageResult> nearbyCoverageListener = new ResponseListener<NearbyCoverageResult>() {
                    @Override
                    public void onError(ErrorCode errorCode, String errorMessage) {
                        Toast.makeText(m_activity,
                                "ERROR: citySearchRequest returned error code: " + errorCode,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(NearbyCoverageResult result) {
                        City cityResult = result.getCity();
                        ExploredCoverage exploreResult = result.getExploredCoverage();
                        /*
                         * The server returns information of the city that contains this location,
                         * e.g. Burnaby Village GeoCoordinate(49.2380074,-122.9736308) to show city
                         * of Burnaby
                         */
                        if (cityResult != null) {
                            GeoCoordinate cityCoordinate = new GeoCoordinate(
                                    cityResult.getLocation());
                            /* Add map marker to the city */
                            addMarkerAtResult(cityCoordinate);
                            /* Set map center to the city */
                            m_map.setCenter(cityCoordinate, Map.Animation.NONE);
                            m_map.setZoomLevel(13);
                            /* Enable the overlay to present the city results in text format */
                            m_cityDetailLayout.setVisibility(View.VISIBLE);
                            m_cityName.setText(cityResult.getDisplayName());
                            m_cityLinesCount.setText("City Lines: "
                                    + Integer.toString(cityResult.getTransportsCount()));
                            m_cityStopsCount.setText(
                                    "City Stops: " + Integer.toString(cityResult.getStopsCount()));
                            m_cityCoverageQuality.setText("City Coverage Quality: "
                                    + qualityFormatter.format(cityResult.getQuality()));
                        }
                        /*
                         * If there is no transit stop within 2 kilometers of the location, the
                         * server returns the first five nearest transit stops outside of the 2
                         * kilometers area; e.g. GeoCoordinate(50.104127, -123.0715547) to show 5
                         * station in Whistler
                         */
                        else if (exploreResult != null) {
                            Collection<Station> stationCollection = exploreResult.getStations();
                            stationList = new ArrayList(stationCollection);

                            if (stationList.size() > 0) {
                                for (int i = 0; i < stationList.size(); i++) {
                                    Station stationResult = stationList.get(i);
                                    /*
                                     * Present the station results with map markers. A listView can
                                     * be added to present the station results in text format
                                     */
                                    addMarkerAtResult(stationResult.getAddress().getCoordinate());
                                    if (i == 1) {
                                        /* Set map center to the first station result */
                                        m_map.setCenter(stationResult.getAddress().getCoordinate(),
                                                Map.Animation.NONE);
                                        m_map.setZoomLevel(14);
                                    }
                                }
                            } else {
                                Toast.makeText(m_activity,
                                        "No station coverage information yet in "
                                                + exploreResult.getRadius() + "m",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        /* No coverage information all */
                        else {
                            Toast.makeText(m_activity,
                                    "No coverage information yet in " + result.getRadius() + "m",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                /*
                 * NearbyCoverageRequest coverage information around a given location, if the city
                 * coverage information contains the given location is available, then this city
                 * coverage information is returned; else if there is no transit stop within 2km of
                 * the given location, then the first five nearest transit stops outside of this
                 * given location are returned.
                 */
                GeoCoordinate exploreCoordinate = new GeoCoordinate(50.104127, -123.0715547);
                NearbyCoverageRequest nearbyCoverageRequest = new RequestManager()
                        .createNearbyCoverageRequest(exploreCoordinate, nearbyCoverageListener);
                nearbyCoverageRequest.setRequestCityDetailsEnabled(true);
                nearbyCoverageRequest.execute();
            }
        });
    }

    private void cleanMap() {
        if (!m_mapObjectList.isEmpty()) {
            m_map.removeMapObjects(m_mapObjectList);
            m_mapObjectList.clear();
        }
        m_detailButton.setVisibility(View.GONE);
        m_cityDetailLayout.setVisibility(View.GONE);
    }

    private void addMarkerAtResult(GeoCoordinate geoLoc) {
        Image img = new Image();
        try {
            img.setImageResource(R.drawable.marker);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MapMarker mapMarker = new MapMarker();
        mapMarker.setIcon(img);
        mapMarker.setCoordinate(geoLoc);
        m_map.addMapObject(mapMarker);
        m_mapObjectList.add(mapMarker);
    }
}
