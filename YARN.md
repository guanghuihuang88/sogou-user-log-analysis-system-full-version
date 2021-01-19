# YARN 概述及其部属

## 1 YARN 概述

> YARN 是 Hadoop2.x 版本新引入的资源管理系统，直接从 MR1 演化而来。Apache Hadoop YARN （Yet Another Resource Negotiator，另一种资源协调者）是一种新的 Hadoop 资源管理器，它是一个通用资源管理系统，可为上层应用提供统一的资源管理和调度，它的引入为集群在利用率、资源统一管理和数据共享等方面带来了巨大好处

核心思想：将 MR1 中 JobTracker 的资源管理和作业调度两个功能分开，分别由 ResourceManager 和 ApplicationMaster 进程来实现

- ResourceManager：负责整个集群的资源管理和调度
- ApplicationMaster：负责应用程序相关的事务，比如任务调度、任务监控和容错等

YARN 的出现，使得多个计算框架运行在一个集群当中

- 每个应用程序对应一个 ApplicationMaster
- 目前可以支持多种计算框架运行在YARN上面，比如 MapReduce、Storm、Spark、Flink

### 在生态圈中的位置

> 如果说 HDFS 相当于数据库，那么 YARN 就相当于操作系统，为上层应用（MapReduce、Hive、HBase、Spark等）提供接口

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/020.jpg" alt="image"  />

###  与 MapReduce 关系

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/021.jpg" alt="image"  />

- YARN 是一个资源管理系统，负责资源管理和调度
- MapReduce 只是运行在 YARN 上的一个应用程序
- 如果把 YARN 看做 "android"，则 MapReduce 只是一个 "app"
- MapReduce 1.0 是一个独立的系统，直接运行在 Linux 之上
- MapReduce 2.0 则是运行 YARN 上的框架，且可与多种框架一起运行在 YARN 上



## 2 YARN 系统架构

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/022.jpg" alt="image"  />

- ResourceManager

  ResourceManager 负责集群中所有资源的统一管理和分配，它接收来自各个节点(NodeManager)的资源汇报信息，并把这些信息按照一定的策略分配给各个应用程序

  ResourceManager 是整个 YARN 集群中最重要的组件之一，它的设计直接决定了系统的可扩展性、可用性和容错性等特点，它的功能较多，包括ApplicationMaster管理(启动、停止等)、NodeManager管理、Application管理、状态机管理等

  概括起来，ResourceManage 主要完成以下几个功能：

  - 与客户端交互，处理来自客户端的请求
  - 启动和管理 ApplicationMaster，并在它运行失败时重新启动它
  - 管理 NodeManager，接收来自 NodeManager 的资源汇报信息，下达管理指令(比如杀死Container等)
  - 资源管理与调度，接收来自 ApplicationMaster 的资源申请请求并命令 NodeManager 为之分配资源

- NodeManager

  NodeManager 是运行在单个节点上的代理，管理 Hadoop 集群中单个计算节点，它需要与应用程序的 ApplicationMaster 和集群管理者 ResourceManager 交互

  - 从ApplicationMaster 上接收有关 Container 的命令并执行(比如启动、停止Container )
  - 向ResourceManager 汇报各个 Container 运行状态和节点健康状况，并领取有关 Container 的命令(比如清理Container)并执行

- ApplicationMaster

  ApplicationMaster 它是与应用程序相关的组件

  - 负责数据的切分，把每份数据分配给对应的 Map Task
  - 为应用程序申请资源并进一步分配给内部的任务。比如从 ResourceManager 获取分配的资源，然后分配给 Task 任务
  - 任务的监控与容错。一旦一个任务挂掉之后，它可以重新向 ResourceManager 申请资源

- Container

  Container 是 YARN 中的资源抽象，它封装了某个节点上的多维度资源，如内存、CPU、磁盘、网络等，当AM 向 RM 申请资源时，RM 为 AM 返回的资源便是用 Container 表示的。YARN 会为每个任务分配一个Container，且该任务只能使用该 Container 中描述的资源。需要注意的是，Container 不同于 MRv1 中的 slot，它是一个动态资源划分单位，是根据应用程序的需求动态生成的



## 3 MapReduce ON YARN

运行在 YARN 上的应用程序主要分为两类：短应用程序和长应用程序

- **短应用程序**

  是指一定时间内可运行完成并正常退出的应用程序，比如 MapReduce 作业

- **长应用程序**

  是指不出意外，永不终止运行的应用程序，通常是一些服务，比如 Storm Service (主要包括 Nimbus 和
  Supervisor 两类服务)，HBase Service (包括 Hmaste 和 RegionServer 两类服务)等，而它们本身作为一个
  框架提供了编程接口供用户使用

当用户向YARN中提交一个应用程序后，YARN将分两个阶段运行该应用程序：

- 第一个阶段是启动 ApplicationMaster
- 第二个阶段是由 ApplicationMaster 创建应用程序，为它申请资源，并监控它的整个运行过程，直到运行
  完成

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/023.jpg" alt="image" style="zoom: 67%;" />

## 4 YARN HA

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/024.jpg" alt="image" style="zoom: 67%;" />

- MasterHADaemon：

  与 Master 服务运行在同一个进程中，可接收外部 RPC 命令，以控制 Master 服务的启动和停止

- SharedStorage：

  共享存储系统，Active Master 将信息写人共享存储系统，而 Standby Master 则读取该信息以保持与 Active Master 的同步

