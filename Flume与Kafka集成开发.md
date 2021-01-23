# Flume+Kafka 集成开发

## 1 项目数据下载与分析

> 业务数据下载地址：http://www.sogou.com/labs/resource/q.php

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/074.jpg" alt="image" style="zoom:;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/075.jpg" alt="image" style="zoom:;" />

- 数据格式为：
  访问时间\t用户 ID\t[查询词]\t该 URL 在返回结果中的排名\t用户点击的顺序号\t用户点击的 URL
  其中，用户 ID 是根据用户使用浏览器访问搜索引擎时的 Cookie 信息自动赋值，即同一次使用浏览器输入的不同查询对应同一个用户 ID


## 2 项目数据预处理

> 数据集可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E6%95%B0%E6%8D%AE%E9%9B%86

我们这里使用迷你版数据 SogouQ.mini，将其放到 hadoop01 节点的路径`/home/hadoop/shell/data`下

- 使用Linux命令：`tail -f SogouQ.sample`查看数据格式

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/091.jpg" alt="image" style="zoom:;" />

- 对项目数据进程预处理：通过 Linux 命令 tr 将原始数据分隔符，比如 tab 或者空格，统一转换为逗号

  - `cat SogouQ.sample|tr "\t" "," > sogoulogs2.log`
  - `cat sogoulogs2.log|tr " " "," > sogoulogs.log`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/092.jpg" alt="image" style="zoom:;" />

## 3 Flume+Kafka 集成配置

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E6%95%B0%E6%8D%AE%E9%9B%86
>
> 按照官方文档中配置 Kafka Sink：User Guide —> Configuration —> Flume Sources —> Kafka Sink

- 在 Kafka 创建 topic：sogoulogs：`bin/kafka-topics.sh --zookeeper localhost:2181/kafka1.0 --create --topic sogoulogs --replication-factor 3 --partitions 3`
- 查看是否创建成功：`bin/kafka-topics.sh --zookeeper localhost:2181/kafka1.0 --list`

### 单点 Flume 集成 Kafka

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/095.jpg" alt="image" style="zoom: 33%;" />

先测试单点 Flume 采集数据给 Kafka，即仅用 hadoop01（Flume） 采集数据，并发给 hadoop02（Kafka）

- 在 hadoop01 创建配置文件`flume-kafka.properties`：

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = execSource
  agent.channels = memoryChannel
  agent.sinks = kafkaSink
  
  # For each one of the sources, the type is defined
  agent.sources.execSource.type = exec
  agent.sources.execSource.channels = memoryChannel
  agent.sources.execSource.command = tail -F /home/hadoop/shell/data/sogoulogs.log
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.kafkaSink.type = org.apache.flume.sink.kafka.KafkaSink
  agent.sinks.kafkaSink.kafka.topic = sogoulogs
  agent.sinks.kafkaSink.kafka.bootstrap.servers = hadoop01:9092,hadoop02:9092,hadoop03:9092
  agent.sinks.kafkaSink.channel = memoryChannel
  ```
  
- 启动 Zookeeper 集群：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 每个节点分别启动 Kafka 集群：`bin/kafka-server-start.sh config/server.properties`

- hadoop02 打开 Kafka console 消费者：`bin/kafka-console-consumer.sh --zookeeper localhost:2181/kafka1.0 --topic sogoulogs`

- 启动 hadoop01 节点 flume 服务：`bin/flume-ng agent -n agent -c conf -f conf/flume-kafka.properties -Dflume.root.logger=INFO,console`

- 若成功，则会在 hadoop02 节点消费者控制台看到数据

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/093.jpg" alt="image" style="zoom:;" />

### Flume 集群集成 Kafka

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/094.jpg" alt="image" style="zoom:;" />

- 使用`deploy.sh`脚本将预处理过的日志数据`sogoulogs.log`从 hadoop01 同步到 hadoop02，hadoop03 的路径`/home/hadoop/data/flume/logs`下：`deploy.sh sogoulogs.log /home/hadoop/data/flume/logs slave`

- 在 hadoop02，hadoop03 修改之前的配置文件`flume-conf.properties`：

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
    agent.sources.execSource.command = tail -F /home/hadoop/data/flume/logs/sogoulogs.log
    
    # Each channel's type is defined.
    agent.channels.memoryChannel.type = memory
    agent.channels.memoryChannel.capacity = 100
    
    # Each sink's type must be defined
    agent.sinks.avroSink.type = avro
    agent.sinks.avroSink.channel = memoryChannel
    agent.sinks.avroSink.hostname = hadoop01
    agent.sinks.avroSink.port = 1234
   ```

- 在 hadoop01 修改之前的配置文件`flume-conf2.properties`：

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = avroSource
  agent.channels = memoryChannel
  agent.sinks = kafkaSink
  
  # For each one of the sources, the type is defined
  agent.sources.avroSource.type = avro
  agent.sources.avroSource.channels = memoryChannel
  agent.sources.avroSource.bind = 0.0.0.0
  agent.sources.avroSource.port = 1234
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.kafkaSink.type = org.apache.flume.sink.kafka.KafkaSink
  agent.sinks.kafkaSink.kafka.topic = sogoulogs
  agent.sinks.kafkaSink.kafka.bootstrap.servers = hadoop01:9092,hadoop02:9092,hadoop03:9092
  agent.sinks.kafkaSink.channel = memoryChannel
  ```

- 启动 Zookeeper 集群：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 每个节点分别启动 Kafka 集群：`bin/kafka-server-start.sh config/server.properties`

- 任意选择 hadoop02 节点打开 Kafka console 消费者：`bin/kafka-console-consumer.sh --zookeeper localhost:2181/kafka1.0 --topic sogoulogs`

- 启动 hadoop01 节点 flume 服务
  - 启动 hadoop01 节点服务，监听并接受 hadoop02，hadoop03 采集过来的数据
  - `bin/flume-ng agent -n agent -c conf -f conf/flume-conf2.properties -Dflume.root.logger=INFO,console`

- 启动采集节点 flume 服务
  - 分别启动 hadoop02，hadoop03 节点 flume 服务，采集搜狗日志数据并发送给 hadoop01 节点的 flume
  - `bin/flume-ng agent -n agent -c conf -f conf/flume-conf.properties -Dflume.root.logger=INFO,console`
  - `bin/flume-ng agent -n agent -c conf -f conf/flume-conf.properties -Dflume.root.logger=INFO,console`

- 若成功，则会在 hadoop02 节点消费者控制台看到数据

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/093.jpg" alt="image" style="zoom:;" />



