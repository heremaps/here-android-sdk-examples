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

import android.content.Context;
import android.content.SharedPreferences;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.fce.FleetConnectivityError;
import com.here.android.mpa.fce.FleetConnectivityEvent;
import com.here.android.mpa.fce.FleetConnectivityJobCancelledEvent;
import com.here.android.mpa.fce.FleetConnectivityJobFinishedEvent;
import com.here.android.mpa.fce.FleetConnectivityJobMessage;
import com.here.android.mpa.fce.FleetConnectivityJobRejectedEvent;
import com.here.android.mpa.fce.FleetConnectivityJobStartedEvent;
import com.here.android.mpa.fce.FleetConnectivityMessage;
import com.here.android.mpa.fce.FleetConnectivityService;
import com.nokia.maps.MapsEngine;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Example of class that aggregates and manages the jobs provided by FleetConnectivityService.
 */
public class JobsManager {
    private static final String TAG = JobsManager.class.getSimpleName();
    private static final String ASSET_ID_KEY = "asset_id";
    private static final String DISPATCHER_ID_KEY = "dispatcher_id";
    // The following part is used for creating sample jobs that can be used for presentation purpose.
    private final String[] SAMPLE_MESSAGES = new String[]{
            "This delivery is very urgent!",
            "Not so urgent, no need to rush.",
            "Please hurry! The customer is waiting! You need to get there as soon as possible!",
            "Please drive there ASAP."
    };
    private final Random mRandom;
    // Available jobs.
    private List<Job> mJobs = new ArrayList<>();
    // Map of jobs to jobs IDs.
    private Map<String, Job> mJobsMap = new HashMap<>();
    // Set of JobsManager.Listener instances.
    private Set<Listener> mListeners = new HashSet<>();
    /**
     * FleetConnectivityService.Listener instance. Awaits incoming messages and reports the result of events dispatch.
     */
    private final FleetConnectivityService.Listener mFleetConnectivityListener = new FleetConnectivityService.Listener() {
        /**
         * Helper method that creates {@link Job} instance from FleetConnectivityJobMessage.
         * @param jobMessage Job message from which the data should be taken.
         * @return Job instance.
         */
        private Job createJob(FleetConnectivityJobMessage jobMessage) {
            final Job job = new Job(jobMessage.getJobId());
            job.setEtaThreshold(jobMessage.getEtaThreshold());
            job.setJobTime(new DateTime(jobMessage.getCreationTimeMilliseconds()));
            job.setMessage(jobMessage.getMessage());
            String[] elements = jobMessage.getContent().get("destination").split(",");
            GeoCoordinate coordinate = new GeoCoordinate(Double.valueOf(elements[0]),
                    Double.valueOf(elements[1]));
            job.setGeoCoordinate(coordinate);
            return job;
        }

        @Override
        public void onMessageReceived(FleetConnectivityMessage message) {
            if (message instanceof FleetConnectivityJobMessage) {
                // New job received!
                final Job job = createJob((FleetConnectivityJobMessage) message);
                addJob(job);
            }
        }

        @Override
        public void onEventAcknowledged(FleetConnectivityEvent fleetConnectivityEvent, FleetConnectivityError fleetConnectivityError) {
            if (fleetConnectivityError != null) {
                // Error occurred!
                dispatchError(fleetConnectivityError);
            }
        }
    };
    // Generated asset ID.
    private String mAssetId;
    // Generated dispatcher ID.
    private String mDispatcherId;

    {
        mRandom = new Random(System.currentTimeMillis());
    }

    /**
     * Starts the FleetConnectivityService.
     *
     * @param ctx Current context.
     * @return True, if the start was successful.
     */
    public synchronized boolean start(Context ctx) {
        final FleetConnectivityService service = FleetConnectivityService.getInstance();
        generateIds(ctx);
        service.setAssetId(mAssetId);
        service.setDispatcherId(mDispatcherId);
        service.setListener(mFleetConnectivityListener);
        return service.start();
    }

