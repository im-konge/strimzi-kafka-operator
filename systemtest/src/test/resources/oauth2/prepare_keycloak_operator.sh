#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "${DIR}/../scripts-common/common.sh"
source "${DIR}/../scripts-common/k8s-utils.sh"

KEYCLOAK_OPERATOR_NAMESPACE=$1
KEYCLOAK_VERSION=$2
KEYCLOAK_INSTANCE_NAMESPACE=$3

SCRIPT_PATH=$(dirname "${BASH_SOURCE[0]}")

function update_keycloak_db_secret() {
  # Expecting postgres secret to be located at KEYCLOAK_OPERATOR_NAMESPACE
  PG_SECRET=$(kubectl get secret postgres-kc-pguser-postgres-kc -n "${KEYCLOAK_OPERATOR_NAMESPACE}" -o yaml)
  cp ${SCRIPT_PATH}/keycloak_db_pg_secret.yaml /tmp/keycloak_db_pg_secret.yaml
  DB_SECRET_YAML_FILE=/tmp/keycloak_db_pg_secret.yaml

  POSTGRES_DATABASE=$(echo "${PG_SECRET}" | egrep "^\s+dbname:" | tr -d " " | cut -d ":" -f 2)
  POSTGRES_EXTERNAL_ADDRESS=$(echo "${PG_SECRET}" | egrep "^\s+host:" | tr -d " " | cut -d ":" -f 2)
  POSTGRES_USERNAME=$(echo "${PG_SECRET}" | egrep "^\s* user:" | tr -d " " | cut -d ":" -f 2)
  POSTGRES_PASSWORD=$(echo "${PG_SECRET}" | egrep "^\s* password:" | tr -d " " | cut -d ":" -f 2)
  POSTGRES_EXTERNAL_PORT=$(echo "${PG_SECRET}" | egrep "^\s* port:" | tr -d " " | cut -d ":" -f 2)

  sed -i "s/CHANGE_KEYCLOAK_OPERATOR_NAMESPACE$/${KEYCLOAK_OPERATOR_NAMESPACE}/" "$DB_SECRET_YAML_FILE"
  sed -i "s/CHANGE_POSTGRES_DATABASE$/${POSTGRES_DATABASE}/" "$DB_SECRET_YAML_FILE"
  sed -i "s/CHANGE_POSTGRES_EXTERNAL_ADDRESS/${POSTGRES_EXTERNAL_ADDRESS}/" "$DB_SECRET_YAML_FILE"
  sed -i "s/CHANGE_POSTGRES_USERNAME/${POSTGRES_USERNAME}/" "$DB_SECRET_YAML_FILE"
  sed -i "s/CHANGE_POSTGRES_PASSWORD/${POSTGRES_PASSWORD}/" "$DB_SECRET_YAML_FILE"
  sed -i "s/CHANGE_POSTGRES_EXTERNAL_PORT/${POSTGRES_EXTERNAL_PORT}/" "$DB_SECRET_YAML_FILE"
}

info "Generate keycloak secret"
mkdir -p /tmp/keycloak
openssl req  -nodes -new -x509  -keyout /tmp/keycloak/keycloak.key -out /tmp/keycloak/keycloak.crt -subj '/CN=keycloak'
kubectl create secret -n ${KEYCLOAK_OPERATOR_NAMESPACE} generic sso-x509-https-secret --from-file=tls.crt=/tmp/keycloak/keycloak.crt --from-file=tls.key=/tmp/keycloak/keycloak.key

if [[ ${KEYCLOAK_VERSION} != "11.0.1" ]]; then
  update_keycloak_db_secret
  kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f "${DB_SECRET_YAML_FILE}"
fi

info "Deploy Keycloak Operator"
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/service_account.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/role_binding.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/role.yaml
curl -s https://raw.githubusercontent.com/keycloak/keycloak-operator/${KEYCLOAK_VERSION}/deploy/cluster_roles/cluster_role_binding.yaml | sed "s#namespace: .*#namespace: ${KEYCLOAK_OPERATOR_NAMESPACE}#g" | kubectl apply  -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f -
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/cluster_roles/cluster_role.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakbackups_crd.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakclients_crd.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakrealms_crd.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloaks_crd.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakusers_crd.yaml
kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/operator.yaml
info "Deploy Keycloak instance"

