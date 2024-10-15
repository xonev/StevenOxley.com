#/bin/zsh
role="arn:aws:iam::713881783677:role/OrganizationAccountAccessRole"
role_session_name="stevenoxley-personal-prod-org-access"
profile="default"

set -x

if [[ "$#" -eq 1 ]]; then
    profile="$1"
fi

unset AWS_SESSION_TOKEN AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
set +x
export TEMP_ROLE=$(aws sts assume-role --role-arn="$role" --role-session-name="${role_session_name}" --profile="$profile" --output json) && \
export AWS_SESSION_TOKEN=$(echo "$TEMP_ROLE" | jq -r '.Credentials.SessionToken') && \
export AWS_ACCESS_KEY_ID=$(echo "$TEMP_ROLE" | jq -r '.Credentials.AccessKeyId') && \
export AWS_SECRET_ACCESS_KEY=$(echo "$TEMP_ROLE" | jq -r '.Credentials.SecretAccessKey') && \

echo "Current identity: $(aws sts get-caller-identity)"
