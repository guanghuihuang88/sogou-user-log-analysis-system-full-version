# HBase 概述及其部署

## 1 HBase 简介

> HBase 是构建在 HDFS 之上的分布式列存储数据库，是一个高可靠性、高性能、面向列、可伸缩的分布式存储系统，利用 HBase 技术可以在廉价 PC Server 上搭建起大规模结构化存储集群
> HBase 是 Google Bigtable 的开源实现，类似 Google Bigtable 利用 GFS 作为其文件存储系统，Google 运行 MapReduce 来处理 Bigtable 中的海量数据，HBase 同样利用 Hadoop MapReduce 来处理 HBase 中的海量数据；Google Bigtable 利用 Chubby 作为协同服务，HBase 利用 Zookeeper 作为对应

### 生态圈中的位置

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/020.jpg" alt="image"  />

### 特点

与传统数据库相比，HBase 具有如下特点：

- 大：单表可以数十亿行，数百万列
- 无模式：同一个表的不同行可以有截然不同的列
- 面向列：存储、权限控制、检索均面向列
- 稀疏：空列不占用存储，表是稀疏的
- 多版本：每个单元中的数据可以有多个版本，默认情况下版本号自动分配，是单元格插入时的时间戳
- 数据类型单一：数据都是字符串，没有类型

### 数据模型

| Row Key | version | ColumnFamily               | ColumnFamily   |
| ------- | ------- | -------------------------- | -------------- |
|         |         | URI                        | Parser         |
| r1      | t2      | url=http://www.taobao.com  | title=天天特价 |
|         | t1      | host=taobao.com            |                |
|         |         |                            |                |
| r2      | t5      | url=http://www.alibaba.com | content=每天…  |
|         | t4      | host=alibaba.com           |                |
|         |         |                            |                |

- 表（table）

  在 HBase 中数据是以表的形式存储的，通过表可以将某些列放在一起访问，同一个表中的数据通常是相关的，可以通过列簇进一步把列放在一起进行访问。用户可以通过命令行或者 Java API 来创建表，创建表时只需要指定表名和至少一个列簇

  HBase 的列式存储结构允许用户存储海量的数据到相同的表中，而在传统数据库中，海量数据需要被切分成多个表进行存储

- 行键（Row Key）

  Rowkey 既是 HBase 表的行键，也是 HBase 表的主键。HBase 表中的记录是按照 Rowkey 的字典顺序进行存储的

  在 HBase 中，为了高效地检索数据，需要设计良好的 Rowkey 来提高查询性能。首先 Rowkey 被冗余存储，所以长度不宜过长，Rowkey 过长将会占用大量的存储空间同时会降低检索效率；其次 Rowkey 应该尽量均匀分布，避免产生热点问题；另外需要保证 Rowkey 的唯一性

- 列簇（ColumnFamily）

  HBase表中的每个列都归属于某个列簇，一个列簇中的所有列成员有着相同的前缀

  比如，列 url 和 host 都是列簇 URI 的成员。列簇是表的 schema 的一部分，必须在使用表之前定义列簇，但列却不是必须的，写数据的时候可以动态加入。一般将经常一起查询的列放在一个列簇中，合理划分列簇将减少查询时加载到缓存的数据，提高查询效率，但也不能有太多的列簇，因为跨列簇访问是非常低效的

- 单元格

  HBase 中通过 Row 和 Column 确定的一个存储单元称为单元格（Cell）。每个单元格都保存着同一份数据的多个版本，不同时间版本的数据按照时间顺序倒序排序，最新时间的数据排在最前面

  为了避免数据存在过多版本造成的管理（包括存储和索引）负担，HBase 提供了两种数据版本回收方式。 一是保存数据的最后 n 个版本；二是保存最近一段时间内的数据版本，比如最近七天。用户可以针对每个列族进行设置

### 物理模型

- 每个 column family 存储在 HDFS 上的一个单独的文件里
- Rowkey 和 version 在每个 column family 里均有一份
- 空值不保存，占位符都没有

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/063.jpg" alt="image"  />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/064.jpg" alt="image"  />

- Table中的所有行都按照row key的字典序排列
- Table 在行的方向上分割为多个Region

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/065.jpg" alt="image"  />

