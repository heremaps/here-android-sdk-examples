package com.here.android.example.geocoding;

import java.util.List;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.ReverseGeocodeRequest2;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class demonstrates the usage of Geocoding and Reverse Geocoding request APIs
 */
public class MainView {
    private Activity m_activity;
    private TextView m_resultTextView;

    public MainView(Activity activity) {
        m_activity = activity;
        initMapEngine();
        initUIElements();
    }

    private void initMapEngine() {
        /*
         * Even though we don't display a map view in this application, in order to access any
         * services that HERE Android SDK provides, the MapEngine must be initialized as the
         * prerequisite.
         */
        MapEngine.getInstance().init(m_activity, new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                Toast.makeText(m_activity, "Map Engine initialized with error code:" + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUIElements() {
        m_resultTextView = (TextView) m_activity.findViewById(R.id.resultTextView);
        Button geocodeButton = (Button) m_activity.findViewById(R.id.geocodeRequestBtn);
        Button revGeocodeButton = (Button) m_activity.findViewById(R.id.revgeocodeRequestBtn);

        geocodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                triggerGeocodeRequest();
            }
        });

        revGeocodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                triggerRevGeocodeRequest();
            }
        });

    }

    private void triggerGeocodeRequest() {
        m_resultTextView.setText("");
        /*
         * Create a GeocodeRequest object with the desired query string, then set the search area by
         * providing a GeoCoordinate and radius before executing the request.
         */
        String query = "4350 Still Creek Dr,Burnaby";
        GeocodeRequest geocodeRequest = new GeocodeRequest(query);
        GeoCoordinate coordinate = new GeoCoordinate(49.266787, -123.056640);
        geocodeRequest.setSearchArea(coordinate, 5000);
        geocodeRequest.execute(new ResultListener<List<Location>>() {
            @Override
            public void onCompleted(List<Location> locations, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    /*
                     * From the location object, we retrieve the coordinate and display to the
                     * screen. Please refer to HERE Android SDK doc for other supported APIs.
                     */
                    StringBuilder sb = new StringBuilder();
                    for (Location loc : locations) {
                        sb.append(loc.getCoordinate().toString());
                        sb.append("\n");
                    }
                    updateTextView(sb.toString());
                } else {
                    updateTextView("ERROR:Geocode Request returned error code:" + errorCode);
                }
            }
        });
    }

    private void triggerRevGeocodeRequest() {
        m_resultTextView.setText("");
        /* Create a ReverseGeocodeRequest object with a GeoCoordinate. */
        GeoCoordinate coordinate = new GeoCoordinate(49.25914, -123.00777);
        ReverseGeocodeRequest2 revGecodeRequest = new ReverseGeocodeRequest2(coordinate);
        revGecodeRequest.execute(new ResultListener<Location>() {
            @Override
            public void onCompleted(Location location, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    /*
                     * From the location object, we retrieve the address and display to the screen.
                     * Please refer to HERE Android SDK doc for other supported APIs.
                     */
                    updateTextView(location.getAddress().toString());
                } else {
                    updateTextView("ERROR:RevGeocode Request returned error code:" + errorCode);
                }
            }
        });
    }

    private void updateTextView(final String txt) {
        m_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_resultTextView.setText(txt);
            }
        });
    }
}
