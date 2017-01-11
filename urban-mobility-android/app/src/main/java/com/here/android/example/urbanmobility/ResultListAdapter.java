/*
 * Copyright © 2011-2017 HERE Europe B.V.
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

package com.here.android.example.urbanmobility;

import java.text.SimpleDateFormat;
import java.util.List;

import com.here.android.mpa.urbanmobility.Departure;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/*List Adapter class to be used by ResultListActivity*/
public class ResultListAdapter extends ArrayAdapter<Departure> {

    private List<Departure> m_resultList;

    public ResultListAdapter(Context context, int resource, List<Departure> results) {
        super(context, resource, results);
        m_resultList = results;
    }

    @Override
    public int getCount() {
        return m_resultList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Departure departureResult = m_resultList.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.result_list_item,
                    parent, false);
        }

        /*
         * Present departure information including direction, line name, and departure time
         */
        TextView tv = (TextView) convertView.findViewById(R.id.direction);
        tv.setText(departureResult.getTransport().getDirection());

        tv = (TextView) convertView.findViewById(R.id.lineTime);
        SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        tv.setText("Line " + departureResult.getTransport().getName() + ",  Departure Time: "
                + dayFormatter.format(departureResult.getTime()));

        return convertView;
    }
}
