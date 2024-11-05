#!/bin/sh

# 1. Edit version:
#   - README.md
#   - manual.md
#   - Main.kt
#   - install.sh
#   - build.sh
# 2. Build:
#   - CEU = 99
#   - Build artifacts...
#   - ./build.sh
# 3. Upload:
#   - https://github.com/fsantanna/mar/releases/new
#   - tag    = <version>
#   - title  = <version>
#   - Attach = { .zip, install.sh }

VER=v0.1.0
DIR=/tmp/mar-build/

rm -Rf $DIR
rm -f  /tmp/mar-$VER.zip
mkdir -p $DIR

cp mar.sh $DIR/mar
cp prelude.mar $DIR/prelude.mar
cp hello-world.mar $DIR/
cp ../out/artifacts/mar_jar/mar.jar $DIR/mar.jar

cd /tmp/
zip mar-$VER.zip -j mar-build/*

echo "-=-=-=-"

cd -

cd $DIR/
./mar --version
echo "-=-=-=-"
./mar hello-world.mar
echo "-=-=-=-"

cd -
cp install.sh install-$VER.sh
cp /tmp/mar-$VER.zip .

ls -l install-*.sh mar-*.zip
echo "-=-=-=-"
unzip -t mar-$VER.zip
