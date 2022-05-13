#!/usr/bin/env bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

if [[ $(uname -s) == "Darwin" ]]; then
  shopt -s expand_aliases
  alias echo="gecho"; alias dirname="gdirname"; alias grep="ggrep"; alias readlink="greadlink"
  alias tar="gtar"; alias sed="gsed"; alias date="gdate"; alias ls="gls"
fi

function check_env_variable() {
    X="${1}"
    info "Checking content of variable ${X}"
    if [[ -z "${!X}" ]]; then
        err_and_exit "Variable ${X} is not defined, exit!!!"
    fi
}


############################################################
# LOGGER
############################################################
function log() {
    local level="${1}"
    shift
    echo -e "[$(date -u +"%Y-%m-%d %H:%M:%S")] [${level}]: ${@}"
}

function err_and_exit() {
    log "${RED}ERROR${NC}" "${1}" >&2
    exit ${2:-1}
}

function err() {
    log "${RED}ERROR${NC}" "${1}" >&2
}

function info() {
    log "${GREEN}INFO${NC}" "${1}"
}

function warn() {
    log "${YELLOW}WARN${NC}" "${1}"
}