Table 默认最初只有一个 Region，随着记录数不断增加而变大后，会逐渐分裂成多个 region，一个 region 由 [startkey,endkey] 表示，不同的 region 会被 Master 分配给相应的 RegionServer 进行管理

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/066.jpg" alt="image" style="zoom:;" />

Region 是 HBase 中分布式存储和负载均衡的**最小单元**。不同 Region 分布到不同 RegionServer 上

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/067.jpg" alt="image" style="zoom:;" />

- Region 虽然是分布式存储的最小单元，但并不是存储的最小单元
- Region 由一个或者多个 Store 组成，每个 store 保存一个 columns family
- 每个 Strore 又由一个 memStore 和0至多个 StoreFile 组成
- memStore 存储在内存中，StoreFile 存储在 HDFS 上

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/068.jpg" alt="image" style="zoom:;" />

## 2 HBase 系统架构

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/069.jpg" alt="image" style="zoom:;" />

HBase 采用 Master/Slave 架构搭建集群，由 HMaster节点、HRegionServer节点、ZooKeeper 集群组成，而在底层它将数据存储于 HDFS 中，因而涉及到 HDFS 的 NameNode、DataNode 等，每个 DataNode 上面最好启动一个 HRegionServer, 这样在一定程度上保持数据的本地性

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/070.jpg" alt="image" style="zoom:;" />

### Zookeeper

ZooKeeper 协调集群所有节点的共享信息，在 HMaster 和 HRegionServer 连接到 ZooKeeper 后创建 Ephemeral 节点，并使用 Heartbeat 机制维持这个节点的存活状态，如果某个 Ephemeral 节点失效，则HMaster 会收到通知，并做相应的处理
HMaster 通过监听 ZooKeeper 中的 Ephemeral 节点(默认：`/hbase/rs/*`)来监控 HRegionServer 的加入和宕机。在第一个 HMaster 连接到 ZooKeeper 时会创建 Ephemeral 节点(默认：`/hbasae/master`)来表示 Active 的 HMaster，其后加进来的 HMaster 则监听该 Ephemeral 节点，如果当前 Active 的 HMaster 宕机，则该节点消失，因而其他 HMaster 得到通知，而将自身转换成 Active 的 HMaster，在变为 Active 的 HMaster 之前，它会创建在`/hbase/back-masters/`下创建自己的 Ephemeral 节点

### Master

- 管理 HRegionServer，实现其 region 负载均衡
- 管理和分配 HRegion，在 HRegion split 时分配新的 HRegion；在 HRegionServer 退出时迁移其内的 HRegion 到其他 HRegionServer 上
- 监控集群中所有 HRegionServer 的状态
- 实现 DDL 操作（Data Definition Language，namespace 和 table 的增删改，columnfamiliy 的增删改等）
- 管理 namespace 和 table 的元数据（实际存储在 HDFS 上）

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/071.jpg" alt="image" style="zoom:;" />

### RegionServer

- Region server 维护 Master 分配给它的 region，处理对这些 region 的 IO 请求
- Region server 负责切分在运行过程中变得过大的 region
- HRegionServer 一般和 DN 在同一台机器上运行，实现数据的本地性
- HRegionServer 包含多个 HRegion，由 WAL(HLog)、BlockCache、MemStore、HFile 组成

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/072.jpg" alt="image" style="zoom:;" />

## 3 HBase 实际应用案例

> 1. 需对数据进行随机读操作或者随机写操作
> 2. 大数据上高并发操作，比如每秒对PB级数据进行上千次操作
> 3. 读写访问均是非常简单的操作

### 淘宝

交易历史记录查询系统

- 百亿行数据表，千亿级二级索引表
- 每天千万行更新
- 查询场景简单，检索条件较少
- 关系型数据库所带来的问题
- 基于userld + time + id rowkey设计
- 成本考虑

### 移动

某移动上网日志查询系统

- 建设目标

  实现客户上网消费账单透明化，即可针对客户投诉或流量质疑等诸多相关数据流量问题实现流量账单明细级查询

- 集群介绍

  - 集群规模：2台namenode，25台datanode，2台web查询服务
  - 存储周期：3 + 1，3个月历史数据，1个月实时数据
  - 每月存储量：45T GPRS上网清单日志
  - 查询响应时间：平均在300ms左右

