#!/bin/bash

set -Eeuo pipefail

if [[ "$#" -gt 0 && "$1" == "clean" ]]; then
    [[ -d public ]] && rm -r public
fi

hugo
export AWS_PROFILE="soxley"
hugo deploy --logLevel info
