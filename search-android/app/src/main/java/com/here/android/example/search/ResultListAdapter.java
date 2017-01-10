/*
 * Copyright © 2011-2017 HERE Europe B.V.
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

package com.here.android.example.search;

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
         * Display title and vicinity information of each result.Please refer to HERE Android SDK
         * API doc for all supported APIs.
         */
        TextView tv = (TextView) convertView.findViewById(R.id.name);
        tv.setText(discoveryResult.getTitle());

        tv = (TextView) convertView.findViewById(R.id.vicinity);
        tv.setText(String.format("Vicinity: %s", discoveryResult.getVicinity()));
        return convertView;
    }
}
