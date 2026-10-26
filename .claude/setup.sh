#!/usr/bin/env bash
# クラウド環境の起動時に呼ばれるセットアップスクリプト
set -euo pipefail

# graalvm_tools は Gradle 8.0 のため JDK 21 では動かず、JDK 17 が必要
java_17_home=/usr/lib/jvm/java-17-openjdk-amd64
if [ ! -d "${java_17_home}" ]; then
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends openjdk-17-jdk-headless
fi

java_17_home_line="export JAVA_17_HOME=${java_17_home}"
for rc in "${HOME}/.bashrc" "${HOME}/.profile"; do
  touch "${rc}"
  grep -qxF "${java_17_home_line}" "${rc}" || echo "${java_17_home_line}" >> "${rc}"
done
