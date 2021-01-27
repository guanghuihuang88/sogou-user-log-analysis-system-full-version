# Flume+Hive+HBase集成开发

## Hive+HBase集成开发

- 在`hive-site.xml`配置文件中配置 Zookeeper，hive 通过这个参数去连接 HBase 集群

  ```xml
  <property>
      <name>hbase.zookeeper.quorum</name>
      <value>hadoop01,hadoop02,hadoop03</value>
  </property>
  ```

- 在`hive-env.sh`添加 hbase 环境变量

  ```shell
  export HBASE_HOME=/home/hadoop/app/hbase
  ```

- 根据业务需求创建数据表结构，关联 HBase 的 sogoulogs表：

  `create external table sogoulogs(id string, logtime string, uid string, keywords string, resultno string, clickno string, url string) STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler' WITH SERDEPROPERTIES("hbase.columns.mapping" = ":key,info:logtime,info:uid,info:keywords,info:resultno,info:clickno,info:url") TBLPROPERTIES("hbase.table.name" = "sogoulogs");`

- 查看创建的外部表 sogoulogs：`show tables;`

### Hive+HBase 离线数据分析

- 统计总记录数：`select count(*) from sogoulogs;`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/118.jpg" alt="image" style="zoom:80%;" />

- 统计新闻话题总量（对 keywords 去重）：`select count(distinct keywords) from sogoulogs;`

- 统计新闻话题浏览量 topn：`select keywords,count(*) as rank from sogoulogs group by keywords order by rank desc limit 10`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/119.jpg" alt="image" style="zoom:80%;" />

- 统计新闻浏览量最高的时段：

  - SQL：`select count(substr(logtime,0,5)) as counter from sogoulogs group by substr(logtime,0,5) order by counter limit 5;`
  - HiveSQL：`select substr(logtime,0,5),count(substr(logtime,0,5)) as counter from sogoulogs group by substr(logtime,0,5) order by counter desc limit 5;`

## Flume+Hive+HBase集成开发

- 清空 HBase：`truncate 'sogoulogs'`

- 清空 hadoop02，hadoop03 的`~/shell/data/test.log`文件：`cat /dev/null > test.log`

- hadoop01 的创建配置文件`flume-hbase.properties`，扩充了缓存大小为1000000：

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = execSource
  agent.channels = memoryChannel
  agent.sinks = hbaseSink
  
  # For each one of the sources, the type is defined
  agent.sources.execSource.type = avro
  agent.sources.execSource.bind = 0.0.0.0
  agent.sources.execSource.port = 1234
  agent.sources.execSource.channels = memoryChannel
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 1000000
  agent.channels.memoryChannel.keep-alive = 60
  
  # Each sink's type must be defined
  agent.sinks.hbaseSink.type = asynchbase
  agent.sinks.hbaseSink.table  = sogoulogs
  agent.sinks.hbaseSink.columnFamily  = info
  agent.sinks.hbaseSink.zookeeperQuorum = hadoop01:2181,hadoop02:2181,hadoop03:2181
  agent.sinks.hbaseSink.serializer.payloadColumn = logtime,uid,keywords,resultno,clickno,url
  agent.sinks.hbaseSink.znodeParent = /hbase
  agent.sinks.hbaseSink.serializer = org.apache.flume.sink.hbase.DsjAsyncHbaseEventSerializer
  agent.sinks.hbaseSink.channel = memoryChannel
  ```

- 修改hadoop02，hadoop03  的配置文件`flume-conf.properties`，扩充了缓存大小为1000000

  ```properties
  agent.channels.memoryChannel.capacity = 1000000
  ```

- 修改 hadoop01 的 fllume 文件`bin/flume.ng`，修改内存大小

  ```shell
  JAVA_OPTS="-Xmx1024m"
  ```

- 拷贝 hadoop01 的 flume 启动脚本，命名为`start_flume_hadoop01_copy.sh`，并授予执行权限（使用配置文件`flume-hbase.properties`）

  ```shell
  #!/bin/sh
  
  home=$(cd `dirname $0`; cd ..; pwd)
  
  . ${home}/bin/common.sh
  
  ${flume_home}/bin/flume-ng agent \
  --conf ${conf_home} \
  -f ${conf_home}/flume-hbase.properties -n agent >> ${logs_home}/flume-hadoop01.log 2>&1 &
  
  echo $! > ${logs_home}/flume-hadoop01.pid
  ```

### Flume+Hive+HBase 离线数据分析

- 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

-  注意集群时钟必须同步：

   同步时间：`sudo ntpdate pool.ntp.org`
   查看时间：`date`

-  启动 HDFS：`sbin/start-dfs.sh`

-  启动 YARN：`sbin/start-yarn.sh`

-  在 hadoop01 启动 HBase：`bin/start-hbase.sh`

   进入`bin/hbase shell`

-  在 hadoop03 启动 MySQL：`sudo service mysqld start`

-  在 hadoop03 启动 Hive：`bin/hive`

- 在 hadoop01 启动 flume：`bin/start_flume_hadoop01_copy.sh`
- 在 hadoop02，hadoop03 启动 flume：`bin/start_flume_hadoop02.sh`
- 在 hadoop02，hadoop03 启动模拟程序脚本：`bin/sogoulogs.sh`

在 hadoop01 查看日志：`tail -F logs/flume_hadoop01.log`，观察到数据持续被采集到 hadoop01；在 hadoop01 的 HBase 持续查询 sogoulogs 表中数据：`count 'sogoulogs'`，观察到数据持续增加；当数据量增加到很大时（比如1000条），在 hadoop03 中 Hive 进行数据分析查询：

- 统计总记录数：`select count(*) from sogoulogs;`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/120.jpg" alt="image" style="zoom:80%;" />

- 统计新闻话题总量（对 keywords 去重）：`select count(distinct keywords) from sogoulogs;`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/121.jpg" alt="image" style="zoom:80%;" />

- 统计新闻话题浏览量 topn：`select keywords,count(*) as rank from sogoulogs group by keywords order by rank desc limit 10;`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/122.jpg" alt="image" style="zoom:80%;" />

- 统计新闻浏览量最高的时段：`select substr(logtime,0,5),count(substr(logtime,0,5)) as counter from sogoulogs group by substr(logtime,0,5) order by counter desc limit 5;`
