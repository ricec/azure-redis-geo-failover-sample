#!/bin/bash
set -euo pipefail

fqdn="$1"
port="$2"
pwd="$3"

cmd_text="echo 'Checking command stats for $fqdn...\n'"
cmd_text="$cmd_text && redis-cli --csv --tls -h '$fqdn' -p '$port' info commandstats | grep -P '_set|_hset|_hmset|_hsetnx|_lset|_mset|_msetnx|_setbit|_setex|_setrange|_setnx' | grep -v '_setclientaddr' && echo"

REDISCLI_AUTH="$pwd" watch -n 5 -d "$cmd_text"