- ZKFailoverController：

  基于 Zookeeper 实现的切换控制器，主要由 ActiveStandbyElector 和 HealthMonitor 两个核心组件构成。其中，ActiveStandbyElector 负责与 Zookeeper  集群交互，通过尝试获取全局锁，以判断所管理的 Master 是进人 Active 还是进人 Standby 状态。HealthMonitor 负责监控各个活动 Master 的状态，以根据它们状态进 行状态切换

- Zookeeper：

  核心功能是通过维护一把全局锁控制整个集群有且仅有一个 Active Master。当然，如果 SharedStorge 采用了 Zookeeper，则还会记录一些其他状态和运行时信息



## 5 YARN 安装部署

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

|                 | hadoop01 | hadoop02 | hadoop03 |
| --------------- | -------- | -------- | -------- |
| ResourceManager | rm1      | rm2      |          |
| NodeManager     | nm       | nm       | nm       |



### 修改配置文件

- 修改`yarn-site.xml`

  ```xml
  <configuration>
  <property>
  	<name>yarn.resourcemanager.connect.retry-interval.ms</name>
  	<value>2000</value>
  </property>
  <property>
  	<name>yarn.resourcemanager.ha.enabled</name>
  	<value>true</value>
  </property>
  <!--打开高可用-->
  <property>
  	<name>yarn.resourcemanager.ha.automatic-failover.enabled</name>
  	<value>true</value>
  </property>
  <!--启动故障自动恢复-->
  <property>
  	<name>yarn.resourcemanager.ha.automatic-failover.embedded</name>
  	<value>true</value>
  </property>
  <!--rm启动内置选举active-->
  <property>
  	<name>yarn.resourcemanager.cluster-id</name>
  	<value>yarn-rm-cluster</value>
  </property>
  <!--给yarn cluster 取个名字yarn-rm-cluster-->
  <property>
  	<name>yarn.resourcemanager.ha.rm-ids</name>
  	<value>rm1,rm2</value>
  </property>
  <!--ResourceManager高可用 rm1,rm2-->
  <property>
  	<name>yarn.resourcemanager.hostname.rm1</name>
  	<value>hadoop01</value>
  </property>
  <property>
  	<name>yarn.resourcemanager.hostname.rm2</name>
  	<value>hadoop02</value>
  </property>
  <property>
  	<name>yarn.resourcemanager.recovery.enabled</name>
  	<value>true</value>
  </property>
  <!--启用resourcemanager 自动恢复-->
  <property>
  	<name>yarn.resourcemanager.zk.state-store.address</name>
  	<value>hadoop01:2181,hadoop02:2181,hadoop03:2181</value>
  </property>
  <!--状态存储地址-->
  <property>
  	<name>yarn.resourcemanager.zk-address</name>
  	<value>hadoop01:2181,hadoop02:2181,hadoop03:2181</value>
  </property>
  <!--配置Zookeeper地址-->
  <property>
  	<name>yarn.resourcemanager.address.rm1</name>
  	<value>hadoop01:8032</value>
  </property>
  <!--rm1端口号-->
  <property>
  	<name>yarn.resourcemanager.scheduler.address.rm1</name>
  	<value>hadoop01:8034</value>
  </property>
  <!-- rm1调度器的端口号-->
  <property>
  	<name>yarn.resourcemanager.webapp.address.rm1</name>
  	<value>hadoop01:8088</value>
  </property>
  <!-- rm1 webapp端口号-->
  <property>
  	<name>yarn.resourcemanager.address.rm2</name>
  	<value>hadoop02:8032</value>
  </property>
  <property>
  	<name>yarn.resourcemanager.scheduler.address.rm2</name>
  	<value>hadoop02:8034</value>
  </property>
  <property>
  	<name>yarn.resourcemanager.webapp.address.rm2</name>
  	<value>hadoop02:8088</value>
  </property>
  <property>
  	<name>yarn.nodemanager.aux-services</name>
  	<value>mapreduce_shuffle</value>
  </property>
  <property>
  	<name>yarn.nodemanager.aux-services.mapreduce_shuffle.class</name>
  	<value>org.apache.hadoop.mapred.ShuffleHandler</value>
  </property>
  <!--执行MapReduce需要配置的shuffle过程-->
  </configuration>
  ```

- 修改`mapred-site.xml`

  ```xml
  <configuration>
  <property>
  	<name>mapreduce.framework.name</name>
  	<value>yarn</value>
  </property>
  <!--MapReduce以yarn模式运行-->
  </configuration>
  ```

- 利用`deploy.sh`脚本将配置文件同步到其他节点

  - `deploy.sh yarn-site.xml /home/hadoop/app/hadoop/etc/hadoop slave`
  - `deploy.sh mapred-site.xml /home/hadoop/app/hadoop/etc/hadoop slave`

### 一键启动YARN

- 一键启动 yarn 集群：`sbin/start-yarn.sh`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/030.jpg" alt="image"  />

- （在 hadoop02 上）启动备用节点的 RM：`sbin/yarn-daemon.sh start resourcemanager`

-  查看 RM 状态：

  - `bin/yarn rmadmin -getServiceState rm1`
  - `bin/yarn rmadmin -getServiceState rm2`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/031.jpg" alt="image"  />

- web 界面查看 yarn：

  - `http://192.168.62.201:8088`
  - `http://192.168.62.202:8088`
  - `http://192.168.62.203:8088`

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/032.jpg" alt="image"  />



