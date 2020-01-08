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

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.here.android.mpa.fce.FleetConnectivityError;

/**
 * Common super-class of Fragments used in this application.
 * Implements {@link JobsManager.Listener} and registers/unregisters itself in {@link JobsManager} when created/destroyed.
 */
abstract public class FleetConnectivityFragment extends Fragment implements JobsManager.Listener, FragmentManager.OnBackStackChangedListener {
    // Indicates if the fragment is top-level (root) fragment.
    private final boolean mIsTopLevel;
    // Bridge instance used for communication between fragments.
    private FleetConnectivityFragmentBridge mBridge;

    /**
     * Default constructor for non-top-level fragment.
     */
    public FleetConnectivityFragment() {
        mIsTopLevel = false;
    }

    /**
     * Constructor that allows to specify if the fragment will be a top-level fragment.
     *
     * @param topLevel True, if the Fragment is top-level.
     */
    public FleetConnectivityFragment(boolean topLevel) {
        mIsTopLevel = topLevel;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBridge = (FleetConnectivityFragmentBridge) getActivity();
        mBridge.getJobsManager().addListener(this);
        getFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onDestroy() {
        mBridge.getJobsManager().removeListener(this);
        getFragmentManager().removeOnBackStackChangedListener(this);
        super.onDestroy();
    }

    /**
     * @return {@link FleetConnectivityFragmentBridge} instance.
     */
    protected FleetConnectivityFragmentBridge getBridge() {
        return mBridge;
    }

    @Override
    public final void onBackStackChanged() {
        if (isActive()) {
            onActivated();
        } else {
            onDeactivated();
        }
    }

    /**
     * Invoked when the Fragment gets added to the stack.
     */
    protected void onActivated() {
    }

    /**
     * Invoked when the Fragment gets removed from the stack.
     */
    protected void onDeactivated() {
    }

    /**
     * @return True, if the Fragment is currently on top of the stack.
     */
    public final boolean isActive() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() == 0 && mIsTopLevel) {
            return true;
        } else if (fm.getBackStackEntryCount() > 0 && getTag() != null
                && getTag().equals(fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onJobAdded(Job job) {
        // Override in subclass
    }

    @Override
    public void onJobRemoved(Job job) {
        // Override in subclass
    }

    @Override
    public void onJobStarted(Job job) {
        // Override in subclass
    }

    @Override
    public void onJobRejected(Job job) {
        // Override in subclass
    }

    @Override
    public void onJobCancelled(Job job) {
        // Override in subclass
    }

    @Override
    public void onJobFinished(Job job) {
        // Override in subclass
    }

    @Override
    public void onError(FleetConnectivityError error) {
        // Override in subclass
    }
}
