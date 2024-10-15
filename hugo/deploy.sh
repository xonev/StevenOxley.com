#!/bin/bash

set -Eeuo pipefail

if [[ "$#" -gt 0 && "$1" == "clean" ]]; then
    [[ -d public ]] && rm -r public
fi

hugo
aws s3 sync --profile soxley --delete public s3://stevenoxley-com-site
aws cloudfront create-invalidation --profile soxley --distribution-id E3CPVBXOIRWO5M --paths "/*"
