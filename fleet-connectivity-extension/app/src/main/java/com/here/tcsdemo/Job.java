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

import com.here.android.mpa.common.GeoCoordinate;

import org.joda.time.DateTime;

/**
 * Job model class used by {@link JobsManager}.
 */
public class Job {
    private final String mJobId;
    private long mEtaThreshold;
    private GeoCoordinate mGeoCoordinate;
    private String mMessage;
    private DateTime mJobTime;

    public Job(String jobId) {
        mJobId = jobId;
    }

    public String getJobId() {
        return mJobId;
    }

    public long getEtaThreshold() {
        return mEtaThreshold;
    }

    public void setEtaThreshold(long etaThreshold) {
        mEtaThreshold = etaThreshold;
    }

    public GeoCoordinate getGeoCoordinate() {
        return mGeoCoordinate;
    }

    public void setGeoCoordinate(GeoCoordinate geoCoordinate) {
        mGeoCoordinate = geoCoordinate;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public DateTime getJobTime() {
        return mJobTime;
    }

    public void setJobTime(DateTime jobTime) {
        mJobTime = jobTime;
    }
}
