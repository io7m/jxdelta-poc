#!/usr/bin/env bash

set -e
set -x

rm -rf zip-src
mkdir -p zip-src

pushd zip-src
wget http://cr.yp.to/software/qmail-1.03.tar.gz
wget http://cr.yp.to/djbdns/djbdns-1.05.tar.gz
wget http://cr.yp.to/publicfile/publicfile-0.52.tar.gz

mkdir 0
pushd 0
tar -x -v -m -f ../qmail-1.03.tar.gz
popd

mkdir 1
pushd 1
tar -x -v -m -f ../qmail-1.03.tar.gz
tar -x -v -m -f ../djbdns-1.05.tar.gz
popd

mkdir 2
pushd 2
tar -x -v -m -f ../qmail-1.03.tar.gz
tar -x -v -m -f ../djbdns-1.05.tar.gz
tar -x -v -m -f ../publicfile-0.52.tar.gz
popd

rm -f qmail-1.03.tar.gz
rm -f djbdns-1.05.tar.gz
rm -f publicfile-0.52.tar.gz

pushd 0
zip ../0.zip $(find . | sort -u)
popd

pushd 1
zip ../1.zip $(find . | sort -u)
popd

pushd 2
zip ../2.zip $(find . | sort -u)
popd

sha256sum -c ../zip-sums.txt

xdelta3 -e -S -s 0.zip 1.zip 1.patch
xdelta3 -e -S -s 1.zip 2.zip 2.patch

sha256sum -c ../zip-patch-sums.txt

xdelta3 merge -v -v -m 1.patch 2.patch 0-2.patch
xdelta3 -d -s 0.zip 0-2.patch final.zip

sha256sum 0.zip
sha256sum 1.zip
sha256sum 2.zip
sha256sum *.patch
sha256sum final.zip
