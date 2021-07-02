#!/bin/sh

PREVIEW_MAX_SIZE=1080
THUMBNAIL_SQUARE_SIZE=200

IDS_DIR=".ids"
PREVIEW_SUFFIX=".preview.jpg"

createThumbnail() {
  local THUMBNAIL_FILE_PATH="$IDS_DIR/$IMAGE_BASE_NAME.thumbnail.jpg"
  if [ ! -e "$THUMBNAIL_FILE_PATH" ]; then
    local SOURCE_FILE="$IDS_DIR/$IMAGE_BASE_NAME$PREVIEW_SUFFIX"
    local IMAGE_SIZE=$(convert "$SOURCE_FILE" -print "%w %h" /dev/null)
    local IMAGE_WIDTH=$(echo "$IMAGE_SIZE" | cut -d ' ' -f 1)
    local IMAGE_HEIGHT=$(echo "$IMAGE_SIZE" | cut -d ' ' -f 2)
    if [ $IMAGE_HEIGHT -gt $IMAGE_WIDTH ]; then
      START_CROP=$(echo "($IMAGE_HEIGHT-$IMAGE_WIDTH)/2" | bc)
      CROP="${IMAGE_WIDTH}x${IMAGE_WIDTH}+0+$START_CROP"
    else
      START_CROP=$(echo "($IMAGE_WIDTH-$IMAGE_HEIGHT)/2" | bc)
      CROP="${IMAGE_HEIGHT}x${IMAGE_HEIGHT}+$START_CROP+0"
    fi
    echo "Start thumbnail generation"
    convert "$SOURCE_FILE" -crop "$CROP" "$THUMBNAIL_FILE_PATH"
    convert "$THUMBNAIL_FILE_PATH" -resize ${THUMBNAIL_SQUARE_SIZE}x${THUMBNAIL_SQUARE_SIZE} "$THUMBNAIL_FILE_PATH"
    echo "Done"
  else
    echo "Thumbnail already exists"
  fi
}

createPreview() {
  local SOURCE_FILE="$IDS_DIR/$IMAGE_BASE_NAME$PREVIEW_SUFFIX"
  if [ ! -e "$SOURCE_FILE" ]; then
    local IMAGE_SIZE=$(convert "$file" -print "%w %h" /dev/null)
    local IMAGE_WIDTH=$(echo "$IMAGE_SIZE" | cut -d ' ' -f 1)
    local IMAGE_HEIGHT=$(echo "$IMAGE_SIZE" | cut -d ' ' -f 2)
    local IMAGE_RATIO=$(awk "BEGIN { print "$IMAGE_WIDTH/$IMAGE_HEIGHT" }")
    local PREVIEW_IMAGE_WIDTH=$(echo "$PREVIEW_MAX_SIZE*$IMAGE_RATIO/1" | bc)
    echo "Start preview generation"
    convert "$file" -resize ${PREVIEW_IMAGE_WIDTH} "$SOURCE_FILE"
    echo "Done"
  else
    echo "Preview already exists"
  fi
}

generateAlternateFormats() {
  local DIRECTORY_PATH="$1"
  local PWD_BACKUP=$(pwd)
  cd "$DIRECTORY_PATH"
  for file in *; do
    if [ -d "$file" ]; then
      generateAlternateFormats "$file"
    elif [ -f "$file" ]; then
      IMAGE_BASE_NAME=$(echo ${file%%.*})
      IMAGE_EXTENSION_LOWER_CASE=$(echo ${file##*.} | tr '[:upper:]' '[:lower:]')
      if [ "$IMAGE_EXTENSION_LOWER_CASE" = "jpg" ] || [ "$IMAGE_EXTENSION_LOWER_CASE" = "jpeg" ]; then
        echo "Manage: $(pwd)/$file"
        mkdir -p "$IDS_DIR"
        createAlternateFormats
      fi
    fi
  done
  cd "$PWD_BACKUP"
}

createAlternateFormats() {
  createPreview
  createThumbnail
}

set -e
ABSOLUTE_DIRECTORY_PATH=$1
generateAlternateFormats "$ABSOLUTE_DIRECTORY_PATH"
