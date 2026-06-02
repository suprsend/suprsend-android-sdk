#!/bin/bash
# Change the version in Deps.kt
# Change the BUILD_TYPE in Deps.kt
# Run: sh publish-bundle.sh <module>
#   module: library | jwtLibrary
# Ex - sh publish-bundle.sh library
# Ex - sh publish-bundle.sh jwtLibrary
# This will generate zip update it on https://central.sonatype.com/publishing
# Login with ssniks
# Click publish button
# Publish Component
# In deployment name enter the zip file name without extension
# Upload zip
# Submit

#For testing lets leverage 0.0.X
#For legacy lets use 1.X.X
#For JWT lets use 2.X.X

MODULE="${1:-}"

usage() {
  echo "Usage: $0 <module>"
  echo ""
  echo "  module must be one of:"
  echo "    library     - publish :library bundle"
  echo "    jwtLibrary  - publish :jwtLibrary bundle"
  echo ""
  echo "Example:"
  echo "  sh $0 library"
  echo "  sh $0 jwtLibrary"
}

case "$MODULE" in
  library|jwtLibrary)
    ;;
  "")
    echo "Error: module name is required."
    echo ""
    usage
    exit 1
    ;;
  *)
    echo "Error: invalid module name '$MODULE'."
    echo ""
    usage
    exit 1
    ;;
esac

echo "Publishing module: :$MODULE"

./gradlew clean
./gradlew ":${MODULE}:publishToMavenLocal"

# Get publication details from Gradle task
eval $(./gradlew ":${MODULE}:printPublicationInfo" -q | grep -E "^(GROUP_ID|ARTIFACT_ID|VERSION)=")
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
