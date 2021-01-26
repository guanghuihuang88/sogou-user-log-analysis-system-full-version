#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

${flume_home}/bin/flume-ng agent \
--conf ${conf_home} \
-f ${conf_home}/flume-conf-logger.properties -n agent1 >> ${logs_home}/behavior_agent.log 2>&1 &

echo $! > ${logs_home}/behavior_agent.pid
