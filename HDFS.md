# HDFS 概述及其部属

## 1 HDFS 分布式文件系统

> HDFS (Hadoop Distributed File System) 是 Hadoop 项目的核心子项目，是分布式计算中数据存储管理的基础，是基于流数据模式访问和处理超大文件的需求而开发的，可以运行于廉价的商用服务器上
>
> 随着互联网的快速发展，数据量越来越多，一台服务器的存储有限，所以数据应该分配到更多的机器的磁盘中，因此需要一种系统来管理多台机器上的文件，这就是分布式文件管理系统 ，它具有高容错的特点，它可以部署在廉价的通用硬件上，提供高吞吐率的数据访问，适合那些需要处理海量数据集的应用程序

它具有如下特点：

- **高容错性**

  上传的**数据自动保存多个副本**。它是通过增加副本的数量，来增加它的容错性

  如果某一个副本丢失，HDFS 机制会复制其他机器上的副本，而我们不必关注它的实现

- **支持超大文件**

  超大文件在这里指的是几百MB、几百GB甚至几TB大小的文件，一般来说，一个Hadoop文件系统会存储T (1TB=1024GB)，P (1P=1024T)级别的数据

- **流式数据访问**
  HDFS 处理的数据规模都比较大，应用一次需要访问大量的数据。同时，这些应用一般是批量处理，而不是用户交互式处理。HDFS 使应用程序能够以流的形式访问数据集，**注重的是数据的吞吐量，而不是数据访问的速度**

- **简化的一致性模型**

  大部分的HDFS程序操作文件时需要一次写入，多次读取。在HDFS中，一个文件一旦经过创建、写入、关闭后，一般就不需要修改了。这样简单一致性模型，有利于提供高吞吐量的数据访问模型



## 2 HDFS 体系结构

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/016.jpg" alt="image"  />

- Client

  - 实现文件切分
  - 与 NameNode 交互，获取文件的位置信息
  - 跟 DataNode 交互，读取对应的数据块
  - 管理和访问 HDFS

- NameNode

  - 管理 HDFS 的名称空间
  - 管理数据块的映射关系
  - 配置副本策略
  - 客户端的读写请求

- DataNode

  - 存储实际的数据块
  - 执行数据块的读写操作

- Block(数据块)

  一个文件是被切分成多个 Block，并且每个 Block 有多个副本，这些副本被分布在多个 DataNode 上，它是 HDFS 的最小存储单元

- 元数据

  是文件系统中文件和目录的信息以及文件和 Block 的对应关系

- 命名空间镜像(FSlmage)

  HDFS 的目录树及文件/目录元信息是保存在内存中的，如果节点掉电或进程崩溃，数据将不再存在，必须将上述信息保存到磁盘，Fslmage 就是保存某一个时刻元数据的信息的磁盘文件

- 镜像编辑日志(EditLog)
  对内存目录树的修改，也必须同步到磁盘元数据上，但每次修改都将内存元数据导出到磁盘，显然是不现实的，为此，namenode引入了镜像编辑日志，将每次的改动都保存在日志中，如果namenode机器宕机或者namenode进程挂掉后可以使用 FSlmage 和 EditLog 联合恢复内存元数据



## 3 HDFS HA

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/017.jpg" alt="image" style="zoom:80%;" />

### NameNode HA工作原理

- 2台独立的机器的来配置 NameNode 角色，无论在任何时候，集群中只能有一个 NameNode 作为 Active 状态，而另一个是 Standby 状态。Active 状态的 NameNode 负责集群中所有的客户端操作，状态为 Standby 的 NameNode 这时候仅仅扮演一个 Slave 的角色，以便于在任何时候 Active 的 NameNode 挂掉时能够第一时间接替它的任务，成为主 NameNode，达到一个热备份的效果
- 为了保持从 NameNode 与主 NameNode 的元数据保持一致，他们之间的交互通过一系列守护的轻量级进程JournalNode 来完成，当任何修改操作在主 NameNode 上执行时，它同时也会记录修改 log 到至少半数以上的JornalNode 中，这时状态为 Standby 的 NameNode 监测到 JournalNode 里面的同步 log 发生变化了，会读取 JornalNode 里面的修改 log，然后同步到自己的的目录镜像树里面
- 当发生故障时，Active 的 NameNode 挂掉后，Standby 的 NameNode 会在它成为 Active NameNode 前，读取所有的 JournalNode 里面的修改日志，这样就能高可靠的保证与挂掉的 NameNode 的目录镜像树一致，然后无缝的接替它的职责，维护来自客户端请求，从而达到一个高可用的目的
- 为了达到快速容错掌握全局的目的，Standby 角色也会接受来自 DataNode 角色汇报的块信息

