#!/bin/sh

echo "Collect apps..."

APP_PROJECTS=$(find "$PWD" -maxdepth 1 -type d -exec [ -x {}/gradlew ] \; -print -prune)

ORIG_PWD="$PWD"

mkdir $HOME/apk

for APP_PATH in $APP_PROJECTS; do
    cp $APP_PATH/app/build/outputs/apk/app-debug.apk $HOME/apk/${APP_PATH##*/}.apk
done

echo "Done"
echo "-----------"