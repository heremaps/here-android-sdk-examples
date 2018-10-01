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

package com.here.android.example.venuesandlogging;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
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
import com.here.android.mpa.venues3d.DeselectionSource;
import com.here.android.mpa.venues3d.Level;
import com.here.android.mpa.venues3d.Space;
import com.here.android.mpa.venues3d.Venue;
import com.here.android.mpa.venues3d.VenueController;
import com.here.android.mpa.venues3d.VenueMapFragment;
import com.here.android.mpa.venues3d.VenueMapFragment.VenueListener;
import com.here.android.mpa.venues3d.VenueService;
import com.here.android.mpa.venues3d.VenueService.VenueServiceListener;
import com.here.android.positioning.DiagnosticsListener;
import com.here.android.positioning.StatusListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BasicVenueActivity extends AppCompatActivity
        implements VenueListener,
            OnGestureListener,
            OnPositionChangedListener,
            OnTransformListener {

    // TAG string for logging purposes
    private static final String TAG = "VenuesAndLogging.BasicVenueActivity";

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    // map embedded in the map fragment
    private Map mMap;

    // Venue map fragment embedded in this activity
    private VenueMapFragment venueMapFragment;

    // Widget for selecting floors of the venue
    private FloorsControllerWidget mFloorsControllerWidget;

    // positioning manager instance
    private PositioningManager mPositioningManager;

    // Flag for using indoor positioning
    private boolean mIndoorPositioning;

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

    // Last known map center
    private GeoCoordinate mLastMapCenter;

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
        public void onWifiIndoorPositioningNotAvailable() {
            Log.v(TAG, "StatusListener.onWifiIndoorPositioningNotAvailable");
        }
    };



    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

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
            mFloorListView.setAdapter(null);
            mFloorListView.setVisibility(View.INVISIBLE);
            mVenueMapFragment.getMap().setTilt(0);
        }

        @Override
        public void onFloorChanged(Venue venue, Level oldLevel, Level newLevel) {
            updateSelectedLevel(mVenueMapFragment.getVenueController(venue));
            mUserControl = true;
            invalidateOptionsMenu();
        }

        @Override
        public void onVenueTapped(Venue venue, float x, float y) {
        }

        @Override
        public void onSpaceSelected(Venue venue, Space space) {
        }

        @Override
        public void onSpaceDeselected(Venue venue, Space space) {
        }

        @Override
        public void onVenueVisibleInViewport(Venue venue, boolean visible) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable logging...
        Log.mEnabled = true;

        setContentView(R.layout.activity_main);
        // checking dynamically controlled permissions
        checkPermissions();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        add_private_venues = menu.findItem(R.id.action_add_private_venues);
        remove_private_venues = menu.findItem(R.id.action_remove_private_venues);
        follow_position = menu.findItem(R.id.follow_position);
        add_indoor_to_position = menu.findItem(R.id.add_indoor_to_position);
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
        final GeoCoordinate coordinate = geoPosition.getCoordinate();
        if (mTransforming) {
            mPendingUpdate = new Runnable() {
                @Override
                public void run() {
                    onPositionUpdated(locationMethod, geoPosition, mapMatched);
                }
            };
        } else {
            if (venueMapFragment != null) {
                mLastMapCenter = coordinate;
                if (!mUserControl) {
                    // when "follow position" options selected than map centered according to position updates
                    mMap.setCenter(coordinate, Map.Animation.NONE);
                    // Correctly displaying indoor position inside the venue
                    if (geoPosition.getPositionSource() == GeoPosition.SOURCE_INDOOR) {
                        if (!geoPosition.getBuildingId().isEmpty() && mPrivateVenues) {
                            venueMapFragment.selectVenueAsync(geoPosition.getBuildingId());
                            venueMapFragment.getVenueController(venueMapFragment.getSelectedVenue());
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
        if (venueMapFragment != null) {
            Venue venue = venueMapFragment.getSelectedVenue();
            if (venue != null) {
                venueMapFragment.setFloorChangingAnimation(true);
                List<Level> levels = venue.getLevels();
                for (Level item : levels) {
                    if (item != null) {
                        if (item.getFloorNumber() == floorId) {
                            venueMapFragment.getVenueController(venue).selectLevel(item);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPositionFixChanged(PositioningManager.LocationMethod locationMethod, PositioningManager.LocationStatus locationStatus) {
        // ignored
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
     * Checks the dynamically controlled permissions and requests missing
     * permissions from end user.
     */
    private void checkPermissions() {
        Log.v(TAG, "checkPermissions");
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // After all permissions were granted start venue map initialization
                startVenueMaps();
                break;
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
        venueMapFragment.selectVenue(venue);
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
        if (venueMapFragment == null) {
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
        switch (geoPosition.getPositionSource()) {
            case GeoPosition.SOURCE_OFFLINE:
                return "OFFLINE";
            case GeoPosition.SOURCE_ONLINE:
                return "ONLINE";
            case GeoPosition.SOURCE_CACHE:
                return "CACHE";
            case GeoPosition.SOURCE_FUSION:
                return "FUSION";
            case GeoPosition.SOURCE_HARDWARE:
                return "HARDWARE";
            case GeoPosition.SOURCE_INDOOR:
                return "INDOOR";
        }
        return "NONE";
    }

    /**
     * Converting position technology to string
     * @param geoPosition latest position
     * @return string representation of position technology
     */
    private String positionTechnologyToString(GeoPosition geoPosition) {
        switch (geoPosition.getPositionTechnology()) {
            case GeoPosition.TECHNOLOGY_WIFI:
                return "WIFI";
            case GeoPosition.TECHNOLOGY_BLE:
                return "BLE";
            case GeoPosition.TECHNOLOGY_CELL:
                return "CELL";
            case GeoPosition.TECHNOLOGY_GNSS:
                return "GNSS";
        }
        return "NONE";
    }

    // Google has deprecated android.app.Fragment class. It is used in current SDK implementation.
    // Will be fixed in future SDK version.
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

        venueMapFragment = getMapFragment();
        mLocationInfo = (TextView) findViewById(R.id.textViewLocationInfo);


        venueMapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {
                    Log.v(TAG, "InitializeVenueMaps: OnEngineInitializationCompleted");

                    mVenueService = venueMapFragment.getVenueService();

                    // Setting venue service content based on menu option
                    if (!mPrivateVenues) {
                        setVenueServiceContent(false, false);// Public only
                    } else {
                        setVenueServiceContent(true, true);// Private + public
                    }
                } else {
                    Log.e(TAG, "onEngineInitializationCompleted: %s", error);
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
                    venueMapFragment.addListener(mActivity);
                    // Set animations on for floor change and venue entering
                    venueMapFragment.setFloorChangingAnimation(true);
                    venueMapFragment.setVenueEnteringAnimation(true);
                    // Ask notification when venue visible; this notification is
                    // part of VenueMapFragment.VenueListener
                    venueMapFragment.setVenuesInViewportCallback(true);

                    // Add Gesture Listener for map fragment
                    venueMapFragment.getMapGesture().addOnGestureListener(mActivity, 0, false);

                    // retrieve a reference of the map from the map fragment
                    mMap = venueMapFragment.getMap();

                    mMap.addTransformListener(mActivity);
                    mMap.setZoomLevel(mMap.getMaxZoomLevel() - 3);


                    // Create floors controller widget
                        mFloorsControllerWidget = new FloorsControllerWidget(mActivity, venueMapFragment,
                                (ListView) findViewById(R.id.floorListView), R.layout.floor_item,
                                R.id.floorName, R.id.floorGroundSep);

                    // Start of Position Updates
                    try {
                        startPositionUpdates();
                        venueMapFragment.getPositionIndicator().setVisible(true);
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
    }

    /**
     * Stop VenueMaps service
     */
    protected void stopVenueMaps() {
        Log.v(TAG, "stopVenueMaps");
        stopPositioningUpdates();
        venueMapFragment = null;
        mMap = null;
        mFloorsControllerWidget = null;
    }
}