### ZKFailoverController

> ZKFailoverController 作为独立的进程运行，简称 ZKFC，对 NameNode 的主备切换进行总体控制。ZKFailoverController 能及时检测到 NameNode 的健康状况，在主 NameNode 故障时借助 Zookeeper 实现自动的主备选举和切换，每个运行 NameNode 的机器上都会运行一个 ZKFC

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/018.jpg" alt="image"  />

ZKFC的作用如下：

- **NameNode健康状况监控：**

  ZKFC 定期以 RPC 的方式向本地的 NameNode 发送健康检查命令，只要 NameNode 及时回复自己是健康的，那么 ZKFC 则认为 NameNode 是健康的。如果 NameNode 宕机、卡死，则 ZKFC 则认为 NameNode 是不健康的

- **ZK会话管理：**
  当本地的 NameNode 是健康的，ZKFC 则以心跳的方式保持一个会话。如果本地的 NameNode 是 Active 的，ZKFC 也在 ZK 上持有一个特殊 lock znode，如果 ZKFC 检测到 NameNode 时不健康的，则停止向 ZK 上报心跳，会话失效，Lock node 过期删除

- **ZK选举：**
  如果本地的 NameNode 是健康的，ZKFC 也没有看到其他 NameNode 持有lock znode，它将试着获取 lock
  znode，如果成功，即赢得了选举，则它本地的 namenode变为 active

ZKFC 为什么要作为一个 deamon 进程从 NN 分离出来？

- 防止因为 NN 的 GC(垃圾回收) 失败导致心跳受影响
- FailoverController 功能的代码应该和应用的分离，提高的容错性
- 使得主备选举成为可插拔式的插件

### 共享存储 QJM

> NameNode 记录了 HDFS 的目录文件等元数据，客户端每次对文件的增删改等操作，Namenode 都会记录一条日志，叫做 editlog，而元数据存储在 fsimage 中。为了保持 Stadnby 与 active 的状态一致，standby 需要尽量实时获取每条 editlog 日志，并应用到 FsImage 中。这时需要一个共享存储存放 editlog，standby 能实时获取日志

有两个关键点需要保证：

- 共享存储是高可用的
- 需要防止两个 NameNode 同时向共享存储写数据导致数据损坏

共享存储常用的方式是 QJM(Qurom Journal Manager)，它包含多个 JournalNode 

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/019.jpg" alt="image"  />



QJM 可以认为是包含一些 JournalNode 的集群，JournalNode 运行在不同的机器上，每个 JournalNode 是一个很轻量的守护进程，所以可以部署在 hadoop 集群的节点上，QJM 中至少要有3个 JournalNode，因为 edit log 必须要写到 JournalNodes 中大部分节点中，比如运行3、5、7个 JournalNode，如果你运行了 N个 JournalNode，那么系统可以容忍最多 (N-1)/2 个节点失败

共享存储实现逻辑：

- 初始化后，Active NN 把 editlog 写到大多数 JN 并返回成功（即大于等于N+1）即认定写成功
- Standby NN 定期从 JN 读取一批 editlog，并应用到内存中的 FsImage 中
- NameNode 每次写 Editlog 都需要传递一个编号 Epoch 给 JN，JN 会对比 Epoch，如果比自己保存的 Epoch 大或相同，则可以写，JN 更新自己的 Epoch 到最新，否则拒绝操作。在切换时，Standby 转换为 Active 时，会把 Epoch+1，这样就防止即使之前的 NameNode 向 JN 写日志，即使写也会失败

