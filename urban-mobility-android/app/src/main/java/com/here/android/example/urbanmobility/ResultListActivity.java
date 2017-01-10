/*
 * Copyright © 2011-2016 HERE Europe B.V.
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

package com.here.android.example.urbanmobility;

import android.app.ListActivity;
import android.os.Bundle;

/* Use a listView to present DepartureResult */
public class ResultListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_list);
        ResultListAdapter listAdapter = new ResultListAdapter(this,
                android.R.layout.simple_list_item_1,
                com.here.android.example.urbanmobility.MapFragmentView.s_ResultList);
        setListAdapter(listAdapter);
    }

}
