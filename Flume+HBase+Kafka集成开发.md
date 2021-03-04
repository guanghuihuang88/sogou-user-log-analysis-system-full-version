# Flume+HBase+Kafka 集成开发

## 1 项目数据流程要求

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/102.jpg" alt="image" style="zoom: 33%;" />

## 2 Flume Channel 选择器原理

Channel 选择器是决定 Source 接收的一个特定事件写入哪些 Channel 的组件，Channel 选择器会告知 Channel处理器将事件写入到每个 Channel

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/103.jpg" alt="image" style="zoom:80%;" />

如果 Source 没有指定 Channel 选择器，那么该 Source 使用复制 Channel 选择器。该复制 Channel 选择器将每个事件复制到 Source 的 channels 参数指定的所有的 Channel 中。Channel 选择器可以指定一组 Channel 是必须的 required，另一组是可选的 optional。如果事件写入 optional Channel 时发生失败，则会忽略这些失败。如果事件写入 required Channel 时发生失败将导致对 Source 抛出异常，并要求 Source 重试

**Flume Channel 选择器配置**

查看官方文档：

- 复制 Channel 选择器：User Guide —> Configuration —> Flume Channels Selectors —> Replicating Channel Selector
- 分发 Channel 选择器：User Guide —> Configuration —> Flume Channels Selectors —> Multiplexing Channel Selector
- 自定义 Channel 选择器：User Guide —> Configuration —> Flume Channels Selectors —> Custom Channel Selector

## 3 Flume+Kafka+HBase 集成开发

> Flume Channel 选择器使用复制选择器，将数据分别完整发送至 kafkaChannel 和 hbaseChannel

- 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 注意集群时钟必须同步：

  同步时间：`sudo ntpdate pool.ntp.org`
  查看时间：`date`

- 启动 HDFS：`sbin/start-dfs.sh`

- 每个节点分别启动 Kafka 集群：`bin/kafka-server-start.sh config/server.properties`

- 在 hadoop01 启动 HBase（在哪个节点启动 HBase，哪个节点就是 master 角色）：

  `bin/start-hbase.sh`

- 修改配置文件`flume-env.sh`配置 Flume 环境变量

  ```SHELL
  export HADOOP_HOME=/home/hadoop/app/hadoop
  export HBASE_HOME=/home/hadoop/app/hbase
  ```

- 在 hadoop01 打开一个 kafka 消费者

  `bin/kafka-console-consumer.sh --zookeeper localhost:2181/kafka1.0 --topic sogoulogs`

### Flume 单节点集成 Kafka 和 HBase

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/104.jpg" alt="image" style="zoom: 33%;" />

