#!/usr/bin/env sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/commerce-snapshot-classes"
mkdir -p "$BUILD_DIR"
find src/main/java src/test/java -name '*.java' -print | sort | xargs javac -d "$BUILD_DIR"
java -cp "$BUILD_DIR" education.storefront.snapshot.SnapshotDocumentTest