    /**
     * Stops the FleetConnectivityService.
     *
     * @return true, if stopping the service was successful.
     */
    public synchronized boolean stop() {
        if (MapEngine.isInitialized()) {
            final FleetConnectivityService service = FleetConnectivityService.getInstance();
            return service.stop();
        } else {
            return false;
        }
    }

    /**
     * Informs the FleetConnectivityService that the job should be started.
     *
     * @param job Job for which start should be executed.
     */
    public synchronized void startJob(Job job) {
        final FleetConnectivityService service = FleetConnectivityService.getInstance();
        final FleetConnectivityJobStartedEvent event = new FleetConnectivityJobStartedEvent();
        event.setJobId(job.getJobId());
        event.setEtaThreshold(job.getEtaThreshold());
        if (service.sendEvent(event)) {
            // Invoking listener callback.
            dispatchJobStarted(job);
        }
    }

    /**
     * Informs the FleetConnectivityService that the job should be rejected.
     *
     * @param job Job which should be rejected.
     */
    public synchronized void rejectJob(Job job) {
        final FleetConnectivityService service = FleetConnectivityService.getInstance();
        final FleetConnectivityJobRejectedEvent event = new FleetConnectivityJobRejectedEvent();
        event.setJobId(job.getJobId());
        if (service.sendEvent(event)) {
            // Removing the job from the collections.
            removeJob(job);
            // Invoking listener callback.
            dispatchJobRejected(job);
        }
    }

    /**
     * Informs the FleetConnectivityService that the currently running job should be cancelled.
     */
    public synchronized void cancelJob() {
        final FleetConnectivityService service = FleetConnectivityService.getInstance();
        final String runningJobId = service.getRunningJobId();

        if (runningJobId != null) {
            Job job = getJob(runningJobId);
            final FleetConnectivityJobCancelledEvent event = new FleetConnectivityJobCancelledEvent();
            if (service.sendEvent(event)) {
                // Removing the job from the collections.
                removeJob(job);
                // Invoking listener callback.
                dispatchJobCancelled(job);
            }
        }
    }

    /**
     * Informs the FleetConnectivityService that the currently running job should be finished.
     */
    public synchronized void finishJob() {
        final FleetConnectivityService service = FleetConnectivityService.getInstance();
        final String runningJobId = service.getRunningJobId();

        if (runningJobId != null) {
            Job job = getJob(runningJobId);
            final FleetConnectivityJobFinishedEvent event = new FleetConnectivityJobFinishedEvent();
            if (service.sendEvent(event)) {
                // Removing the job from the collections.
                removeJob(job);
                // Invoking listener callback.
                dispatchJobFinished(job);
            }
        }
    }

    /**
     * @return Generated asset ID.
     */
    public String getAssetId() {
        return mAssetId;
    }

    /**
     * @return Generated dispatcher ID.
     */
    public String getDispatcherId() {
        return mDispatcherId;
    }

    /**
     * Adds the listener to the listeners set.
     *
     * @param listener Listener to be registered.
     * @return False, if the listener was already added.
     */
    public synchronized boolean addListener(Listener listener) {
        return mListeners.add(listener);
    }

    /**
     * Removes the listener from the listeners set.
     *
     * @param listener Listener to be unregistered.
     * @return False, if the listener was already removed.
     */
    public synchronized boolean removeListener(Listener listener) {
        return mListeners.remove(listener);
    }

    /**
     * @return Collection of currently available jobs.
     */
    public synchronized List<Job> getJobs() {
        return mJobs;
    }

    /**
     * @param jobId ID of the requested job.
     * @return Job with the given jobId.
     */
    public synchronized Job getJob(String jobId) {
        return mJobsMap.get(jobId);
    }

    /**
     * @return Number of available jobs.
     */
    public synchronized int getJobsCount() {
        return mJobs.size();
    }

    /**
     * @return Instance of currently running job, if there is any. Null if the service is idle.
     */
    public synchronized Job getRunningJob() {
        final FleetConnectivityService service = FleetConnectivityService.getInstance();
        final String runningJobId = service.getRunningJobId();
        if (runningJobId != null) {
            return mJobsMap.get(runningJobId);
        }
        return null;
    }