### 防止脑裂

确保只有一个 NN 能命令 DN

- 每个 NN 改变状态的时候，向 DN 发送自己的状态和一个序列号
- DN 在运行过程中维护此序列号，当 failover 时，新的 NN 在返回 DN 心跳时，会返回自己的 active 状态和一个更大的序列号。DN 接收到这个返回时，认为该 NN 为新的 active
- 如果这时原来的 active（比如GC）恢复，返回给 DN 的心跳信息包含active状态和原来的序列号，这时DN就会拒绝这个 NN 的命令



## 4 HDFS 安装部署

### 4.1 安装配置 Hadoop2.0

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

节点分配：

|             | hadoop01       | hadoop02       | hadoop03       |
| ----------- | -------------- | -------------- | -------------- |
| zookeeper   | QuorumPeerMain | QuorumPeerMain | QuorumPeerMain |
| namenode    | nn1            | nn2            |                |
| datanode    | dn1            | dn2            | dn3            |
| journalnode | jn1            | jn2            | jn3            |
| zkfc        | zkfc           | zkfc           |                |

- 下载 Hadoop 安装包

  Apache 版本下载地址：https://archive.apache.org/dist/hadoop/common/

  CDH 版本下载地址：http://archive-primary.cloudera.com/cdh5/cdh/5/

  下载对应版本 Hadoop，这里下载`hadoop-2.6.0-cdh5.10.0.tar.gz`版本，并上传至`/home/hadoop/app`目录下

- 解压 Hadoop：`tar -zxvf hadoop-2.6.0-cdh5.10.0.tar.gz`

- 创建软链接：`ln -s hadoop-2.6.0-cdh5.10.0/ hadoop`

- 修改配置文件，依次分别修改`core-site.xml`、`hdfs-site.xml`、`slaves`、`hadoop-env.sh`配置文件

  - 修改`core-site.xml`

    ```xml
    <configuration>
            <property>
                    <name>fs.defaultFS</name>
                    <value>hdfs://mycluster</value>
            </property>
            <!--默认的HDFS路径-->
            <property>
                    <name>hadoop.tmp.dir</name>
                    <value>/home/hadoop/data/tmp</value>
            </property>
            <!--hadoop的临时目录，如果需要配置多个目录，需要逗号隔开-->
            <property>
            <name>ha.zookeeper.quorum</name>
            <value>hadoop01:2181,hadoop02:2181,hadoop03:2181</value>
            </property>
            <!--配置Zookeeper 管理HDFS-->
    </configuration>
    ```

  - 修改`hdfs-site.xml`

    ```xml
    <configuration>
            <property>
                    <name>dfs.replication</name>
                    <value>3</value>
            </property>
                    <!--数据块副本数为3-->
            <property>
                    <name>dfs.permissions</name>
                    <value>false</value>
            </property>
            <property>
                    <name>dfs.permissions.enabled</name>
                    <value>false</value>
            </property>
                    <!--权限默认配置为false-->
            <property>
                    <name>dfs.nameservices</name>
                    <value>mycluster</value>
            </property>
            <property>
                    <name>dfs.ha.namenodes.mycluster</name>
                    <value>nn1,nn2</value>
            </property>
            <property>
                    <name>dfs.namenode.rpc-address.mycluster.nn1</name>
                    <value>hadoop01:9000</value>
            </property>
            <property>
                    <name>dfs.namenode.http-address.mycluster.nn1</name>
                    <value>hadoop01:50070</value>
            </property>
            <property>
                    <name>dfs.namenode.rpc-address.mycluster.nn2</name>
                    <value>hadoop02:9000</value>
            </property>
            <property>
                    <name>dfs.namenode.http-address.mycluster.nn2</name>
                    <value>hadoop02:50070</value>
            </property>
            <property>
                    <name>dfs.ha.automatic-failover.enabled</name>
                    <value>true</value>
            </property>
                    <!--启动故障自动恢复-->
            <property>
                    <name>dfs.namenode.shared.edits.dir</name>
                    <value>qjournal://hadoop01:8485;hadoop02:8485;hadoop03:8485/mycluster</value>
            </property>
                    <!--指定NameNode的元数据在JournalNode上的存放位置-->
            <property>
                    <name>dfs.client.failover.proxy.provider.mycluster</name>
                    <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
            </property>
                    <!--指定 mycluster 出故障时，哪个实现类负责执行故障切换-->
            <property>
                    <name>dfs.journalnode.edits.dir</name>
                    <value>/home/hadoop/data/journaldata/jn</value>
            </property>
                    <!-- 指定JournalNode在本地磁盘存放数据的位置 -->
            <property>
                    <name>dfs.ha.fencing.methods</name>
                    <value>shell(/bin/true)</value>
            </property>
                    <!-- 配置隔离机制,shell通过ssh连接active namenode节点，杀掉进程-->
            <property>
                    <name>dfs.ha.fencing.ssh.private-key-files</name>
                    <value>/home/hadoop/.ssh/id_rsa</value>
            </property>
                    <!-- 为了实现SSH登录杀掉进程，还需要配置免密码登录的SSH密匙信息 -->
            <property>
                    <name>dfs.ha.fencing.ssh.connect-timeout</name>
                    <value>10000</value>
            </property>
            <property>
                    <name>dfs.namenode.handler.count</name>
                    <value>100</value>
            </property>
    </configuration>
    ```

  - 修改`slave`

    ```xml
    hadoop01
    hadoop02
    hadoop03
    ```

  - 修改`hadoop-env.sh`

    ```sh
    export JAVA_HOME=/home/hadoop/app/jdk
    export HADOOP_HOME=/home/hadoop/app/hadoop
    ```

