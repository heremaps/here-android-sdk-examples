[![Build Status](https://travis-ci.org/heremaps/here-android-sdk-examples.svg?branch=master)](https://travis-ci.org/heremaps/here-android-sdk-examples)

# HERE Mobile SDK 3.x for Android example projects
# Deprecated

Copyright (c) 2011-2021 HERE Europe B.V.

This repository holds a series of Java-based projects using the **HERE Mobile SDK 3.x for Android**. More information about the API can be found on the HERE Developer Portal's [Mobile SDKs](https://developer.here.com/develop/mobile-sdks) page.

Note:
This service is no longer being actively developed. We will only provide critical fixes for this service in future. Instead, use the new [HERE SDK 4.x](https://developer.here.com/documentation/android-sdk-navigate/4.10.5.0/dev_guide/index.html)
HERE Premium SDK (3.x) is superseded by new 4.x SDK variants and the Premium SDK will be maintained until 31 December 2022 with only critical bug fixes and no feature development / enhancements.
Current users of the HERE Premium SDK (3.x) are encouraged to migrate to Lite, Explore or Navigate HERE SDK (4.x) variants based on licensed use cases before 31 December 2022. Most of the Premium SDK features are already available in the new SDK variants.
Onboarding of new customers for Premium SDK is not possible.


This set of individual, use-case based projects is designed to be cloned by developers for their own use.

**Note:** In order to be able to build the examples, you have to [sign up](https://developer.here.com/develop/mobile-sdks?create=Evaluation&keepState=true&step=account) for a *90-day Free Trial*. After signing in with a *HERE account*, follow these steps to download the *HERE Mobile SDK (Premium)*:

1. Choose to *Generate App ID and App Code* for use with the *HERE Mobile SDK for Android*:
![Generate App ID and App Code](/.screenshots/Premium%20SDK%20-%20Generate%20App%20ID%20and%20App%20Code.png?raw=true)

2. Enter the package name of the example you want to build, e.g. [com.here.android.example.map.basic](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L3). **The package name entered here must match the name in your app**:
![Enter package name](/.screenshots/Premium%20SDK%20-%20Enter%20package%20name.png?raw=true)

3. Click on *GENERATE* to obtain the App ID, App Code, and License Key:
![Enter package name](/.screenshots/Premium%20SDK%20-%20Generated%20license.png?raw=true)

4. Click on *Download SDK* to get a files named like `HERE_Android_SDK_Premium_v3.16.2_101.zip` (your version number might differ).

5. Extract `HERE_Android_SDK_Premium_v3.16.2_101.zip` as well as the contained `HERE-sdk.zip`.

6. Copy the contained `HERE-sdk/libs/HERE-sdk.aar` file to your example's `libs` directory. Again taking the *map-rendering* example, the `libs` directory would be [here](https://github.com/heremaps/here-android-sdk-examples/tree/master/map-rendering/app/libs).

7. Replace the instances of the `{YOUR_APP_ID}`, `{YOUR_APP_CODE}` and `{YOUR_LICENSE_KEY}` placeholders in the example's `AndroidManifest.xml` with your obtained values. Yet again looking at the *map-rendering* examples, this would be [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L29), [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L32) and [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L35).

8. Replace the instances of the `{YOUR_LABEL_NAME}` placeholders in the example's `AndroidManifest.xml` with your own custom values. Do not reuse HERE Mobile SDK defaults.

9. Launch [Android Studio](https://developer.android.com/studio/) and import the example's `build.gradle` file.

10. Run the app.

## License

Unless otherwise noted in `LICENSE` files for specific files or directories, the [LICENSE](LICENSE) in the root applies to all content in this repository.

## HERE Mobile SDK for Android (Premium)

All of the following projects use latest version(currently 3.16) of the HERE Mobile SDK for Android (Premium)

* [3D Model](3DModel) - Show 3D model on the map
* [3D Venues](3d-venues-and-indoor-routing) - Show venues in 3D mode
* [Advanced Navigation](advanced-navigation) - Handle user interactions and position updates during navigation
* [CLE2](cle2) - Use custom location extensions
* [Geocoding and Reverse Gecoding](geocoder-and-reverse-geocoder) - Trigger a Geocode and Reverse Geocode request in HERE Mobile SDK
* [Map Attribute](map-attribute) - Map attributes manipulations
* [Map Customization](map-customization) - Customize the map scheme
* [Map Downloader](map-downloader) - Download offline map data
* [Map Gestures](map-gestures) - Define custom gesture actions
* [Map Objects](map-objects) - Add map objects onto HERE map
* [Map Rendering](map-rendering) - Display the HERE map on a device
* [Routing](routing) - Create a route from HERE Burnaby office to Langely BC and display it on the map
* [Routing TTA](route-tta) - Create a route and retrieves Time To Arrival
* [Venue Positioning](positioning-venues-and-logging) - Use HERE location data source in venue
* [Positioning](positioning) - Use HERE location data source
* [Search](search) - Send different types of search requests
* [AutoSuggest](autoSuggest) - Send different types of AutoSuggest requests
* [Turn-by-Turn Navigation](turn-by-turn-navigation) - Trigger a turn-by-turn navigation from HERE Burnaby office to Langley BC
