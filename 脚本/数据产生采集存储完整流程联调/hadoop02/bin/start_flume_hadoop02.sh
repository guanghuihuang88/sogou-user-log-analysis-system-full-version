#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

${flume_home}/bin/flume-ng agent \
--conf ${conf_home} \
-f ${conf_home}/flume-conf.properties -n agent >> ${logs_home}/flume-hadoop02.log 2>&1 &

echo $! > ${logs_home}/flume-hadoop02.pid
