#!/usr/bin/env bash
# Builds the Luna projects and their Docker images (linux/amd64, see README.md, step 9).
#
#   ./build.sh                          all projects with tests
#   ./build.sh luna-core frontend       only the given projects
#   ./build.sh --skip-tests [project…]  images only, e.g. on the server after git pull
#
# Backends need Java 25 (and Docker for Testcontainers), the frontend tests need Node 24; the frontend image itself
# is built in Docker. Images are tagged with the version of pom.xml (plus latest) or frontend/package.json.
set -euo pipefail

cd "$(dirname "$0")"

BACKENDS=(luna-core luna-task luna-alert luna-note luna-appointment luna-mail)
FRONTEND=frontend
PROJECTS=("${BACKENDS[@]}" "$FRONTEND")

usage() {
  echo "Usage: $0 [--skip-tests] [project ...]"
  echo "Projects: ${PROJECTS[*]}"
}

skip_tests=false
selected=()
for argument in "$@"; do
  case "$argument" in
    --skip-tests) skip_tests=true ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      if [[ " ${PROJECTS[*]} " != *" $argument "* ]]; then
        echo "Unknown project: $argument" >&2
        usage >&2
        exit 1
      fi
      selected+=("$argument")
      ;;
  esac
done
if [[ ${#selected[@]} -eq 0 ]]; then
  selected=("${PROJECTS[@]}")
fi

if ! docker info > /dev/null 2>&1; then
  echo "Docker is not running; it is needed for the images and the tests." >&2
  exit 1
fi

build_backend() {
  local backend=$1
  if [[ "$skip_tests" == true ]]; then
    (cd "$backend" && ./mvnw -B package jib:dockerBuild -DskipTests)
  else
    # verify also checks the coverage of 100 %.
    (cd "$backend" && ./mvnw -B verify jib:dockerBuild)
  fi
}

build_frontend() {
  local version
  version=$(sed -n 's/^  "version": "\(.*\)",$/\1/p' "$FRONTEND/package.json")
  if [[ "$skip_tests" == false ]]; then
    (cd "$FRONTEND" && npm ci && npm run lint && npm run coverage)
  fi
  docker build --platform linux/amd64 -t "luna-frontend:$version" "$FRONTEND"
}

built=()
for project in "${selected[@]}"; do
  echo "==> Building $project"
  if [[ "$project" == "$FRONTEND" ]]; then
    build_frontend
  else
    build_backend "$project"
  fi
  built+=("$project")
done

echo "==> Built: ${built[*]}"
docker images --format '{{.Repository}}:{{.Tag}}' | grep -E "^($(IFS='|'; echo "${built[*]/#$FRONTEND/luna-frontend}")):" | sort
