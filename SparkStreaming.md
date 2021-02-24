# Spark Streaming 实时计算

## 1 Spark Streaming 概述

Spark-Core & RDD 本质上是离线计算

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/152.jpg" alt="image" style="zoom:80%;" />

Spark Streaming 是核心 Spark API 的扩展，支持实时数据流的可伸缩、高吞吐量、容错流处理，即实时计算

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/153.jpg" alt="image" style="zoom:80%;" />

**一个 Spark Streaming 案例**

- 先安装 nc：`sudo yum install -y nc`

- 开两个 hadoop01 的会话窗口，用其中一个开启 spark实时wordcount 案例：

  `spark-alone/bin/run-example streaming.NetworkWordCount localhost 9999`

- 另一个会话窗口输入数据：

  - 方式一：在命令行输入数据：`nc -lk 9999`

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/154.jpg" alt="image" style="zoom:80%;" />

  - 方式二：把文件通过管道作为 nc 的输入：`cat test.txt | nc -lk 9999`

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/155.jpg" alt="image" style="zoom:80%;" />

## 2 Spark Streaming 运行原理

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/156.jpg" alt="image" style="zoom:80%;" />

- Spark Streaming 不断的从数据源获取数据(连续的数据流)，并将这些数据按照周期划分为 batch
- Spark Streaming 将每个 batch 的数据交给 Spark Engine 来处理(每个 batch 的处理实际上还是批处理，只不过批量很小，计算速度很快)
- 整个过程是持续的

### DStream

- 为了便于理解，Spark Streaming 提出了 DStream 抽象，代表连续不断的数据流（Dstream相当于对RDD封装了一层）
- DStream 是一个持续的 RDD 序列
- 可以从外部输入源创建 DStream，也可以对其他 DStream 应用进行转化操作得到新 Dstream（过程类似于 RDD，对 Dstream 操作最终会映射到每个 RDD 上，每个 RDD 的操作又会映射到每条数据上）

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/157.jpg" alt="image" style="zoom:80%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/158.jpg" alt="image" style="zoom: 67%;" />

### batch duration

- Spark Streaming 按照设定的 batch duration 来累积数据，周期结束时把周期内的数据作为一个 RDD，并提交任务给 Spark Engine
- batch duration 的大小决定了 Spark Streaming 提交作业的频率和处理延迟
- batch duration 的大小设定取决于用户的需求，一般不会太大

```scala
val sparkconf = new SparkConf().SetAppName("NetworkWordCount")
val ssc = new StreamingContext(sparkConf, Seconds(1))
```

## 3 Spark Streaming 编程模型

### 依赖

- 依赖管理

  ```xml
  <dependency>
      <groupId>org.apache.spark</groupId>
      <artifactId>spark-streaming_2.11</artifactId>
      <version>2.3.0</version>
      <scope>provided</scope>
  </dependency>
  ```

- Source 相关依赖

  | Source  | Artifact                                                    |
  | ------- | ----------------------------------------------------------- |
  | Kafka   | spark-streaming-kafka_ 2.11                                 |
  | Flume   | spark-streaming-flume_ 2.11                                 |
  | Kinesis | spark-streaming-kinesis-asl_ 2.11 [Amazon Software License] |
  | Twitter | spark- streaming-twitter.2.11                               |
  | ZeroMQ  | spark-streaming-zeromq_ 2.11                                |
  | MQTT    | spark-streaming-mqtt_ 2.11                                  |

### 编程模板

```scala
// 1 参数处理
if(args.length < 2){
    System.err.println("Usage:NetworkWordCount <hostname> <post>")
    System.exit(1)
}
// 2 初始化 StreamingContext
val sparkConf = new SparkConf().setAppName("NetworkWordCount")
val ssc = new StreamingContext(sparkConf, Seconds(1))
// 3 从 source 获取数据创建 DStream
val lines = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)
// 4 对 DStream 进行各种操作
val words = lines.flatMap(_split(" "))
val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
// 5 处理计算结果
wordCounts.print()
// 6 启动 Spark Streaming
ssc.start()
ssc.awaitTermination()
```

编写实时计算版本的 wordcount： NetworkWordCount.scala（点击[源码](https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/streaming/NetworkWordCount.scala)）

