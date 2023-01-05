#!/usr/bin/env bash
set +x
# This script is using oauth2 tests

USERNAME=$1
PASSWORD=$2
URL=$3

TOKEN_CURL_OUT=$(curl --insecure -X POST -d "client_id=admin-cli&client_secret=aGVsbG8td29ybGQtcHJvZHVjZXItc2VjcmV0&grant_type=password&username=$USERNAME&password=$PASSWORD" "https://$URL/auth/realms/master/protocol/openid-connect/token")
echo "[INFO] TOKEN_CURL_OUT: ${TOKEN_CURL_OUT}\n"
TOKEN=$(echo ${TOKEN_CURL_OUT} | awk -F '\"' '{print $4}')

RESULT=$(curl -v --insecure "https://$URL/auth/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Postman-Token: 3a6cd746-03b5-46fe-a54a-014fc7c51983' \
  -H 'cache-control: no-cache' \
  -d '')

if [[ ${RESULT} != "" && ${RESULT} != *"Conflict detected"* ]]; then
  echo "[ERROR] $(date -u +"%Y-%m-%d %H:%M:%S") Authorization realm wasn't imported!"
  exit 1
fi

echo "[INFO] $(date -u +"%Y-%m-%d %H:%M:%S") Authorization realm was successfully imported!"

exit 0
