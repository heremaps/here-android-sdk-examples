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

package com.here.android.example.venuesandlogging;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.LocationDataSourceHERE;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.PositioningManager.OnPositionChangedListener;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.Map.OnTransformListener;
import com.here.android.mpa.mapping.MapGesture.OnGestureListener;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.venues3d.BaseLocation;
import com.here.android.mpa.venues3d.CombinedRoute;
import com.here.android.mpa.venues3d.DeselectionSource;
import com.here.android.mpa.venues3d.Level;
import com.here.android.mpa.venues3d.LevelLocation;
import com.here.android.mpa.venues3d.OutdoorLocation;
import com.here.android.mpa.venues3d.RoutingController;
import com.here.android.mpa.venues3d.Space;
import com.here.android.mpa.venues3d.SpaceLocation;
import com.here.android.mpa.venues3d.Venue;
import com.here.android.mpa.venues3d.VenueController;
import com.here.android.mpa.venues3d.VenueInfo;
import com.here.android.mpa.venues3d.VenueMapFragment;
import com.here.android.mpa.venues3d.VenueMapFragment.VenueListener;
import com.here.android.mpa.venues3d.VenueRouteOptions;
import com.here.android.mpa.venues3d.VenueService;
import com.here.android.mpa.venues3d.VenueService.VenueServiceListener;
import com.here.android.positioning.DiagnosticsListener;
import com.here.android.positioning.StatusListener;
import com.here.android.positioning.helpers.RadioMapLoadHelper;
import com.here.android.positioning.radiomap.RadioMapLoader;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class BasicVenueActivity extends AppCompatActivity
        implements VenueListener,
            OnGestureListener,
            OnPositionChangedListener,
            OnTransformListener,
            RoutingController.RoutingControllerListener {

    // TAG string for logging purposes
    private static final String TAG = "VenuesAndLogging.BasicVenueActivity";

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    // map embedded in the map fragment
    private Map mMap;

    // Venue map fragment embedded in this activity
    private VenueMapFragment mVenueMapFragment;

    // Venue map fragment routing controller
    RoutingController mRoutingController;

    // True if route is shown
    private boolean mRouteShown;

    // Widget for selecting floors of the venue
    private FloorsControllerWidget mFloorsControllerWidget;

    // positioning manager instance
    private PositioningManager mPositioningManager;

    // Flag for using indoor positioning
    private boolean mIndoorPositioning;

    // Flag for using indoor routing
    private boolean mIndoorRouting;

    // HERE location data source instance
    private LocationDataSourceHERE mHereLocation;

    // Instance of Venue Service
    private VenueService mVenueService;

    // Current activity
    private BasicVenueActivity mActivity;

    // Text view for position updates
    private TextView mLocationInfo;

    // Location method currently in use
    private PositioningManager.LocationMethod mLocationMethod;

    // Menu
    private MenuItem add_private_venues;
    private MenuItem remove_private_venues;
    private MenuItem follow_position;
    private MenuItem add_indoor_to_position;
    private MenuItem indoor_routing;

    // Last known map center
    private GeoCoordinate mLastMapCenter;

    // Last received position update
    private GeoPosition mLastReceivedPosition;

    // flag that indicates whether maps is being transformed
    private boolean mTransforming;

    // Flag for usage of Private Venues context
    private boolean mPrivateVenues;

    // Flag for user control over the map
    private boolean mUserControl;

    // callback that is called when transforming ends
    private Runnable mPendingUpdate;

    // Constant for not found
    public static final int NOT_FOUND = -1;

    /** Positioning status listener. */
    private StatusListener mPositioningStatusListener = new StatusListener() {

        @Override
        public void onOfflineModeChanged(boolean offline) {
            Log.v(TAG, "StatusListener.onOfflineModeChanged: %b", offline);
        }

        @Override
        public void onAirplaneModeEnabled() {
            Log.v(TAG, "StatusListener.onAirplaneModeEnabled");
        }

        @Override
        public void onWifiScansDisabled() {
            Log.v(TAG, "StatusListener.onWifiScansDisabled");
        }

        @Override
        public void onBluetoothDisabled() {
            Log.v(TAG, "StatusListener.onBluetoothDisabled");
        }

        @Override
        public void onCellDisabled() {
            Log.v(TAG, "StatusListener.onCellDisabled");
        }

        @Override
        public void onGnssLocationDisabled() {
            Log.v(TAG, "StatusListener.onGnssLocationDisabled");
        }

        @Override
        public void onNetworkLocationDisabled() {
            Log.v(TAG, "StatusListener.onNetworkLocationDisabled");
        }

        @Override
        public void onServiceError(ServiceError error) {
            Log.v(TAG, "StatusListener.onServiceError: %s", error);
        }

        @Override
        public void onPositioningError(PositioningError error) {
            Log.v(TAG, "StatusListener.onPositioningError: %s", error);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onWifiIndoorPositioningNotAvailable() {
            Log.v(TAG, "StatusListener.onWifiIndoorPositioningNotAvailable");
        }

        @Override
        public void onWifiIndoorPositioningDegraded() {
            // called when running on Android 9.0 (Pie) or newer
        }
    };

    // Venue load listener to request radio map loading for the loaded venue.
    private final VenueService.VenueLoadListener mVenueLoadListener = new VenueService.VenueLoadListener() {
        @Override
        public void onVenueLoadCompleted(Venue venue, VenueInfo venueInfo, VenueService.VenueLoadStatus venueLoadStatus) {
            if (venueLoadStatus != VenueService.VenueLoadStatus.FAILED) {
                Log.v(TAG, "onVenueLoadCompleted: loading radio maps for " + venue.getId());
                mRadioMapLoader.load(venue);
            }
        }
    };

    // Radio map loader helper instance.
    private RadioMapLoadHelper mRadioMapLoader;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Latest selected venue
    private Venue mSelectedVenue;

    // Latest selected space
    private Space mSelectedSpace;

    /**
     * An example list view adapter for floor switcher
     */
    public class VenueFloorAdapter extends BaseAdapter {

        final Level[] mLevels;
        private final int mFloorItem;
        private final int mFloorName;
        private final int mFloorGroundSep;
        private LayoutInflater mInflater;

        public VenueFloorAdapter(Context context, List<Level> levels, int floorItemId,
                                 int floorNameId, int floorGroundSepId) {
            mLevels = new Level[levels.size()];
            mFloorItem = floorItemId;
            mFloorName = floorNameId;
            mFloorGroundSep = floorGroundSepId;
            for (int i = 0; i < levels.size(); i++) {
                mLevels[levels.size() - i - 1] = levels.get(i);
            }
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mLevels.length;
        }

        @Override
        public Object getItem(int position) {
            return mLevels[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int getLevelIndex(Level level) {
            for (int i = 0; i < mLevels.length; i++) {
                if (mLevels[i].getFloorNumber() == level.getFloorNumber()) {
                    return i;
                }
            }

            return NOT_FOUND;
        }

        /**
         * Changing font in floor changing widget depending on text size
         * @param text shown in floor changing widget
         */
        public void updateFont(TextView text) {
            int size = text.getText().length();
            switch (size) {
                case 1:
                    text.setTextSize(24);
                    break;
                case 2:
                    text.setTextSize(21);
                    break;
                case 3:
                    text.setTextSize(18);
                    break;
                case 4:
                    text.setTextSize(15);
                    break;
                default:
                    text.setTextSize(12);
                    break;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View mView = convertView;
            if (mView == null)
                mView = mInflater.inflate(mFloorItem, null);
            TextView text = (TextView) mView.findViewById(mFloorName);
            text.setText(mLevels[position].getFloorSynonym());
            updateFont(text);
            int showSep = mLevels[position].getFloorNumber() == 0
                    && position != mLevels.length - 1 ? View.VISIBLE : View.INVISIBLE;
            View separator = mView.findViewById(mFloorGroundSep);
            separator.setVisibility(showSep);
            return mView;
        }
    }

    /** An example implementation for floor controller widget. Needs to implement
     * VenueMapFragment.VenueListener interface in order
     * to update floor selection
     */
    public class FloorsControllerWidget implements AdapterView.OnItemClickListener, VenueListener {

        private Context mActivity;
        private VenueMapFragment mVenueMapFragment;
        private ListView mFloorListView;
        private final int mFloorItem;
        private final int mFloorName;
        private final int mFloorGroundSep;

        public FloorsControllerWidget(Context context, VenueMapFragment venueLayer,
                                     ListView listView, int floorItemId, int floorNameId, int floorGroundSepId) {
            mActivity = context;
            mFloorItem = floorItemId;
            mFloorName = floorNameId;
            mFloorGroundSep = floorGroundSepId;

            mVenueMapFragment = venueLayer;
            mVenueMapFragment.addListener(this);

            mFloorListView = listView;
            mFloorListView.setOnItemClickListener(this);

            if (mVenueMapFragment.getSelectedVenue() != null) {
                onVenueSelected(mVenueMapFragment.getSelectedVenue());
            }
        }

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
            view.setSelected(true);
            VenueController controller = mVenueMapFragment
                    .getVenueController(mVenueMapFragment.getSelectedVenue());
            if (controller != null) {
                int levelIndex = controller.getVenue().getLevels().size() - 1 - index;
                Level level = controller.getVenue().getLevels().get(levelIndex);
                controller.selectLevel(level);
            }
        }

        @Override
        public void onVenueSelected(Venue venue) {
            mSelectedVenue = venue;
            mFloorListView.setAdapter(new VenueFloorAdapter(mActivity, venue.getLevels(),
                    mFloorItem, mFloorName, mFloorGroundSep));
            updateSelectedLevel(mVenueMapFragment.getVenueController(venue));

            mFloorListView.setVisibility(View.VISIBLE);
        }

        /**
         * Updating controller view based on level selection
         * @param controller to be updated
         */
        private void updateSelectedLevel(VenueController controller) {
            Level selectedLevel = controller.getSelectedLevel();
            if (selectedLevel != null) {
                int pos = ((VenueFloorAdapter) mFloorListView.getAdapter())
                        .getLevelIndex(selectedLevel);
                if (pos != -1) {
                    mFloorListView.setSelection(pos);
                    mFloorListView.smoothScrollToPosition(pos);
                    mFloorListView.setItemChecked(pos, true);
                }
            }
        }

        @Override
        public void onVenueDeselected(Venue venue, DeselectionSource source) {
            mSelectedVenue = null;
            mSelectedSpace = null;
            mFloorListView.setAdapter(null);
            mFloorListView.setVisibility(View.INVISIBLE);
            mVenueMapFragment.getMap().setTilt(0);
            clearRoute();
        }

        @Override
        public void onFloorChanged(Venue venue, Level oldLevel, Level newLevel) {
            VenueController controller = mVenueMapFragment.getVenueController(venue);
            if (controller != null) {
                updateSelectedLevel(controller);
                mUserControl = true;
                invalidateOptionsMenu();
            }
        }

        @Override
        public void onVenueTapped(Venue venue, float x, float y) {
        }

        @Override
        public void onSpaceSelected(Venue venue, Space space) {
            clearRoute();
            mSelectedSpace = space;
            showOrHideRoutingButton();
        }

        @Override
        public void onSpaceDeselected(Venue venue, Space space) {
            clearRoute();
            mSelectedSpace = null;
            showOrHideRoutingButton();
        }

        @Override
        public void onVenueVisibleInViewport(Venue venue, boolean visible) {
        }
    }

    /**
     * Clears the route if shown
     */
    private void clearRoute() {
        if (mRouteShown) {
            if (mRoutingController != null) {
                mRoutingController.hideRoute();
            }
            mRouteShown = false;
        }
    }

    /**
     * Show or hide indoor routing button
     */
    public void showOrHideRoutingButton() {
        Button showRouteButton = findViewById(R.id.buttonShowRoute);
        if (showRouteButton != null) {
            if (mSelectedSpace != null && mIndoorRouting) {
                showRouteButton.setVisibility(View.VISIBLE);
            } else {
                showRouteButton.setVisibility(View.GONE);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable logging...
        Log.mEnabled = true;

        setContentView(R.layout.activity_main);
        // checking dynamically controlled permissions
        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            startVenueMaps();
        } else {
            ActivityCompat
                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (mRouteShown) {
            clearRoute();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        add_private_venues = menu.findItem(R.id.action_add_private_venues);
        remove_private_venues = menu.findItem(R.id.action_remove_private_venues);
        follow_position = menu.findItem(R.id.follow_position);
        add_indoor_to_position = menu.findItem(R.id.add_indoor_to_position);
        indoor_routing = menu.findItem(R.id.indoor_routing);
        if (!mPrivateVenues) {
            add_private_venues.setVisible(true);
            remove_private_venues.setVisible(false);
        } else {
            add_private_venues.setVisible(false);
            remove_private_venues.setVisible(true);
        }
        if (!mUserControl) {
            follow_position.setChecked(true);
        } else {
            follow_position.setChecked(false);
        }
        if (mIndoorPositioning) {
            add_indoor_to_position.setChecked(true);
        } else {
            add_indoor_to_position.setChecked(false);
        }
        if (mIndoorRouting) {
            indoor_routing.setChecked(true);
        } else {
            indoor_routing.setChecked(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected");
        if (mHereLocation == null) {
            return false;
        }
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_add_private_venues:
            case R.id.action_remove_private_venues:
                changeVenuesContent();
                return true;
            case R.id.follow_position:
                followPosition();
                return true;
            case R.id.add_indoor_to_position:
                indoorToPosition();
                return true;
            case R.id.indoor_routing:
                indoorRouting();
                showOrHideRoutingButton();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Changing venue map content by adding or removing private venues support
     */
    private void changeVenuesContent() {
        Log.v(TAG, "changeVenuesContent");
        if (!mPrivateVenues) {
            mPrivateVenues = true;
            Log.v(TAG,"Private Venues content added");
        } else {
            mPrivateVenues = false;
            Log.v(TAG,"Private Venues content removed");
        }
        // Reinitialization of the map
        refreshMapView();
    }

    /**
     * Option for map to follow position updates
     */
    private void followPosition() {
        Log.v(TAG, "followPosition");
        if (mUserControl) {
            mUserControl = false;
            mMap.setCenter(mLastMapCenter, Map.Animation.NONE);
            Log.v(TAG, "Following position enabled");
        } else {
            mUserControl = true;
            Log.v(TAG, "Following position disabled");
        }
        invalidateOptionsMenu();
    }

    /**
     * Adding indoor position to position manager
     */
    private void indoorToPosition() {
        Log.v(TAG, "indoorToPosition");
        if (mIndoorPositioning) {
            mIndoorPositioning = false;
            Log.v(TAG, "Indoor positioning removed");
        } else {
            mIndoorPositioning = true;
            Log.v(TAG, "Indoor positioning added");
        }
        stopPositioningUpdates();
        startPositionUpdates();
        invalidateOptionsMenu();
    }

    /**
     * Enable or disable indoor routing
     */
    private void indoorRouting() {
        Log.v(TAG, "indoorRouting");
        if (mIndoorRouting) {
            mIndoorRouting = false;
            Log.v(TAG, "Indoor routing disabled");
        } else {
            mIndoorRouting = true;
            Log.v(TAG, "Indoor routing enabled");
        }
        invalidateOptionsMenu();
    }

    /**
     * Refreshing map view if parameters for venue map service changed
     */
    protected void refreshMapView() {
        Log.v(TAG, "refreshMapView");
        // Set main activity on pause
        onPause();
        // Remember center of the map for future
        if (mMap != null) {
            mLastMapCenter = mMap.getCenter();
        }
        // Reinitialization of the map
        stopVenueMaps();
        startVenueMaps();
        // Resume main activity
        onResume();
    }

    @Override
    public void onPositionUpdated(final PositioningManager.LocationMethod locationMethod, final GeoPosition geoPosition, final boolean mapMatched) {
        mLastReceivedPosition = geoPosition;
        GeoCoordinate receivedCoordinate = mLastReceivedPosition.getCoordinate();
        if (mTransforming) {
            mPendingUpdate = new Runnable() {
                @Override
                public void run() {
                    onPositionUpdated(locationMethod, geoPosition, mapMatched);
                }
            };
        } else {
            if (mVenueMapFragment != null) {
                mLastMapCenter = receivedCoordinate;
                if (!mUserControl) {
                    // when "follow position" options selected than map centered according to position updates
                    mMap.setCenter(receivedCoordinate, Map.Animation.NONE);
                    // Correctly displaying indoor position inside the venue
                    if (geoPosition.getPositionSource() == GeoPosition.SOURCE_INDOOR) {
                        if (!geoPosition.getBuildingId().isEmpty() && mPrivateVenues) {
                            mVenueMapFragment.selectVenueAsync(geoPosition.getBuildingId());
                            mVenueMapFragment.getVenueController(mVenueMapFragment.getSelectedVenue());
                            selectLevelByFloorId(geoPosition.getFloorId());
                        }
                    }
                }
            }
            // Write updated position to the log and to info view
            updateLocationInfo(locationMethod, geoPosition);
        }
    }

    /**
     * Selecting venue level by given floorID
     * @param floorId current indoor position
     */
    protected void selectLevelByFloorId(int floorId) {
        if (mVenueMapFragment != null) {
            Venue venue = mVenueMapFragment.getSelectedVenue();
            if (venue != null) {
                mVenueMapFragment.setFloorChangingAnimation(true);
                List<Level> levels = venue.getLevels();
                for (Level item : levels) {
                    if (item != null) {
                        if (item.getFloorNumber() == floorId) {
                            mVenueMapFragment.getVenueController(venue).selectLevel(item);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
        if (locationStatus == PositioningManager.LocationStatus.OUT_OF_SERVICE) {
            // Out of service, last received position no longer valid for route calculation
            mLastReceivedPosition = null;
        }
    }

    @Override
    public void onMapTransformStart() {
        Log.v(TAG, "onMapTransformStart");
        mTransforming = true;
    }

    @Override
    public void onMapTransformEnd(MapState mapState) {
        Log.v(TAG, "onMapTransformEnd");
        mTransforming = false;
        if (mPendingUpdate != null) {
            mPendingUpdate.run();
            mPendingUpdate = null;
        }
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat
                                .shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                                   + " not granted. "
                                                   + "Please go to settings and turn on for sample app",
                                           Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                startVenueMaps();
                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onDoubleTapEvent(PointF point) {
        return false;
    }

    @Override
    public boolean onLongPressEvent(PointF point) {
        return false;
    }

    @Override
    public void onLongPressRelease() {
    }

    @Override
    public boolean onMapObjectsSelected(List<ViewObject> arg0) {
        return false;
    }

    @Override
    public void onMultiFingerManipulationEnd() {
    }

    @Override
    public void onMultiFingerManipulationStart() {
    }

    @Override
    public void onPanEnd() {
    }

    @Override
    public void onPanStart() {
        if (mMap != null ) {
            // User takes control of map instead of position updates
            mUserControl = true;
            invalidateOptionsMenu();
        }

    }

    @Override
    public void onPinchLocked() {
    }

    @Override
    public boolean onPinchZoomEvent(float scaleFactor, PointF point) {
        return false;
    }

    @Override
    public boolean onRotateEvent(float angle) {
        return false;
    }

    @Override
    public void onRotateLocked() {
    }

    @Override
    public boolean onTiltEvent(float angle) {
        return false;
    }

    @Override
    public boolean onTwoFingerTapEvent(PointF arg0) {
        return false;
    }

    @Override
    public boolean onTapEvent(PointF p) {
        if (mMap == null){
            Toast.makeText(BasicVenueActivity.this, "Initialization of venue service is in progress...", Toast.LENGTH_SHORT).show();
            return false;
        }
        GeoCoordinate touchLocation = mMap.pixelToGeo(p);
        double lat = touchLocation.getLatitude();
        double lon = touchLocation.getLongitude();
        String StrGeo = String.format("%.6f, %.6f", lat, lon);
        Toast.makeText(getApplicationContext(), StrGeo, Toast.LENGTH_SHORT).show();
        mUserControl = true;
        invalidateOptionsMenu();
        return false;
    }

    @Override
    public void onVenueTapped(Venue venue, float x, float y) {
        Log.v(TAG, "onVenueTapped");
        mMap.pixelToGeo(new PointF(x, y));
        mVenueMapFragment.selectVenue(venue);
    }

    @Override
    public void onSpaceDeselected(Venue venue, Space space) {
    }

    @Override
    public void onSpaceSelected(Venue venue, Space space) {
        Log.v(TAG, "onSpaceSelected");
        onSpaceSelectedMapMode(space);
    }

    @Override
    public void onFloorChanged(Venue venue, Level oldLevel, Level newLevel) {
    }

    @Override
    public void onVenueVisibleInViewport(Venue venue, boolean visible) {
    }

    @Override
    public void onVenueSelected(Venue venue) {
        Log.v(TAG, "onVenueSelected: %s", venue.getId());
        if (mVenueMapFragment == null) {
            return;
        }
        String venueId = venue.getId();
        String venueName = venue.getContent().getName();
        Toast.makeText(getApplicationContext(), venueId + ": " + venueName, Toast.LENGTH_SHORT)
                .show();
        Log.v(TAG, "Venue selected: %s: %s", venueId, venueName);
    }

    @Override
    public void onVenueDeselected(Venue var1, DeselectionSource var2) {
        mUserControl = true;
        invalidateOptionsMenu();
    }

    /**
     * Showing information about space selected from the venue map
     * and logging it
     * @param space selected space
     */
    private void onSpaceSelectedMapMode(Space space) {
        Log.v(TAG, "onSpaceSelectedMapMode");
        String spaceName = space.getContent().getName();
        String parentCategory = space.getContent().getParentCategoryId();
        String placeCategory = space.getContent().getPlaceCategoryId();
        Toast.makeText(getApplicationContext(), "Space " + spaceName + ", parent category: "
                + parentCategory + ", place category: " + placeCategory, Toast.LENGTH_SHORT).show();
        Log.v(TAG,"Space selected: %s, Parent category: %s, Place category: %s", spaceName, parentCategory, placeCategory);
    }

    /**
     * Update location information to the log and location view.
     * @param geoPosition Latest geo position update.
     */
    private void updateLocationInfo(PositioningManager.LocationMethod locationMethod, GeoPosition geoPosition) {
        if (mLocationInfo == null) {
            return;
        }
        final StringBuffer sb = new StringBuffer();
        final GeoCoordinate coord = geoPosition.getCoordinate();
        if(geoPosition.getPositionSource() != GeoPosition.UNKNOWN) {
            sb.append("Position Source: ").append(String.format(Locale.US, "%s\n", positionSourceToString(geoPosition)));
        }
        if (geoPosition.getPositionTechnology() != GeoPosition.UNKNOWN) {
            sb.append("Position Technology: ").append(String.format(Locale.US, "%s\n", positionTechnologyToString(geoPosition)));
        }
        sb.append("Coordinate:").append(String.format(Locale.US, "%.6f, %.6f\n", coord.getLatitude(), coord.getLongitude()));
        if (geoPosition.getLatitudeAccuracy() != GeoPosition.UNKNOWN) {
            sb.append("Uncertainty:").append(String.format(Locale.US, "%.2fm\n", geoPosition.getLatitudeAccuracy()));
        }
        if (coord.getAltitude() != GeoCoordinate.UNKNOWN_ALTITUDE) {
            sb.append("Altitude:").append(String.format(Locale.US, "%.2fm\n", coord.getAltitude()));
        }
        if (geoPosition.getHeading() != GeoPosition.UNKNOWN) {
            sb.append("Heading:").append(String.format(Locale.US, "%.2f\n", geoPosition.getHeading()));
        }
        if (geoPosition.getSpeed() != GeoPosition.UNKNOWN) {
            sb.append("Speed:").append(String.format(Locale.US, "%.2fm/s\n", geoPosition.getSpeed()));
        }
        if (geoPosition.getBuildingName() != null) {
            sb.append("Building: ").append(geoPosition.getBuildingName());
            if (geoPosition.getBuildingId() != null) {
                sb.append(" (").append(geoPosition.getBuildingId()).append(")\n");
            } else {
                sb.append("\n");
            }
        }
        if (geoPosition.getFloorId() != null) {
            sb.append("Floor ID: ").append(geoPosition.getFloorId()).append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        mLocationInfo.setText(sb.toString());
        Log.v(TAG, "Position Updated:\n%s", sb.toString());
    }

    /**
     * Converting position source to string
     * @param geoPosition latest position
     * @return string representation of position source
     */
    private String positionSourceToString(GeoPosition geoPosition) {
        final int sources = geoPosition.getPositionSource();
        if (sources == GeoPosition.SOURCE_NONE) {
            return "NONE";
        }
        final StringBuilder result = new StringBuilder();
        if ((sources & GeoPosition.SOURCE_CACHE) != 0) {
            result.append("CACHE ");
        }
        if ((sources & GeoPosition.SOURCE_FUSION) != 0) {
            result.append("FUSION ");
        }
        if ((sources & GeoPosition.SOURCE_HARDWARE) != 0) {
            result.append("HARDWARE ");
        }
        if ((sources & GeoPosition.SOURCE_INDOOR) != 0) {
            result.append("INDOOR ");
        }
        if ((sources & GeoPosition.SOURCE_OFFLINE) != 0) {
            result.append("OFFLINE ");
        }
        if ((sources & GeoPosition.SOURCE_ONLINE) != 0) {
            result.append("ONLINE ");
        }
        return result.toString().trim();
    }

    /**
     * Converting position technology to string
     * @param geoPosition latest position
     * @return string representation of position technology
     */
    private String positionTechnologyToString(GeoPosition geoPosition) {
        final int technologies = geoPosition.getPositionTechnology();
        if (technologies == GeoPosition.TECHNOLOGY_NONE) {
            return "NONE";
        }
        final StringBuilder result = new StringBuilder();
        if ((technologies & GeoPosition.TECHNOLOGY_BLE) != 0) {
            result.append("BLE ");
        }
        if ((technologies & GeoPosition.TECHNOLOGY_CELL) != 0) {
            result.append("CELL ");
        }
        if ((technologies & GeoPosition.TECHNOLOGY_GNSS) != 0) {
            result.append("GNSS ");
        }
        if ((technologies & GeoPosition.TECHNOLOGY_WIFI) != 0) {
            result.append("WIFI ");
        }
        if ((technologies & GeoPosition.TECHNOLOGY_SENSORS) != 0) {
            result.append("SENSORS ");
        }
        return result.toString().trim();
    }

    // Google has deprecated android.app.Fragment class. It is used in current HERE Mobile SDK
    // implementation. Will be fixed in future HERE Mobile SDK version.
    @SuppressWarnings("deprecation")
    private VenueMapFragment getMapFragment() {
        return (VenueMapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
    }

    /**
     * Initializes HERE Venue Maps. Called after permission check.
     */
    private void startVenueMaps() {
        Log.v(TAG, "InitializeVenueMaps");
        mActivity = this;

        mVenueMapFragment = getMapFragment();
        mLocationInfo = (TextView) findViewById(R.id.textViewLocationInfo);

        mVenueMapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {
                    Log.v(TAG, "InitializeVenueMaps: OnEngineInitializationCompleted");

                    mVenueService = mVenueMapFragment.getVenueService();
                    mRoutingController = mVenueMapFragment.getRoutingController();
                    if (mRoutingController != null) {
                        mRoutingController.addListener(BasicVenueActivity.this);
                    }
                    // Setting venue service content based on menu option
                    if (!mPrivateVenues) {
                        setVenueServiceContent(false, false);// Public only
                    } else {
                        setVenueServiceContent(true, true);// Private + public
                    }
                } else {
                    new AlertDialog.Builder(BasicVenueActivity.this).setMessage(
                            "Error : " + error.name() + "\n\n" + error.getDetails())
                            .setTitle(R.string.engine_init_error)
                            .setNegativeButton(android.R.string.cancel,
                                               new DialogInterface.OnClickListener() {
                                                   @Override
                                                   public void onClick(
                                                           DialogInterface dialog,
                                                           int which) {
                                                       BasicVenueActivity.this.finish();
                                                   }
                                               }).create().show();
                }
            }
        }, new VenueServiceListener() {
            @Override
            public void onInitializationCompleted(VenueService.InitStatus result) {
                Log.v(TAG, "VenueServiceListener: OnInitializationCompleted with result: %s", result);
                switch (result) {
                case IN_PROGRESS:
                    Log.v(TAG, "Initialization of venue service is in progress...");
                    Toast.makeText(BasicVenueActivity.this, "Initialization of venue service is in progress...", Toast.LENGTH_SHORT).show();
                    break;
                case OFFLINE_SUCCESS:
                case ONLINE_SUCCESS:
                    // Adding venue listener to map fragment
                    mVenueMapFragment.addListener(mActivity);
                    // Set animations on for floor change and venue entering
                    mVenueMapFragment.setFloorChangingAnimation(true);
                    mVenueMapFragment.setVenueEnteringAnimation(true);
                    // Ask notification when venue visible; this notification is
                    // part of VenueMapFragment.VenueListener
                    mVenueMapFragment.setVenuesInViewportCallback(true);

                    // Add Gesture Listener for map fragment
                    mVenueMapFragment.getMapGesture().addOnGestureListener(mActivity, 0, false);

                    // retrieve a reference of the map from the map fragment
                    mMap = mVenueMapFragment.getMap();

                    mMap.addTransformListener(mActivity);
                    mMap.setZoomLevel(mMap.getMaxZoomLevel() - 3);


                    // Create floors controller widget
                        mFloorsControllerWidget = new FloorsControllerWidget(mActivity, mVenueMapFragment,
                                (ListView) findViewById(R.id.floorListView), R.layout.floor_item,
                                R.id.floorName, R.id.floorGroundSep);

                    // Start of Position Updates
                    try {
                        startPositionUpdates();
                        mVenueMapFragment.getPositionIndicator().setVisible(true);
                    } catch (Exception ex) {
                        Log.w(TAG, "startPositionUpdates: Could not register for location updates: %s", Log.getStackTraceString(ex));
                    }
                    if (mLastMapCenter == null) {
                        mMap.setCenter(new GeoCoordinate(61.497961, 23.763606, 0.0), Map.Animation.NONE);
                    } else {
                        mMap.setCenter(mLastMapCenter, Map.Animation.NONE);
                    }
                    break;
                }
            }
        });
    }

    /**
     * Setting usage of Private Venues along with Public Venues
     * @param mCombined used for setting Combined content
     * @param mPrivate used for setting Private content
     * VenueServiceListener should be initialized after setting this content
     * More details in documentation
     */
    private void setVenueServiceContent(boolean mCombined, boolean mPrivate) {
        try {
            Log.v(TAG, "setVenueServiceContent: Combined = %s, Private = %s", mCombined, mPrivate);
            mVenueService.setPrivateContent(mPrivate);
            mVenueService.setIsCombinedContent(mCombined);
        } catch (java.security.AccessControlException e) {
            e.printStackTrace();
            Toast.makeText(BasicVenueActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "SetVenueServiceContent error: %s", Log.getStackTraceString(e));
        }
    }

    /** Initialization of Position Updates
     * Called after map initialization
     */
    private void startPositionUpdates() {
        Log.v(TAG, "Start of Positioning Updates");
        mPositioningManager = PositioningManager.getInstance();
        if (mPositioningManager == null) {
            Log.w(TAG, "startPositionUpdates: PositioningManager is null");
            return;
        }

        mHereLocation = LocationDataSourceHERE.getInstance(mPositioningStatusListener);

        if (mHereLocation == null) {
            Log.w(TAG, "startPositionUpdates: LocationDataSourceHERE is null");
            finish();
            return;
        }

        mHereLocation.setDiagnosticsListener(
                new DiagnosticsListener() {
                    @Override
                    public void onDiagnosticEvent(Event event) {
                        Log.v(TAG, "onDiagnosticEvent: %s", event.getDescription());
                    }
                }
        );

        mPositioningManager.setDataSource(mHereLocation);
        try {
            mPositioningManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(mActivity));
            if (mIndoorPositioning) {
                mLocationMethod = PositioningManager.LocationMethod.GPS_NETWORK_INDOOR;
                Log.v(TAG, "Location method set to GPS_NETWORK_INDOOR");
            } else {
                mLocationMethod = PositioningManager.LocationMethod.GPS_NETWORK;
                Log.v(TAG, "Location method set to GPS_NETWORK");
            }
            if (!mPositioningManager.start(mLocationMethod)) {
                Log.e(TAG, "startPositionUpdates: PositioningManager.start returned error");
            }
        } catch (Exception ex) {
            Log.w(TAG, "startPositionUpdates: Could not register for location updates: %s", Log.getStackTraceString(ex));
        }

        try {
            mRadioMapLoader = new RadioMapLoadHelper(LocationDataSourceHERE.getInstance().getRadioMapLoader(),
                    new RadioMapLoadHelper.Listener() {
                        @Override
                        public void onError(@NonNull Venue venue, RadioMapLoader.Status status) {
                            // Radio map loading failed with status.
                        }

                        @Override
                        public void onProgress(@NonNull Venue venue, int progress) {
                            // Radio map loading progress.
                        }

                        @Override
                        public void onCompleted(@NonNull Venue venue, RadioMapLoader.Status status) {
                            Log.i(TAG, "Radio map for venue: " + venue.getId() + ", completed with status: " + status);
                            // Radio map loading completed with status.
                        }
                    });
            mVenueService.addVenueLoadListener(mVenueLoadListener);
        } catch (Exception ex) {
            Log.e(TAG, "startPositionUpdates: setting up radio map loader failed", ex);
            mRadioMapLoader = null;
        }
    }

    /**
     * Stop position updates
     * and remove listener
     */
    protected void stopPositioningUpdates() {
        Log.v(TAG, "stopPositioningUpdates");
        if (mPositioningManager != null) {
            mPositioningManager.stop();
            mPositioningManager.removeListener(mActivity);
        }
        if (mRadioMapLoader != null) {
            mVenueService.removeVenueLoadListener(mVenueLoadListener);
            mRadioMapLoader = null;
        }
    }

    /**
     * Stop VenueMaps service
     */
    protected void stopVenueMaps() {
        Log.v(TAG, "stopVenueMaps");
        stopPositioningUpdates();
        mVenueMapFragment = null;
        mMap = null;
        mFloorsControllerWidget = null;
    }

    // Setup routing parameters and calculate route.
    public void onCalculateRouteClick(View v) {
        if (mLastReceivedPosition != null && mSelectedVenue != null && mSelectedSpace != null ) {
            VenueRouteOptions venueRouteOptions = new VenueRouteOptions();
            RouteOptions options = venueRouteOptions.getRouteOptions();
            // Set algorithm mode shortest and transport mode pedestrian in this case
            options.setRouteType(RouteOptions.Type.SHORTEST);
            options.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
            options.setRouteCount(1);
            venueRouteOptions.setRouteOptions(options);
            VenueController selectedVenueController = mVenueMapFragment.getVenueController(mSelectedVenue);
            if (selectedVenueController != null && mRoutingController != null) {
                Toast.makeText(BasicVenueActivity.this, "Calculating route...", Toast.LENGTH_SHORT).show();
                // Determine start location either from the venue as
                // LevelLocation, or from outside as OutdoorLocation
                BaseLocation startLocation;
                if ((mLastReceivedPosition.getPositionSource() == GeoPosition.SOURCE_INDOOR)) {
                    Level startLevel = selectedVenueController.getSelectedLevel();
                    for (final Level level : selectedVenueController.getVenue().getLevels()) {
                        if (level.getFloorNumber() == mLastReceivedPosition.getFloorId()) {
                            startLevel = level;
                            break;
                        }
                    }
                    startLocation = new LevelLocation(startLevel,
                            mLastReceivedPosition.getCoordinate(), selectedVenueController);
                } else {
                    startLocation = new OutdoorLocation(mLastReceivedPosition.getCoordinate());
                }
                // End location is in this case always the selected space
                BaseLocation endLocation = new SpaceLocation(mSelectedSpace, selectedVenueController);
                // This is an async function, the logic to display route is in callback
                // onCombinedRouteCompleted(CombinedRoute route)
                mRoutingController.calculateCombinedRoute(startLocation, endLocation, venueRouteOptions);
            }
        } else {
            Toast.makeText(BasicVenueActivity.this, "Unable to calculate route", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onCombinedRouteCompleted(CombinedRoute route) {
        Log.v(TAG, "onCombinedRouteCompleted");
        CombinedRoute.VenueRoutingError error = route.getError();
        if (error == CombinedRoute.VenueRoutingError.NO_ERROR) {
            if (mVenueMapFragment != null) {
                VenueController selectedVenueController = mVenueMapFragment.getVenueController(mSelectedVenue);
                if (selectedVenueController != null && mRoutingController != null) {
                    Log.i(TAG, "onCombinedRouteCompleted route found");
                    Toast.makeText(BasicVenueActivity.this, "Route found", Toast.LENGTH_SHORT).show();
                    // Use RoutingController to show route
                    mRoutingController.showRoute(route);
                    mRouteShown = true;
                }
            }
        } else {
            Toast.makeText(BasicVenueActivity.this, "No route found", Toast.LENGTH_SHORT).show();
        }
    }
}
