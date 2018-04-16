/*
 * Copyright (c) 2011-2018 HERE Europe B.V.
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

package com.here.android.example.map.downloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.here.android.mpa.odml.MapPackage;

import java.util.List;

class MapListAdapter extends ArrayAdapter<MapPackage> {
    private List<MapPackage> m_list;

    MapListAdapter(Context context, int resource, List<MapPackage> results) {
        super(context, resource, results);
        m_list = results;
    }

    @Override
    public int getCount() {
        return m_list.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MapPackage mapPackage = m_list.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent,
                    false);
        }

        /*
         * Display title and size information of each map package.Please refer to HERE Android SDK
         * API doc for all supported APIs.
         */
        TextView tv = (TextView) convertView.findViewById(R.id.mapPackageName);
        tv.setText(mapPackage.getTitle());
        tv = (TextView) convertView.findViewById(R.id.mapPackageState);
        tv.setText(mapPackage.getInstallationState().toString());
        tv = (TextView) convertView.findViewById(R.id.mapPackageSize);
        /*
         * getSize() returns the maximum install size of a map.If other packages have already been
         * installed, the result returned by this method doesn't reflect the actual memory space
         * required by this map,because common data between map packages doesn't need to be
         * installed again.To get the most accurate information of the disk space that has been used
         * for a particular map package installation, use the onInstallationSize() callback method
         * in the MapLoader.Listener.
         */
        tv.setText(String.valueOf(mapPackage.getSize()) + "KB");
        return convertView;
    }
}
