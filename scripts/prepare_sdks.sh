#!/bin/bash
set -e

echo "=== Preparing SDKs ==="
mkdir -p app/libs
mkdir -p app/src/main/jniLibs

# Baidu SDK
if [ -f "BaiduLBS_Android.jar" ]; then
  echo "Found BaiduLBS_Android.jar, copying to app/libs/"
  cp BaiduLBS_Android.jar app/libs/
fi

# AMap SDK
echo "Downloading AMap SDK..."
wget -qO amap_sdk.zip https://a.amap.com/lbs/static/zip/AMap_Android_SDK_All.zip
unzip -qo amap_sdk.zip -d amap_sdk_extracted

# 提取内部的 SDK 压缩包
echo "Extracting inner SDK zip..."
unzip -qo amap_sdk_extracted/AMap3DMap_AMapSearch_AMapLocation.zip -d amap_sdk_inner || true
for z in amap_sdk_extracted/*.zip; do
  if [[ "$z" != *"DemoDocs"* ]] && [[ "$z" != *"2DMap"* ]]; then
    unzip -qo "$z" -d amap_sdk_inner || true
  fi
done

# 复制 jar 包
find amap_sdk_inner -name "*.jar" -exec cp {} app/libs/ \;

# 将可能不同的 jar 名称重命名为代码中引用的名称
AMAP_JAR=$(find app/libs -maxdepth 1 -name "AMap*.jar" | head -n 1)
TARGET_AMAP_JAR="app/libs/AMap3DMap_11.1.000_AMapSearch_9.7.4_AMapLocation_11.1.000_20260306.jar"
if [ -n "$AMAP_JAR" ] && [ "$AMAP_JAR" != "$TARGET_AMAP_JAR" ]; then
  echo "Renaming $(basename "$AMAP_JAR") to $(basename "$TARGET_AMAP_JAR")"
  mv "$AMAP_JAR" "$TARGET_AMAP_JAR"
fi

# 复制 so 文件 (寻找包含 so 的目录架构)
find amap_sdk_inner -type d -name "armeabi-v7a" | while read DIR; do
  PARENT_DIR=$(dirname "$DIR")
  echo "Found native libs in $PARENT_DIR, copying..."
  cp -r "$PARENT_DIR"/* app/src/main/jniLibs/ 2>/dev/null || true
done

echo "=== SDKs prepared ==="
ls -lh app/libs/
ls -lh app/src/main/jniLibs/ || true
