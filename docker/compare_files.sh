#!/bin/bash
#===================================================================================================
#
# Docker Compose Wrapper for Compare Files
#
#===================================================================================================

# Run compare-files service with docker compose
# - Mount current directory to /data and config to /app/config
# - Remove container after execution
# - Pass all arguments to the script
exec docker compose run --rm compare-files "$@"