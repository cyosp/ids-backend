#!/bin/sh

PREVIEW_MAX_SIZE=1080
THUMBNAIL_SQUARE_SIZE=200

IDS_DIR=".ids"
PREVIEW_SUFFIX=".preview.jpg"
PREVIEW_VIDEO_SUFFIX=".preview.mp4"
THUMBNAIL_SUFFIX=".thumbnail.jpg"

createThumbnailImage() {
  local THUMBNAIL_FILE_PATH="$IDS_DIR/$MEDIA_BASE_NAME$THUMBNAIL_SUFFIX"
  if [ ! -e "$THUMBNAIL_FILE_PATH" ]; then
    local SOURCE_FILE="$1"
    local IMAGE_SIZE=$(magick identify -format "%w %h" "$SOURCE_FILE")
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
    magick "$SOURCE_FILE" -crop "$CROP" "$THUMBNAIL_FILE_PATH"
    magick "$THUMBNAIL_FILE_PATH" -resize ${THUMBNAIL_SQUARE_SIZE}x${THUMBNAIL_SQUARE_SIZE} "$THUMBNAIL_FILE_PATH"
    echo "Done"
  else
    echo "Thumbnail already exists"
  fi
}

createThumbnailVideo() {
  local THUMBNAIL_FILE_PATH="$IDS_DIR/$MEDIA_BASE_NAME$THUMBNAIL_SUFFIX"
  if [ ! -e "$THUMBNAIL_FILE_PATH" ]; then
    local PREVIEW_FILE="$IDS_DIR/$MEDIA_BASE_NAME$PREVIEW_VIDEO_SUFFIX"
    VIDEO_DURATION_IN_SECOND=$(ffprobe -i "$PREVIEW_FILE" -show_entries format=duration -v quiet -of csv="p=0")
    # Move from float to integer
    VIDEO_DURATION_IN_SECOND=$(echo "$VIDEO_DURATION_IN_SECOND/1" | bc)

    IMAGE_EXTRACT_TIME_IN_SECOND=3
    if [ $VIDEO_DURATION_IN_SECOND -lt $IMAGE_EXTRACT_TIME_IN_SECOND ]; then
        IMAGE_EXTRACT_TIME_IN_SECOND=$(echo "$VIDEO_DURATION_IN_SECOND/2" | bc)
    fi

    echo "Start to extract image from video"
    local IMAGE_FILE_PATH="$IDS_DIR/$MEDIA_BASE_NAME.image.jpg"
    ffmpeg -ss $IMAGE_EXTRACT_TIME_IN_SECOND -hide_banner -loglevel error -i "$PREVIEW_FILE" -frames:v 1 "$IMAGE_FILE_PATH"
    echo "Done"

    createThumbnailImage "$IMAGE_FILE_PATH"
    if [ -e "$IMAGE_FILE_PATH" ]; then rm "$IMAGE_FILE_PATH"; fi
  else
    echo "Thumbnail already exists"
  fi
}

createPreviewImage() {
  local PREVIEW_FILE="$IDS_DIR/$MEDIA_BASE_NAME$PREVIEW_SUFFIX"
    if [ ! -e "$PREVIEW_FILE" ]; then
      local IMAGE_SIZE=$(magick identify -format "%w %h" "$file")
      local IMAGE_WIDTH=$(echo "$IMAGE_SIZE" | cut -d ' ' -f 1)
      local IMAGE_HEIGHT=$(echo "$IMAGE_SIZE" | cut -d ' ' -f 2)
      local IMAGE_RATIO=$(awk "BEGIN { print "$IMAGE_WIDTH/$IMAGE_HEIGHT" }")
      local PREVIEW_IMAGE_WIDTH=$(echo "$PREVIEW_MAX_SIZE*$IMAGE_RATIO/1" | bc)
      echo "Start preview generation"
      magick "$file" -resize ${PREVIEW_IMAGE_WIDTH} "$PREVIEW_FILE"
      echo "Done"
    else
      echo "Preview already exists"
    fi
}

createPreviewVideo() {
  local PREVIEW_FILE="$IDS_DIR/$MEDIA_BASE_NAME$PREVIEW_VIDEO_SUFFIX"
  if [ ! -e "$PREVIEW_FILE" ]; then
    echo "Start preview generation"
    ffmpeg -hide_banner -loglevel error -i "$file" -acodec mp3 -ab 128k -vcodec h264 -b:v 1200k "$PREVIEW_FILE"
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
      MEDIA_BASE_NAME=$(echo ${file%%.*})
      IMAGE_EXTENSION_LOWER_CASE=$(echo ${file##*.} | tr '[:upper:]' '[:lower:]')
      if [ "$IMAGE_EXTENSION_LOWER_CASE" = "jpg" ] || [ "$IMAGE_EXTENSION_LOWER_CASE" = "jpeg" ]; then
        createAlternateImageFormats
      elif [ "$IMAGE_EXTENSION_LOWER_CASE" = "mov" ]; then
        createAlternateVideoFormats
      fi
    fi
  done
  cd "$PWD_BACKUP"
}

createIdsDir() {
  mkdir -p "$IDS_DIR"
}

createAlternateImageFormats() {
  echo "Manage image: $(pwd)/$file"
  createIdsDir
  createPreviewImage
  createThumbnailImage "$IDS_DIR/$MEDIA_BASE_NAME$PREVIEW_SUFFIX"
}

createAlternateVideoFormats() {
  echo "Manage video: $(pwd)/$file"
  createIdsDir
  createPreviewVideo
  createThumbnailVideo
}

set -e
ABSOLUTE_DIRECTORY_PATH=$1
generateAlternateFormats "$ABSOLUTE_DIRECTORY_PATH"
