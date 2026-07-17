#!/bin/bash
# download secrets from vault and set them locally

function add_to_file() {
  local _data=$1
  for item in $(echo "${_data}" | jq -r 'keys[]'); do
  
      if [[ "$OSTYPE" == "msys" ]]; then 
          # jq output has a spurious \r character at the end
          item="${item%%[[:cntrl:]]}"
      fi
    
      value=$(echo "${_data}" | jq -r ".${item}")
      key=$(echo "${item}" | tr '[:lower:]' '[:upper:]')
      echo "${key}=${value}" >> .password_envs
  done
}


export VAULT_ADDR=https://vault.dev.jspaas.uk
vault login -method=oidc -path=jwt role=op-css-kafka-team-role
echo "PROFILE=local" > .password_envs

passwords=$(vault kv get -format=json secret/css-kafka/kafka-ui/passwords | jq -r '.data.data')
add_to_file "$passwords"

clientsecret=$(vault kv get -format=json secret/css-kafka/kafka-ui/clientsecret | jq -r '.data.data')
add_to_file "$clientsecret"


azure_kafbat=$(vault kv get -format=json secret/css-kafka/kafka-ui/azure_kafbat | jq -r '.data.data')
add_to_file "$azure_kafbat"

cert=$(vault kv get -format=json secret/css-kafka/kafka-ui/made-ssl | jq -r '.data.data."cert"')
echo "${cert}" > config/certificates/css-kafka-ui.crt

cat .password_envs

aws_creds=$(vault read aws/creds/op-css-kafka-default -format=json)
if [[ -z $aws_creds ]] ; then
  echo "Failed to obtain AWS creds from Vault" 1>&2
  exit 1
fi

# get AWS credentials to be able to pull docker image from css-kafka/kafbat-ui
export AWS_ACCESS_KEY_ID=$(jq -r .data.access_key <<<$aws_creds)
export AWS_SECRET_ACCESS_KEY=$(jq -r .data.secret_key <<<$aws_creds)
export AWS_SESSION_TOKEN=$(jq -r .data.security_token <<<$aws_creds)
export AWS_DEFAULT_REGION=eu-west-1

echo "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}"
echo "AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}"
echo "AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN}"

export AWS_PASSWORD=$(aws ecr get-login-password --region eu-west-1)
docker login --username AWS --password ${AWS_PASSWORD} 539613588543.dkr.ecr.eu-west-1.amazonaws.com