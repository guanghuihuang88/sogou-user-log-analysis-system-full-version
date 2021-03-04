# Kafka 概述及其部属

## 1 Kafka 消息系统

> kafka 是 LinkedIn 开发并开源的一个分布式MQ系统，现在是Apache的一个孵化项目。在它的主页描述 kafka 为一个高吞吐量的分布式（能将消息分散到不同的节点上）MQ。Kafka 仅仅由7000行 Scala 编写，据了解，Kafka 每秒可以生产约25万消息（50MB），每秒处理55万消息（110 MB）。目前越来越多的开源分布式处理系统如 Cloudera、Apache Storm、Spark 都支持与 Kafka 集成

### Kafka 在 LinkedIn 的应用

LinkedIn 的工程师团队已经把 Kafka 打造为管理信息流的开源解决方案。他们把 Kafka 作为消息中枢，帮助公司的各个应用以松耦合的方式在一起工作。LinkedIn 已经严重依赖于 kafka，并且基于 Kafka 的生态系统，LinkedIn 开发出了一些开源组件和公司内部组件。Kafka 在 LinkedIn 中的应用场景如下所述：

- 系统监控：LinkedIn 内所有的主机都会往 Kafka 发送系统健康信息和运行信息，负责展示运维信息和报警的系统，则从 Kafka 订阅获取这些运维信息，处理后进行业务的展示和告警业务的触发。更进一步，LinkedIn 通过自家的实时流处理系统 Samza 对这些运维数据进行实时的处理分析，生成实时的调用图分析
- 传统的消息队列：LinkedIn 内大量的应用系统把 Kafka 作为一个分布式消息队列进行使用。这些应用囊括了搜索、内容相关性反馈等应用。这些应用将处理后的数据通过 Kafka 传递到 Voldemort 分布式存储系统
- 分析：LinkedIn 会搜索所有的数据以更好地了解用户是如何使用 LinkedIn 产品的。哪些网页被浏览，哪些内容被点击这样的信息都会发送到每个数据中心的 Kafka。这些数据被汇总起来并通过 Kafka 发送到 Hadoop 集群进行分析和每日报表生成

### Kafka 设计目标

kafka 作为一种分布式的、基于发布/订阅的消息系统，其主要设计目标如下：

- 以时间复杂度为O(1)的方式提供消息持久化能力，即使对TB级以上的数据也能保证常数时间的访问性能
- 高吞吐率，即使在非常廉价的商用机器上也能做到单机支持每秒100k条的消息的传输
- 支持 Kafka Server 间的消息分区，以及分布式消费，同时保证每个分区内的消息顺序传输
- 支持离线数据处理和实时数据处理
- 支持在线水平扩展

### Kafka 特点

- 高性能、高吞吐量

  单节点支持上千个客户端，每秒钟有上百M（百万级）的吞吐，基本上达到了网卡的极限

- 持久性

  Kafka 消息直接持久化到普通磁盘，性能非常好，而且数据不会丢失，数据会顺序写，消费的数据也会顺序读，持久化的同时还保证了数据的读写顺序

- 分布式

  基于分布式的扩展、和容错机制；Kafka 的数据都会复制到几台服务器上。当某一台故障失效时，生产者和消费者转而使用其它的机器

- 灵活性

  消息长时间持久化到磁盘，client维护消费的状态，因此非常的灵活

  原因一：消息持久化数据时间跨度比较长，可以设置为一天、一周或者更长

  原因二：消息的状态自己维护，消费到哪里自己知道

### 在生态圈中的位置

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/076.jpg" alt="image" style="zoom:;" />



## 2 Kafka 系统架构

在 Kafka 集群中生产者将消息发送给以 Topic 命名的消息队列 Queue 中，消费者订阅发往某个 Topic 命名的消息队列 Queue 中的消息。其中 Kafka 集群由若干个 Broker 组成，Topic 由若干个 Partition 组成，每个 Partition 里面的消息通过 Offset 来获取

- Broker

  一台 Kafka 服务器就是一个 Broker，一个集群由多个 Broker 组成，一个 Broker 可以容纳多个 Topic，Broker 和 Broker之间没有 Master 和 Standby 的概念，它们之间的地位基本是**平等的**

- Topic

  每条发送到 Kafka 集群的消息都属于某个主题，这个主题就称为 Topic。物理上不同 Topic 的消息分开存储，逻辑上一个 Topic 的消息虽然保存在一个或多个 Broker 上，但是用户只需指定消息的主题 Topic 即可生产或者消费数据而不需要去关心数据存放在何处

