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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.util.List;

/**
 * Fragment containing list of currently available jobs.
 */
public class JobsFragment extends FleetConnectivityFragment implements AdapterView.OnItemClickListener {
    public static final String TAG = JobsFragment.class.getSimpleName();

    private ListView mJobsListView;
    private JobsListAdapter mJobsListAdapter;

    /**
     * JobsFragment factory method.
     *
     * @return New JobsFragment instance.
     */
    public static JobsFragment newInstance() {
        JobsFragment fragment = new JobsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.jobs_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mJobsListView = (ListView) view.findViewById(R.id.jobs_list);
        mJobsListAdapter = new JobsListAdapter(getActivity(), getBridge().getJobsManager());
        mJobsListView.setEmptyView(view.findViewById(R.id.jobs_list_empty));
        mJobsListView.setOnItemClickListener(this);
        mJobsListView.setAdapter(mJobsListAdapter);
    }

    @Override
    protected void onActivated() {
        zoomToJobs();
        mJobsListAdapter.notifyDataSetChanged();
        getBridge().setFollowPosition(false);
    }

    /**
     * Zooms half of the viewport on the available jobs destinations.
     */
    private void zoomToJobs() {
        List<Job> jobs = getBridge().getJobsManager().getJobs();
        if (jobs.size() > 0) {
            double minLat = Double.MAX_VALUE;
            double maxLat = Double.MIN_VALUE;
            double minLon = Double.MAX_VALUE;
            double maxLon = Double.MIN_VALUE;
            for (Job job : jobs) {
                GeoCoordinate coor = job.getGeoCoordinate();
                minLat = Math.min(coor.getLatitude(), minLat);
                maxLat = Math.max(coor.getLatitude(), maxLat);
                minLon = Math.min(coor.getLongitude(), minLon);
                maxLon = Math.max(coor.getLongitude(), maxLon);
            }
            GeoCoordinate topLeft = new GeoCoordinate(maxLat, minLon);
            GeoCoordinate bottomRight = new GeoCoordinate(minLat, maxLon);
            GeoBoundingBox box = new GeoBoundingBox(topLeft, bottomRight);
            if (jobs.size() == 1) {
                box.expand(1000, 1000);
            }
            Utils.extendBoundingBox(box);
            final Map map = getBridge().getMap();
            map.zoomTo(box, Map.Animation.LINEAR, 0);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Displaying job details.
        getBridge().showJobDetails(getBridge().getJobsManager().getJobs().get(position).getJobId());
    }

    @Override
    public void onJobAdded(Job job) {
        // Job added, refreshing jobs list and updating viewport.
        mJobsListAdapter.notifyDataSetChanged();
        if (isActive()) {
            zoomToJobs();
        }
    }

    @Override
    public void onJobRemoved(Job job) {
        // Job added, refreshing jobs list and updating viewport.
        mJobsListAdapter.notifyDataSetChanged();
        if (isActive()) {
            zoomToJobs();
        }
    }

    @Override
    public void onJobStarted(Job job) {
        // Job started, we need to refresh the list.
        mJobsListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onJobFinished(Job job) {
        // Job finished, we need to refresh the list.
        mJobsListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onJobCancelled(Job job) {
        // Job cancelled, we need to refresh the list.
        mJobsListAdapter.notifyDataSetChanged();
    }

    private static class JobsListAdapter extends BaseAdapter {
        private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
                .appendHourOfDay(2)
                .appendLiteral(':')
                .appendMinuteOfHour(2)
                .toFormatter();
        private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
                .appendDayOfMonth(2)
                .appendLiteral('/')
                .appendMonthOfYear(2)
                .appendLiteral('/')
                .appendYearOfCentury(2, 2)
                .toFormatter();
        private final LayoutInflater mInflater;
        private final JobsManager mJobsManager;

        public JobsListAdapter(Context context, JobsManager jobsManager) {
            mInflater = LayoutInflater.from(context);
            mJobsManager = jobsManager;
        }

        @Override
        public int getCount() {
            return mJobsManager.getJobs().size();
        }

        @Override
        public Object getItem(int position) {
            return mJobsManager.getJobs().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                convertView = mInflater.inflate(R.layout.job_item, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            }
            Job job = mJobsManager.getJobs().get(position);
            holder.jobId.setText(job.getJobId());
            holder.jobMessage.setText(job.getMessage());
            // Showing ProgressBar when the job is running, date & time info otherwise.
            holder.jobStateSwitcher.setDisplayedChild(job != mJobsManager.getRunningJob() ? 0 : 1);
            holder.jobTime.setText(TIME_FORMATTER.print(job.getJobTime()));
            holder.jobDate.setText(DATE_FORMATTER.print(job.getJobTime()));
            return convertView;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        static class ViewHolder {
            TextView jobId;
            TextView jobMessage;
            TextView jobTime;
            TextView jobDate;
            ViewSwitcher jobStateSwitcher;

            ViewHolder(View item) {
                jobId = (TextView) item.findViewById(R.id.job_id);
                jobMessage = (TextView) item.findViewById(R.id.job_message);
                jobTime = (TextView) item.findViewById(R.id.job_time);
                jobDate = (TextView) item.findViewById(R.id.job_date);
                jobStateSwitcher = (ViewSwitcher) item.findViewById(R.id.job_state_switcher);
            }
        }
    }
}
