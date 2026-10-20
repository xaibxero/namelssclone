#!/bin/bash
BUILD_DIR="build"
OUTPUT_ZIP="../namelssclone_zygisk_module.zip"

mkdir -p module_out/zygisk
if [ -f "/libnamelssclone.so" ]; then
    cp "/libnamelssclone.so" module_out/zygisk/arm64-v8a.so
fi
cp module.prop module_out/
cd module_out && zip -r "" ./* && cd ..
rm -rf module_out
echo "[+] Module created: "
