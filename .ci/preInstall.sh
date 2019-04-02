#!/bin/bash -e

# Download and extract the HERE SDK.
curl -s -S -o HERE_SDK.zip "$HERE_SDK_URL"

# Extract the downloaded outer ZIP file.
unzip -j -o 'HERE_SDK.zip' -d 'HERE_SDK'

# Extract the contained inner ZIP.
unzip -j -o 'HERE_SDK/HERE-sdk.zip' 'libs/HERE-sdk.aar' -d 'HERE_SDK'

## Find paths that contain an app module
APP_PROJECTS=$(find "$PWD" -maxdepth 1 -type d -exec [ -d {}/app/libs ] \; -print -prune)

for APP_PATH in $APP_PROJECTS; do
    cp 'HERE_SDK/HERE-sdk.aar' "$APP_PATH/app/libs"
done

rm -rf 'HERE_SDK'
rm -f 'HERE_SDK.zip'
