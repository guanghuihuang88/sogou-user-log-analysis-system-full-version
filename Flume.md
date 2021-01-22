# Flume 概述及其部属

## 1 Flume 日志采集系统

> Flume 是 Cloudera 提供的一个高可用的、高可靠的、分布式的海量日志采集、聚合和传输的系统
>
> - Flume 支持在日志系统中定制各类数据发送方，用于收集数据
> - Flume 提供对数据进行简单处理，并写到各种数据接受方（可定制）的能力

### 使用场景

- Flume 将数据表示为事件（Event），每个事件本质上必须是一个独立的记录，而不是记录的一部分。如果数据不能表示为多条独立的记录，Flume 可能不适用于此场景
- 如果每几个小时才产生几个 G 的数据，并不值得使用 Flume 组件
- Flume 适合实时推送事件，处理海量、持续的数据流

### 在生态圈中的位置

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/076.jpg" alt="image" style="zoom:;" />

## 2 Flume 系统架构

> Agent：一个独立的 Flume 进程，一个 agent 中包含多个 sources、channels 和 sinks
>
> Event：Flume 的基本数据单位，可以是日志记录、 avro 对象等
>
> Source：采集外部数据，传递给 Channel
>
> Channel：中转 Event 的一个临时存储，保存 Source 组件传递过来的 Event，其实就是连接 Source 和 Sink，有点像一个消息队列
>
> Sink：从 Channel 拉取数据，运行在一个独立线程，将数据发送给下一个 Agent 或者下一个系统

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/086.jpg" alt="image" style="zoom:;" />

- Source
  - Source 负责接收 Event 或通过特殊机制产生 Event，并将 Events 批量的放到一个或多个 Channel
  - Source 有不同的类型
    - 与系统集成的 Source，比如 Syslog，NetCat
    - 自动生成事件的 Source，比如 Exec
    - 用于 Agent 和 Agent 之间的通信的 RPC Source，比如 Avro，Thrift
  - Source 必须与 Channel 进行关联

- Channel
  - Channel 位于 Source 和 Sink 之间，用于缓存进来的 Event
  - 当 Sink 成功的将 Event 发送到下一个 Agent 中的 Channel 或最终目的地，Event 才从 Channel 中移除
  - 不同的 Channel 提供的持久化水平也是不一样的：
    - Memory Channel：volatile（不稳定）
    - `File` Channel：基于 WAL（Write-Ahead Logging）实现
    - JDBC Channel：基于嵌入 Database 实现
  - Channel 可以和任何数量的 Source 和 Sink 工作
- Sink
  - Sink 负责将 Event 传输到下一个 Agent 或最终目的地，成功完成后将 Event 从 Channel 移除
  - 有不同类型的 Sink：
    - 存储 Event 到最终目的的终端 Sink，比如 HDFS，HBase
    - 自动消耗的 Sink，比如：Null Sink
    - 用于 Agent 间通信的 RPC sink，比如 Avro，thrift

## 3 Flume 安装部署

Flume系统要求及版本选择

- Flume1.6：http://flume.apache.org/releases/content/1.6.0/FlumeUserGuide.html
- Flume1.7：http://flume.apache.org/releases/1.7.0.html
- Flume1.8：http://flume.apache.org/FlumeUserGuide.html

版本下载选择

- Cdh版本下载地址：http://archive-primary.cloudera.com/cdh5/cdh/5/
- Apache版本下载地址：http://archive.apache.org/dist/flume/

本项目使用`apache-flume-1.7.0-bin.tar.gz`

- 解压安装包：`tar -zxvf apache-flume-1.7.0.tar.gz`
- 创建软连接：`ln -s apache-flume-1.7.0-bin hbase`
- 同步到其他节点：`deploy.sh apache-flume-1.7.0-bin /home/hadoop/app slave` 

- 可以使用默认配置文件 `flume-conf.properties.template`，稍后集群构建会分别修改配置文件

  启动 flume：`bin/flume-ng agent -n agent -c conf -f conf/flume-conf.properties.template -Dflume.root.logger=INFO,console`

### Flume 集群构建

在数据采集过程中，数据需要从一个 Flume Agent 发送到另外一个 Agent，Flume 内置了专门的 RPC sink-source（Avro 或Thrift）对来实现 Flume Agent 之间的通信，那么首先的就是 Avro Sink-Avro Source 对，因为它在 Flume 实际生成环境中使用更成熟、更优秀

- Avro RPC 只支持 Java 和其他 JVM 语言

- Thrift RPC 支持非 JVM 语言

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/087.jpg" alt="image" style="zoom:;" />

> 可查看官方文档进行配置：http://flume.apache.org/releases/content/1.9.0/FlumeUserGuide.html
>
> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/088.jpg" alt="image" style="zoom:;" />

|          | Source     | Channel       | Sink       |
| -------- | ---------- | ------------- | ---------- |
| hadoop01 | AvroSource | memoryChannel | LoggerSink |
| hadoop02 | ExecSource | memoryChannel | AvroSink   |
| hadoop03 | ExecSource | memoryChannel | AvroSink   |

我们将 hadoop02，hadoop03 作为采集事件的 Agent，他们分别发送数据给 hadoop01。（注意：hadoop02，hadoop03 的 AvroSink 端口号要与 hadoop01 的 AvroSource 端口号保持一致，如下面配的1234）

- 先在 hadoop02，hadoop03 节点配置`flume-conf.properties`，分别按照官方文档中

  - User Guide —> Configuration —> Flume Sources —> Exec Source 配置
  - User Guide —> Configuration —> Flume Channels —> memoryChannel 配置
  - User Guide —> Configuration —> Flume Sinks —> Avro Sink 配置

  作为采集事件的 Agent，hadoop02，hadoop03 需要分别创建`test.log`日志文件及其父目录，路径可以自定义为：`/home/hadoop/data/flume/logs/test.log`

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

- 然后在 hadoop01 节点配置`flume-conf2.properties`，分别按照官方文档中

  - User Guide —> Configuration —> Flume Sources —> Avro Source 配置
  - User Guide —> Configuration —> Flume Channels —> memoryChannel 配置
  - User Guide —> Configuration —> Flume Sinks —> Logger Sink 配置

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = avroSource
  agent.channels = memoryChannel
  agent.sinks = loggerSink
  
  # For each one of the sources, the type is defined
  agent.sources.avroSource.type = avro
  agent.sources.avroSource.channels = memoryChannel
  agent.sources.avroSource.bind = 0.0.0.0
  agent.sources.avroSource.port = 1234
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.loggerSink.type = logger
  agent.sinks.loggerSink.channel = memoryChannel
  ```

### 启动 flume 集群

- hadoop02：`bin/flume-ng agent -n agent -c conf -f conf/flume-conf.properties -Dflume.root.logger=INFO,console`

- hadoop03：`bin/flume-ng agent -n agent -c conf -f conf/flume-conf.properties -Dflume.root.logger=INFO,console`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/089.jpg" alt="image" style="zoom:;" />

- hadoop01：`bin/flume-ng agent -n agent -c conf -f conf/flume-conf2.properties -Dflume.root.logger=INFO,console`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/090.jpg" alt="image" style="zoom:;" />



