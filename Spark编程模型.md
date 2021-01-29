# Spark 编程模型

**回顾 MapReduce 计算过程**

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/130.jpg" alt="image" style="zoom:80%;" />

**MapReduce VS Spark**

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/131.jpg" alt="image" style="zoom:80%;" />

## 1 核心概念

- **Application**：基于 Spark 的用户程序，包含了 driver 程序和集群上运行的 executor
- **Driver Program**：运行 main 函数并且新建 SparkContext 的程序
- **Executor**：是在一个 worker node 上为某应用启动的一个进程，该进程负责运行任务，并且负责将数据存在内存或者磁盘上。每个应用都有各自独立的 executors（多个）
- **Cluster Mmanager**：在集群上获取资源的外部服务（例如：standalone，Mesos，Yarn）
- **Worker Node**：集群中任何可以运行应用代码的节点
- **Task**：被送到某个 executor 上的工作单元
- **Job**：包含很多任务的并行计算，可以看作和 Spark 的 action 对应
- **Stage**：一个 Job 会被拆分很多组任务，每组任务被称为 Stage（就像 MapReduce 分 Map 任务和 Reduce 任务一样）

## 2 Application 编程模型

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/132.jpg" alt="image" style="zoom:80%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/133.jpg" alt="image" style="zoom:80%;" />

- Driver Program （ SparkContext ）
- Executor （ RDD 操作）
  - 输入：Base -> RDD
  - Transformation：RDD -> RDD
  - Action：RDD -> driver or Base
- 共享变量
  - broadcast variables（广播变量）
  - accumulators（累加器）

## 3 RDD 弹性分布式数据集

RDD 是 Spark 提供的核心抽象，全名叫做弹性分布式数据集(Resillient Distributed DataSet)。很抽象，很难懂，
但是是核心。根据RDD这个名字，可以分解为三点：

- 数据集：RDD 在抽象上说就是一种元素集合，单从逻辑上的表现来说，它就是一个数据集合。 咱们就把
  它简单的理解成为 java 里面的 list 或者数据库里面的一张表。一个 RDD 就是一个 hdfs 文件
- 分布式：因为 RDD 是分区的，每个分区分布在集群中不同的节点上，从而让 RDD 中的数据可以并行的操作
- 弹性：RDD 默认情况下是存放在内存中，但是在内存中资源不足时，spark 会自动将 RDD 数据写入磁盘进
  行保存。对于用户来说， 我不用去管 RDD 的数据存储在哪里，内存还是磁盘，这是 spark 底层去做的。我
  们只需要针对 RDD 来进行计算和处理就行了。RDD 自动进行内存和磁盘之间权衡和切换的机制，是 RDD
  的弹性的特点所在
  RDD 还有一个特性就是提供了容错性，可以自动从节点失败中恢复过来。即如果某个节点上的 RDD
  partition 因为节点故障，导致数据丢失了，不用担心，RDD 会自动通过自己的数据来源重新计算该分区。当然，这是Spark底层自己做的，对用户透明

### RDD 本质

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/134.jpg" alt="image" style="zoom: 67%;" />

### RDD 依赖

RDD 之间的依赖关系分为两类：

- 窄依赖

  每个父 RDD 的分区都至多被一个子 RDD 的分区使用，即为 OneToOneDependecies

- 宽依赖

  多个子 RDD 的分区依赖一个父 RDD 的分区，即为 ShuffleDependency 。 例如，map 操作是一种窄依赖，而 join 操作是一种宽依赖（除非父 RDD 已经基于 Hash 策略被划分过了，co-partitioned）

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/135.jpg" alt="image" style="zoom:80%;" />

相比于宽依赖，窄依赖对优化很有利 ，主要基于以下两点：

- 第一点：

  宽依赖往往对应着 shuffle 操作，需要在运行过程中将同一个父 RDD 的分区传入到不同的子 RDD 分区中，中间可能涉及多个节点之间的数据传输；而窄依赖的每个父 RDD 的分区只会传入到一个子 RDD 分区中，通常可以在一个节点内完成转换

