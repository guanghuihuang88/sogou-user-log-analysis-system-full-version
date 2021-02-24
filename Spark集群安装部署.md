# Spark 集群安装部署

| 运行模式          | 说明                                                   |
| ----------------- | ------------------------------------------------------ |
| Local[N]模式      | 本地模式，使用N个线程                                  |
| Local Cluster模式 | 伪分布式模式，可配置所需的worker,core,memory           |
| Standalone模式    | 需要启动Spark的自己的运行时环境，spark://hostname:port |
| Mesos模式         | mesos://hostname:port，需要部署Spark和Mesos到相关节点  |
| YARN cluster模式  | 需要部署YARN，Driver运行在集群中                       |
| YARN client模式   | 需要部署YARN，Drive运行在本地                          |

## 1 Spark Standalone 运行模式

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/146.jpg" alt="image" style="zoom:80%;" />

### 安装部署

> spark 默认配置文件地址：
> http://spark.apache.org/docs/2.3.0/spark-standalone.html#cluster-launch-scripts
> Spark HA 配置选项地址：
> http://spark.apache.org/docs/2.3.0/spark-standalone.html#standby-masters-with-zookeeper

- 拷贝之前最简安装的 Spark 留作单节点和 Spark on Yarn 的备份：

  - `cp -r spark-2.3.0-bin-hadoop2.6/ spark-alone`
  - `cp -r spark-2.3.0-bin-hadoop2.6/ spark-yarn`

- 通过远程脚本创建日志目录：`runRemoteCmd.sh "mkdir -p /home/hadoop/data/spark/logs" all`

- 修改配置文件：`mv spark-env.sh.template spark-env.sh`

  ```shell
  #配置 jdk
  export JAVA_HOME=/home/hadoop/app/jdk
  #配置 hadoop 配置文件目录
  export HADOOP_CONF_DIR=/home/hadoop/app/hadoop/etc/hadoop
  #配置 hadoop 根目录
  export HADOOP_HOME=/home/hadoop/app/hadoop
  #spark master webui 端口，默认是 8080，跟 tomcat 冲突
  SPARK_MASTER_WEBUI_PORT=8888
  #配置 spark HA 配置
  SPARK_DAEMON_JAVA_OPTS="-Dspark.deploy.recoveryMode=ZOOKEEPER -Dspark.deploy.zookeeper.url=hadoop01:2181,hadoop02:2181,hadoop03:2181 -Dspark.deploy.zookeeper.dir=/myspark"
  #spark 配置文件目录
  SPARK_CONF_DIR=/home/hadoop/app/spark/conf
  #spark 日志目录
  SPARK_LOG_DIR=/home/hadoop/data/spark/logs
  #spark 进程 ip 文件保存位置
  SPARK_PID_DIR=/home/hadoop/data/spark/logs
  ```

- 修改配置文件：`mv slaves.template slaves`

  ```
  # A Spark Worker will be started on each of the machines listed below.
  hadoop01
  hadoop02
  hadoop03
  ```

- 拷贝 hdfs 配置文件到 spark 目录（与 hdfs 读写数据）：

  `cp /home/hadoop/app/hadoop/etc/hadoop/core-site.xml /home/hadoop/app/spark/conf/`

  `cp /home/hadoop/app/hadoop/etc/hadoop/hdfs-site.xml /home/hadoop/app/spark/conf/`

- 使用`deploy.sh`脚本同步 spark 和 scala 安装目录，并在每个节点创建 spark 的软连接

  - `deploy.sh spark-2.3.0-bin-hadoop2.6 /home/hadoop/app/ slave`

    `ln -s spark-2.3.0-bin-hadoop2.6 spark`

  - `deploy.sh scala-2.11.8/ /home/hadoop/app/ slave`

    `ln -s scala-2.11.8/ scala`

- 配置 scala 环境变量：`vi ~/.bashrc`，并使其生效：`source ~/.bashrc`

  ```shell
  SCALA_HOME=/home/hadoop/app/scala
  PATH=$JAVA_HOME/bin:$SCALA_HOME/bin:
  ```

### 集群启动

- 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 启动 spark 集群，在主节点执行以下命令：`sbin/start-all.sh`
- 再选择一个节点作为 master 启动：`sbin/start-master.sh`

