/*
 * Copyright (c) 2011-2017 HERE Europe B.V.
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
