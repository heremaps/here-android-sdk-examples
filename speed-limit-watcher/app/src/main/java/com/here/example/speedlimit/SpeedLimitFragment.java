package com.here.example.speedlimit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.MatchedGeoPosition;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.RoadElement;
import com.here.android.mpa.prefetcher.MapDataPrefetcher;

import java.lang.ref.WeakReference;


public class SpeedLimitFragment extends Fragment {

    private LinearLayout currentSpeedContainerView;
    private TextView currentSpeedView;
    private TextView currentSpeedLimitView;

    private boolean fetchingDataInProgress = false;

    public SpeedLimitFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroy() {
        if (MapEngine.isInitialized()) {
            PositioningManager.getInstance().removeListener(positionLister);
        }
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_speed_limit, container, false);

        setElements(fragmentView);
        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private int meterPerSecToKmPerHour (double speed) {
        return (int) (speed * 3.6);
    }

    private int meterPerSecToMilesPerHour (double speed) {
        return (int) (speed * 2.23694);
    }

    public void startListeners() {
        PositioningManager.getInstance().addListener(new WeakReference<>(positionLister));
        MapDataPrefetcher.getInstance().addListener(prefetcherListener);
    }

    public void stopWatching() {
        PositioningManager.getInstance().removeListener(positionLister);
        MapDataPrefetcher.getInstance().removeListener(prefetcherListener);
    }

    private void setElements(View fragmentView) {
        currentSpeedView = (TextView) fragmentView.findViewById(R.id.currentSpeed);
        currentSpeedLimitView = (TextView) fragmentView.findViewById(R.id.currentSpeedLimit);
        currentSpeedContainerView = (LinearLayout) fragmentView.findViewById(R.id.currentSpeedContainer);
    }

    private void updateCurrentSpeedView(int currentSpeed, int currentSpeedLimit) {

        int color;
        if (currentSpeed > currentSpeedLimit && currentSpeedLimit > 0) {
            color = getResources().getColor(R.color.notAllowedSpeedBackground);
        } else {
            color = getResources().getColor(R.color.allowedSpeedBackground);
        }
        currentSpeedContainerView.setBackgroundColor(color);
        currentSpeedView.setText(String.valueOf(currentSpeed));
    }

    private void updateCurrentSpeedLimitView(int currentSpeedLimit) {

        String currentSpeedLimitText;
        int textColorId;
        int backgroundImageId;

        if (currentSpeedLimit > 0) {
            currentSpeedLimitText = String.valueOf(currentSpeedLimit);
            textColorId = R.color.limitText;
            backgroundImageId = R.drawable.limit_circle_background;
        } else {
            currentSpeedLimitText = getResources().getString(R.string.navigation_speed_limit_default);
            textColorId = R.color.noLimitText;
            backgroundImageId = R.drawable.no_limit_circle_background;
        }
        currentSpeedLimitView.setText(currentSpeedLimitText);
        currentSpeedLimitView.setTextColor(getResources().getColor(textColorId));
        currentSpeedLimitView.setBackgroundResource(backgroundImageId);
    }


    MapDataPrefetcher.Adapter prefetcherListener = new MapDataPrefetcher.Adapter() {
        @Override
        public void onStatus(int requestId, PrefetchStatus status) {
            if(status != PrefetchStatus.PREFETCH_IN_PROGRESS) {
                fetchingDataInProgress = false;
            }
        }
    };

    PositioningManager.OnPositionChangedListener positionLister = new PositioningManager.OnPositionChangedListener() {
        @Override
        public void onPositionUpdated(PositioningManager.LocationMethod locationMethod,
                                      GeoPosition geoPosition, boolean b) {

            if (PositioningManager.getInstance().getRoadElement() == null && !fetchingDataInProgress) {
                GeoBoundingBox areaAround = new GeoBoundingBox(geoPosition.getCoordinate(), 500, 500);
                MapDataPrefetcher.getInstance().fetchMapData(areaAround);
                fetchingDataInProgress = true;
            }

            if (geoPosition.isValid() && geoPosition instanceof MatchedGeoPosition) {

                MatchedGeoPosition mgp = (MatchedGeoPosition) geoPosition;

                int currentSpeedLimitTransformed = 0;
                int currentSpeed = meterPerSecToKmPerHour(mgp.getSpeed());

                if (mgp.getRoadElement() != null) {
                    double currentSpeedLimit = mgp.getRoadElement().getSpeedLimit();
                    currentSpeedLimitTransformed  = meterPerSecToKmPerHour(currentSpeedLimit);
                }

                updateCurrentSpeedView(currentSpeed, currentSpeedLimitTransformed);
                updateCurrentSpeedLimitView(currentSpeedLimitTransformed);

            } else {
                //handle error
            }
        }

        @Override
        public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod,
                                         PositioningManager.LocationStatus locationStatus) {

        }
    };

}
