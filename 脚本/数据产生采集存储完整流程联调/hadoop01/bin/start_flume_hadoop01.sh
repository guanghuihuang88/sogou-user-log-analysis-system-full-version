#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

${flume_home}/bin/flume-ng agent \
--conf ${conf_home} \
-f ${conf_home}/flume-kafka-hbase.properties -n agent >> ${logs_home}/flume-hadoop01.log 2>&1 &

echo $! > ${logs_home}/flume-hadoop01.pid
