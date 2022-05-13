#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "${DIR}/../scripts-common/common.sh"
source "${DIR}/../scripts-common/k8s-utils.sh"

PGO_INSTANCE_NAMESPACE=$1
PGO_VERSION=$2
PGO_VERSION="${PGO_VERSION:=main}"
PGO_BASEDIR=/tmp/pgo/poe

info "Deploy Postgres Operator"
[[ -d "${PGO_BASEDIR}" ]] && rm -rf "${PGO_BASEDIR}"
mkdir -p "${PGO_BASEDIR}"
git clone https://github.com/CrunchyData/postgres-operator-examples.git --branch ${PGO_VERSION} "${PGO_BASEDIR}"

for file in $(grep -rin "namespace: postgres-operator" "${PGO_BASEDIR}" | cut -d ":" -f 1);
do
  sed -i "s/namespace: .*/namespace: ${PGO_INSTANCE_NAMESPACE}/" "${file}";
done

kubectl apply --server-side -k "${PGO_BASEDIR}/kustomize/install/default"
# Wait for deployment to show up. If we're too quick, kubectl wait command might fail on non-existing resource
sleep 10

info "Wait for Postgres Operator readiness"

wait_for_resource_condition deployment/pgo available 90s ${PGO_INSTANCE_NAMESPACE}
wait_for_resource_condition deployment/pgo-upgrade available 90s ${PGO_INSTANCE_NAMESPACE}

info "Wait for postgres keycloak instance deployment"
yq eval -i '.| del(.spec.instances.0.affinity)' "${PGO_BASEDIR}/kustomize/postgres/postgres.yaml"
sed 's/name: hippo/name: postgres-kc/' "${PGO_BASEDIR}/kustomize/postgres/postgres.yaml" | kubectl apply -f - -n "${PGO_INSTANCE_NAMESPACE}"

# This is needed to avoid race condition when pods are not created yet before waiting for pods condition
RETRY=12
while [[ $(kubectl get pods -n ${PGO_INSTANCE_NAMESPACE}) != *"postgres-kc-backup"* && ${RETRY} -gt 0 ]]
do
    info "postgres-kc-backup pod does not exist! Going to check it in 5 seconds (${RETRY})"
    sleep 10
    (( RETRY-=1 ))
done

if [ $RETRY -eq 0 ]; then
  err "Failed to deploy postgres-kc-backup pod! Exiting."
  exit 1
fi

DB_BACKUP_POD=$(kubectl get pods -l postgres-operator.crunchydata.com/pgbackrest-backup="replica-create" -o jsonpath="{.items[0].metadata.name}" -n ${PGO_INSTANCE_NAMESPACE})
wait_for_resource_condition pod/${DB_BACKUP_POD} containersready 300s ${PGO_INSTANCE_NAMESPACE}

PG_POD_NAME=$(kubectl get pods -n "${PGO_INSTANCE_NAMESPACE}" | grep "postgres-kc-instance" | cut -d " " -f 1)
wait_for_resource_condition pod/${PG_POD_NAME} containersready 300s ${PGO_INSTANCE_NAMESPACE}

# Wait for existence of a secret
count=0
until kubectl get secret postgres-kc-pguser-postgres-kc -n "${PGO_INSTANCE_NAMESPACE}" || (( count++ >= 10 ))
do
  info "Wait until secret is created: postgres-kc-pguser-postgres-kc"
  sleep 5
done

if [ $count -ge 10 ]; then
  err "Secret postgres-kc-pguser-postgres-kc wasn't created! Exiting."
  exit 1
fi

info "Postgres Operator and Database successfully deployed"
