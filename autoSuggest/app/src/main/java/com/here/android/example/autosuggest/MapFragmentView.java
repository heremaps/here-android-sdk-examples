/*
 * Copyright (c) 2011-2021 HERE Europe B.V.
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

import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
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

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.search.AddressFilter;
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
    private Spinner m_countryCodeSpinner;
    private AutoSuggestAdapter m_autoSuggestAdapter;
    private List<AutoSuggest> m_autoSuggests;
    private ListView m_resultsListView;
    private TextView m_collectionSizeTextView;
    private LinearLayout m_filterOptionsContainer;
    private CheckBox m_useFilteringCheckbox;

    private static final String[] COUNTRY_CODES = {
            "",
            "ALB",
            "AND",
            "ANT",
            "ARE",
            "ARG",
            "ARM",
            "ASM",
            "ATA",
            "ATF",
            "ATG",
            "AUS",
            "AUT",
            "AZE",
            "BDI",
            "BEL",
            "BEN",
            "BFA",
            "BGD",
            "BGR",
            "BHR",
            "BHS",
            "BIH",
            "BLM",
            "BLR",
            "BLZ",
            "BMU",
            "BOL",
            "BRA",
            "BRB",
            "BRN",
            "BTN",
            "BVT",
            "BWA",
            "CAF",
            "CAN",
            "CCK",
            "CHE",
            "CHL",
            "CHN",
            "CIV",
            "CMR",
            "COD",
            "COG",
            "COK",
            "COL",
            "COM",
            "CPV",
            "CRI",
            "CUB",
            "CXR",
            "CYM",
            "CYP",
            "CZE",
            "DEU",
            "DJI",
            "DMA",
            "DNK",
            "DOM",
            "DZA",
            "ECU",
            "EGY",
            "ERI",
            "ESH",
            "ESP",
            "EST",
            "ETH",
            "FIN",
            "FJI",
            "FLK",
            "FRA",
            "FRO",
            "FSM",
            "GAB",
            "GBR",
            "GEO",
            "GGY",
            "GHA",
            "GIB",
            "GIN",
            "GLP",
            "GMB",
            "GNB",
            "GNQ",
            "GRC",
            "GRD",
            "GRL",
            "GTM",
            "GUF",
            "GUM",
            "GUY",
            "HKG",
            "HMD",
            "HND",
            "HRV",
            "HTI",
            "HUN",
            "IDN",
            "IMN",
            "IND",
            "IOT",
            "IRL",
            "IRN",
            "IRQ",
            "ISL",
            "ISR",
            "ITA",
            "JAM",
            "JEY",
            "JOR",
            "JPN",
            "KAZ",
            "KEN",
            "KGZ",
            "KHM",
            "KIR",
            "KNA",
            "KOR",
            "KWT",
            "LAO",
            "LBN",
            "LBR",
            "LBY",
            "LCA",
            "LIE",
            "LKA",
            "LSO",
            "LTU",
            "LUX",
            "LVA",
            "MAC",
            "MAF",
            "MAR",
            "MCO",
            "MDA",
            "MDG",
            "MDV",
            "MEX",
            "MHL",
            "MKD",
            "MLI",
            "MLT",
            "MMR",
            "MNE",
            "MNG",
            "MNP",
            "MOZ",
            "MRT",
            "MSR",
            "MTQ",
            "MUS",
            "MWI",
            "MYS",
            "MYT",
            "NAM",
            "NCL",
            "NER",
            "NFK",
            "NGA",
            "NIC",
            "NIU",
            "NLD",
            "NOR",
            "NPL",
            "NRU",
            "NZL",
            "OMN",
            "PAK",
            "PAN",
            "PCN",
            "PER",
            "PHL",
            "PLW",
            "PNG",
            "POL",
            "PRI",
            "PRK",
            "PRT",
            "PRY",
            "PSE",
            "PYF",
            "QAT",
            "REU",
            "ROU",
            "RUS",
            "RWA",
            "SAU",
            "SDN",
            "SEN",
            "SGP",
            "SGS",
            "SHN",
            "SJM",
            "SLB",
            "SLE",
            "SLV",
            "SMR",
            "SOM",
            "SPM",
            "SRB",
            "STP",
            "SUR",
            "SVK",
            "SVN",
            "SWE",
            "SWZ",
            "SYC",
            "SYR",
            "TCA",
            "TCD",
            "TGO",
            "THA",
            "TJK",
            "TKL",
            "TKM",
            "TLS",
            "TON",
            "TTO",
            "TUN",
            "TUR",
            "TUV",
            "TWN",
            "TZA",
            "UGA",
            "UKR",
            "UMI",
            "URY",
            "USA",
            "UZB",
            "VAT",
            "VCT",
            "VEN",
            "VGB",
            "VIR",
            "VNM",
            "VUT",
            "WLF",
            "WSM",
            "YEM",
            "ZAF",
            "ZMB",
            "ZWE",
    };

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

        // This will use external storage to save map cache data, it is also possible to set
        // private app's path
        String path = new File(m_activity.getExternalFilesDir(null), ".here-map-data")
                .getAbsolutePath();
        // This method will throw IllegalArgumentException if provided path is not writable
        com.here.android.mpa.common.MapSettings.setDiskCacheRootPath(path);

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
                        new android.app.AlertDialog.Builder(m_activity).setMessage(
                                "Error : " + error.name() + "\n\n" + error.getDetails())
                                .setTitle(R.string.engine_init_error)
                                .setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                                m_activity.finish();
                                            }
                                        }).create().show();
                    }
                }
            });
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

        m_countryCodeSpinner = m_activity.findViewById(R.id.countryCodeSpinner);
        ArrayAdapter<CharSequence> countryCodeAdapter = new ArrayAdapter<CharSequence>(
                m_activity, android.R.layout.simple_spinner_item, COUNTRY_CODES);
        countryCodeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_countryCodeSpinner.setAdapter(countryCodeAdapter);

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

    private String getSelectedCountryCode() {
        if (m_countryCodeSpinner.getSelectedItemPosition() == 0) {
            return null;
        } else {
            return COUNTRY_CODES[m_countryCodeSpinner.getSelectedItemPosition()];
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

        String countryCode = getSelectedCountryCode();
        if (!TextUtils.isEmpty(countryCode)) {
            AddressFilter addressFilter = new AddressFilter();
            // Also available filtering by state code, county, district, city and zip code
            addressFilter.setCountryCode(countryCode);
            textAutoSuggestionRequest.setAddressFilter(addressFilter);
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
