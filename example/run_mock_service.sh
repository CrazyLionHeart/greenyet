#!/bin/bash

set -e

SCRIPT_DIR=$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)

cd "$SCRIPT_DIR/status_responses"
python -m SimpleHTTPServer