- Rowkey设计：

  MD5(手机号码)取前三位 + 手机号码 + 开始时间(到秒)+ 唯一标识，如：

  f9a1860113421020140624234531f059b84aba2e6095

  唯一标识是对整条记录取 md5 值，防止 rowkey 重复

- 建表原则：

  每月定期建表，表名为 GPRS_yyyyMM，如：GPRS_201407

- 预建分区：

  提前对表进行分区，防止 region 分裂，北京移动生产环境每张表800个分区

- 建表方式：

  通过crontab定期建未来三个月表，包括 hbase 表、phoenix 表

- 压缩方式：

  采用 SNAPPY + PefixTree 的压缩方式，HFile 文件压缩采用 SNAPPY，dataBlock 压缩采用 PrefixTree，压缩
  比大概为1:1.6

## 4 HBase 分布式集群部署与设计

### 4.1 HBase 安装前须知

**必备条件**

- 硬件

  HBase 是 Java 编写的，需要支持当前的 Java 运行时环境。Region 服务器的内存主要服务于内部数据结构，例如 MemStore 和 BlockCache，因此需要安装64位操作系统才能分配和使用大于 4G 的内存空间。HBase 与 Hadoop 安装在一起，能实现数据本地化，很大程度地减少网络I/O的需求，同时加快处理速度。要有效运行所有 Hadoop 和 HBase 进程，需要拥有一定数量的磁盘、CPU 和内存资源

- 磁盘

  因为数据存储在 datanode（RegionServer）节点上，因此服务器需要大量的存储空间。通常使用的磁盘大
  小是 1TB 或者 2TB 的磁盘，而且根据数据规模和存储周期可以挂载多块磁盘（2T*5）

- CPU

  CPU 使用单核 CPU 机器，同时运行3个或者更多的 Java 进程和操作系统的服务进程效率比较低。在生产环境中，通常采用的是多核处理器，比如说四核处理器或者六核处理器，所以系统可以选择双路四核或者六核，这样基本可以保证每个 Java 进程都可以独立占用一个核

- 内存

  给单个进程配置内存是不是越多越好呢？实践证明，使用 Java 时不应该为一个进程设置过多的内存。内存在 Java 术语中称为堆（heap），会在使用过程中产生许多碎片，在最坏情况下，Java运行时环境会暂停所有进程内的逻辑并进行清理，设置的堆越大，这个过程花费的时间就越长

  其实进程并不需要大量的内存，合适的内存可以避免上述问题，但是 region 服务器和 BlockCache 在理论上没有上限

- 软件

  - SSH

    如果用户需要通过脚本来管理 Hadoop 与 HBase 进程必须安装 ssh，而且要求用户在 master 节点通过 ssh 免密码登录其他 slave 节点

  - Java

    需要安装 jdk 才能运行 HBase，jdk 推荐安装 jdk1.7 及以上版本对 HBase 支持较好

  - 时钟同步

    集群中节点的时间必须是一致的，稍微有一点时间偏差是可以容忍的，但是偏差较多会产生一些奇怪的行为，比如 Hmaster、HRegionServer 节点上线之后很快消失等

**运行模式**

HBase 有两种运行模式：单机模式和分布式模式

- 单机模式

  单机模式是默认模式，在单机模式中，HBase 并不使用 HDFS，Zookeeper 程序与 HBase 程序运行在同一个 JVM 进程中，Zookeeper 绑定到客户端的常用端口上，以便客户端可以与 HBase 进程通信

- 分布式模式

  分布式模式又分为：**伪分布式模式**和**完全分布式模式**。伪分布式模式，所有守护进程都运行在单个节点上。完全分布式模式，进程运行在物理服务器集群中

### 4.2 HBase 安装部署

#### 集群规划

- 主机规划

  |               | hadoop01 | hadoop02 | hadoop03 |
  | ------------- | -------- | -------- | -------- |
  | Namenode      | √        | √        |          |
  | Datanode      | √        | √        | √        |
  | HMaster       | √        | √        |          |
  | HRegionServer | √        | √        | √        |
  | Zookeeper     | √        | √        | √        |