- Partition

  为了实现可扩展性，一个 Topic 可以被分为多个 Partition，从而分布到多台 Broker 上。Partition 中的每条消息都会被分配一个自增 Id（Offset）。Kafka 只保证按一个 Partition 中的顺序将消息发送给消费者，但是不保证单个 Topic 中的多个 Partition 之间的顺序

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/077.jpg" alt="image" style="zoom:;" />

- Offset

  消息在 Topic 的 Partition 中的位置，同一个 Partition 中的消息随着消息的写入，其对应的 Offset 也自增，其内部实现原理如下图所示

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/078.jpg" alt="image" style="zoom:;" />

- Replica

  副本。Topic 的 Partition 含有N个 Replica，N为副本因子。其中一个 Replica 为 Leader，其他都为Follower，Leader 处理 Partition 的所有读写请求，与此同时，Follower 会定期地去同步 Leader 上的数据

- Message

  消息，是通信的基本单位。每个 Producer 可以向一个 Topic（主题）发布一些消息

- Producer

  消息生产者，即将消息发布到指定的 Topic 中，同时 Producer 也能决定此消息所属的 Partition：比如基于 Round-Robin（轮询）方式或者 Hash（哈希）方式等一些算法

- Consumer：

  消息消费者，即向指定的 Topic 获取消息，根据指定 Topic 的分区索引及其对应分区上的消息偏移量来获取消息

- Consumer Group

  消费者组，每个 Consumer 属于一个 Consumer Group；反过来，每个 Consumer Group 中可以包含多个 Consumer。如果所有的 Consumer 都具有相同的 Consumer Group，那么消息将会在 Consumer 之间进行负载均衡。也就是说一个 Partition 中的消息只会被相同 Consumer Group 中的某个 Consumer 消费，每个Consumer Group 消息消费是相互独立的

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/079.jpg" alt="image" style="zoom:;" />

- Zookeeper

  存放 Kafka 集群相关元数据的组件。在 Zookeeper 集群中会保存 Topic 的状态信息，例如分区的个数、分区的组成、分区的分别情况等。保存 Broker 的状态信息；保存消费者的消费信息等。通过这些信息，Kafka 很好地将消息生产、消息存储、消息消费的过程结合起来

## 3 Kafka 拓朴结构

一个典型的 Kafka 集群中包含若干个 Producer（可以是某个模块下，发的 Command，或者是 Web 前端产生 Page View，或者是服务器日志等），若干个 Broker（Kafka 集群支持水平扩展，一般 Broker 数量越多，整个 Kafka 集群的吞吐率也就越高），若干个 Consumer Group，以及一个 Zookeeper 集群。Kafka 通过 Zookeeper管理集群配置。Producer 使用push 模式将消息发布到 Broker 上，Consumer 使用 pull 模式从 Broker 上订阅并消费消息

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/082.jpg" alt="image" style="zoom:;" />

一个简单的消息发送流程如下所示

- Producer 根据指定的路由方法（Round-Robin、Hash等），将消息 Push 到 Topic 的某个 Partition 里面
- Kafka 集群接收到 Producer 发过来的消息后，将其持久化到磁盘，并保留消息指定时长（可配置），而不关注消息是否被消费
- Consumer 从 Kafka 集群 Pull 数据，并控制获取消息的 Offset

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/077.jpg" alt="image" style="zoom:;" />

ZooKeeper 用于管理、协调 Kafka 代理

- Broker 注册

  Broker 在 zookeeper 中保存为一个临时节点，节点的路径是 `/brokers/ids/[brokerid]`，每个节点会保存对应 broker 的 IP 以及端口等信息

- Topic 注册

  在 kafka 中,一个 topic 会被分成多个区并被分到多个 broker 上，分区的信息以及 broker 的分布情况都保存在 zookeeper 中，根节点路径为`/brokers/topics`，每个 topic 都会在 topics 下建立独立的子节点，每个 topic 节点下都会包含分区以及broker的对应信息

- 生产者负载均衡

  当 Broker 启动时，会注册该 Broker 的信息，以及可订阅的 topic 信息。生产者通过注册在 Broker 以及Topic 上的 watcher 动态的感知 Broker 以及 Topic 的分区情况，从而将 Topic 的分区动态的分配到 broker 上

## 4 Kafka 集群安装部署

Kafka 版本选择：

- Flume 对 Kafka 版本的要求：http://flume.apache.org/FlumeUserGuide.html#kafka-sink
- Spark 对 Kafka 版本的要求：http://spark.apache.org/docs/2.3.0/structured-streaming-kafka-integration.html

Kafka 版本下载：

- 0.10 版本以上下载：http://mirrors.hust.edu.cn/apache/kafka/
- 所有版本下载地址：http://kafka.apache.org/downloads

