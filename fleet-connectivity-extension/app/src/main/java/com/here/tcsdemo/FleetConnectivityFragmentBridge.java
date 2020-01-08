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

import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapRoute;

public interface FleetConnectivityFragmentBridge {
    void onEngineInitialized(Map map);

    Map getMap();

    JobsManager getJobsManager();

    void showJobs();

    void showJobDetails(String jobId);

    void updateJobMarker(String jobId, JobMarkerType type);

    void setActiveRoute(MapRoute route);

    void setFollowPosition(boolean follow);

    boolean isSimulationEnabled();

    void setSimulationEnabled(boolean enabled);

    boolean isTrafficEnabled();

    void setTrafficEnabled(boolean enabled);

    void setTruckRestrictionsEnabled(boolean enabled);

    boolean areTruckRestrictionsEnabled();
}
