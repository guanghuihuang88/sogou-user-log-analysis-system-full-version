#!/bin/bash
if [ $# -lt 3 ]
then
  echo "Usage: ./deploy.sh srcFile(or Dir) descFile(or Dir) MachineTag"
  echo "Usage: ./deploy.sh srcFile(or Dir) descFile(or Dir) MachineTag confFile"
  exit
fi

src=$1
dest=$2
tag=$3

if [ 'a'$4'a' == 'aa' ]
then
  confFile=/home/hadoop/tools/deploy.conf
else
  confFile=$4
fi

if [ -f $confFile ]
then
  if [ -f $src ]
  then
    for server in `cat $confFile | grep -v '^#'|grep ','$tag','|awk -F',' '{print $1}'`
    do
      scp $src $server":"${dest}
    done
  elif [ -d $src ]
  then
    for server in `cat $confFile | grep -v '^#'|grep ','$tag','|awk -F',' '{print $1}'`
    do 
      scp -r $src $server":"${dest}
    done
  else
    echo "Error: No source file exist"
  fi
else
  echo "Error: Please assign config file or run deploy.sh command with deploy.conf in same directory"
fi
