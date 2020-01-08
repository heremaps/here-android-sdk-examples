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

package com.here.android.example.autosuggest;

import java.util.List;

import com.here.android.mpa.search.DiscoveryResult;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/*List Adapter class to be used by ResultListActivity*/
public class ResultListAdapter extends ArrayAdapter<DiscoveryResult> {

    private List<DiscoveryResult> m_discoveryResultList;

    public ResultListAdapter(Context context, int resource, List<DiscoveryResult> results) {
        super(context, resource, results);
        m_discoveryResultList = results;
    }

    @Override
    public int getCount() {
        return m_discoveryResultList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DiscoveryResult discoveryResult = m_discoveryResultList.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.result_list_item,
                    parent, false);
        }

        /*
         * Display title and vicinity information of each result. Please refer to HERE Android SDK
         * API doc for all supported APIs.
         */
        TextView tv = (TextView) convertView.findViewById(R.id.name);
        tv.setText(discoveryResult.getTitle());

        tv = (TextView) convertView.findViewById(R.id.vicinity);
        tv.setText(String.format("Vicinity: %s", discoveryResult.getVicinity()));
        return convertView;
    }
}