- 软件规划

  | 软件      | 版本                             | 位数 |
  | --------- | -------------------------------- | ---- |
  | JDK       | 1.8                              | 64   |
  | Centos    | 6.5                              | 64   |
  | Zookeeper | zookeeper-3.4.5-cdh5.10.0.tar.gz | 稳定 |
  | HBase     | hbase-1.2.0-cdh5.10.0.tar.gz     | 稳定 |
  | Hadoop    | hadoop-2.6.0-cdh5.10.0.tar.gz    | 稳定 |

- 用户规划

  | 节点名称 | 用户组 | 用户   |
  | -------- | ------ | ------ |
  | Hadoop01 | Hadoop | Hadoop |
  | Hadoop02 | Hadoop | Hadoop |
  | Hadoop03 | Hadoop | Hadoop |

- 目录规划

  | 名称         | 路径                         |
  | ------------ | ---------------------------- |
  | 所有软件目录 | /home/hadoop/app             |
  | 脚本目录     | /home/hadoop/tools           |
  | 日志目录     | /home/hadoop/data/hbase/logs |
  | Pids 目录    | /home/hadoop/data/hbase/pids |

#### HBase安装

- 下载安装包

  Apache 版本：http://archive.apache.org/dist/hbase/

  CDH 版本：http://archive-primary.cloudera.com/cdh5/cdh/5/

  这里选择下载 hbase-1.2.0-cdh5.10.0.tar.gz 版本的安装包，用 Xftp 上传至主节点 app 目录

- 解压安装包：`tar -zxvf hbase-1.2.0-cdh5.10.0.tar.gz`
- 创建软连接：`ln -s hbase-1.2.0-cdh5.10.0 hbase`

#### 修改配置文件

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

- 修改`hbase-site.xml`：

  在 HBase 中，用户需要增加配置信息，就需要将配置添加到`conf/hbase-site.xml`文件中，进程启动后，服务器会先读取`hbase-default.xml`文件，然后读取`hbase-site.xml`文件，`hbase-site.xml`的内容会覆盖`hbase-default.xml`中的内容。每次修改配置文件需要重启进程才能得到最新的配置

  ```xml
  <property>
      <name>hbase.zookeeper.quorum</name>
      <value>hadoop01,hadoop02,hadoop03</value>
      <!--指定Zookeeper集群节点-->
  </property>
  <property>
      <name>hbase.zookeeper.property.dataDir</name>
      <value>/home/hadoop/data/zookeeper/zkdata</value>
      <!--指定Zookeeper数据存储目录-->
  </property>
  <property>
      <name>hbase.zookeeper.property.clientPort</name>
      <value>2181</value>
      <!--指定Zookeeper端口号-->
  </property>
  <property>
      <name>hbase.rootdir</name>
      <value>hdfs://mycluster/hbase</value>
      <!--指定HBase在HDFS上的根目录-->
  </property>
  <property>
      <name>hbase.cluster.distributed</name>
      <value>true</value>
      <!--指定true为分布式集群部署-->
  </property>
  ```

- 修改`hbase-env.sh`：

  HBase 环境变量等信息需要在这个文件中配置，例如 HBase 守护进程的 JVM 启动参数。还可以设置 HBase 配置文件的目录、日志目录、进程 pid 文件的目录等

  ```shell
  export JAVA_HOME=/home/hadoop/app/jdk
  export HBASE_LOG_DIR=/home/hadoop/data/hbase/logs
  export HBASE_PID_DIR=/home/hadoop/data/hbase/pids
  export HBASE_MANAGES_ZK=false
  ```

- 修改`RegionServer`：

  这个文件罗列了所有 region 服务器的主机名，它是纯文本文件，文件中的每一行都是主机名。HBase 的运维脚本会依次迭代访问每一行来启动所有 region 服务器进程

  ```
  hadoop01
  hadoop02
  hadoop03
  ```

- 修改`backup-masters`：

  这个文件配置 HBase master 的备用节点，它是纯文本文件，每行填写主机名即可

  ```
  hadoop02
  ```

- 添加 hdfs 配置文件：

  因为 HBase 启动依赖 hdfs 配置信息，需要将 hdfs 配置文件拷贝到主节点 hbase 的 conf 目录下

  - `cp core-site.xml /home/hadoop/app/hbase/conf/`
  - `cp hdfs-site.xml /home/hadoop/app/hbase/conf/`

