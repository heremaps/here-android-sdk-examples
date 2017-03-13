# HERE SDK for Android example projects

Copyright (c) 2011-2017 HERE Europe B.V.

This repository holds a series of Java-based projects using the **HERE SDK for Android**. More information about the API can be found on [developer.here.com](https://developer.here.com/develop/mobile-sdks) under the *Android & iOS SDKs* section.

This set of self-contained, use-case based projects is designed to be cloned by developers for their own use.

> **Note:** In order to get the example code to work, you **must** replace all instances of `{YOUR_APP_ID}`, `{YOUR_APP_CODE}` and `{YOUR_LICENSE_KEY}` within the code and use your own HERE credentials.
> You can obtain a set of HERE credentials from the [Contact Us](https://developer.here.com/contact-us) page or by clicking on the *90-day Free Trial* button at [developer.here.com](https://developer.here.com).
> In order to register for a license key, the provided package name must match the one from the example you are using. E.g. for the [map-rendering-android](https://github.com/heremaps/here-android-sdk-examples/tree/master/map-rendering-android) example, the package name would be "com.here.android.example.map.basic" as specified in the [AndroidManifest.xml](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering-android/app/src/main/AndroidManifest.xml#L3) file. After obtaining the application id, application token, and license key, insert the values [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering-android/app/src/main/AndroidManifest.xml#L29), [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering-android/app/src/main/AndroidManifest.xml#L32) and [here](https://github.com/heremaps/here-android-sdk-examples/blob/master/map-rendering-android/app/src/main/AndroidManifest.xml#L35), respectively, and recompile.

## License

Unless otherwise noted in `LICENSE` files for specific files or directories, the [LICENSE](LICENSE) in the root applies to all content in this repository.

## Android Premium SDK

All of the following projects use **version 3.3** of the Android Premium SDK

* [3D Venues](3d-venues-and-indoor-routing) - Show venues in 3D mode
* [Geocoding and Reverse Gecoding](geocoder-and-reverse-geocoder-android) - Trigger a Geocode and Reverse Geocode request in HERE SDK
* [Map Attribute](map-attribute-android) - Map attributes manipulations
* [Map Customization](map-customization-android) - Customize the map scheme
* [Map Downloader](map-downloader-android) - Download offline map data
* [Map Gestures](map-gestures-android) - Define custom gesture actions
* [Map Objects](map-objects-android) - Add map objects onto HERE map
* [Map Rendering](map-rendering-android) - Display the HERE map on a device
* [Routing](rotuing-android) - Create a route from HERE Burnaby office to Langely BC and display it on the map
* [Search](search-android) - Send different types of search requests
* [Turn-by-Turn Navigation](turn-by-turn-navigation-android) - Trigger a turn-by-turn navigation from HERE Burnaby office to Langley BC
* [Urban Mobility](urban-mobility-android) - Send different types of Urban Mobility requests
