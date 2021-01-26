#!/bin/sh
home=$(cd `dirname $0`; cd ..; pwd)
. ${home}/bin/common.sh
echo "start kafka consumer *****************"
${kafka_home}/bin/kafka-console-consumer.sh  --zookeeper hadoop01:2181,hadoop02:2181,hadoop03:2181/kafka1.0  --topic sogoulogs
