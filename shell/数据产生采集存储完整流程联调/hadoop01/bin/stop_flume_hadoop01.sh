#!/bin/sh

home=$(cd `dirname $0`; cd ..; pwd)

. ${home}/bin/common.sh

pid=`cat ${logs_home}/flume-hadoop01.pid | head -1`

kill ${pid}
