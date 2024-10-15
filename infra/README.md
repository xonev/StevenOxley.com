This infrastructure provides everything needed to deploy my static website in AWS:
- Cloudfront Distribution
- S3 bucket (where the files are uploaded to)
- Cloudfront function to handle pretty URLs, etc.

# Applying changes
Everything should be configured using the correct role, so you can just `terraform apply` changes (as long as your default AWS profile has permissions to assume the role).

# Running CLI commands in the account
The `assume-prod.sh` script can be sourced to assume a role in the account (as long as you have permissions to assume the role).
