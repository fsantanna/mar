#!/bin/sh

VER=v0.1.0

if [ -z "$1" ]
then
    echo "invalid target directory : missing argument"
    exit 1
fi

if [ -f "$1" ]
then
    echo "invalid target directory : target already exists"
    exit 1
fi

if [ -f "mar-$VER.zip" ]
then
    echo "invalid source file : file \"mar-$VER.zip\" already exists"
    exit 1
fi

echo Downloading...
wget -nv https://github.com/fsantanna/mar/releases/download/$VER/mar-$VER.zip
# --show-progress --progress=bar:force

echo Unziping...
mkdir $1/
unzip mar-$VER.zip -d $1/
