/*
 * Copyright (c) 2011-2019 HERE Europe B.V.
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

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.IconCategory;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.AudioPlayerDelegate;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoicePackage;
import com.here.android.mpa.guidance.VoiceSkin;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.lang.ref.WeakReference;
import java.util.List;

public class MapFragmentView {

    private static final String TAG = "TTS-BasicMapActivity";
    /*
     * ID of en-US voice skin package
     */
    private final int EN_US_ID = 206;

    private Map map = null;
    private MapFragment mapFragment = null;
    private Button downloadLanguage;
    private Button checkLanguageUpdate;
    private Switch ttsEngineChoice;
    private Button startNavigation;

    /**
     * Nuance TTS
     */
    private Nuance nuance;

    /**
     * Using to decide to use Nuance or no, by default TTS engine on device will be used
     */
    private boolean useNuance = false;

    /**
     * Helper for the very first fix after startup (we want to jump to that position then)
     */
    private boolean firstPositionSet = false;

    private MapRoute currentRoute;

    /**
     * Listener for navigation instructions
     */
    private NavigationManager.NewInstructionEventListener instructionHandler = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
            Maneuver m = NavigationManager.getInstance().getNextManeuver();
            Log.i(TAG, "New instruction : in " + NavigationManager.getInstance().getNextManeuverDistance() + " m do " + m.getTurn().name() + " / " + m.getAction().name() + " on " + m.getNextRoadName() + " (from " + m.getRoadName() + ")");
            // ...
            // do something with this information, e.g. show it to the user
            super.onNewInstructionEvent();
        }
    };

    /**
     * Implementation of audio player with Nuance TTS usage
     */
    private AudioPlayerDelegate player = new AudioPlayerDelegate() {
        @Override
        public boolean playText(final String s) {
            Log.i(TAG, "Text for TTS: " + s);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    nuance.speak(s);
                }
            });
            return true;
        }

        @Override
        public boolean playFiles(String[] files) {
            // we don't want to play audio files
            return false;
        }
    };
    private Activity activity;

    /**
     * Listener for changes of position
     */
    private PositioningManager.OnPositionChangedListener mapPositionHandler = new PositioningManager.OnPositionChangedListener() {
        @Override
        public void onPositionUpdated(PositioningManager.LocationMethod method, GeoPosition position, boolean isMapMatched) {
            if (!position.isValid())
                return;
            if (!firstPositionSet) {
                map.setCenter(position.getCoordinate(), Map.Animation.BOW);
                firstPositionSet = true;
            }
            GeoCoordinate pos = position.getCoordinate();
            Log.d(TAG, "New position: " + pos.getLatitude() + " / " + pos.getLongitude() + " / " + pos.getAltitude());
            // ... do something with position ...
        }

        @Override
        public void onPositionFixChanged(PositioningManager.LocationMethod method, PositioningManager.LocationStatus status) {
            Log.i(TAG, "Position fix changed : " + status.name() + " / " + method.name());
            // only allow guidance, when we have a position fix
            if (status == PositioningManager.LocationStatus.AVAILABLE &&
                    (method == PositioningManager.LocationMethod.NETWORK || method == PositioningManager.LocationMethod.GPS)) {
                // we have a fix, so allow start of guidance now
                if (!startNavigation.isEnabled())
                    startNavigation.setEnabled(true);
                if (startNavigation.getText().equals(activity.getText(R.string.wait_gps)))
                    startNavigation.setText(R.string.navigation_start);
            }
        }
    };

    /**
     * Initialization of UI buttons on map fragment view.
     *
     * @param activity
     */
    public MapFragmentView(Activity activity) {
        this.activity = activity;
        initMapFragment();
        initUi();
    }

    private void initMapFragment() {
        /*
         * Locate the mapFragment UI element
         */
        mapFragment = (MapFragment) activity.getFragmentManager()
                .findFragmentById(R.id.mapfragment);

        if (mapFragment != null) {
            /*
             * Initialize the MapFragment, results will be given via the called back.
             */
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == Error.NONE) {
                        map = mapFragment.getMap();

                        /**
                         * Set map parameters
                         */
                        map.setProjectionMode(Map.Projection.GLOBE);  // globe projection
                        map.setExtrudedBuildingsVisible(true);  // enable 3D building footprints
                        map.setLandmarksVisible(true);  // 3D Landmarks visible
                        map.setCartoMarkersVisible(IconCategory.ALL, true);  // show embedded map markers
                        map.setSafetySpotsVisible(true); // show speed cameras as embedded markers on the map
                        map.setMapScheme(Map.Scheme.NORMAL_DAY);   // normal day mapscheme
                        map.setTrafficInfoVisible(false); // traffic options

                        /**
                         * Set positioning, position indicator and event listener
                         */
                        PositioningManager.getInstance().addListener(new WeakReference<>(mapPositionHandler));
                        mapFragment.getPositionIndicator().setVisible(true);
                        mapFragment.getPositionIndicator().setAccuracyIndicatorVisible(true);
                        // use GPS, cell and wifi
                        PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK_INDOOR);
                        GeoPosition lkp = PositioningManager.getInstance().getLastKnownPosition();
                        if (lkp != null && lkp.isValid())
                            map.setCenter(lkp.getCoordinate(), Map.Animation.NONE);

                        /**
                         * For Navigation, you need to assign the map instance to navigation manager
                         */
                        NavigationManager.getInstance().setMap(map);
                    } else {
                        /*
                         * Process errors during initialization.
                         */
                        Log.e(TAG, "Error on map fragment initialization: " + error);
                        Log.e(TAG, error.getDetails());
                        Log.e(TAG, error.getStackTrace());
                    }
                }
            });
        }
    }

    private void initUi() {
        downloadLanguage = (Button) activity.findViewById(R.id.download);
        checkLanguageUpdate = (Button) activity.findViewById(R.id.update);
        ttsEngineChoice = (Switch) activity.findViewById(R.id.tts_selector);

        /**
         *
         */
        ttsEngineChoice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                useNuance = isChecked;
                if (isChecked && !nuance.isInitialized()) {
                    Toast.makeText(activity.getApplicationContext(), R.string.nuance_not_initialized, Toast.LENGTH_LONG).show();
                }
            }
        });
        startNavigation = (Button) activity.findViewById(R.id.navigation);
        startNavigation.setEnabled(false);
        startNavigation.setText(R.string.wait_gps);

        /**
         * Initialization of Nuance TTS engine
         */
        nuance = new Nuance(activity.getApplicationContext());

        downloadLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadCatalogAndSkin();
            }
        });

        checkLanguageUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateInstalledVoices();
            }
        });

        startNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startNavigation.getText().equals(activity.getText(R.string.navigation_start))) {
                    startNavigation.setText(R.string.navigation_stop);
                } else if (startNavigation.getText().equals(activity.getText(R.string.navigation_stop))) {
                    startNavigation.setText(R.string.navigation_start);
                }
                startRouting();
            }
        });
    }

    /**
     * Check for updates for voices, download it and update
     */
    private void updateInstalledVoices() {
        /**
         * First get the voice catalog from the backend that contains all available languages (so called voiceskins) for download
         */
        VoiceCatalog.getInstance().downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(activity.getApplicationContext(), "Failed to download catalog", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity.getApplicationContext(), "Catalog downloaded", Toast.LENGTH_LONG).show();

                    boolean result = false;
                    List<VoicePackage> packages = VoiceCatalog.getInstance().getCatalogList();
                    List<VoiceSkin> local = VoiceCatalog.getInstance().getLocalVoiceSkins();

                    /**
                     * If successful, check for updated version in catalog compared to local installed ones
                     */
                    for (VoiceSkin voice : local) {
                        for (VoicePackage pkg : packages) {
                            if (voice.getId() == pkg.getId() && !voice.getVersion().equals(pkg.getVersion())) {
                                Toast.makeText(activity.getApplicationContext(), "New version detected....downloading", Toast.LENGTH_LONG).show();
                                downloadVoice(voice.getId());
                                result = true;
                            }
                        }
                    }

                    if (!result)
                        Toast.makeText(activity.getApplicationContext(), "No updates found", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Download and install voice
     */
    private void downloadCatalogAndSkin() {
        /**
         * First get the voice catalog from the backend that contains all available languages (so called voiceskins) for download
         */
        VoiceCatalog.getInstance().downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(activity.getApplicationContext(), "Failed to download catalog", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity.getApplicationContext(), "Catalog downloaded", Toast.LENGTH_LONG).show();

                    /**
                     * If catalog was successfully downloaded, you can iterate over it,
                     * show it to the user,
                     * select a skin for download
                     */
                    downloadVoice(EN_US_ID);
                }
            }
        });
    }

    /**
     * Download voice by ID
     *
     * @param skin_id ID of voice for download
     */
    private void downloadVoice(final long skin_id) {
        VoiceCatalog.getInstance().downloadVoice(skin_id, new VoiceCatalog.OnDownloadDoneListener() {
            @Override
            public void onDownloadDone(VoiceCatalog.Error error) {
                if (error != VoiceCatalog.Error.NONE) {
                    Toast.makeText(activity.getApplicationContext(), "Failed downloading voice skin", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity.getApplicationContext(), "Voice skin downloaded and activated", Toast.LENGTH_LONG).show();

                    /**
                     * Set output format for Nuance TTS
                     */
                    if (useNuance && nuance.isInitialized()) {
                        NavigationManager.getInstance().setTtsOutputFormat(NavigationManager.TtsOutputFormat.NUANCE);
                    }

                    /**
                     * Set downloaded skin
                     */
                    NavigationManager.getInstance().setVoiceSkin(VoiceCatalog.getInstance().getLocalVoiceSkin(skin_id));
                }
            }
        });
    }

    /**
     * Calculate route
     */
    private void startRouting() {
        /**
         * If currently a guidance session is running, stop it
         */
        if (NavigationManager.getInstance().getRunningState() == NavigationManager.NavigationState.RUNNING) {
            Log.d(TAG, "Stop guidance guidance...");
            NavigationManager.getInstance().stop();
            return;
        }

        Log.i(TAG, "Calculating new route...");

        /**
         * Calculate a route that we can use for guidance later
         */
        RouteOptions ro = new RouteOptions();
        ro.setTransportMode(RouteOptions.TransportMode.CAR);
        ro.setRouteType(RouteOptions.Type.SHORTEST);

        RoutePlan rp = new RoutePlan();
        rp.setRouteOptions(ro);

        GeoCoordinate lastPosition = PositioningManager.getInstance().getLastKnownPosition().getCoordinate();

        /**
         * Start route on current position
         */
        rp.addWaypoint(new RouteWaypoint(lastPosition));

        /**
         * End route somewhere close to current position
         */
        rp.addWaypoint(new RouteWaypoint(new GeoCoordinate(lastPosition.getLatitude() - 0.1, lastPosition.getLongitude() + 0.2)));  // random position

        CoreRouter rm = new CoreRouter();
        rm.calculateRoute(rp, new CoreRouter.Listener() {
            @Override
            public void onProgress(int i) {
                // you can use it to get information about route calculation progress
            }

            @Override
            public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
                if (routingError != RoutingError.NONE) {
                    Log.e(TAG, "Could not calculate route: " + routingError);
                    return;
                }

                Log.i(TAG, "Route calculated successful!");

                if (list != null && list.size() > 0) {
                    if (currentRoute != null)
                        map.removeMapObject(currentRoute); // remove old route before showing new one

                    currentRoute = new MapRoute(list.get(0).getRoute());
                    map.addMapObject(currentRoute);

                    startGuidance();
                }
            }
        });

    }

    /**
     * Start guidance with provided parameters
     */
    private void startGuidance() {
        Log.i(TAG, "Start guidance...");

        /**
         * Set settings to make guidance better
         */
        map.setMapScheme(Map.Scheme.CARNAV_DAY);
        map.setTilt(45);
        map.setZoomLevel(18);

        /**
         * Set guidance view to position with road ahead, tilt and zoom level was setup before manually
         * Choose other update modes for different position and zoom behavior
         */
        NavigationManager.getInstance().setMapUpdateMode(NavigationManager.MapUpdateMode.POSITION_ANIMATION);

        /**
         * Set instruction listener
         */
        NavigationManager.getInstance().addNewInstructionEventListener(new WeakReference<>(instructionHandler));

        /**
         * Set audio player
         */
        if (useNuance && nuance.isInitialized()) {
            // will use Nuance TTS
            NavigationManager.getInstance().getAudioPlayer().setDelegate(player);
        } else {
            // passing null delete any custom audio player that was set earlier
            NavigationManager.getInstance().getAudioPlayer().setDelegate(null);
        }

        /**
         * Start simulation with speed of 10 m/s
         */
        NavigationManager.Error e = NavigationManager.getInstance().simulate(currentRoute.getRoute(), 10);

        // start real guidance
        //NavigationManager.Error e = NavigationManager.getInstance().startNavigation(currentRoute.getRoute());

        Log.i(TAG, "Guidance start result : " + e.name());
    }

}
