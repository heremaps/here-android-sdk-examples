#!/bin/sh

echo "Setting credentials..."

for file in $(find . -name AndroidManifest.xml -type f); do 
	echo "Set credentials to:" $file
	sed -i -- 's|{YOUR_APP_ID}|'"$SDK_EXAMPLES_APP_ID"'|g' $file
	sed -i -- 's|{YOUR_APP_CODE}|'"$SDK_EXAMPLES_APP_TOKEN"'|g' $file
	sed -i -- 's|{YOUR_LICENSE_KEY}|'"$SDK_EXAMPLES_LICENSE"'|g' $file
done

echo "Done"
echo "-----------"