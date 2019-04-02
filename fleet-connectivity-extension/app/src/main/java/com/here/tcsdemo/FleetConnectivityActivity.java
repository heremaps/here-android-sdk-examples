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
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.fce.FleetConnectivityError;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapRoute;

import java.util.EnumSet;

/**
 * Main activity of our application.
 * Implements @{link FleetConnectivityFragmentBridge}, which is used for communication between fragments.
 * <p/>
 * To create new jobs for demo purposes, please access:
 * https://tcs.ext.here.com/examples/v3/fce_simulating_dispatcher?app_id=<app_id_of_this_app>&app_code=<app_code_of_this_app>&custom_endpoint=https://fce.api.here.com/1
 * and enter proper asset ID and dispatcher ID (can be accessed from the Menu -> Asset Details).
 */
public class FleetConnectivityActivity extends AppCompatActivity implements FleetConnectivityFragmentBridge {
    // Helper class which manages incoming jobs.
    private final JobsManager mJobsManager;
    // Snackbar containing information about new job.
    private View mSnackbarContainer;
    /**
     * @{link JobsManager.Listener} for this Activity.
     */
    private final JobsManager.Listener mJobsManagerListener = new JobsManager.Listener() {
        @Override
        public void onJobRemoved(Job job) {
        }

        @Override
        public void onJobAdded(final Job job) {
            // Let's inform the user that new job is available.
            Snackbar.make(mSnackbarContainer, getString(R.string.job_snackbar, job.getJobId()), Snackbar.LENGTH_LONG)
                    .setAction(R.string.show_action, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showJobDetails(job.getJobId());
                        }
                    })
                    .show();
        }

        @Override
        public void onJobStarted(Job job) {
        }

        @Override
        public void onJobRejected(Job job) {
        }

        @Override
        public void onJobCancelled(Job job) {
        }

        @Override
        public void onJobFinished(Job job) {
        }

        @Override
        public void onError(FleetConnectivityError error) {
            // Let's inform the user about error in communication.
            final String errorMessage;
            if (error.getType() == FleetConnectivityError.Type.CONNECTION_ERROR) {
                // Network is not available.
                errorMessage = getString(R.string.connection_error_message, error.getErrorId());
            } else {
                // Server reported error.
                StringBuilder errorMessageBuilder = new StringBuilder(getString(R.string.server_error_message, error.getErrorId()));
                for (FleetConnectivityError.Issue issue : error.getIssues()) {
                    errorMessageBuilder.append(" ").append(issue.getMessage()).append("(").append(issue.getCode()).append(")");
                }
                errorMessage = errorMessageBuilder.toString();
            }
            Toast.makeText(FleetConnectivityActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
    };
    // HERE Map instance.
    private Map mMap;
    // Initial fragment containing MapView.
    private MapFragment mMapFragment;
    // Indicates if guidance should be simulated.
    private boolean mSimulationEnabled = true;
    // Indicates if traffic info should be displayed and used in routing and guidance.
    private boolean mTrafficEnabled = true;
    // Indicates if truck restrictions should be displayed.
    private boolean mTruckRestrictionsEnabled = true;

    /**
     * Activity constructor. Initializes the {@link JobsManager} and sets the {@link JobsManager.Listener}.
     */
    public FleetConnectivityActivity() {
        mJobsManager = new JobsManager();
        mJobsManager.addListener(mJobsManagerListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Retrieving the SnackBar view.
        mSnackbarContainer = findViewById(R.id.snackbar_container);

        // Initializing the root fragment.
        if (savedInstanceState == null) {
            mMapFragment = MapFragment.newInstance();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, mMapFragment, MapFragment.TAG);
            ft.commit();
        } else {
            mMapFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag(MapFragment.TAG);
        }

        // Setting listener that will hide/show the up button based on the fragments stack size.
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                ActionBar actionBar = getSupportActionBar();
                boolean showUp = getSupportFragmentManager().getBackStackEntryCount() > 0;
                actionBar.setHomeButtonEnabled(showUp);
                actionBar.setDisplayHomeAsUpEnabled(showUp);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        // Stopping the JobsManager, which in turn will stop the FleetConnectivityService.
        mJobsManager.stop();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getSupportFragmentManager().popBackStack();
                return true;
            case R.id.action_details:
                showAssetDetails();
                return true;
            case R.id.action_truck_restrictions:
                item.setChecked(!item.isChecked());
                setTruckRestrictionsEnabled(item.isChecked());
                return true;
            case R.id.action_traffic:
                item.setChecked(!item.isChecked());
                setTrafficEnabled(item.isChecked());
                return true;
            case R.id.action_simulate_guidance:
                item.setChecked(!item.isChecked());
                setSimulationEnabled(item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Callback invoked by @{link MapFragment} upon Map initialization.
     *
     * @param map Map instance provided by the MapEngine.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void onEngineInitialized(Map map) {
        mMap = map;
        // Enabling positioning.
        PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK);
        mMap.getPositionIndicator().setVisible(true);
        mMap.getPositionIndicator().setAccuracyIndicatorVisible(true);
    }

    /**
     * Triggers update of MapMarker for the given jobId.
     *
     * @param jobId Id of the job for which the MapMarker should be updated.
     * @param type  Type of MapMarker to which the job's marker should be updated.
     */
    @Override
    public void updateJobMarker(String jobId, JobMarkerType type) {
        mMapFragment.updateJobMarker(jobId, type);
    }

    /**
     * Updates the route that should be used for guidance.
     *
     * @param route Route for which the guidance should run.
     */
    @Override
    public void setActiveRoute(MapRoute route) {
        mMapFragment.setActiveRoute(route);
    }

    /**
     * Informs the @{link MapFragment} if the current position should be tracked.
     *
     * @param follow True, if the current position should be tracked.
     */
    @Override
    public void setFollowPosition(boolean follow) {
        mMapFragment.setFollowPosition(follow);
    }

    /**
     * Displays a dialog with the information about asset ID and dispatcher ID,
     * which were generated by this device.
     */
    private void showAssetDetails() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.asset_details,
                        mJobsManager.getAssetId(),
                        mJobsManager.getDispatcherId()))
                .create();
        dialog.show();
    }

    /**
     * Pushes the @{link JobsFragment} on the stack.
     */
    @Override
    public void showJobs() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.bottom_enter, R.anim.bottom_exit, R.anim.bottom_enter, R.anim.bottom_exit);
        ft.add(R.id.container, JobsFragment.newInstance(), JobsFragment.TAG);
        ft.addToBackStack(JobsFragment.TAG);
        ft.commit();
    }

    /**
     * Pushes the @{link JobDetailsFragment} on the stack.
     *
     * @param jobId ID of the job of which the details should be displayed.
     */
    @Override
    public void showJobDetails(String jobId) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0 && JobDetailsFragment.TAG.equals(fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName())) {
            fm.popBackStack();
        }
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.anim.right_enter, R.anim.right_exit, R.anim.right_enter, R.anim.right_exit);
        ft.add(R.id.container, JobDetailsFragment.newInstance(jobId), JobDetailsFragment.TAG);
        ft.addToBackStack(JobDetailsFragment.TAG);
        ft.commit();
    }

    /**
     * @return True, if guidance simulation is enabled.
     */
    @Override
    public boolean isSimulationEnabled() {
        return mSimulationEnabled;
    }

    /**
     * Enables/disables guidance simulation.
     *
     * @param simulationEnabled True, if the next guidance start should use position simulation.
     */
    @Override
    public void setSimulationEnabled(boolean simulationEnabled) {
        mSimulationEnabled = simulationEnabled;
    }

    /**
     * @return True, if traffic info usage is enabled.
     */
    @Override
    public boolean isTrafficEnabled() {
        return mTrafficEnabled;
    }

    /**
     * Enables/disables traffic info usage.
     *
     * @param trafficEnabled True, if the traffic info should be visible on map and used by routing and guidance.
     */
    @Override
    public void setTrafficEnabled(boolean trafficEnabled) {
        mTrafficEnabled = trafficEnabled;
        if (mMap != null) {
            mMap.setTrafficInfoVisible(mTrafficEnabled);
        }
    }

    /**
     * Enables/disables truck restrictions and congestion zones visibility.
     *
     * @param truckRestrictionsEnabled True, if the truck restrictions should be visible.
     */
    @Override
    public void setTruckRestrictionsEnabled(boolean truckRestrictionsEnabled) {
        mTruckRestrictionsEnabled = truckRestrictionsEnabled;
        if (mMap != null) {
            if (mTruckRestrictionsEnabled) {
                mMap.setFleetFeaturesVisible(EnumSet.allOf(Map.FleetFeature.class));
            } else {
                mMap.setFleetFeaturesVisible(EnumSet.noneOf(Map.FleetFeature.class));
            }
        }
    }

    /**
     * @return True, if truck restrictions visibility is enabled.
     */
    @Override
    public boolean areTruckRestrictionsEnabled() {
        return mTruckRestrictionsEnabled;
    }

    /**
     * @return Shared @{link JobsManager} instance.
     */
    @Override
    public JobsManager getJobsManager() {
        return mJobsManager;
    }

    /**
     * @return Shared Map instance.
     */
    @Override
    public Map getMap() {
        return mMap;
    }
}
