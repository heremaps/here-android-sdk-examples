/*
 * Copyright (c) 2011-2020 HERE Europe B.V.
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

package com.here.tcsdemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapView;
import com.here.android.mpa.routing.Route;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

/**
 * Root Fragment for this application.
 * Contains the HERE MapView.
 */
public class MapFragment extends FleetConnectivityFragment {
    public static final String TAG = MapFragment.class.getSimpleName();
    // Shared Map instance.
    private Map mMap;
    /**
     * GestureListener used to trigger creation of mock job on long press action.
     */
    private final MapGesture.OnGestureListener mOnGestureListener = new MapGesture.OnGestureListener() {
        @Override
        public void onPanStart() {
        }

        @Override
        public void onPanEnd() {
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
            return false;
        }

        @Override
        public boolean onTiltEvent(float v) {
            return false;
        }

        @Override
        public boolean onLongPressEvent(PointF pointF) {
            if (mMap != null) {
                // We create a mock job on long press.
                final GeoCoordinate coordinate = mMap.pixelToGeo(pointF);
                getBridge().getJobsManager().createSampleJob(coordinate);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onLongPressRelease() {
        }

        @Override
        public boolean onTwoFingerTapEvent(PointF pointF) {
            return false;
        }
    };
    // MapRoute for which the guidance is running.
    private MapRoute mNavigatedRoute;
    /**
     * NavigationManager state listener.
     */
    private final NavigationManager.NavigationManagerEventListener mNavigationListener = new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onEnded(NavigationManager.NavigationMode navigationMode) {
            // The user reached the job destination, informing JobsManager.
            getBridge().getJobsManager().finishJob();
        }

        @Override
        public void onRunningStateChanged() {

        }

        @Override
        public void onRouteUpdated(Route newRoute) {
            // Route has been recalculated (for example due to traffic), updating it...
            Toast.makeText(getActivity(), R.string.route_recalculated, Toast.LENGTH_SHORT).show();
            setActiveRoute(new MapRoute(newRoute));
        }
    };
    // MapView instance.
    private MapView mMapView;
    // Contains generic information about the application status.
    private TextView mJobStatus;
    // Opens the JobsFragment.
    private Button mShowJobs;
    // Cached normal marker icon.
    private Image mNormalMarkerIcon;
    // Cached active marker icon.
    private Image mActiveMarkerIcon;
    // Indicates if the user position should be followed.
    private boolean mFollowPosition;
    /**
     * PositionListener used for following the current position.
     */
    private final NavigationManager.PositionListener mPositionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {
            if (mFollowPosition) {
                GeoBoundingBox box = new GeoBoundingBox(geoPosition.getCoordinate(), 1000, 1000);
                Utils.extendBoundingBox(box);
                mMap.zoomTo(box, Map.Animation.NONE, 0);
            }
        }
    };
    // Map of MapMarkers for given job IDs.
    private HashMap<String, MapMarker> mJobMarkers = new HashMap<>();

    /**
     * Default fragment constructor.
     */
    public MapFragment() {
        super(true);
    }

    /**
     * MapFragment factory method.
     *
     * @return New MapFragment instance.
     */
    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.map_overview_fragment, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mJobStatus = view.findViewById(R.id.job_status);
        mShowJobs = view.findViewById(R.id.show_jobs);
        mMapView = view.findViewById(R.id.map_view);

        initMapEngine();

        mShowJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBridge().showJobs();
            }
        });

        updateShowJobsButton();
    }

    private void initMapEngine() {
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getActivity().getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps");

        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
        } else {
            // Initializing the MapEngine.
            MapEngine.getInstance().init(new ApplicationContext(getActivity()), new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(
                        Error error) {
                    if (error == Error.NONE) {
                        mMap = new Map();
                        mMapView.setMap(mMap);
                        mMapView.getMapGesture().addOnGestureListener(mOnGestureListener, 0, false);
                        mMap.setCenter(new GeoCoordinate(52.531027, 13.3827493, 0.0), Map.Animation.NONE);
                        mMap.setZoomLevel((mMap.getMaxZoomLevel() + mMap.getMinZoomLevel()) / 2);
                        // Switching to Truck Day map scheme.
                        mMap.setMapScheme(Map.Scheme.TRUCK_DAY);
                        // Enabling truck restrictions.
                        mMap.setFleetFeaturesVisible(EnumSet.allOf(Map.FleetFeature.class));
                        // Enabling traffic info.
                        mMap.setTrafficInfoVisible(true);
                        onEngineInitialized();
                        // Starting JobsManager.
                        if (!getBridge().getJobsManager().start(getActivity())) {
                            Log.e(TAG, "Could not start the service!");
                            Toast.makeText(getActivity(), R.string.service_start_failure, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "ERROR: Cannot initialize Map Fragment: " + error);
                        Log.e(TAG, error.getDetails());
                        Log.e(TAG, error.getStackTrace());

                        new AlertDialog.Builder(getActivity()).setMessage(
                                "Error : " + error.name() + "\n\n" + error.getDetails())
                                .setTitle(R.string.engine_init_error)
                                .setNegativeButton(android.R.string.cancel,
                                                   new DialogInterface.OnClickListener() {
                                                       @Override
                                                       public void onClick(
                                                               DialogInterface dialog,
                                                               int which) {
                                                           getActivity().finish();
                                                       }
                                                   }).create().show();
                    }
                }
            });
        }
    }

    /**
     * Callback invoked when the MapEngine gets initialized.
     */
    private void onEngineInitialized() {
        getBridge().onEngineInitialized(mMap);
        NavigationManager manager = NavigationManager.getInstance();
        manager.addPositionListener(new WeakReference<>(mPositionListener));
        manager.addNavigationManagerEventListener(new WeakReference<>(mNavigationListener));
        // Caching marker icons.
        mNormalMarkerIcon = new Image();
        mActiveMarkerIcon = new Image();
        try {
            mNormalMarkerIcon.setImageResource(R.drawable.icon_marker);
            mActiveMarkerIcon.setImageResource(R.drawable.icon_finish);
        } catch (IOException e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mMapView != null) {
            MapEngine.getInstance().onResume();
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mMapView != null) {
            mMapView.onPause();
            MapEngine.getInstance().onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mMap != null) {
            NavigationManager manager = NavigationManager.getInstance();
            manager.removeNavigationManagerEventListener(mNavigationListener);
            manager.removePositionListener(mPositionListener);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivated() {
        // Following the user position when this Fragment is on top of the stack and a job is running.
        if (getBridge().getJobsManager().getRunningJob() != null) {
            setFollowPosition(true);
        } else {
            setFollowPosition(false);
        }
    }

    @Override
    public void onJobAdded(Job job) {
        updateShowJobsButton();
        // Adding MapMarker for the new Job.
        MapMarker jobMarker = new MapMarker(job.getGeoCoordinate(), mNormalMarkerIcon);
        mMap.addMapObject(jobMarker);
        mJobMarkers.put(job.getJobId(), jobMarker);
        updateStatusLabel();
    }

    @Override
    public void onJobRemoved(Job job) {
        // Removes the Job marker.
        MapMarker jobMarker = mJobMarkers.remove(job.getJobId());
        mMap.removeMapObject(jobMarker);
        updateShowJobsButton();
        updateStatusLabel();
    }

    @Override
    public void onJobStarted(Job job) {
        updateStatusLabel();
        if (mNavigatedRoute != null) {
            // If the route object was passed to this fragment, we start the guidance.
            NavigationManager manager = NavigationManager.getInstance();
            if (getBridge().isTrafficEnabled()) {
                manager.setTrafficAvoidanceMode(NavigationManager.TrafficAvoidanceMode.DYNAMIC);
            } else {
                manager.setTrafficAvoidanceMode(NavigationManager.TrafficAvoidanceMode.DISABLE);
            }
            if (getBridge().isSimulationEnabled()) {
                manager.simulate(mNavigatedRoute.getRoute(), 22);
            } else {
                manager.startNavigation(mNavigatedRoute.getRoute());
            }
        }
    }

    @Override
    public void onJobFinished(Job job) {
        updateStatusLabel();
        // Removing the route for which the navigation has just been finished.
        setActiveRoute(null);
    }

    @Override
    public void onJobCancelled(Job job) {
        updateStatusLabel();
        // Removing the route
        setActiveRoute(null);
        // Stopping the navigation.
        NavigationManager.getInstance().stop();
    }

    /**
     * Enables/disables "Show Jobs" button.
     */
    private void updateShowJobsButton() {
        mShowJobs.setEnabled(getBridge().getJobsManager().getJobsCount() > 0);
    }

    /**
     * Updates the status label based on the number of available jobs
     * and the state of FleetConnectivityService.
     */
    private void updateStatusLabel() {
        JobsManager manager = getBridge().getJobsManager();
        Job runningJob = manager.getRunningJob();
        int jobCount = manager.getJobsCount();
        if (runningJob != null) {
            mJobStatus.setText(getString(R.string.job_running, runningJob.getJobId()));
        } else if (jobCount > 0) {
            mJobStatus.setText(getResources().getQuantityString(R.plurals.jobs_pending, jobCount, jobCount));
        } else {
            mJobStatus.setText(R.string.no_jobs_available);
        }
    }

    /**
     * Updates the icon of the MapMarker for the given job ID.
     *
     * @param jobId Job ID for which the marker should be updated.
     * @param type  Icon is selected based on this type.
     */
    public void updateJobMarker(String jobId, JobMarkerType type) {
        Job job = getBridge().getJobsManager().getJob(jobId);
        if (job != null) {
            MapMarker jobMarker = mJobMarkers.get(job.getJobId());
            if (type == JobMarkerType.ACTIVE) {
                jobMarker.setIcon(mActiveMarkerIcon);
            } else {
                // Preventing switch for running job
                if (job != getBridge().getJobsManager().getRunningJob()) {
                    jobMarker.setIcon(mNormalMarkerIcon);
                }
            }
        }
    }

    /**
     * Adds or removes the MapRoute object from the Map.
     * This MapRoute instance is used by guidance.
     *
     * @param route MapRoute instance that is handed over to this MapFragment.
     */
    public void setActiveRoute(MapRoute route) {
        if (mNavigatedRoute != null) {
            mMap.removeMapObject(mNavigatedRoute);
        }
        mNavigatedRoute = route;
        if (mNavigatedRoute != null) {
            mMap.removeMapObject(mNavigatedRoute);
            // Active (navigated) route should have different color.
            mNavigatedRoute.setColor(0xFF51FF51);
            mMap.addMapObject(mNavigatedRoute);
        }
    }

    /**
     * Enables/disables user position tracking.
     *
     * @param followPosition True, if the map viewport should be automatically updated to show the user position in the center.
     */
    public void setFollowPosition(boolean followPosition) {
        mFollowPosition = followPosition;
    }
}
