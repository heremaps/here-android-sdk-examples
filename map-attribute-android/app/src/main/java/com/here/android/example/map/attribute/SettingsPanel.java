/*
 * Copyright © 2011-2017 HERE Europe B.V.
 * All rights reserved.
 * The use of this software is conditional upon having a separate agreement
 * with a HERE company for the use or utilization of this software. In the
 * absence of such agreement, the use of the software is not allowed.
 */

package com.here.android.example.map.attribute;

import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapTrafficLayer;
import com.here.android.mpa.mapping.MapTransitLayer;

import android.app.Activity;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;

/**
 * This class encapsulates the properties and functionality of the settings panel,which provides the
 * UI elements to control the map attributes.
 */
public class SettingsPanel {
    // Initialize UI elements
    private RadioGroup m_mapModeGroup;
    private RadioGroup m_mapTransitGroup;

    private Switch m_flowSwitch;
    private Switch m_incidentSwitch;
    private Switch m_coverageSwitch;

    private Activity m_activity;
    private Map m_map;

    public SettingsPanel(Activity activity, Map map) {
        m_activity = activity;
        m_map = map;
        initUIElements();
    }

    private void initUIElements() {
        m_mapModeGroup = (RadioGroup) m_activity.findViewById(R.id.mapModeRadioGroup);
        m_mapTransitGroup = (RadioGroup) m_activity.findViewById(R.id.transitGroup);
        m_flowSwitch = (Switch) m_activity.findViewById(R.id.flowSwitch);
        m_incidentSwitch = (Switch) m_activity.findViewById(R.id.incidentSwitch);
        m_coverageSwitch = (Switch) m_activity.findViewById(R.id.coverageSwitch);

        setUIListeners();
    }

    /**
     * Change map scheme as selected option.
     */
    private void setUIListeners() {
        m_mapModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    /*
                     * Please refer to javadoc or call Map.getMapSchemes() for all supported map
                     * schemes
                     */
                    case R.id.mapModeBtn:
                        m_map.setMapScheme(Map.Scheme.NORMAL_DAY);
                        break;
                    case R.id.hybridModeBtn:
                        m_map.setMapScheme(Map.Scheme.HYBRID_DAY);
                        break;
                    case R.id.terrainModeBtn:
                        m_map.setMapScheme(Map.Scheme.TERRAIN_DAY);
                        break;
                    default:
                }
            }
        });

        /**
         * Change map transit layer as selected option
         */
        m_mapTransitGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.nothingTransitbBtn:
                        m_map.getMapTransitLayer().setMode(MapTransitLayer.Mode.NOTHING);
                        break;
                    case R.id.stopTransitBtn:
                        m_map.getMapTransitLayer().setMode(MapTransitLayer.Mode.STOPS_AND_ACCESSES);
                        break;
                    case R.id.everythingTransitBtn:
                        m_map.getMapTransitLayer().setMode(MapTransitLayer.Mode.EVERYTHING);
                        break;
                    default:
                }
            }
        });

        /**
         * Enable or disable FLOW map traffic layer.
         */
        m_flowSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                /* TrafficInfo has to be turned on first */
                m_map.setTrafficInfoVisible(isChecked);
                m_map.getMapTrafficLayer().setEnabled(MapTrafficLayer.RenderLayer.FLOW, isChecked);
            }
        });
        /**
         * Enable or disable INCIDENT map traffic layer.
         */
        m_incidentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                /* TrafficInfo has to be turned on first */
                m_map.setTrafficInfoVisible(isChecked);
                m_map.getMapTrafficLayer().setEnabled(MapTrafficLayer.RenderLayer.INCIDENT,
                        isChecked);
            }
        });

        /**
         * Turn on or off street level coverage.
         */
        m_coverageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                m_map.setStreetLevelCoverageVisible(isChecked);
            }
        });
    }
}
