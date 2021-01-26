#!/bin/sh
home=$(cd `dirname $0`; cd ..; pwd)
. ${home}/bin/common.sh
echo "start analog data ****************"
java -cp ${lib_home}/analogData.jar com.hadoop.java.AnalogData  ${data_home}/sogoulogs.log ${data_home}/test.log