if [[ ${KEYCLOAK_VERSION} != "11.0.1" ]]; then
  warn "Deploying keycloak with external database."
  # Ideally we should use https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/examples/keycloak/keycloak.yaml
  # like we did previously, but there is issue with using external DB, so we have to workaround it.
  # See https://github.com/keycloak/keycloak-operator/issues/426
  kubectl apply -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f "${SCRIPT_PATH}/keycloak_pg_example.yaml"
else
  warn "Skipping deployment of keycloak with external database due to old keycloak version specified."
  kubectl apply -n ${KEYCLOAK_INSTANCE_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/examples/keycloak/keycloak.yaml
fi

# This is needed to avoid race condition when pods are not created yet before waiting for pods condition
RETRY=12
while [[ $(kubectl get pods -n ${KEYCLOAK_OPERATOR_NAMESPACE}) != *"keycloak-0"* && ${RETRY} -gt 0 ]]
do
	info "keycloak-0 does not exists! Going to check it in 5 seconds (${RETRY})"
	sleep 5
	((RETRY-=1))
done

if [ $RETRY -eq 0 ]; then
  err "Failed to deploy postgres-kc-backup pod! Exiting."
  exit 1
fi

info "Wait for Keycloak Operator readiness"
wait_for_resource_condition deployment/keycloak-operator available 90s ${KEYCLOAK_OPERATOR_NAMESPACE}

info "Wait for Keycloak readiness"
wait_for_resource_condition pod/keycloak-0 containersready 300s ${KEYCLOAK_INSTANCE_NAMESPACE}

info "Copy realm scripts"
kubectl cp  -n ${KEYCLOAK_INSTANCE_NAMESPACE} ${SCRIPT_PATH}/create_realm.sh keycloak-0:/tmp/create_realm.sh
kubectl cp  -n ${KEYCLOAK_INSTANCE_NAMESPACE} ${SCRIPT_PATH}/create_realm_authorization.sh keycloak-0:/tmp/create_realm_authorization.sh
kubectl cp  -n ${KEYCLOAK_INSTANCE_NAMESPACE} ${SCRIPT_PATH}/create_realm_scope_audience.sh keycloak-0:/tmp/create_realm_scope_audience.sh

info "Get Admin password"
PASSWORD=$(kubectl get secret -n ${KEYCLOAK_INSTANCE_NAMESPACE} credential-example-keycloak -o=jsonpath='{.data.ADMIN_PASSWORD}' | base64 -d)
USERNAME=$(kubectl get secret -n ${KEYCLOAK_INSTANCE_NAMESPACE} credential-example-keycloak -o=jsonpath='{.data.ADMIN_USERNAME}' | base64 -d)

info "Import realms - USER:${USERNAME} - PASS:${PASSWORD}"
AUTHENTICATION_REALM_OUTPUT=$(kubectl exec keycloak-0 -n ${KEYCLOAK_INSTANCE_NAMESPACE} -- /tmp/create_realm.sh ${USERNAME} ${PASSWORD} localhost:8443)
echo ${AUTHENTICATION_REALM_OUTPUT}
if [[ ${AUTHENTICATION_REALM_OUTPUT} == *"Realm wasn't imported!"* ]]; then
  err "Authentication realm wasn't imported!"
  exit 1
fi

AUTHORIZATION_REALM_OUTPUT=$(kubectl exec keycloak-0 -n ${KEYCLOAK_INSTANCE_NAMESPACE} -- /tmp/create_realm_authorization.sh ${USERNAME} ${PASSWORD} localhost:8443)
if [[ ${AUTHORIZATION_REALM_OUTPUT} == *"Realm wasn't imported!"* ]]; then
  err "Authorization realm wasn't imported!"
  exit 1
fi

SCOPE_AUDIENCE_REALM_OUTPUT=$(kubectl exec keycloak-0 -n ${KEYCLOAK_INSTANCE_NAMESPACE} -- /tmp/create_realm_scope_audience.sh ${USERNAME} ${PASSWORD} localhost:8443)
if [[ ${SCOPE_AUDIENCE_REALM_OUTPUT} == *"Realm wasn't imported!"* ]]; then
  err "Scope & audience realm wasn't imported!"
  exit 1
fi

info "All realms were successfully imported!"
