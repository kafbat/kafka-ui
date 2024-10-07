#!/bin/bash
# Smile
smile_latest_release=$(curl -s "https://api.github.com/repos/kafbat/ui-serde-smile/releases/latest")
smile_package_url=$(printf '%s' "$smile_latest_release" | jq -r '.assets[0].browser_download_url')
smile_serde_version=$(printf '%s' "$smile_latest_release" | jq -r '.tag_name')
echo "Smile leatest version: $smile_serde_version"
echo "Smile package link: $smile_package_url"
if [[ ! -z "$CI" ]]; then
  echo "smile_serde_version=$smile_serde_version" >>$GITHUB_OUTPUT
  echo "smile_package_url=$smile_package_url" >>$GITHUB_OUTPUT
fi

# Glue
glue_latest_release=$(curl -s "https://api.github.com/repos/kafbat/ui-serde-glue/releases/latest")
glue_package_url=$(printf '%s' "$glue_latest_release" | jq -r '.assets[0].browser_download_url')
glue_serde_version=$(printf '%s' "$glue_latest_release" | jq -r '.tag_name')
echo "Glue leatest version: $glue_serde_version"
echo "Glue package link: $glue_package_url"
if [[ ! -z "$CI" ]]; then
  echo "glue_serde_version=$glue_serde_version" >>$GITHUB_OUTPUT
  echo "glue_package_url=$glue_package_url" >>$GITHUB_OUTPUT
fi

# Run build if --build option passed
if [ "$1" == "--build" ]; then
  echo "Run build in local docker"
  docker build -f ./Dockerfile-Serdes --no-cache -t kafka-ui-serders \
    --build-arg SERDE_SMILE_JAR=$smile_package_url \
    --build-arg SERDE_SMILE_VERSION=$smile_serde_version \
    --build-arg SERDE_GLUE_JAR=$glue_package_url \
    --build-arg SERDE_GLUE_VERSION=$glue_serde_version \
    .
fi
