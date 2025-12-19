#!/bin/bash

./gradlew clean
./gradlew :library:publishToMavenLocal

# Get publication details from Gradle task
eval $(./gradlew :library:printPublicationInfo -q | grep -E "^(GROUP_ID|ARTIFACT_ID|VERSION)=")
FOLDER_PATH="$GROUP_ID.$ARTIFACT_ID"

FOLDER_STRUCTURE=$(echo "$FOLDER_PATH" | tr '.' '/')
SOURCE_DIR="$HOME/.m2/repository/$FOLDER_STRUCTURE/$VERSION"
TEMP_DIR="downloads/temp"
TARGET_DIR="$TEMP_DIR/$FOLDER_STRUCTURE/$VERSION"
mkdir -p "$TARGET_DIR"
mv "$SOURCE_DIR"/* "$TARGET_DIR/"

cd "$TEMP_DIR"
zip -r "../${FOLDER_PATH//./-}-${VERSION}.zip" "$FOLDER_STRUCTURE"
cd ..
rm -rf temp
echo "Moved to /downloads director in this project:" $(pwd)