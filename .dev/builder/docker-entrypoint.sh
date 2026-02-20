#!/bin/bash

# The volume is mounted from the host but the contents are intact, need to clean
find /build/frontend/node_modules -mindepth 1 -delete

exec "$@"
