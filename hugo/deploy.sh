#!/bin/bash

set -Eeuo pipefail

[[ -d public ]] && rm -r public

hugo
aws s3 sync --profile soxley --delete public s3://stevenoxley-com-site
aws cloudfront create-invalidation --profile soxley --distribution-id E3CPVBXOIRWO5M --paths "/*"
