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

package com.here.android.example.venue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.Map.Animation;
import com.here.android.mpa.mapping.MapGesture.OnGestureListener;
import com.here.android.mpa.mapping.PositionIndicator;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RouteOptions.TransportMode;
import com.here.android.mpa.routing.RouteOptions.Type;
import com.here.android.mpa.search.Address;
import com.here.android.mpa.venues3d.BaseLocation;
import com.here.android.mpa.venues3d.CombinedRoute;
import com.here.android.mpa.venues3d.DeselectionSource;
import com.here.android.mpa.venues3d.Level;
import com.here.android.mpa.venues3d.OutdoorLocation;
import com.here.android.mpa.venues3d.RoutingController;
import com.here.android.mpa.venues3d.RoutingController.RoutingControllerListener;
import com.here.android.mpa.venues3d.Space;
import com.here.android.mpa.venues3d.SpaceLocation;
import com.here.android.mpa.venues3d.Venue;
import com.here.android.mpa.venues3d.VenueController;
import com.here.android.mpa.venues3d.VenueInfo;
import com.here.android.mpa.venues3d.VenueMapFragment;
import com.here.android.mpa.venues3d.VenueMapFragment.VenueListener;
import com.here.android.mpa.venues3d.VenueRouteOptions;
import com.here.android.mpa.venues3d.VenueService.InitStatus;
import com.here.android.mpa.venues3d.VenueService.VenueServiceListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Venue3dActivity extends FragmentActivity
        implements VenueListener, OnGestureListener, RoutingControllerListener {

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    // An example list view adapter for floor switcher
    public class VenueFloorAdapter extends BaseAdapter {

        Level[] m_levels;
        private final int m_floorItem;
        private final int m_floorName;
        private final int m_floorGroundSep;
        private LayoutInflater m_inflater = null;

        public VenueFloorAdapter(Context context, List<Level> levels, int floorItemId,
                                 int floorNameId, int floorGroundSepId) {
            this.m_levels = new Level[levels.size()];
            m_floorItem = floorItemId;
            m_floorName = floorNameId;
            m_floorGroundSep = floorGroundSepId;
            for (int i = 0; i < levels.size(); i++) {
                this.m_levels[levels.size() - i - 1] = levels.get(i);
            }
            m_inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return m_levels.length;
        }

        @Override
        public Object getItem(int position) {
            return m_levels[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int getLevelIndex(Level level) {
            for (int i = 0; i < m_levels.length; i++) {
                if (m_levels[i].getFloorNumber() == level.getFloorNumber()) {
                    return i;
                }
            }

            return -1;
        }

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
                    text.setTextSize(28);
                    break;
                case 4:
                    text.setTextSize(15);
                    break;
                default:
                    text.setTextSize(16);
                    break;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (vi == null)
                vi = m_inflater.inflate(m_floorItem, null);
            TextView text = (TextView) vi.findViewById(m_floorName);
            text.setText(m_levels[position].getFloorSynonym());
            updateFont(text);
            int showSep = m_levels[position].getFloorNumber() == 0
                    && position != m_levels.length - 1 ? View.VISIBLE : View.INVISIBLE;
            View separator = vi.findViewById(m_floorGroundSep);
            separator.setVisibility(showSep);
            return vi;
        }
    }

    // An example implementation for floor controller widget. Needs to implement
    // VenueMapFragment.VenueListener interface in order
    // to update floor selection
    public class VenueFloorsController implements OnItemClickListener, VenueListener {

        private Context m_activity = null;
        private VenueMapFragment m_venueMapFragment = null;
        private ListView m_floorListView = null;
        private final int m_floorItem;
        private final int m_floorName;
        private final int m_floorGroundSep;

        public VenueFloorsController(Context context, VenueMapFragment venueLayer,
                ListView listView, int floorItemId, int floorNameId, int floorGroundSepId) {
            m_activity = context;
            m_floorItem = floorItemId;
            m_floorName = floorNameId;
            m_floorGroundSep = floorGroundSepId;

            m_venueMapFragment = venueLayer;
            m_venueMapFragment.addListener(this);

            m_floorListView = listView;
            m_floorListView.setOnItemClickListener(this);

            if (m_venueMapFragment.getSelectedVenue() != null) {
                onVenueSelected(m_venueMapFragment.getSelectedVenue());
            }
        }

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int index, long arg3) {
            view.setSelected(true);
            VenueController controller = m_mapFragment
                    .getVenueController(m_venueMapFragment.getSelectedVenue());
            if (controller != null) {
                int levelIndex = controller.getVenue().getLevels().size() - 1 - index;
                Level level = controller.getVenue().getLevels().get(levelIndex);
                controller.selectLevel(level);
            }
        }

        @Override
        public void onVenueSelected(Venue venue) {
            m_floorListView.setAdapter(new VenueFloorAdapter(m_activity, venue.getLevels(),
                    m_floorItem, m_floorName, m_floorGroundSep));
            updateSelectedLevel(m_mapFragment.getVenueController(venue));

            m_floorListView.setVisibility(View.VISIBLE);
        }

        private void updateSelectedLevel(VenueController controller) {
            Level selectedLevel = controller.getSelectedLevel();
            if (selectedLevel != null) {
                int pos = ((VenueFloorAdapter) m_floorListView.getAdapter())
                        .getLevelIndex(selectedLevel);
                if (pos != -1) {
                    m_floorListView.setSelection(pos);
                    m_floorListView.smoothScrollToPosition(pos);
                    m_floorListView.setItemChecked(pos, true);
                }
            }
        }

        @Override
        public void onVenueDeselected(Venue venue, DeselectionSource source) {
            m_floorListView.setAdapter(null);
            m_floorListView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onFloorChanged(Venue venue, Level oldLevel, Level newLevel) {
            updateSelectedLevel(m_mapFragment.getVenueController(venue));
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

    private static final int SELECTED_COLOR = Color.rgb(68, 167, 221);

    private Map m_map = null;
    private VenueMapFragment m_mapFragment = null;
    private AtomicBoolean m_initCompleted = new AtomicBoolean(false);

    private EditText m_venueIdEditText;

    private LinearLayout m_mainControlLayout;
    private LinearLayout m_routeInfoLayout;
    private Spinner m_routingOptionType;
    private Spinner m_routingOptionMode;

    private TextView m_routeStartGuideText;
    private TextView m_routeEndGuideText;
    private TextView m_routingFromText;
    private TextView m_routingToText;

    private Button m_showRouteButton;

    private BaseLocation startLocation = null;
    private BaseLocation endLocation = null;

    private boolean m_is_routing_mode = false;

    private VenueController m_currentVenue;
    private VenueFloorsController m_floorsController = null;

    private Venue3dActivity m_activity = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
    }

    private void initialize() {
        setContentView(R.layout.venues3d);

        m_activity = this;

        // Search for the map fragment in order to finish setup by calling init().
        m_mapFragment = (VenueMapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        m_venueIdEditText = (EditText) findViewById(R.id.venueIdEditText);

        m_mainControlLayout = (LinearLayout) findViewById(R.id.venueOpeningLayout);
        m_routeInfoLayout = (LinearLayout) findViewById(R.id.routeInfoLayout);
        m_routingOptionType = (Spinner) findViewById(R.id.routeOptionType);
        m_routingOptionMode = (Spinner) findViewById(R.id.routeOptionMode);

        m_routeStartGuideText = (TextView) findViewById(R.id.startLocationGuideText);
        m_routeEndGuideText = (TextView) findViewById(R.id.endLocationGuideText);
        m_routingFromText = (TextView) findViewById(R.id.startLocationText);
        m_routingToText = (TextView) findViewById(R.id.endLocationText);

        m_showRouteButton = (Button) findViewById(R.id.buttonShowRoute);

        // Fill dropDownList with routing type names.
        String[] type_values = new String[] { "Fastest", "Shortest" };
        configureSpinner(m_routingOptionType, type_values);

        // Fill dropDownList with routing mode names.
        String[] mode_values = new String[] { "Car", "Pedestrian", "Public Transport" };
        configureSpinner(m_routingOptionMode, mode_values);

        // Set default values to: Fastest and Pedestrian.
        m_routingOptionType.setSelection(0);
        m_routingOptionMode.setSelection(1);

        // initialise the Map Fragment to have a map created and attached to
        // the fragment
        m_mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    m_map = m_mapFragment.getMap();
                    // Set the map center, zoom level, orientation and tilt
                    m_map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0),
                            Map.Animation.NONE);
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment" + error.toString());
                }
            }
        }, new VenueServiceListener() {
            @Override
            public void onInitializationCompleted(InitStatus result) {
                if (result == InitStatus.ONLINE_SUCCESS || result == InitStatus.OFFLINE_SUCCESS) {
                    // Register the activity class as VenueMapFragment.VenueListener
                    m_mapFragment.addListener(m_activity);
                    // Set animations on for floor change and venue entering
                    m_mapFragment.setFloorChangingAnimation(true);
                    m_mapFragment.setVenueEnteringAnimation(true);
                    // Ask notification when venue visible; this notification is
                    // part of VenueMapFragment.VenueListener
                    m_mapFragment.setVenuesInViewportCallback(true);

                    // Add listener for onCombinedRouteCompleted.
                    m_mapFragment.getRoutingController().addListener(m_activity);

                    // Add listener for map gesture.
                    m_mapFragment.getMapGesture().addOnGestureListener(m_activity);

                    // Create floor change widget
                    m_floorsController = new VenueFloorsController(m_activity, m_mapFragment,
                            (ListView) findViewById(R.id.floorListView), R.layout.floor_item,
                            R.id.floorName, R.id.floorGroundSep);
                    m_initCompleted.set(true);

                    // Start position tracking
                    PositioningManager positioningManager = PositioningManager.getInstance();
                    positioningManager.start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR);

                    // Set positioning indicator visible
                    PositionIndicator positionIndicator = m_mapFragment.getPositionIndicator();
                    positionIndicator.setVisible(true);
                }
            }

            @Override
            public void onGetVenueCompleted(Venue venue) {
            }
        });
    }

    /**
     * Checks the dynamically controlled permissions and requests missing permissions from end user.
     */
    private void checkPermissions() {
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
                // all permissions were granted
                initialize();
                break;
        }
    }

    public void onOpenVenueClick(View v) {
        if (m_initCompleted.get()) {
            openVenueAsync();
        }
    }

    // Show routing UI panel and hide standard UI panel
    public void onShowRoutingPanelClick(View v) {
        int visibility = m_routeInfoLayout.getVisibility();
        if (visibility == View.VISIBLE) {
            return;
        }
        m_mainControlLayout.setVisibility(View.GONE);
        m_routeInfoLayout.setVisibility(View.VISIBLE);
        m_routingFromText.setText("");
        m_routingToText.setText("");
        m_routeStartGuideText.setTextColor(Color.WHITE);
        m_routeEndGuideText.setTextColor(Color.WHITE);

        // Don't allow to calculate a route until you set start and end destinations.
        m_showRouteButton.setBackgroundColor(Color.GRAY);
        m_showRouteButton.setClickable(false);
        startLocation = null;
        endLocation = null;
        m_is_routing_mode = true;
    }

    // Hide routing UI panel and show standard UI panel.
    public void onHideRoutingPanelClick(View v) {
        m_mainControlLayout.setVisibility(View.VISIBLE);
        m_routeInfoLayout.setVisibility(View.GONE);
        m_is_routing_mode = false;
        m_mapFragment.getRoutingController().hideRoute();
    }

    // Change the color of the start field to indicate user that he has set a start point.
    public void onSetStartClick(View v) {
        m_routeStartGuideText.setTextColor(SELECTED_COLOR);
        m_routingFromText.setTextColor(SELECTED_COLOR);
        m_routingFromText.setText(R.string.tap_start_point);
        m_routeEndGuideText.setTextColor(Color.WHITE);
        m_routingToText.setTextColor(Color.WHITE);
    }

    // Change the color of the end field to indicate user that he has set a destination point.
    public void onSetEndClick(View v) {
        m_routeEndGuideText.setTextColor(SELECTED_COLOR);
        m_routingToText.setTextColor(SELECTED_COLOR);
        m_routingToText.setText(R.string.tap_end_point);
        m_routeStartGuideText.setTextColor(Color.WHITE);
        m_routingFromText.setTextColor(Color.WHITE);
    }

    // Setup routing parameters and calculate route.
    public void onCalculateRouteClick(View v) {
        if ((startLocation == null) || (endLocation == null)) {
            Toast.makeText(getApplicationContext(), "you have to set start and stop point",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        VenueRouteOptions venueRouteOptions = new VenueRouteOptions();
        RouteOptions options = venueRouteOptions.getRouteOptions();

        // Set algorithm mode (fastest, shortest).
        options.setRouteType(Type.values()[m_routingOptionType.getSelectedItemPosition()]);

        // Set transport mode (pedestrian, car, public_transport).
        options.setTransportMode(
                TransportMode.values()[m_routingOptionMode.getSelectedItemPosition()]);

        options.setRouteCount(1);
        venueRouteOptions.setRouteOptions(options);
        RoutingController routingController = m_mapFragment.getRoutingController();

        // This is an async function, the logic to display route is in callback
        // onCombinedRouteCompleted(CombinedRoute route)
        routingController.calculateCombinedRoute(startLocation, endLocation, venueRouteOptions);
    }

    private void openVenueAsync() {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(m_venueIdEditText.getWindowToken(), 0);

        String venueId = m_venueIdEditText.getText().toString();
        VenueInfo result = m_mapFragment.selectVenueAsync(venueId);
        String textResult = "NOT IN THE INDEX FILE";
        if (result != null) {
            textResult = "TRYING TO OPEN...";
            m_map.setCenter(result.getBoundingBox().getCenter(), Animation.NONE, 17, 0, 1);
        }

        Toast.makeText(getApplicationContext(), "Open venue async:" + textResult,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVenueTapped(Venue venue, float x, float y) {
        m_currentVenue = m_mapFragment.getVenueController(venue);
        m_map.pixelToGeo(new PointF(x, y));
        m_currentVenue.getVenue().getId();
        m_mapFragment.selectVenue(venue);
    }

    @Override
    public void onVenueDeselected(Venue venue, DeselectionSource source) {
    }

    // Store selected location(geo_point or space) and display name in UI.
    private void addToRoute(BaseLocation location, String uiText) {
        if (m_routeStartGuideText.getCurrentTextColor() == SELECTED_COLOR) {
            startLocation = location;
            m_routingFromText.setText(uiText);
        } else if (m_routeEndGuideText.getCurrentTextColor() == SELECTED_COLOR) {
            endLocation = location;
            m_routingToText.setText(uiText);
        }

        // If start and destination are properly set then you can calculate route.
        if ((startLocation != null) && (endLocation != null)) {
            m_showRouteButton.setBackgroundColor(SELECTED_COLOR);
            m_showRouteButton.setClickable(true);
        }
    }

    @Override
    public void onSpaceSelected(Venue venue, Space space) {
        if (!m_is_routing_mode) {
            onSpaceSelectedMapMode(space);
            return;
        }

        String uiText = space.getContent().getName();
        addToRoute(new SpaceLocation(space, m_mapFragment.getVenueController(venue)), uiText);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean onTapEvent(PointF p) {
        if (!m_is_routing_mode) {
            return false;
        }

        GeoCoordinate touchLocation = m_map.pixelToGeo(p);
        double lat = touchLocation.getLatitude();
        double lon = touchLocation.getLongitude();
        String StrGeo = String.format("%.6f, %.6f", lat, lon);
        Toast.makeText(getApplicationContext(), StrGeo, Toast.LENGTH_SHORT).show();
        addToRoute(new OutdoorLocation(touchLocation), StrGeo);
        return false;
    }

    private void onSpaceSelectedMapMode(Space space) {

        String spaceName = space.getContent().getName();
        String parentCategory = space.getContent().getParentCategoryId();
        String placeCategory = space.getContent().getPlaceCategoryId();
        Toast.makeText(getApplicationContext(), "Space " + spaceName + ", parent category: "
                + parentCategory + ", place category: " + placeCategory, Toast.LENGTH_SHORT).show();

        Address address = space.getContent().getAddress();
        if (address != null) {
            System.out.println("Space address: " + address.getStreet() + " "
                    + address.getPostalCode() + " " + address.getCity());
            System.out.println("Space floor: " + address.getFloorNumber() + " place cat: "
                    + space.getContent().getPlaceCategoryId());
        }
    }

    @Override
    public void onSpaceDeselected(Venue venue, Space space) {
    }

    @Override
    public void onFloorChanged(Venue venue, Level oldLevel, Level newLevel) {
    }

    @Override
    public void onVenueVisibleInViewport(Venue venue, boolean visible) {
    }

    @Override
    public void onBackPressed() {
        VenueController controller = m_mapFragment
                .getVenueController(m_mapFragment.getSelectedVenue());
        if (controller == null) {
            super.onBackPressed();
        } else {
            if( controller.getSelectedSpace() == null ) {
                m_mapFragment.deselectVenue();
                if (m_currentVenue != null) {
                    m_currentVenue = null;
                }
            } else {
                controller.deselectSpace();
            }
        }
    }

    @Override
    public void onVenueSelected(Venue venue) {
        m_currentVenue = m_mapFragment.getVenueController(venue);

        String venueId = venue.getId();
        String venueName = venue.getContent().getName();
        Toast.makeText(getApplicationContext(), venueId + ": " + venueName, Toast.LENGTH_SHORT)
                .show();
    }

    // Add values to dropDownList
    private void configureSpinner(Spinner spinner, String[] listItems) {
        int spinner_item = android.R.layout.simple_spinner_item;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, spinner_item, listItems) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.BLACK);
                return v;
            }

            public View getDropDownView(int pos, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(pos, convertView, parent);
                ((TextView) v).setTextSize(22);
                return v;
            }
        };
        spinner.setAdapter(adapter);
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

    // Display computed route on the map.
    private boolean DisplayRoute(CombinedRoute route) {
        RoutingController routingController = m_mapFragment.getRoutingController();
        routingController.showRoute(route);
        return true;
    }

    @Override
    public void onCombinedRouteCompleted(CombinedRoute route) {
        boolean result = false;
        RoutingController routingController = m_mapFragment.getRoutingController();

        if (route.getRouteSections().size() > 0) {
            result = DisplayRoute(route);
        }

        if (!result) {
            routingController.hideRoute();
        }

        String textResult = "Combined route result:" + (result ? "SUCCESS" : "FAIL");
        Toast.makeText(getApplicationContext(), textResult, Toast.LENGTH_SHORT).show();

    }

}
