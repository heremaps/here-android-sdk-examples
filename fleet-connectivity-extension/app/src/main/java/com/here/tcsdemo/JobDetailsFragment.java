/*
 * Copyright (c) 2011-2019 HERE Europe B.V.
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

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.DynamicPenalty;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteTta;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.util.List;

/**
 * Fragment showing information about specific job.
 * Calculates the route to the job destination and displays the distance and the estimated time to arrival.
 */
public class JobDetailsFragment extends FleetConnectivityFragment implements View.OnClickListener {
    public static final String TAG = JobDetailsFragment.class.getSimpleName();
    private static final String JOB_ID_EXTRA = "job_id";
    // Instance of the Job for which info will be shown.
    private Job mJob;
    // Shared Map instance.
    private Map mMap;
    // Route to the job destination.
    private Route mRoute;
    // Visual representation of the route.
    private MapRoute mMapRoute;
    // Shared CoreRouter instance.
    private CoreRouter mCoreRouter;

    private TextView mJobIdLabel;
    private TextView mJobMessageLabel;
    private ViewGroup mJobDetails;
    private ProgressBar mProgressBar;
    private TextView mJobTtaLabel;
    private TextView mJobDistanceLabel;
    private Button mStartJobButton;
    /**
     * Route calculation listener.
     */
    private final CoreRouter.Listener mRouteListener = new CoreRouter.Listener() {
        @Override
        public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
            // We only handle the result if Fragment is attached.
            if (isAdded()) {
                if (routingError != RoutingError.NONE) {
                    // Calculation error occurred!
                    Toast.makeText(getActivity(), getString(R.string.route_calculation_error, routingError), Toast.LENGTH_LONG).show();
                } else {
                    RouteResult result = list.get(0);
                    mRoute = result.getRoute();
                    RouteTta tta = getBridge().isTrafficEnabled()
                            ? mRoute.getTtaIncludingTraffic(Route.WHOLE_ROUTE)
                            : mRoute.getTtaExcludingTraffic(Route.WHOLE_ROUTE);

                    int distanceInMeters = mRoute.getLength();
                    // Updating route info.
                    updateTtaLabel(tta.getDuration());
                    updateDistanceLabel(distanceInMeters);

                    // Adding MapRoute object to the Map.
                    mMapRoute = new MapRoute(mRoute);
                    mMapRoute.setColor(0xFF5E66FF);
                    mMap.addMapObject(mMapRoute);
                    // Zooming to the route.
                    GeoBoundingBox routeBox = mRoute.getBoundingBox();
                    Utils.extendBoundingBox(routeBox);
                    mMap.zoomTo(routeBox, Map.Animation.LINEAR, 0);
                    // Enabling Start button only if no other job is running.
                    mStartJobButton.setEnabled(getBridge().getJobsManager().getRunningJob() == null);
                    // Displaying Start button.
                    mStartJobButton.setVisibility(View.VISIBLE);
                }
                mProgressBar.setVisibility(View.INVISIBLE);
                mJobDetails.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onProgress(int i) {

        }
    };

    private Button mAbortJobButton;
    private Button mRejectJobButton;
    private Handler mHandler;
    /**
     * Runnable responsible for requesting TTA and remaining distance information from NavigationManager.
     */
    private final Runnable mNavigationProgressRunnable = new Runnable() {
        private final NavigationManager mNavigationManager = NavigationManager.getInstance();

        @Override
        public void run() {
            // Continuing only if the Fragment is attached to the activity and guidance is in progress.
            if (isAdded() && mNavigationManager.getRunningState() != NavigationManager.NavigationState.IDLE) {
                RouteTta tta = mNavigationManager.getTta(getBridge().isTrafficEnabled() ? Route.TrafficPenaltyMode.OPTIMAL : Route.TrafficPenaltyMode.DISABLED, true);
                if (tta != null) {
                    updateTtaLabel(tta.getDuration());
                }

                long distanceInMeters = mNavigationManager.getDestinationDistance();
                updateDistanceLabel(distanceInMeters);

                // Let's schedule the update to run after 1s.
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * JobDetailsFragment factory method.
     *
     * @param jobId Job ID of the job described in this Fragment.
     * @return New JobDetailsFragment instance.
     */
    public static JobDetailsFragment newInstance(String jobId) {
        JobDetailsFragment fragment = new JobDetailsFragment();
        Bundle args = new Bundle();
        args.putString(JOB_ID_EXTRA, jobId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        String jobId = getArguments().getString(JOB_ID_EXTRA);
        mJob = getBridge().getJobsManager().getJob(jobId);
        mMap = getBridge().getMap();
        if (mJob == null) {
            // In case if job was already removed.
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * Changes the MapMarker icon of this job to ACTIVE and centers the viewport on it.
     */
    private void showDestination() {
        GeoBoundingBox box = new GeoBoundingBox(mJob.getGeoCoordinate(), 1000, 1000);
        Utils.extendBoundingBox(box);
        mMap.zoomTo(box, Map.Animation.NONE, 0);
        getBridge().updateJobMarker(mJob.getJobId(), JobMarkerType.ACTIVE);
    }

    /**
     * Changes the MapMarker icon of this job back to NORMAL.
     */
    private void hideDestination() {
        getBridge().updateJobMarker(mJob.getJobId(), JobMarkerType.NORMAL);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.job_details_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mJobDetails = (ViewGroup) view.findViewById(R.id.job_details);
        mProgressBar = (ProgressBar) view.findViewById(R.id.job_details_progress);
        mJobIdLabel = (TextView) view.findViewById(R.id.job_id);
        mJobMessageLabel = (TextView) view.findViewById(R.id.job_message);
        mJobTtaLabel = (TextView) view.findViewById(R.id.job_tta);
        mJobDistanceLabel = (TextView) view.findViewById(R.id.job_distance);
        mStartJobButton = (Button) view.findViewById(R.id.start_job);
        mStartJobButton.setOnClickListener(this);
        mAbortJobButton = (Button) view.findViewById(R.id.abort_job);
        mAbortJobButton.setOnClickListener(this);
        mRejectJobButton = (Button) view.findViewById(R.id.reject_job);
        mRejectJobButton.setOnClickListener(this);

        if (mJob != null) {
            // Let's center the viewport on the destination.
            showDestination();
            mJobIdLabel.setText(mJob.getJobId());
            mJobIdLabel.setSelected(true);
            mJobMessageLabel.setText(mJob.getMessage());
            JobsManager manager = getBridge().getJobsManager();
            if (manager.getRunningJob() == mJob) {
                // Job is already running, let's follow the user position and display TTA and remaining distance.
                trackNavigationProgress();
                // Job can be aborted, so let's display the abort button.
                mAbortJobButton.setVisibility(View.VISIBLE);
            } else {
                // Job is not yet running, so no need to track the user position.
                getBridge().setFollowPosition(false);
                // Job is not running, so it can be rejected.
                mRejectJobButton.setVisibility(View.VISIBLE);
                // Let's calculate the route to our destination.
                calculateRoute();
            }
        }
    }

    /**
     * Starts following the user position.
     * Also triggers periodical updates of TTA and remaining distance.
     */
    private void trackNavigationProgress() {
        getBridge().setFollowPosition(true);
        mProgressBar.setVisibility(View.INVISIBLE);
        mJobDetails.setVisibility(View.VISIBLE);
        mHandler.postDelayed(mNavigationProgressRunnable, 3000);
    }

    /**
     * Triggers route calculation to the job destination.
     */
    private void calculateRoute() {
        boolean error = false;
        // We need a valid position to calculate the route.
        if (PositioningManager.getInstance().hasValidPosition()) {
            GeoPosition position = PositioningManager.getInstance().getPosition();
            GeoCoordinate coordinate = position.getCoordinate();
            if (coordinate.isValid()) {
                mCoreRouter = new CoreRouter();
                DynamicPenalty penalty = new DynamicPenalty();
                // Using traffic if it is enabled.
                if (getBridge().isTrafficEnabled()) {
                    penalty.setTrafficPenaltyMode(Route.TrafficPenaltyMode.OPTIMAL);
                } else {
                    penalty.setTrafficPenaltyMode(Route.TrafficPenaltyMode.DISABLED);
                }
                mCoreRouter.setDynamicPenalty(penalty);
                // Setting route coordinates.
                RoutePlan routePlan = new RoutePlan();
                routePlan.addWaypoint(new RouteWaypoint(coordinate));
                routePlan.addWaypoint(new RouteWaypoint(mJob.getGeoCoordinate()));
                // Specifying route options.
                RouteOptions routeOptions = new RouteOptions();
                routeOptions.setTransportMode(RouteOptions.TransportMode.TRUCK);
                routePlan.setRouteOptions(routeOptions);
                mCoreRouter.calculateRoute(routePlan, mRouteListener);
            } else {
                error = true;
            }
        }
        if (error) {
            // Position not known, reporting this to the user.
            Toast.makeText(getActivity(), getString(R.string.no_position), Toast.LENGTH_LONG).show();
            mProgressBar.setVisibility(View.INVISIBLE);
            mJobDetails.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Removes the calculated route from the map.
     * Stops route calculation if it is in progress.
     */
    private void cleanupRoute() {
        if (mCoreRouter != null && mCoreRouter.isBusy()) {
            mCoreRouter.cancel();
        }
        if (mMapRoute != null) {
            mMap.removeMapObject(mMapRoute);
        }
    }

    @Override
    public void onDestroy() {
        // Performing clean up.
        mHandler.removeCallbacks(mNavigationProgressRunnable);
        hideDestination();
        cleanupRoute();
        super.onDestroy();
    }

    /**
     * Updates the TTA label content.
     *
     * @param seconds TTA to be displayed.
     */
    private void updateTtaLabel(int seconds) {
        int durationInMinutes = seconds / 60;
        int durationInHours = durationInMinutes / 60;
        if (durationInHours > 0) {
            mJobTtaLabel.setText(getString(R.string.tta_h_m, durationInHours, durationInMinutes % 60));
        } else {
            mJobTtaLabel.setText(getString(R.string.tta_m, durationInMinutes));
        }
    }

    /**
     * Updates the distance label content.
     *
     * @param meters Remaining distance to be displayed.
     */
    private void updateDistanceLabel(long meters) {
        float distanceInKilometers = meters / 1000.0f;
        if (distanceInKilometers > 0) {
            mJobDistanceLabel.setText(getString(R.string.distance_km, distanceInKilometers));
        } else {
            mJobDistanceLabel.setText(getString(R.string.distance_m, meters));
        }
    }

    @Override
    public void onJobRemoved(Job job) {
        if (mJob == job) {
            // Job displayed by this Fragment has been removed, closing...
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onJobFinished(Job job) {
        if (mJob != job) {
            // Another job was finished, while we were on details screen of this job,
            // now we can enable the Start button.
            mStartJobButton.setEnabled(true);
        }
    }

    @Override
    public void onJobStarted(Job job) {
        if (mJob == job) {
            // Job displayed by this Fragment has been started.
            // Displaying abort button and hiding all others.
            mAbortJobButton.setVisibility(View.VISIBLE);
            mRejectJobButton.setVisibility(View.INVISIBLE);
            mStartJobButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_job: {
                // Passing route to map fragment, so it can be used by guidance later on.
                getBridge().setActiveRoute(mMapRoute);
                // We pass the ownership of the MapRoute object to the MapFragment.
                mMapRoute = null;
                // We ask the JobManager to start the job.
                getBridge().getJobsManager().startJob(mJob);
                // Now we should follow the user position.
                trackNavigationProgress();
            }
            break;
            case R.id.abort_job: {
                // Cancels currently running job (depicted by this Fragment).
                getBridge().getJobsManager().cancelJob();
            }
            break;
            case R.id.reject_job: {
                // Rejects the job depicted by this Fragment.
                getBridge().getJobsManager().rejectJob(mJob);
            }
            break;
        }
    }
}
