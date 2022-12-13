#!/bin/bash
set -euo pipefail

# Retrieve cache1 resource
echo "Loading cache details..."
cache1_id="$1"
cache1_json="$(az redis show --ids "$cache1_id")"
if [[ -z $cache1_json ]]
then
  echo The specified Redis Cache was not found.
  echo Please double-check the supplied resource ID.
  exit 1
fi
cache1_fqdn="$(echo "$cache1_json" | jq -r '.hostName')"
cache1_port="$(echo "$cache1_json" | jq -r '.sslPort')"
cache1_shards="$(echo "$cache1_json" | jq -r '.shardCount')"
if [[ $cache1_shards != 'null' ]]
then
  cache1_clustered=true
else
  cache1_clustered=false
fi
cache1_pwd="$(az redis list-keys --ids "$cache1_id" --query primaryKey -o tsv)"


# Retrieve geo-replication links
server_links_json="$(az rest -u "https://management.azure.com${cache1_id}/linkedServers?api-version=2022-06-01" -m GET --headers 'Content-Type=application/json')"
server_link="$(echo "$server_links_json" | jq -r '.value[0]')"
if [[ -z $server_link ]]
then
  echo The specified Redis Cache is not setup for geo-replication.
  echo Please double-check geo-replication configuration.
  exit 1
fi
geo_fqdn="$(echo "$server_link" | jq -r '.properties.geoReplicatedPrimaryHostName')"

# Retrieve cache2 resource
cache2_id="$(echo "$server_link" | jq -r '.properties.linkedRedisCacheId')"
cache2_json="$(az redis show --ids "$cache2_id")"
cache2_fqdn="$(echo "$cache2_json" | jq -r '.hostName')"
cache2_pwd="$(az redis list-keys --ids "$cache2_id" --query primaryKey -o tsv)"

# Setup tmux panes
tmux_session='redis_sample'
tmux new-session -d -s $tmux_session -e "cache1_pwd=$cache1_pwd" -e "cache2_pwd=$cache2_pwd"
tmux split-window -t $tmux_session.0 -v -l '20%'
tmux split-window -t $tmux_session.0 -h -l '50%'
tmux split-window -t $tmux_session.1 -v -l '66%'
tmux split-window -t $tmux_session.2 -v -l '50%'
sleep 1

# Start sample
tmux send-keys -t $tmux_session.0 'cd ./azure-redis-failover-sample/ && clear' Enter
tmux send-keys -t $tmux_session.0 "mvn package && CACHE_FQDN='$geo_fqdn' CACHE_PORT='$cache1_port' CACHE_CLUSTERED='$cache1_clustered' CACHE_ACCESS_KEY="'"$cache1_pwd" CACHE_ALT_ACCESS_KEY="$cache2_pwd" java -jar ./target/azure-redis-failover-sample-1.0-SNAPSHOT.jar' Enter

# Monitor writes on both caches
tmux send-keys -t $tmux_session.1 "./monitor-writes.sh '$cache1_fqdn' '$cache1_port'" ' "$cache1_pwd"' Enter
tmux send-keys -t $tmux_session.2 "./monitor-writes.sh '$cache2_fqdn' '$cache1_port'" ' "$cache2_pwd"' Enter

# Monitor geo FQDN for changes
tmux send-keys -t $tmux_session.3 "watch -n 5 -d=permanent nslookup '$geo_fqdn'" Enter

# Setup easy exit for those unfamiliar w/ tmux
tmux send-keys -t $tmux_session.4 "clear && echo 'Run tmux kill-session to exit this sample...'" Enter
tmux send-keys -t $tmux_session.4 'tmux kill-session'

tmux select-pane -t $tmux_session.4
tmux attach -t $tmux_session
