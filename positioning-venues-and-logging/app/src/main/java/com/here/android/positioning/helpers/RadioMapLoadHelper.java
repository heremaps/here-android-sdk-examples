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

package com.here.android.positioning.helpers;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.here.android.mpa.venues3d.Venue;
import com.here.android.positioning.radiomap.RadioMapLoader;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Helper class for handling radio map downloading in sequence.
 */
public class RadioMapLoadHelper {

    private static final String TAG = "RadioMapLoadHelper";

    // Radio map loader
    private final RadioMapLoader mApi;

    // Helper listener
    private final Listener mListener;

    // Android handler
    private final Handler mHandler;

    // Loader implementation
    private class Loader implements RadioMapLoader.Listener, Runnable {

        // Current venue queue
        private final Set<Venue> mLoadQue = new LinkedHashSet<>();

        // Active download job
        private Pair<Venue, RadioMapLoader.Job> mCurrentJob;

        @Override
        public void onProgressUpdated(RadioMapLoader.Job job) {
            Log.i(TAG, "onProgressUpdated: " + mCurrentJob.first.getId() + ", progress: " + job.getProgress());
            mListener.onProgress(mCurrentJob.first, job.getProgress());
        }

        @Override
        public void onCompleted(RadioMapLoader.Job job) {
            if (mCurrentJob == null) {
                Log.i(TAG, "onCompleted: status: " + job.getStatus());
            } else {
                Log.i(TAG, "onCompleted: " + mCurrentJob.first.getId() + ", status: " + job.getStatus());
                mListener.onCompleted(mCurrentJob.first, job.getStatus());
                mCurrentJob = null;
            }
            schedule();
        }

        @Override
        public void run() {
            if (mCurrentJob != null) {
                Log.w(TAG, "Loader.run: downloading already in process");
                return;
            }
            final Iterator<Venue> it = mLoadQue.iterator();
            if (!it.hasNext()) {
                Log.v(TAG, "Loader.run: everything downloaded");
                return;
            }
            try {
                final Venue venue = it.next();
                final RadioMapLoader.Job job = mApi.load(this, venue);
                switch (job.getStatus()) {
                    case OK:
                    case PENDING:
                        mCurrentJob = new Pair<>(venue, job);
                        break;
                    default:
                        mListener.onError(venue, job.getStatus());
                        schedule();
                        break;
                }
            } finally {
                it.remove();
            }
        }

        // Load venue
        boolean load(@NonNull Venue venue) {
            if (mLoadQue.add(venue)) {
                return schedule();
            }
            return false;
        }

        // Cancels current download and clears download que.
        void cancel() {
            mLoadQue.clear();
            mHandler.removeCallbacks(this);
            if (mCurrentJob != null) {
                mCurrentJob.second.cancel();
                mCurrentJob = null;
            }
        }

        // Schedule downloading
        private boolean schedule() {
            if (mCurrentJob != null) {
                return true;
            }
            mHandler.removeCallbacks(this);
            return mHandler.post(this);
        }
    }

    // Loader
    private final Loader mLoader = new Loader();

    /**
     * Helper listener interface definition.
     */
    public interface Listener {
        void onError(@NonNull  Venue venue, RadioMapLoader.Status status);
        void onProgress(@NonNull Venue venue, int progress);
        void onCompleted(@NonNull Venue venue, RadioMapLoader.Status status);
    }

    /**
     * Constructor
     * @param api Radio map loader
     * @param listener Helper listener
     */
    public RadioMapLoadHelper(@NonNull RadioMapLoader api, @NonNull Listener listener) {
        mApi = api;
        mListener = listener;
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Que venue for loading
     * @param venue Venue instance
     * @return True if venue queued successfully
     */
    public boolean load(@NonNull Venue venue) {
        Log.v(TAG, "load: " + venue.getId());
        return mLoader.load(venue);
    }

    /**
     * Cancel downloads and clear download que
     */
    public void cancel() {
        Log.v(TAG, "cancel");
        mLoader.cancel();
    }

}