- 将项目代码打包到 Spark节点（hadoop01）并提交：

  - 在 hadoop01 会话窗口执行：`bin/spark-submit --class com.dsj.spark.testSpark.NetworkWordCount NetworkWordCount.jar hadoop01 9999`

  - 在另一个 hadoop01 会话窗口执行：`nc -lk 9999`

- 可观察到同概述中的案例相同的效果

### DStream 输入源

Spark内置了两类Source：

| Source 分类      | 举例                                               | 说明                                               |
| ---------------- | -------------------------------------------------- | -------------------------------------------------- |
| Basic sources    | file systems, socket, connections, and Akka actors | StreamingContext直接就可以创建，无需引入额外的依赖 |
| Advanced sources | Kafka, Flume, Kinesis, Twitter, etc                | 需要引入相关依赖，并且需要通过相关的工具类来创建   |

- `val lines = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)`
- `val kafkaStream = KafkaUtils.createStream(streamingContext, [ZK quorum], [consumer group id], [per-topic number of Kafka partitions to consume])`

### 无状态转换

无状态转化操作就是把简单的 RDD 转化操作应用到每个批次上，也就是转化 DStream 中的每一个 RDD (对 Dstream 的操作会映射到每个批次的 RDD 上)。无状态转换操作不会跨多个 batch 的 RDD 去执行（每个批次的 RDD 结果不能累加）

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/159.jpg" alt="image" style="zoom: 67%;" />

### 有状态转换

#### updateStateByKey 函数

有时我们需要在 DStream 中跨所有批次维护状态（例如跟踪用户访问网站的会话）。针对这种情况，updateStateByKey() 为我们提供了对一个状态变量的访问，用于键值对形式的 Dstream

使用 updateStateByKey 需要完成两步工作：

- 定义状态：可以是任意数据类型
- 定义状态更新函数：updateFunc

**update(events, oldState)**

- events：是在当前批次中收到的事件的列表（可能为空）
- oldState：是一个可选的状态对象，存放在 Option 内；如果一个键没有之前的状态，这个值可以空缺
- newState：由函数返回，也以 Option 形式存在；我们可以返回一个空的 Option 来表示想要删除该状态
  注意：有状态转化操作需要在你的 StreamingContext 中打开检查点机制来确保容错性 ssc.checkpoint("hdfs://...")

#### window 函数

基于窗口的操作会在一个比 StreamingContext 的批次间隔更长的时间范围内，通过整合多个批次的结果，计算出整个窗口的结果

所有基于窗口的操作都需要两个参数，分别为 windowDuration 以及 slideDuration，两者都必须是 StreamContext 的批次间隔的整数倍

```scala
//窗口时长为3个批次，滑动步长为2个批次；每隔2个批次就对前3个批次的数据进行一次计算
val ssc = new StreamingContext(sparkConf, Seconds(10))
…
val accessLogsWindow = accessLogsDStream.window(Seconds(30), Seconds(20))
val windowCounts = accessLogsWindow.count()
```

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/160.jpg" alt="image" style="zoom: 67%;" />

**普通规约和增量规约**

增量规约只考虑新进入窗口的数据和离开窗口的数据，让 Spark 增量计算归约结果。这种特殊形式需要提供归约函数的一个逆函数，比如 + 对应的逆函数为 -

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/161.jpg" alt="image" style="zoom: 67%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/162.jpg" alt="image" style="zoom: 67%;" />

### DStream 输出

常见输出操作

- print
  每个批次中抓取DStream 的前十个元素打印出来。
- foreachRDD
- saveAsObjectFiles（保存对象文件）
- saveAsTextFiles
- saveAsHadoopFiles

惰性求值

### 持久化操作

- 允许用户调用 persist 来持久化(将 Dstream 中的 RDD 持久化)

- 默认的持久化：`EMORY_ONLY_SER`

- 对于来自网络的数据源(Kafka, Flume, sockets 等)：`MEMORY_AND_DISK_SER_2`（2表示副本，因为网络数据不一定可靠）

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/163.jpg" alt="image"  />

- 对于 window 和 stateful 操作默认持久化（默认会将数据持久化）







