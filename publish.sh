#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: ./publish.sh <version>}"
ENGINE="${ENGINE:-podman}"
IMAGE="docker.io/ublog/ublog"

echo "Building uBlog version $VERSION..."
"$ENGINE" build -f Containerfile -t "$IMAGE:$VERSION" .

echo "Logging in to Docker Hub..."
"$ENGINE" login docker.io -u ublog

echo "Publishing $IMAGE:$VERSION..."
"$ENGINE" push "$IMAGE:$VERSION"

echo "Tagging and publishing latest..."
"$ENGINE" tag "$IMAGE:$VERSION" "$IMAGE:latest"
"$ENGINE" push "$IMAGE:latest"

echo
echo "Successfully published:"
echo "  $IMAGE:$VERSION"
echo "  $IMAGE:latest"

