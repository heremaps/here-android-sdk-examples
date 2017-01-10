/*
 * Copyright © 2011-2016 HERE Europe B.V.
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

package com.here.android.example.search;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.Place;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.PlaceRequest;
import com.here.android.mpa.search.ResultListener;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*A list view to present DiscoveryResult */
public class ResultListActivity extends ListActivity {
    private LinearLayout m_placeDetailLayout;
    private TextView m_placeName;
    private TextView m_placeLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.result_list);
        initUIElements();
        ResultListAdapter listAdapter = new ResultListAdapter(this,
                android.R.layout.simple_list_item_1, MapFragmentView.s_ResultList);
        setListAdapter(listAdapter);
    }

    private void initUIElements() {
        /*
         * An overlay layout will pop up to display some place details.To simplify the logic, this
         * layout is currently not being handled for screen rotation event.It disappears if the
         * screen is being rotated.
         */
        m_placeDetailLayout = (LinearLayout) findViewById(R.id.placeDetailLayout);
        m_placeDetailLayout.setVisibility(View.GONE);

        m_placeName = (TextView) findViewById(R.id.placeName);
        m_placeLocation = (TextView) findViewById(R.id.placeLocation);

        Button closePlaceDetailButton = (Button) findViewById(R.id.closeLayoutButton);
        closePlaceDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_placeDetailLayout.getVisibility() == View.VISIBLE) {
                    m_placeDetailLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    /* Retrieve details of the place selected */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        DiscoveryResult result = MapFragmentView.s_ResultList.get(position);
        if (result.getResultType() == DiscoveryResult.ResultType.PLACE) {
            /* Fire the PlaceRequest */
            PlaceLink placeLink = (PlaceLink) result;
            PlaceRequest placeRequest = placeLink.getDetailsRequest();
            placeRequest.execute(m_placeResultListener);
        } else if (result.getResultType() == DiscoveryResult.ResultType.DISCOVERY) {
            /*
             * Another DiscoveryRequest object can be obtained by calling DiscoveryLink.getRequest()
             */
            Toast.makeText(this, "This is a DiscoveryLink result", Toast.LENGTH_SHORT).show();
        }
    }

    private ResultListener<Place> m_placeResultListener = new ResultListener<Place>() {
        @Override
        public void onCompleted(Place place, ErrorCode errorCode) {
            if (errorCode == ErrorCode.NONE) {
                /*
                 * No error returned,let's show the name and location of the place that just being
                 * selected.Additional place details info can be retrieved at this moment as well,
                 * please refer to the HERE Android SDK API doc for details.
                 */
                m_placeDetailLayout.setVisibility(View.VISIBLE);
                m_placeName.setText(place.getName());
                GeoCoordinate geoCoordinate = place.getLocation().getCoordinate();
                m_placeLocation.setText(geoCoordinate.toString());
            } else {
                Toast.makeText(getApplicationContext(),
                        "ERROR:Place request returns error: " + errorCode, Toast.LENGTH_SHORT)
                        .show();
            }

        }
    };
}
