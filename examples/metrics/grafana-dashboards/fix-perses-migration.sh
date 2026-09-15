#!/bin/bash
#
# Post-processing script for percli migrate output.
#
# Usage:
#   percli migrate --grafana-dashboard input.json --perses-output output.json
#   ./fix-perses-migration.sh output.json
#
# Fixes:
#   1. Datasource references: object format -> "$variable" string
#   2. Variable names: "${VAR}" -> "VAR" (strip Grafana syntax)
#   3. Cert Expiry query (strimzi-operators specific, skipped if not present)

set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <perses-dashboard.json> [output.json]"
  echo "  If output is omitted, the file is modified in place."
  exit 1
fi

INPUT="$1"
OUTPUT="${2:-$1}"

jq '
  # Fix 1: Convert datasource object refs to variable string refs in queries.
  #   {"kind": "PrometheusDatasource", "name": "${DS_PROMETHEUS}"}  ->  "$DS_PROMETHEUS"
  #   {"kind": "PrometheusDatasource", "name": "$DS_PROMETHEUS"}    ->  "$DS_PROMETHEUS"
  (.. | objects | select(.kind == "PrometheusTimeSeriesQuery") | .spec) |=
    if .datasource | type == "object" then
      .datasource = ("$" + (.datasource.name | gsub("[\\$\\{\\}]"; "")))
    else . end |

  # Fix 2: Strip ${} from variable names (Grafana syntax -> Perses syntax).
  #   "${DS_PROMETHEUS}" -> "DS_PROMETHEUS"
  (.spec.variables // [] | .[].spec) |=
    if .name | test("^\\$\\{.*\\}$") then
      .name = (.name | gsub("[\\$\\{\\}]"; ""))
    else . end |

  # Fix 3: Fix migration_from_grafana_not_supported for known queries.
  #   The Cert Expiry table uses Grafana transformations that percli cannot migrate.
  (.. | objects | select(
    .kind == "PrometheusTimeSeriesQuery" and
    .spec.query == "migration_from_grafana_not_supported"
  )) |= (
    if (.spec.datasource // "" | tostring | test("DS_PROMETHEUS")) then
      .spec.query = "sort (min by (cluster, type, resource_namespace) (strimzi_certificate_expiration_timestamp_ms))"
    else . end
  )
' "$INPUT" > "${INPUT}.tmp" && mv "${INPUT}.tmp" "$OUTPUT"

echo "Fixed: $OUTPUT"
