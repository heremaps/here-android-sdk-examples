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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.odml.MapLoader;
import com.here.android.mpa.odml.MapPackage;

import java.util.ArrayList;
import java.util.List;

class MapListView {
    private static final String TAG = MapListView.class.getSimpleName();

    private ListActivity m_activity;
    private TextView m_progressTextView;
    private MapLoader m_mapLoader;
    private MapListAdapter m_listAdapter;
    private List<MapPackage> m_currentMapPackageList;// Global variable to keep track of the map
                                                     // package list currently being displayed on
                                                     // screen

    MapListView(ListActivity activity) {
        m_activity = activity;
        initMapEngine();
    }

    private void initMapEngine() {
        MapEngine.getInstance().init(m_activity, new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {
                    /*
                     * Similar to other HERE Android SDK objects, the MapLoader can only be
                     * instantiated after the MapEngine has been initialized successfully.
                     */
                    getMapPackages();
                } else {
                    Log.e(TAG, "Failed to initialize MapEngine: " + error);
                }
            }
        });
    }

    private void initUIElements() {
        Button cancelButton = (Button) m_activity.findViewById(R.id.cancelBtn);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_mapLoader.cancelCurrentOperation();
            }
        });
        m_progressTextView = (TextView) m_activity.findViewById(R.id.progressTextView);
        Button mapUpdateButton = (Button) m_activity.findViewById(R.id.mapUpdateBtn);
        mapUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Because all operations of MapLoader are mutually exclusive, if there is any other
                 * operation which has been triggered previously but yet to receive its call
                 * back,the current operation cannot be triggered at the same time.
                 */
                Boolean success = m_mapLoader.checkForMapDataUpdate();
                if (!success) {
                    Toast.makeText(m_activity, "MapLoader is being busy with other operations",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getMapPackages() {
        Log.d(TAG, "getMapPackages()");
        m_mapLoader = MapLoader.getInstance();
        // Add a MapLoader listener to monitor its status
        m_mapLoader.addListener(m_listener);
        m_mapLoader.getMapPackages();
        initUIElements();
    }

    // Handles the click action on map list item.
    void onListItemClicked(ListView l, View v, int position, long id) {
        MapPackage clickedMapPackage = m_currentMapPackageList.get(position);
        List<MapPackage> children = clickedMapPackage.getChildren();
        if (children.size() > 0) {
            // Children map packages exist.Show them on the screen.
            refreshListView(new ArrayList<>(children));
        } else {
            /*
             * No children map packages are available, we should perform downloading or
             * un-installation action.
             */
            List<Integer> idList = new ArrayList<>();
            idList.add(clickedMapPackage.getId());
            if (clickedMapPackage
                    .getInstallationState() == MapPackage.InstallationState.INSTALLED) {
                Boolean success = m_mapLoader.uninstallMapPackages(idList);
                if (!success) {
                    Toast.makeText(m_activity, "MapLoader is being busy with other operations",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(m_activity, "Uninstalling...", Toast.LENGTH_SHORT).show();
                }
            } else {
                Boolean success = m_mapLoader.installMapPackages(idList);
                if (!success) {
                    Toast.makeText(m_activity, "MapLoader is being busy with other operations",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(m_activity, "Downloading " + clickedMapPackage.getTitle(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Listener to monitor all activities of MapLoader.
    private MapLoader.Listener m_listener = new MapLoader.Listener() {
        @Override
        public void onProgress(int i) {
            if (i < 100) {
                m_progressTextView.setText("Progress: " + i);
            } else {
                m_progressTextView.setText("Installing...");
            }
            Log.d(TAG, "onProgress()");
        }

        @Override
        public void onInstallationSize(long l, long l1) {
        }

        @Override
        public void onGetMapPackagesComplete(MapPackage rootMapPackage,
                MapLoader.ResultCode resultCode) {
            Log.d(TAG, "onGetMapPackagesComplete()");
            /*
             * Please note that to get the latest MapPackage status, the application should always
             * use the rootMapPackage that being returned here. The same applies to other listener
             * call backs.
             */
            if (resultCode == MapLoader.ResultCode.OPERATION_SUCCESSFUL) {
                List<MapPackage> children = rootMapPackage.getChildren();
                refreshListView(new ArrayList<>(children));
            } else if (resultCode == MapLoader.ResultCode.OPERATION_BUSY) {
                // The map loader is still busy, just try again.
                m_mapLoader.getMapPackages();
            }
        }

        @Override
        public void onCheckForUpdateComplete(boolean updateAvailable, String current, String update,
                MapLoader.ResultCode resultCode) {
            Log.d(TAG, "onCheckForUpdateComplete()");
            if (resultCode == MapLoader.ResultCode.OPERATION_SUCCESSFUL) {
                if (updateAvailable) {
                    // Update the map if there is a new version available
                    Boolean success = m_mapLoader.performMapDataUpdate();
                    if (!success) {
                        Toast.makeText(m_activity, "MapLoader is being busy with other operations",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(m_activity, "Starting map update from current version:"
                                + current + " to " + update, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(m_activity, "Current map version: " + current + " is the latest",
                            Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == MapLoader.ResultCode.OPERATION_BUSY) {
                // The map loader is still busy, just try again.
                m_mapLoader.checkForMapDataUpdate();
            }
        }

        @Override
        public void onPerformMapDataUpdateComplete(MapPackage rootMapPackage,
                MapLoader.ResultCode resultCode) {
            Log.d(TAG, "onPerformMapDataUpdateComplete()");
            if (resultCode == MapLoader.ResultCode.OPERATION_SUCCESSFUL) {
                Toast.makeText(m_activity, "Map update is completed", Toast.LENGTH_SHORT).show();
                refreshListView(new ArrayList<>(rootMapPackage.getChildren()));
            }
        }

        @Override
        public void onInstallMapPackagesComplete(MapPackage rootMapPackage,
                MapLoader.ResultCode resultCode) {
            m_progressTextView.setText("");
            if (resultCode == MapLoader.ResultCode.OPERATION_SUCCESSFUL) {
                Toast.makeText(m_activity, "Installation is completed", Toast.LENGTH_SHORT).show();
                List<MapPackage> children = rootMapPackage.getChildren();
                refreshListView((new ArrayList<>(children)));
            } else if (resultCode == MapLoader.ResultCode.OPERATION_CANCELLED) {
                Toast.makeText(m_activity, "Installation is cancelled...", Toast.LENGTH_SHORT)
                        .show();
            }
        }

        @Override
        public void onUninstallMapPackagesComplete(MapPackage rootMapPackage,
                MapLoader.ResultCode resultCode) {
            if (resultCode == MapLoader.ResultCode.OPERATION_SUCCESSFUL) {
                Toast.makeText(m_activity, "Uninstallation is completed", Toast.LENGTH_SHORT)
                        .show();
                List<MapPackage> children = rootMapPackage.getChildren();
                refreshListView((new ArrayList<>(children)));
            } else if (resultCode == MapLoader.ResultCode.OPERATION_CANCELLED) {
                Toast.makeText(m_activity, "Uninstallation is cancelled...", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    };

    /*
     * Helper function to refresh the map list upon the completion of any MapLoader
     * operations.Please note that for the code simplicity, this app refreshes the list to display
     * the highest level of the map hierarchies i.e continent map whenever a map
     * installation/un-installation has been completed.Application developers can implement their
     * own logic in this case to handle how they want to present to end users
     */
    private void refreshListView(ArrayList<MapPackage> list) {
        if (m_listAdapter != null) {
            m_listAdapter.clear();
            m_listAdapter.addAll(list);
            m_listAdapter.notifyDataSetChanged();
        } else {
            m_listAdapter = new MapListAdapter(m_activity, android.R.layout.simple_list_item_1,
                    list);
            m_activity.setListAdapter(m_listAdapter);
        }
        m_currentMapPackageList = list;
    }
}