- 创建配置文件`flume-kafka-hbase-single.properties`：

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = execSource
  agent.channels = kafkaChannel hbaseChannel
  agent.sinks = kafkaSink hbaseSink
  
  # For each one of the sources, the type is defined
  agent.sources.execSource.type = exec
  agent.sources.execSource.channels = kafkaChannel hbaseChannel
  agent.sources.execSource.command = tail -F /home/hadoop/data/flume/logs/test.log
  
  agent.sources.execSource.selector.type = replicating
  
  # Each channel's type is defined.
  agent.channels.kafkaChannel.type = memory
  agent.channels.kafkaChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.kafkaSink.type = org.apache.flume.sink.kafka.KafkaSink
  agent.sinks.kafkaSink.kafka.topic = sogoulogs
  agent.sinks.kafkaSink.kafka.bootstrap.servers = hadoop01:9092,hadoop02:9092,hadoop03:9092
  agent.sinks.kafkaSink.channel = kafkaChannel
  #******************************************************************
  # Each channel's type is defined.
  agent.channels.hbaseChannel.type = memory
  agent.channels.hbaseChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.hbaseSink.type = asynchbase
  agent.sinks.hbaseSink.table  = sogoulogs
  agent.sinks.hbaseSink.columnFamily  = info
  agent.sinks.hbaseSink.zookeeperQuorum = hadoop01:2181,hadoop02:2181,hadoop03:2181
  agent.sinks.hbaseSink.serializer.payloadColumn = logtime,uid,keywords,resultno,clickno,url
  agent.sinks.hbaseSink.znodeParent = /hbase
  agent.sinks.hbaseSink.serializer = org.apache.flume.sink.hbase.DsjAsyncHbaseEventSerializer
  agent.sinks.hbaseSink.channel = hbaseChannel
  ```

- 先清空`/home/hadoop/data/flume/logs/test.log`中的数据：

  `cat /dev/null > /home/hadoop/data/flume/logs/test.log`

- `bin/hbase shell`进入HBase，清空`sogoulogs`表：`truncate 'sogoulogs'`，扫描表是否为空：`scan 'sogoulogs'`

- 启动 flume 监控日志文件

  `bin/flume-ng agent -n agent -c conf -f conf/flume-kafka-hbase-single.properties -Dflume.root.logger=INFO,console`

- 随便复制一条 `sogoulogs.log` 的数据到`test.log`文件

  `echo “00:09:40,2739469928397069,[汶川震后十分钟],8,29,nv.qianlong.com/33530/2008/05/16/2400@4444820.htm” >> test.log`

- 在启动后的 Flume 会话中可以看到数据插入信息：

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/100.jpg" alt="image"  />

- 进入`bin/hbase shell`的会话中执行`scan 'sogoulogs'`，可以看到传入 HBase 的数据：

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/101.jpg" alt="image"  />

- 在打开的 kafka 消费者控制台也可以看到 kafka节点消费了这条数据

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/105.jpg" alt="image"  />

### Flume 集群集成 Kafka 和 HBase

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/106.jpg" alt="image" style="zoom: 33%;" />

- hadoop01 创建配置文件`flume-kafka-hbase-multi.properties`：

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = execSource
  agent.channels = kafkaChannel hbaseChannel
  agent.sinks = kafkaSink hbaseSink
  
  # For each one of the sources, the type is defined
  agent.sources.execSource.type = avro
  agent.sources.execSource.channels = kafkaChannel hbaseChannel
  agent.sources.execSource.bind = 0.0.0.0
  agent.sources.execSource.port = 1234
  
  agent.sources.execSource.selector.type = replicating
  
  # Each channel's type is defined.
  agent.channels.kafkaChannel.type = memory
  agent.channels.kafkaChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.kafkaSink.type = org.apache.flume.sink.kafka.KafkaSink
  agent.sinks.kafkaSink.kafka.topic = sogoulogs
  agent.sinks.kafkaSink.kafka.bootstrap.servers = hadoop01:9092,hadoop02:9092,hadoop03:9092
  agent.sinks.kafkaSink.channel = kafkaChannel
  #******************************************************************
  # Each channel's type is defined.
  agent.channels.hbaseChannel.type = memory
  agent.channels.hbaseChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.hbaseSink.type = asynchbase
  agent.sinks.hbaseSink.table  = sogoulogs
  agent.sinks.hbaseSink.columnFamily  = info
  agent.sinks.hbaseSink.zookeeperQuorum = hadoop01:2181,hadoop02:2181,hadoop03:2181
  agent.sinks.hbaseSink.serializer.payloadColumn = logtime,uid,keywords,resultno,clickno,url
  agent.sinks.hbaseSink.znodeParent = /hbase
  agent.sinks.hbaseSink.serializer = org.apache.flume.sink.hbase.DsjAsyncHbaseEventSerializer
  agent.sinks.hbaseSink.channel = hbaseChannel
  ```

- hadoop02，hadoop03 修改之前的配置文件`flume-conf.properties`：

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = execSource
  agent.channels = memoryChannel
  agent.sinks = avroSink
  
  # For each one of the sources, the type is defined
  agent.sources.execSource.type = exec
  agent.sources.execSource.channels = memoryChannel
  agent.sources.execSource.command = tail -F /home/hadoop/data/flume/logs/test.log
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.avroSink.type = avro
  agent.sinks.avroSink.channel = memoryChannel
  agent.sinks.avroSink.hostname = hadoop01
  agent.sinks.avroSink.port = 1234
  ```

- 在 hadoop02，hadoop03 中清空`/home/hadoop/data/flume/logs/test.log`中的数据：

  `cat /dev/null > /home/hadoop/data/flume/logs/test.log`

- `bin/hbase shell`进入 HBase，清空`sogoulogs`表：`truncate 'sogoulogs'`，扫描表是否为空：`scan 'sogoulogs'`

- hadoop01 启动 flume 监控日志文件

  `bin/flume-ng agent -n agent -c conf -f conf/flume-kafka-hbase-multi.properties -Dflume.root.logger=INFO,console`

-  hadoop02，hadoop03 分别启动 flume 采集数据

  `bin/flume-ng agent -n agent -c conf -f conf/flume-conf.properties -Dflume.root.logger=INFO,console`

- 在 hadoop02 随便复制一条 `sogoulogs.log` 的数据到`test.log`文件（在 hadoop03 上同理）

  `echo “00:09:40,2739469928397069,[汶川震后十分钟],8,29,nv.qianlong.com/33530/2008/05/16/2400@4444820.htm” >> test.log`

- 在启动后的 Flume 会话中可以看到数据插入信息：

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/100.jpg" alt="image"  />

- 进入`bin/hbase shell`的会话中执行`scan 'sogoulogs'`，可以看到传入 HBase 的数据：

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/101.jpg" alt="image"  />

- 在打开的 kafka 消费者控制台也可以看到 kafka节点消费了这条数据

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/105.jpg" alt="image"  />















