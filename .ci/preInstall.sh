#!/bin/sh

# Download and extract the HERE SDK.
wget "$HERE_SDK_URL" -q -O 'HERE_SDK.zip'
unzip -j -d 'HERE_SDK' -o 'HERE_SDK.zip'
unzip -j 'HERE_SDK/HERE-sdk.zip' 'HERE-sdk/libs/HERE-sdk.aar' -d 'HERE_SDK'
rm 'HERE_SDK.zip'

# Find paths that contain an app module
APP_PROJECTS=$(find "$PWD" -maxdepth 1 -type d -exec [ -d {}/app/libs ] \; -print -prune)

for APP_PATH in $APP_PROJECTS; do
    cp 'HERE_SDK/HERE-sdk.aar' "$APP_PATH/app/libs"
done

rm -rf 'HERE_SDK'