这里选择下载`kafka_2.11-0.10.2.2.tgz`版本，并利用 Xftp 上传至主节点 app 目录下

解压：`tar -zxvf kafka_2.11-0.10.2.2.tgz`

软连接：`ln -s kafka_2.11-0.10.2.2 kafka`

### 修改配置文件

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

- 修改 zookeeper.properties

  ```properties
  #Zookeeper 数据存储路径与 Zookeeper 配置文件保持一致
  dataDir=/home/hadoop/data/zookeeper/zkdata
  ```

- 修改 consumer.properties

  ```properties
  #配置 Zookeeper 集群连接地址
  zookeeper.connect=hadoop01:2181,hadoop02:2181,hadoop03:2181
  ```

- 修改 producer.properties

  ```properties
  #修改 kafka 集群配置地址（0.10）
  bootstrap.servers=hadoop01:9092,hadoop02:9092,hadoop03:9092
  ```

- 修改 server.properties

  ```properties
  #分别在hadoop01节点上配置为1，hadoop02节点上配置为2，hadoop03节点上配置为3
  broker.id=1
  #配置 Zookeeper 集群地址
  zookeeper.connect=hadoop01:2181,hadoop02:2181,hadoop03:2181/kafka1.0
  #存储日志文件目录
  log.dirs=/home/hadoop/data/kafka-logs
  ```

- 利用脚本`deploy.sh`同步目录到其他节点，并创建软连接，然后分别修改 server.properties：`broker.id=2`和`broker.id=3`

  `deploy.sh kafka_2.11-0.10.2.2/ /home/hadoop/app/ slave`

- 利用脚本`runRemoteCmd.sh`同步创建日志目录

  `runRemoteCmd.sh "mkdir /home/hadoop/data/kafka-logs" all`

### 集群启动/关闭

- 启动 Zookeeper 集群：

  `runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 注意集群时钟必须同步：

  同步时间：`sudo ntpdate pool.ntp.org`
  查看时间：`date`

需要分别在每个节点上启动：

- 后台启动 Kafka 集群：`bin/kafka-server-start.sh config/server.properties &`
- 前台启动 Kafka 集群：`bin/kafka-server-start.sh config/server.properties `，按 ctrl+c 关闭集群

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/083.jpg" alt="image" style="zoom:;" />

### 创建/查看 topic

- 查看 topic 列表（如果配了根目录，Zookeeper 路径需要加上 root 目录，例如 localhost:2181/kafka1.0）
  `bin/kafka-topics.sh --zookeeper localhost:2181/kafka1.0 --list`

- 创建 topic：`bin/kafka-topics.sh --zookeeper localhost:2181/kafka1.0 --create --topic test --replication-factor 3 --partitions 3`
- 查看 topic 详情：`bin/kafka-topics.sh --zookeeper localhost:2181/kafka1.0 --describe --topic test`

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/085.jpg" alt="image" style="zoom:;" />

### topic 数据发送与消费

- hadoop02 使用自带脚本消费 topic 数据

  `bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test`

- hadoop01 使用自带脚本向 topic 发送数据

  `bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test`

- hadoop03 使用自带脚本消费 topic 数据（此时消费最新数据）

  `bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test`

- hadoop03 使用自带脚本消费 topic 数据（从头消费数据）

  `bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning`

- 查看特定 consumer group 详情

  `bin/kafka-consumer-groups.sh --new-consumer --bootstrap-server localhost:9092 --list`

- 查看 topic 每个 partition 数据消费情况

  `bin/kafka-consumer-groups.sh --new-consumer --bootstrap-server localhost:9092 --group console-consumer-xxxxx --describe`

  ```
  参数说明：
    Group 消费者组
    TOPIC：曾经消费或正在消费的 topic
    PARTITION：分区编号
    CURRENT-OFFSET：consumer group 最后一次提交的 offset
    LOG-END-OFFSET：最后提交的生产消息 offset
    LAG：消费 offset 与生产 offset 之间的差值
    CONSUMER-ID：当前消费 topic-partition 的 group 成员 id
    HOST：消费节点的 ip 地址
    CLIENT-ID：客户端 id
  ```

### Kafka 集群监控

KafkaOffsetMonitor 是一个可以用于监控 Kafka 的 Topic 及 Consumer 消费状况的工具。以程序一个 jar 包的形式运行，部署较为方便。只有监控功能，使用起来也较为安全

作用：

- 监控 Kafka 集群状态，Topic、Consumer Group 列表
- 图形化展示 topic 和 Consumer 之间的关系
- 图形化展示 Consumer 的 offset、Lag 等信息