### 4.2 安装目录同步到其他节点

- 用`deploy.sh`脚本将 hadoop01 上安装配置好的目录分发到其他节点`deploy.sh hadoop-2.6.0-cdh5.10.0/ /home/hadoop/app/ slave`

- 各个节点分别创建软连接：`ln -s hadoop-2.6.0-cdh5.10.0/ hadoop`

### 4.3 启动并测试 HDFS

> `cd ~/app/hadoop/`目录下执行：

#### 第一次需要先格式化

-  启动所有 Zookeeper 节点

  `runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

-  启动所有 journalnode 节点

  `runRemoteCmd.sh "/home/hadoop/app/hadoop/sbin/hadoop-daemon.sh start journalnode" all`

- nn1 节点格式化 namenode：`bin/hdfs namenode -format`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/027.jpg" alt="image"  />

- nn1 节点格式化 zkfc：`bin/hdfs zkfc -formatZK`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/026.jpg" alt="image"  />

- nn1 节点启动 namenode：`bin/hdfs namenode`（先不要按 ctrl+c 关闭 namenode）

- nn2 节点同步 nn1 节点元数据信息（在 hadoop02 下执行）

  - `bin/hdfs namenode -bootstrapStandby`
  - 在 hadoop01 下按 ctrl+c 关闭 namenode

- 关闭所有节点 journalnode

  `runRemoteCmd.sh "/home/hadoop/app/hadoop/sbin/hadoop-daemon.sh stop journalnode" all`

#### 常规启动

- **一键启动 hdfs**：`sbin/start-dfs.sh`

  **一键启动 hdfs**：`sbin/stop-dfs.sh`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/025.jpg" alt="image"  />

- hdfs 启动之后可以通过如下命令查看 namenode 状态：

  - `bin/hdfs haadmin -getServiceState nn1`
  - `bin/hdfs haadmin -getServiceState nn2`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/028.jpg" alt="image"  />

- Web 界面查看 hdfs：

  - `http://192.168.62.201:50070`
  - `http://192.168.62.202:50070`
  - `http://192.168.62.203:50070`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/029.jpg" alt="image"  />

#### 常用命令

- 创建目录：`bin/hdfs dfs -mkdir /test`
- 查看目录：`bin/hdfs dfs -ls /`
- 上传文件到hdfs：`bin/hdfs dfs -put wc.txt /test`
- 查看文件：`bin/hdfs dfs -cat /test/wc.txt`