- 第二点：

  - 当 RDD 分区丢失时（某个节点故障），spark 会对数据进行重算

  - 对于窄依赖，由于父 RDD 的一个分区只对应一个子 RDD 分区，这样只需要重算和子 RDD 分区对应的父 RDD 分区即可，所以这个重算对数据的利用率是100%的

  - 对于宽依赖，重算的父 RDD 分区对应多个子 RDD 分区，这样实际上父 RDD 中只有一部分的数据是被用于恢复这个丢失的子 RDD 分区的，另一部分对应子 RDD 的其它未丢失分区，这就造成了多余的计算；另外，宽依赖中子 RDD 分区通常来自多个父 RDD 分区，极端情况下，所有的父 RDD 分区都要进行重新计算

  - （如下图所示，b1分区丢失，则需要重新计算a1,a2和a3，这就产生了冗余计算(a1,a2,a3中对应b2的数
    据)

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/136.jpg" alt="image"  />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/137.jpg" alt="image"  />

- 窄依赖相比宽依赖更高效，资源消耗更少
  - 允许在单个集群节点上流水线式执行，这个节点可以计算所有父级分区。例如，可以逐个元素地依次执行 filter 操作和 map 操作（多步操作在同一个节点可以执行，否则第一步和第二步不在一个节点操作）
  - 而宽依赖则需要首先计算好所有父分区数据，然后在节点之间进行 Shuffle，这与 MapReduce 类似（map 都计算完毕，才计算 reduce）
- 在窄依赖中，节点失败后的恢复更加高效
  - 因为只有丢失的父级分区需要重新计算，并且这些丢失的父级分区可以并行地在不同节点上重新计算
  - 而对于一个宽依赖关系的 Lineage 图，单个节点失效导致这个 RDD 丢失部分分区，可能所有祖先 RDD 因此需要整体重新计算

### RDD 创建

- 从集合创建 RDD

  - 创建方式一：`parallelize`

    ```scala
    val arr = Array(1, 2, 3, 4, 5 ,6 )
    val paraRDD = sc.parallelize(arr)
    val list = List(1, 2, 3, 4, 5 ,6 )
    val listRDD = sc.parallelize(list)
    ```

  - 创建方式二：`makeRDD`

    ```scala
    val arr = Array(1, 2, 3, 4, 5 ,6 )
    val paraRDD = sc.makeRDD(arr)
    val list = List(1, 2, 3, 4, 5 ,6 )
    val listRDD = sc.makeRDD(list)
    ```

- 读取外部存储创建RDD

  - 多文件格式支持

    ```scala
    val localFile = sc.textFile("/home/hadoop/app/spark/test.txt")
    ```

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/138.jpg" alt="image"  />

  - 多系统支持
    - 本地文件系统
    - S3
    - HDFS
    - Kafka

### RDD 算子

RDD 的两种核心操作算子：**Transformation**（变换）与 **Action**（行动）

- Spark 的输入、 运行转换、 输出

  在运行转换中通过算子对 RDD 进行转换。算子是 RDD 中定义的函数，可以对 RDD 中的数据进行转换和操作

- **Transformation**

  Transformation操作是延迟计算（惰性求值）的，也就是说从一个 RDD 转换生成另一个 RDD 的转换操作不是马上执行，需要等到有 Actions 操作时，才真正触发运算。此时 RDD 的 Transformation 操作只是返回新 RDD 的操作而已

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/139.jpg" alt="image"  />

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/140.jpg" alt="image"  />

- **Action**

  本质上，在 Actions 算子中，通过 SparkContext 执行提交作业的 run Job 操作，此时触发了 RDD DAG 的执行

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/141.jpg" alt="image"  />

- 控制操作

  - persist操作

    可以将 RDD 持久化到不同层次的存储介质，以便后续操作重复使用
    cache:RDD[T]
    persist:RDD[T]
    Persist(level:StorageLevel):RDD[T]

  - checkpoint

    将 RDD 持久化到 HDFS 中，与persist操作不同的是 checkpoint 会切断此 RDD 之前的依赖关系，而 persist 依然保留 RDD 的依赖关系

## 4 PairRDD

包含键值对类型的 RDD 被称作 Pair RDD

- Pair RDD 通常用来进行聚合计算
- Pair RDD 通常由普通 RDD 做 ETL 转换而来

### PairRDD Transformation

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/142.jpg" alt="image"  />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/143.jpg" alt="image"  />

### PairRDD Action

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/144.jpg" alt="image"  />

## 5 Spark topn实现

- `val fileRDD = sc.textFile("/home/hadoop/app/spark/test.txt")`

- `val lines = fileRDD.flatMap(_.split(\\s+")).map((_,1)).reduceByKey(_+_).map(t =>(t._2,t._1)).sortByKey(false).map(t =>(t._2,t._1))`
- `lines.take(5).foreach(println)`

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/145.jpg" alt="image"  />







