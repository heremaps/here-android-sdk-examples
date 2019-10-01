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

package com.here.android.example.autosuggest;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.search.AutoSuggest;
import com.here.android.mpa.search.AutoSuggestPlace;
import com.here.android.mpa.search.AutoSuggestQuery;
import com.here.android.mpa.search.AutoSuggestSearch;
import com.here.android.mpa.search.DiscoveryRequest;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.Place;
import com.here.android.mpa.search.PlaceRequest;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.TextAutoSuggestionRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static android.view.View.GONE;

/**
 * This class encapsulates the properties and functionality of the Map view. It also implements 3
 * types of AutoSuggest requests that HERE Mobile SDK for Android (Premium) provides as example.
 */
public class MapFragmentView {
    public static List<DiscoveryResult> s_discoverResultList;
    private AndroidXMapFragment m_mapFragment;
    private AppCompatActivity m_activity;
    private View m_mapFragmentContainer;
    private Map m_map;
    private Button m_placeDetailButton;
    private SearchView m_searchView;
    private SearchListener m_searchListener;
    private List<MapObject> m_mapObjectList = new ArrayList<>();
    private Spinner m_localeSpinner;
    private AutoSuggestAdapter m_autoSuggestAdapter;
    private List<AutoSuggest> m_autoSuggests;
    private ListView m_resultsListView;
    private TextView m_collectionSizeTextView;
    private LinearLayout m_filterOptionsContainer;
    private CheckBox m_useFilteringCheckbox;