    /**
     * Adds the job to the collections.
     *
     * @param job Job instance to be added.
     */
    private synchronized void addJob(Job job) {
        if (job != null) {
            mJobsMap.put(job.getJobId(), job);
            mJobs.add(job);
            dispatchJobAdded(job);
        }
    }

    /**
     * Removes the job from the collections.
     *
     * @param job Job instance to be removed.
     */
    private synchronized void removeJob(Job job) {
        if (job != null) {
            mJobs.remove(job);
            mJobsMap.remove(job.getJobId());
            dispatchJobRemoved(job);
        }
    }

    /**
     * Executes the onJobAdded callback.
     *
     * @param job Job for which callback should be invoked.
     */
    private synchronized void dispatchJobAdded(Job job) {
        for (Listener listener : mListeners) {
            listener.onJobAdded(job);
        }
    }

    /**
     * Executes the onJobRemoved callback.
     *
     * @param job Job for which callback should be invoked.
     */
    private synchronized void dispatchJobRemoved(Job job) {
        for (Listener listener : mListeners) {
            listener.onJobRemoved(job);
        }
    }

    /**
     * Executes the onJobStarted callback.
     *
     * @param job Job for which callback should be invoked.
     */
    private synchronized void dispatchJobStarted(Job job) {
        for (Listener listener : mListeners) {
            listener.onJobStarted(job);
        }
    }

    /**
     * Executes the onJobRejected callback.
     *
     * @param job Job for which callback should be invoked.
     */
    private synchronized void dispatchJobRejected(Job job) {
        for (Listener listener : mListeners) {
            listener.onJobRejected(job);
        }
    }

    /**
     * Executes the onJobCancelled callback.
     *
     * @param job Job for which callback should be invoked.
     */
    private synchronized void dispatchJobCancelled(Job job) {
        for (Listener listener : mListeners) {
            listener.onJobCancelled(job);
        }
    }

    /**
     * Executes the onJobFinished callback.
     *
     * @param job Job for which callback should be invoked.
     */
    private synchronized void dispatchJobFinished(Job job) {
        for (Listener listener : mListeners) {
            listener.onJobFinished(job);
        }
    }

    /**
     * Executes the onError callback.
     *
     * @param error Object containing error details.
     */
    private void dispatchError(FleetConnectivityError error) {
        for (Listener listener : mListeners) {
            listener.onError(error);
        }
    }

    /**
     * Generates asset ID and dispatcher ID upon first start.
     *
     * @param ctx Current context.
     */
    private void generateIds(Context ctx) {
        if (mAssetId == null || mDispatcherId == null) {
            SharedPreferences pref = ctx.getSharedPreferences(TAG, Context.MODE_PRIVATE);
            if (pref.contains(ASSET_ID_KEY) && pref.contains(DISPATCHER_ID_KEY)) {
                mAssetId = pref.getString(ASSET_ID_KEY, null);
                mDispatcherId = pref.getString(DISPATCHER_ID_KEY, null);
            } else {
                mAssetId = "DemoTruck-" + ((int) (Math.random() * 10000));
                mDispatcherId = "DemoDispatcher-" + ((int) (Math.random() * 10000));
                pref.edit()
                        .putString(ASSET_ID_KEY, mAssetId)
                        .putString(DISPATCHER_ID_KEY, mDispatcherId)
                        .commit();
            }
        }
    }

    private int nextRandom(int max) {
        return mRandom.nextInt(max);
    }

    /**
     * Creates a sample job at given position.
     *
     * @param coordinate Job destination.
     */
    public void createSampleJob(GeoCoordinate coordinate) {
        final Job job = new Job("Job-" + nextRandom(10000));
        job.setEtaThreshold(5);
        job.setMessage(SAMPLE_MESSAGES[nextRandom(SAMPLE_MESSAGES.length)]);
        job.setGeoCoordinate(coordinate);
        addJob(job);
    }

    public interface Listener {
        void onJobAdded(Job job);

        void onJobRemoved(Job job);

        void onJobStarted(Job job);

        void onJobRejected(Job job);

        void onJobCancelled(Job job);

        void onJobFinished(Job job);

        void onError(FleetConnectivityError error);
    }
}
