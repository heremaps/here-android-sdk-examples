#!/bin/sh

# Find paths that contain the Gradle wrapper executable.
APP_PROJECTS=$(find "$PWD" -maxdepth 1 -type d -exec [ -x {}/gradlew ] \; -print -prune)

ORIG_PWD="$PWD"

for APP_PATH in $APP_PROJECTS; do
    cd $APP_PATH
    ./gradlew build
    ERROR=$?
    if [ $ERROR -ne 0 ]; then
        break
    fi
done

cd "$ORIG_PWD"

exit $ERROR
