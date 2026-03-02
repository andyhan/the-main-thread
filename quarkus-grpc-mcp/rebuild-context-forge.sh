#!/bin/bash
# Rebuild the context-forge image from scratch (no cache) and recreate the container.
# Use this when you change the base image tag (e.g. BETA-2 → RC1) so the new version is used.
set -e
cd "$(dirname "$0")"

echo "Building context-forge image (no cache)..."
podman-compose build --no-cache context-forge

echo "Recreating context-forge container..."
podman-compose up -d --force-recreate context-forge

echo "Done. Open http://localhost:4444 and check the version in the UI."