#### 同步目录到其他节点

- 利用`deploy.sh`脚本同步 HBase 安装目录到其他节点：
  - `deploy.sh hbase-1.2.0-cdh5.10.0 /home/hadoop/app/ slave`
  - 分别创建软链接：`ln -s hbase-1.2.0-cdh5.10.0 hbase`

- 创建规划目录

  在所有节点创建之前规划好的目录：`runRemoteCmd.sh "mkdir -p /home/hadoop/data/hbase/logs" all`

#### 启动/关闭 HBase

- 启动 Zookeeper（集群依赖 Zookeeper 集群）
  `runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 启动 hdfs（Hbase 数据都存储在 hdfs 上，依赖 hdfs 集群）

  `sbin/start-dfs.sh`

- 启动 HBase（在哪个节点启动 HBase，哪个节点就是 master 角色）

  `bin/start-hbase.sh`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/073.jpg" alt="image" style="zoom:;" />

- 关闭 HBase：运行以下命令可以停止 HBase 集群，一旦启动了这个脚本，我们将会看到一条描述集群正在
  停止的信息，该信息会周期性地打印”. ”字符。（这仅仅表明脚本正在运行，并不是进度的反馈或者隐藏有用的信息）

  `bin/stop-hbase.sh`

  关闭脚本大概需要几分钟完成，如果集群机器比较多，需要执行更长的时间。如果用户运行的是分布式集群，在关闭 hadoop 集群之前一定要确认 hbase 已经被正常关闭

- web 查看 hbase：`http://hadoop01:60010`

#### Shell 测试运行 HBase 数据库

> HBase 为用户提供了一个非常方便的使用方式，我们称之为“HBase Shell”
>
> HBase Shell 提供了大多数的 HBase 命令，通过 HBase Shell 用户可以方便地创建、删除及修改表，还可以向表中添加数据、列出表中的相关信息等
>
> 注意，HBase Shell 命令行中，“<—”是向后删除，正常删除需要“ctrl + <—”

- 启动：`bin/hbase shell`
- 查看命令组：`hbase> help`
- 查询 HBase 服务器状态：`hbase> status`
- 查看 HBase 版本：`hbase> version`
- 退出：`exit`

**DDL操作**：(Data Definition Language)数据定义语言

- 创建一个表：`create 'myhbase','cf'`
- 查看 HBase 所有表：`list`
- 描述表结构：`describe 'myhbase'`
- 删除表：
  - `disable 'myhbase'`
  - `drop 'myhbase'`

**DML操作**：(Data Manipulation Language)数据操纵语言

- 先创建一个表：`create 'user','cf'`

- 插入一条数据：`put 表名 rowkey 列名 数据值`

  ```hbase shell
  put 'user', '1', 'cf:name', 'xiaoli'
  put 'user', '1', 'cf:age', '24'
  put 'user', '1', 'cf:birthday', '1987-06-17'
  put 'user', '1', 'cf:company', 'alibaba'
  put 'user', '1', 'cf:contry', 'china'
  put 'user', '1', 'cf:province', 'zhejiang'
  put 'user', '1', 'cf:city', 'hangzhou'
  ```

- 扫描表所有数据：`scan 'user'`

- 根据 rowkey 获取数据：`get 'user','1'`

- 根据 rowkey 更新一条数据：`put 'user', '1', 'cf:age', '28'`

- 查询表中总记录数据：`count 'user'`

- 删除某一列数据：` delete 'user', '1', 'cf:age'`

- 清空 hbase 表数据：`truncate 'user'`

### 4.3 HBase 搜狗项目业务建模

> 业务数据下载地址：http://www.sogou.com/labs/resource/q.php

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/074.jpg" alt="image" style="zoom:;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/075.jpg" alt="image" style="zoom:;" />

- 数据格式为：
  访问时间\t用户 ID\t[查询词]\t该 URL 在返回结果中的排名\t用户点击的顺序号\t用户点击的 URL
  其中，用户 ID 是根据用户使用浏览器访问搜索引擎时的 Cookie 信息自动赋值，即同一次使用浏览器输入的不同查询对应同一个用户 ID

- 创建好搜狗业务表：`create 'sogoulogs','info'`