    private static final String[] AVAILABLE_LOCALES = {
            "",
            "af-ZA",
            "sq-AL",
            "ar-SA",
            "az-Latn-AZ",
            "eu-ES",
            "be-BY",
            "bg-BG",
            "ca-ES",
            "zh-CN",
            "zh-TW",
            "hr-HR",
            "cs-CZ",
            "da-DK",
            "nl-NL",
            "en-GB",
            "en-US",
            "et-EE",
            "fa-IR",
            "fil-PH",
            "fi-FI",
            "fr-FR",
            "fr-CA",
            "gl-ES",
            "de-DE",
            "el-GR",
            "ha-Latn-NG",
            "he-IL",
            "hi-IN",
            "hu-HU",
            "id-ID",
            "it-IT",
            "ja-JP",
            "kk-KZ",
            "ko-KR",
            "lv-LV",
            "lt-LT",
            "mk-MK",
            "ms-MY",
            "nb-NO",
            "pl-PL",
            "pt-BR",
            "pt-PT",
            "ro-RO",
            "ru-RU",
            "sr-Latn-CS",
            "sk-SK",
            "sl-SI",
            "es-MX",
            "es-ES",
            "sv-SE",
            "th-TH",
            "tr-TR",
            "uk-UA",
            "uz-Latn-UZ",
            "vi-VN"
    };

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        m_searchListener = new SearchListener();
        m_autoSuggests = new ArrayList<>();
        /*
         * The map fragment is not required for executing AutoSuggest requests. However in this example,
         * we will use it to simplify of the SDK initialization.
         */
        initMapFragment();
        initControls();
    }

    private AndroidXMapFragment getMapFragment() {
        return (AndroidXMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = getMapFragment();
        m_mapFragmentContainer = m_activity.findViewById(R.id.mapfragment);

        // Set path of disk cache
        String diskCacheRoot = m_activity.getFilesDir().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = m_activity.getPackageManager().getApplicationInfo(m_activity
                    .getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(), "Failed to find intent name, NameNotFound: "
                    + e.getMessage());
        }

        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot, intentName);
        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            if (m_mapFragment != null) {
            /* Initialize the AndroidXMapFragment, results will be given via the called back. */
                m_mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                        if (error == Error.NONE) {
                            m_map = m_mapFragment.getMap();
                            m_map.setCenter(new GeoCoordinate(49.259149, -123.008555),
                                    Map.Animation.NONE);
                            m_map.setZoomLevel(13.2);
                        } else {
                            Toast.makeText(m_activity,
                                    "ERROR: Cannot initialize Map with error " + error,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }

    private void initControls() {
        m_searchView = m_activity.findViewById(R.id.search);
        m_searchView.setOnQueryTextListener(m_searchListener);
        m_localeSpinner = m_activity.findViewById(R.id.localeSpinner);
        m_collectionSizeTextView = m_activity.findViewById(R.id.editText_collectionSize);
        ArrayAdapter<CharSequence> localeAdapter = new ArrayAdapter<CharSequence>(
                m_activity, android.R.layout.simple_spinner_item, AVAILABLE_LOCALES);
        localeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        m_localeSpinner.setAdapter(localeAdapter);

        m_resultsListView = m_activity.findViewById(R.id.resultsListViev);
        m_autoSuggestAdapter = new AutoSuggestAdapter(m_activity,
                android.R.layout.simple_list_item_1, m_autoSuggests);
        m_resultsListView.setAdapter(m_autoSuggestAdapter);
        m_resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AutoSuggest item = (AutoSuggest) parent.getItemAtPosition(position);
                handleSelectedAutoSuggest(item);
            }
        });
        // Initialize filter options view
        LinearLayout linearLayout = m_activity.findViewById(R.id.filterOptionsContainer);
        m_filterOptionsContainer = new LinearLayout(m_activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        m_useFilteringCheckbox = new CheckBox(m_activity);
        m_useFilteringCheckbox.setText("Use filter");
        m_useFilteringCheckbox.setChecked(false);
        m_useFilteringCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        m_filterOptionsContainer.setVisibility(isChecked?View.VISIBLE:GONE);
                    }
                });

        m_filterOptionsContainer.setVisibility(GONE);
        m_filterOptionsContainer.setOrientation(LinearLayout.VERTICAL);
        m_filterOptionsContainer.setPadding(50, 0, 0, 0);

        TextAutoSuggestionRequest.AutoSuggestFilterType[] filterOptions =
                TextAutoSuggestionRequest.AutoSuggestFilterType.values();
        for (TextAutoSuggestionRequest.AutoSuggestFilterType filterOption : filterOptions) {
            CheckBox curCB = new CheckBox(m_activity);
            curCB.setChecked(false);
            curCB.setText(filterOption.toString());
            m_filterOptionsContainer.addView(curCB);
        }

        linearLayout.addView(m_useFilteringCheckbox);
        linearLayout.addView(m_filterOptionsContainer);
    }

    /**
     * Applies selected  filter to {@link TextAutoSuggestionRequest}
     * in order to filtrate according to selected settings.
     * @param request
     */
    private void applyResultFiltersToRequest(TextAutoSuggestionRequest request) {
        if (m_useFilteringCheckbox.isChecked()) {
            TextAutoSuggestionRequest.AutoSuggestFilterType[] filterOptions =
                    TextAutoSuggestionRequest.AutoSuggestFilterType.values();
            int totalFilterOptionsCount = m_filterOptionsContainer.getChildCount();
            List<TextAutoSuggestionRequest.AutoSuggestFilterType> filtersToApply =
                    new ArrayList<>(filterOptions.length);
            for (int i = 0; i < totalFilterOptionsCount; i++) {
                if (((CheckBox) m_filterOptionsContainer.getChildAt(i)).isChecked()) {
                    filtersToApply.add(filterOptions[i]);
                }
            }
            if (!filtersToApply.isEmpty()) {
                request.setFilters(EnumSet.copyOf(filtersToApply));
            }
        }
    }

    private Locale getSelectedLocale() {
        if (m_localeSpinner.getSelectedItemPosition() == 0) {
            return null;
        } else {
            return new Locale(AVAILABLE_LOCALES[m_localeSpinner.getSelectedItemPosition()]);
        }
    }

    private class SearchListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (!newText.isEmpty()) {
                doSearch(newText);
            } else {
                setSearchMode(false);
            }
            return false;
        }
    }

    private void doSearch(String query) {
        setSearchMode(true);
        /*
         Creates new TextAutoSuggestionRequest with current map position as search center
         and selected collection size with applied filters and chosen locale.
         For more details how to use TextAutoSuggestionRequest
         please see documentation for HERE Mobile SDK for Android.
         */
        TextAutoSuggestionRequest textAutoSuggestionRequest = new TextAutoSuggestionRequest(query);
        textAutoSuggestionRequest.setSearchCenter(m_map.getCenter());
        textAutoSuggestionRequest.setCollectionSize(Integer.parseInt(m_collectionSizeTextView.getText().toString()));
        applyResultFiltersToRequest(textAutoSuggestionRequest);
        Locale locale = getSelectedLocale();
        if (locale != null) {
            textAutoSuggestionRequest.setLocale(locale);
        }
        /*
           The textAutoSuggestionRequest returns its results to non-UI thread.
           So, we have to pass the UI update with returned results to UI thread.
         */
        textAutoSuggestionRequest.execute(new ResultListener<List<AutoSuggest>>() {
            @Override
            public void onCompleted(final List<AutoSuggest> autoSuggests, ErrorCode errorCode) {
                if (errorCode == errorCode.NONE) {
                    processSearchResults(autoSuggests);
                } else {
                    handleError(errorCode);
                }
            }
        });
    }

    private void processSearchResults(final List<AutoSuggest> autoSuggests) {
        m_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_autoSuggests.clear();
                m_autoSuggests.addAll(autoSuggests);
                m_autoSuggestAdapter.notifyDataSetChanged();
            }
        });
    }

    private void handleSelectedAutoSuggest(AutoSuggest autoSuggest) {
        int collectionSize = Integer.parseInt(m_collectionSizeTextView.getText().toString());
        switch (autoSuggest.getType()) {
            case PLACE:
                /*
                 Gets initialized PlaceRequest with location context that allows retrieving details
                 about the place on the selected location.
                 */
                AutoSuggestPlace autoSuggestPlace = (AutoSuggestPlace) autoSuggest;
                PlaceRequest detailsRequest = autoSuggestPlace.getPlaceDetailsRequest();
                detailsRequest.execute(new ResultListener<Place>() {
                    @Override
                    public void onCompleted(Place place, ErrorCode errorCode) {
                        if (errorCode == ErrorCode.NONE) {
                            handlePlace(place);
                        } else {
                            handleError(errorCode);
                        }
                    }
                });
                break;
            case SEARCH:
                /*
                 Gets initialized AutoSuggestSearch with location context that allows retrieving
                 DiscoveryRequest for further processing as it is described in
                 the official documentation.
                 Some example of how to handle DiscoveryResultPage you can see in
                 com.here.android.example.autosuggest.ResultListActivity
                 */
                AutoSuggestSearch autoSuggestSearch = (AutoSuggestSearch) autoSuggest;
                DiscoveryRequest discoverRequest = autoSuggestSearch.getSuggestedSearchRequest();
                discoverRequest.setCollectionSize(collectionSize);
                discoverRequest.execute(new ResultListener<DiscoveryResultPage>() {
                    @Override
                    public void onCompleted(DiscoveryResultPage discoveryResultPage,
                                            ErrorCode errorCode) {
                        if (errorCode == ErrorCode.NONE) {
                            s_discoverResultList = discoveryResultPage.getItems();
                            Intent intent = new Intent(m_activity, ResultListActivity.class);
                            m_activity.startActivity(intent);
                        } else {
                            handleError(errorCode);
                        }
                    }
                });
                break;
            case QUERY:
                /*
                 Gets TextAutoSuggestionRequest with suggested autocomplete that retrieves
                 the collection of AutoSuggest objects which represent suggested.
                 */
                final AutoSuggestQuery autoSuggestQuery = (AutoSuggestQuery) autoSuggest;
                TextAutoSuggestionRequest queryReqest = autoSuggestQuery
                        .getRequest(getSelectedLocale());
                queryReqest.setCollectionSize(collectionSize);
                queryReqest.execute(new ResultListener<List<AutoSuggest>>() {
                    @Override
                    public void onCompleted(List<AutoSuggest> autoSuggests, ErrorCode errorCode) {
                        if (errorCode == ErrorCode.NONE) {
                            processSearchResults(autoSuggests);
                            m_searchView.setOnQueryTextListener(null);
                            m_searchView.setQuery(autoSuggestQuery.getQueryCompletion(),
                                    false);
                            m_searchView.setOnQueryTextListener(m_searchListener);
                        } else {
                            handleError(errorCode);
                        }
                    }
                });
                break;
            //Do nothing.
            case UNKNOWN:
                default:
        }
    }

    public void setSearchMode(boolean isSearch) {
        if (isSearch) {
            m_mapFragmentContainer.setVisibility(View.INVISIBLE);
            m_resultsListView.setVisibility(View.VISIBLE);
        } else {
            m_mapFragmentContainer.setVisibility(View.VISIBLE);
            m_resultsListView.setVisibility(View.INVISIBLE);
        }
    }

    private void handlePlace(Place place) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(place.getName() + "\n");
        sb.append("Alternative name:").append(place.getAlternativeNames());
        showMessage("Place info", sb.toString(), false);
    }

    private void handleError(ErrorCode errorCode) {
        showMessage("Error", "Error description: " + errorCode.name(), true);
    }

    private void showMessage(String title, String message, boolean isError) {
        AlertDialog.Builder builder = new AlertDialog.Builder(m_activity);
        builder.setTitle(title).setMessage(message);
        if (isError) {
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        } else {
            builder.setIcon(android.R.drawable.ic_dialog_info);
        }
        builder.setNeutralButton("OK", null);
        builder.create().show();
    }
}
