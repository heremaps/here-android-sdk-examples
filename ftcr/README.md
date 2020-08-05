# FTCR Routing Sample App

## Prerequisites

Update manifest file with proper license key as described above in root [README file](../README.md).

## Purpose

Gives a basic idea of how to calculate a route that is using preloaded custom roads into the fleet telematics server, using HERE Mobile SDK, more details are given in [Direction SDK documentation](https://developer.here.com/documentation/android-premium/dev_guide/topics/fleet-telematics-custom-route.html).

## Example

Code snippet which builds route between predefined coordinates.

## Note
After uploading the route on the fleet telematics server, sometimes, the route does not take into account immediately. So you have to wait a certain time and try again to calculate the route on the custom roads.

## References
 - [Fleet telematics](https://developer.here.com/documentation/fleet-telematics/api-reference.html)
 - [Direction SDK documentation](https://developer.here.com/documentation/android-premium/dev_guide/topics/routing.html)
 - [Route with TTA](../route-tta/README.md) - example of TTA calculation for route