- 查看主节点 web 界面：http://hadoop01:8888/

### 提交 spark 作业

- 作业提交格式：

  ```shell
  ./bin/spark-submit \
  --class <main-class> \
  --master <master-url> \
  --deploy-mode <deploy-mode> \
  --conf <key>=<value> \
  ... # other options <application-jar> \
  [application-arguments]
  ```

- 使用 MyWordCount.jar 对 HDFS 中的文件进行计算，计算结果保存在 HDFS 中：

  `bin/spark-submit --class com.spark.MyJavaWordCount --master spark://hadoop01:7077 javaWordCount.jar hdfs://mycluster/test/wd.txt hdfs://mycluster/test/out`

- 常见问题：
  - 问题一：Exception in thread "main" java.lang.SecurityException: Invalid signature file digest for Manifest main attributes
    解决：zip -d test-spark-1.0-SNAPSHOT.jar META-INF/*.RSA META-INF/*.DSA META-INF/*.SF
  - 问题二：Caused by: java.lang.IllegalArgumentException:java.net.UnknownHostException: mycluster
    解决：
    - 向$SPARK_HOME/conf/目录拷贝 hdfs-site.xml 和 core-site.xml 配置文件
    - 向 spark-defaults.conf 配置文件提交以下内容：
      spark.files file:///home/hadoop/app/spark/conf/hdfs-site.xml,
      file:///home/hadoop/app/spark/conf/core-site.xml

## 2 Spark Standalone 工作流程

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/147.jpg" alt="image" style="zoom:80%;" />

- 应用的提交包含以下两种方式

  - **Client 模式**，Driver 进程运行在客户端，对应用进行管理监控

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/148.jpg" alt="image" style="zoom:80%;" />

  - **Cluster 模式**，Driver 从集群中的 worker 节点中任取一个运行驱动程序，负责整个应用的监控

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/149.jpg" alt="image" style="zoom:80%;" />

- 用两种模式提交 spark 作业：`deploy-mode cluster/client`
  - `bin/spark-submit --class com.spark.MyJavaWordCount --master spark://hadoop01:7077 -deploy-mode cluster javaWordCount.jar hdfs://mycluster/test/wd.txt hdfs://mycluster/test/out`
  - `bin/spark-submit --class com.spark.MyJavaWordCount --master spark://hadoop01:7077 -deploy-mode client javaWordCount.jar hdfs://mycluster/test/wd.txt hdfs://mycluster/test/out`

## 3 Spark On YARN 运行模式

**Yarn Client (YarnClusterScheduler)**

> YARN standalone是0.9及之前版本的叫法，1.0开始更名为YARN cluster
> Driver和AM运行在一起，Client单独的一个节点提交作业

`./bin/spark-submit --class path.to.your.Class --master yarn --deploy-mode cluster [options] <app jar> [appoptions]`

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/150.jpg" alt="image" style="zoom:80%;" />

**Yarn Client (YarnClientClusterScheduler)**

> Client和Driver运行在一起(运行在本地)，AM只用来管理资源

`./bin/spark-submit --class path.to.your.Class --master yarn --deploy-mode client [options] <app jar> [appptions]`

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/151.jpg" alt="image" style="zoom:80%;" />

**选择哪种模式**：

如果需要返回数据到 client 就用 YARN client 模式
数据存储到 hdfs 的建议用 YARN cluster 模式(可以在 hdfs 查看日志信息)

### 安装部署

进入之前下载并拷贝好的 Spark 目录：`~/app/spark-yarn`

- 添加 HADOOP_CONF_DIR 或者 YARN_CONF_DIR 环境变量，让 Spark 知道 Yarn 的配置信息：

  - `cd spark-yarn/conf`
  - `mv spark-env.sh.template spark-env.sh`
  - `vi spark-env.sh`

  - 添加如下内容：`HADOOP_CONF_DIR=/home/hadoop/app/hadoop/etc/hadoop`

- 提交作业到 yarn

  格式：`./bin/spark-submit --class path.to.your.Class --master yarn --deploy-mode cluster [options] <app jar> [app options]`

  `bin/spark-submit --class xxx --master yarn --deploy-mode cluster/client xxx.jar input output`