#!/bin/sh

env=$1
#切换到当前目录的父目录
home=$(cd `dirname $0`; cd ..; pwd)
bin_home=$home/bin
conf_home=$home/conf
logs_home=$home/logs
data_home=$home/data
lib_home=$home/lib
#flume的根目录
flume_home=/home/hadoop/app/apache-flume-1.7.0-bin
