[![Build Status](https://travis-ci.org/heremaps/here-android-sdk-examples.svg?branch=master)](https://travis-ci.org/heremaps/here-android-sdk-examples)

# HERE SDK for Android example projects

Copyright (c) 2011-2017 HERE Europe B.V.

This repository holds a series of Java-based projects using the **HERE SDK for Android**. More information about the API can be found on the HERE Developer Portal's [Mobile SDKs](https://developer.here.com/develop/mobile-sdks) page.

This set of individual, use-case based projects is designed to be cloned by developers for their own use.

**Note:** In order to be able to build the examples, you have to [sign up](https://developer.here.com/develop/mobile-sdks?create=Evaluation&keepState=true&step=account) for a *90-day Free Trial*. After signing in with a *HERE account*, follow these steps to download the *Premium SDK*:

1. Choose to *Generate App ID and App Code* for use with the *Android SDK*:
![Generate App ID and App Code](/.screenshots/Premium%20SDK%20-%20Generate%20App%20ID%20and%20App%20Code.png?raw=true)

2. Enter the package name of the example you want to build, e.g. [com.here.android.example.map.basic](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L3):
![Enter package name](/.screenshots/Premium%20SDK%20-%20Enter%20package%20name.png?raw=true)

3. Click on *GENERATE* to obtain the App ID, App Code, and License Key:
![Enter package name](/.screenshots/Premium%20SDK%20-%20Generated%20license.png?raw=true)

4. CLick on *Download SDK* to get a files named like `HERE_Android_SDK_Premium_v3.3.1_205.zip` (your version number might differ).

5. Extract `HERE_Android_SDK_Premium_v3.3.1_205.zip` as well as the contained `HERE-sdk.zip`.

6. Copy the contained `HERE-sdk/libs/HERE-sdk.aar` file to your example's `libs` directory. Again taking the *map-rendering* example, the `libs` directory would be [here](https://github.com/heremaps/here-android-sdk-examples/tree/master/map-rendering/app/libs).

7. Replace the instances of the `{YOUR_APP_ID}`, `{YOUR_APP_CODE}` and `{YOUR_LICENSE_KEY}` placeholders in the example's `AndroidManifest.xml` with your obtained values. Yet again looking at the *map-rendering* examples, this would be [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L29), [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L32) and [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering/app/src/main/AndroidManifest.xml#L35).

8. Launch [Android Studio](https://developer.android.com/studio/) and import the example's `build.gradle` file.

9. Run the app.

## License

Unless otherwise noted in `LICENSE` files for specific files or directories, the [LICENSE](LICENSE) in the root applies to all content in this repository.

## Android Premium SDK

All of the following projects use **version 3.5** of the Android Premium SDK

* [3D Venues](3d-venues-and-indoor-routing) - Show venues in 3D mode
* [Advanced Navigation](advanced-navigation) - Handle user interactions and position updates during navigation
* [Geocoding and Reverse Gecoding](geocoder-and-reverse-geocoder) - Trigger a Geocode and Reverse Geocode request in HERE SDK
* [Map Attribute](map-attribute) - Map attributes manipulations
* [Map Customization](map-customization) - Customize the map scheme
* [Map Downloader](map-downloader) - Download offline map data
* [Map Gestures](map-gestures) - Define custom gesture actions
* [Map Objects](map-objects) - Add map objects onto HERE map
* [Map Rendering](map-rendering) - Display the HERE map on a device
* [Natural Language Processing](nlp) - Press volume up and talk to the app to start a place search or clear the map.
* [Routing](routing) - Create a route from HERE Burnaby office to Langely BC and display it on the map
* [Search](search) - Send different types of search requests
* [Turn-by-Turn Navigation](turn-by-turn-navigation) - Trigger a turn-by-turn navigation from HERE Burnaby office to Langley BC
* [Urban Mobility](urban-mobility) - Send different types of Urban Mobility requests
