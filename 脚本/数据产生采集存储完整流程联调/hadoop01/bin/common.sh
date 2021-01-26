#!/bin/sh
#切换到当前目录的父目录
home=$(cd `dirname $0`; cd ..; pwd)
bin_home=$home/bin
conf_home=$home/conf
logs_home=$home/logs
lib_home=$home/lib

flume_home=/home/hadoop/app/apache-flume-1.7.0-bin
kafka_home=/home/hadoop/app/kafka

export HADOOP_HOME=/home/hadoop/app/hadoop
export HBASE_HOME=/home/hadoop/app/hbase 
