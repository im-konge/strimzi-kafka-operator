#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "${DIR}/../scripts-common/common.sh"

KEYCLOAK_OPERATOR_NAMESPACE=$1
KEYCLOAK_VERSION=$2

info "Delete Keycloak & Keycloak Operator"
kubectl delete -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/examples/keycloak/keycloak.yaml
kubectl delete -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/operator.yaml
kubectl delete -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakusers_crd.yaml
kubectl delete -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloaks_crd.yaml
kubectl delete -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakclients_crd.yaml
kubectl delete -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakrealms_crd.yaml
kubectl delete -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/crds/keycloak.org_keycloakbackups_crd.yaml
kubectl delete -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/cluster_roles/cluster_role.yaml
kubectl delete -f https://raw.githubusercontent.com/keycloak/keycloak-operator/${KEYCLOAK_VERSION}/deploy/cluster_roles/cluster_role_binding.yaml
kubectl delete -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/role.yaml
kubectl delete -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/role_binding.yaml
kubectl delete -n ${KEYCLOAK_OPERATOR_NAMESPACE} -f https://github.com/keycloak/keycloak-operator/raw/${KEYCLOAK_VERSION}/deploy/service_account.yaml
