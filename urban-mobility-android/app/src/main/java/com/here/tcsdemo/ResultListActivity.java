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

package com.here.tcsdemo;

import android.app.ListActivity;
import android.os.Bundle;

/* Use a listView to present DepartureResult */
public class ResultListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_list);
        ResultListAdapter listAdapter = new ResultListAdapter(this,
                android.R.layout.simple_list_item_1, MapFragmentView.s_ResultList);
        setListAdapter(listAdapter);
    }

}
