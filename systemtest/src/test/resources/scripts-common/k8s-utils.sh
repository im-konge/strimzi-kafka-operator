#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "${DIR}/../scripts-common/common.sh"

function wait_for_resource_condition() {
  echo "$1"
  echo "$2"
  echo "$3"
  echo "$4"
  kubectl wait "$1" --for=condition="$2" --timeout="$3" -n "$4"
  if [ $? -ne 0 ]; then
    err "Error waiting for $1 to reach condition $2"
    exit 1
  fi
}