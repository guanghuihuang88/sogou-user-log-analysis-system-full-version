# Spark SQL 集成开发

## 1 Spark SQL 与 Hive 集成开发

- 修改配置文件

  - 在 hadoop03 节点将 hive 的配置文件`hive/conf/hive-site.xml`拷贝到 spark节点（hadoop01）的`spark-alone/conf`目录：`scp hive-site.xml hadoop@hadoop01:/home/hadoop/app/spark-alone/conf`

  - 同时在`hive-site.xml`中修改 metastore 的 url 配置：`vi hive-site.xml`

    ```xml
    <property>
        <name>hive.metastore.uris</name>
        <value>thrift://hadoop03:9083</value>
    </property>
    ```

    （备注：SparkSQL 通过连接 hive 提供的 metastore 服务来获取 hive 表的元数据，直接启动 hive 的 metastore 服务即可完成 SparkSQL 和 Hive 的集成）

  - 拷贝 hive 中的 mysql jar 包到 spark 的 jar 目录下：
    `scp /home/hadoop/app/hive/lib/mysql-connector-java-5.1.38.jar hadoop@hadoop01:/home/hadoop/app/spark-alone/jars/`

  - 检查 spark-env.sh 文件中的 hadoop 配置项：`vi spark-env.sh`
    `HADOOP_CONF_DIR=/home/hadoop/app/hadoop/etc/hadoop`

- 在 hadoop03 启动 mysql 服务

  - 查看状态：`service mysqld status`
  - 启动：`service mysqld start`

- 在 hadoop01 启动 zookeeper 集群：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 在 hadoop01 启动 hadoop 集群

  - `sbin/start-dfs.sh`
  - `sbin/start-yarn.sh`
  - 在 hadoop02 启动 yarn rm实现高可用：`sbin/yarn-daemon.sh start resourcemanager`

- 在 hadoop03 

  - 启动 hive metastore 服务：`bin/hive --service metastore &`
  - 启动 hive：`bin/hive`

- 在 hadoop01 

  - 启动 spark-shell：`spark-alone/bin/spark-shell`

    在 scala>命令行输入：`spark.sql("select * from hive.stu").show`即可查询 Hive 中的 stu表

  - 启动 spark-sql：`spark-alone/bin/spark-sql`

    在 spark-sql>命令行直接输入查询语句即可

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/174.jpg" alt="image"  />

**Spark 也支持 ThriftServer 和 beeline**

- 在 hadoop01 启动 ThriftServer：`spark-alone/sbin/start-thriftserver.sh`

  ThriftServer 是一个 JDBC/ODBC 接口，用户可以通过 JDBC/ODBC 连接 ThriftServer 来访问 Spark SQL 数据。ThriftServer 在启动的时候，会启动一个 Spark SQL 的应用程序。Spark SQL 中使用的 ThriftServer 是 org.apache.spark.sql.hive.thriftserver.HiveThriftServer2

- 启动 beeline：

  - `spark-alone/bin/beeline`
  - beeline>命令行输入：`!connect jdbc:hive2://hadoop01:10000（输入用户名密码 hive）`

  远程客户端使用 spark beeline 访问 hive。Spark 自带了Beeline（Java 等其他语言也可以）客户端程序，我们可以通过 Beeline 连接 JDBC 服务，从而使用 Spark SQL，然后进一步访问 Hive（数据源）

## 2 Spark SQL 与 MySQL 集成开发

- 准备Spark SQL代码

  ```scala
  val df = spark
  .read
  .format("jdbc")
  .option("url", "jdbc:mysql://hadoop03:3306/test")
  .option("dbtable", "newscount")
  .option("user", "hive")
  .option("password", "hive")
  .load()
  ```

- 启动spark-shell：`spark-alone/sbin/spark-shell`

  - spark-shell 命令行中输入`:paste`，然后拷贝上述完整代码
  - 输入 ctr+d 退出整段输入，然后再输入`df.show`打印读取数据

## 3 Spark SQL 与 HBase 集成开发

- 导入相关依赖包
- 启动HBase服务
- 启动spark-shell

> Spark SQL 与 HBase 集成，其核心就是 Spark Sql 通过 hive 外部表来获取 HBase 的表数据

- 拷贝 HBase 的包和 hive 包到 spark 的 jars 目录下

  - 在 hadoop01 拷贝：

    `cp hbase-client-1.2.0-cdh5.10.0.jar /home/hadoop/app/spark-alone/jars/`
    `cp hbase-common-1.2.0-cdh5.10.0.jar /home/hadoop/app/spark-alone/jars/`
    `cp hbase-protocol-1.2.0-cdh5.10.0.jar /home/hadoop/app/spark-alone/jars/`
    `cp hbase-server-1.2.0-cdh5.10.0.jar /home/hadoop/app/spark-alone/jars/`
    `cp htrace-core-3.2.0-incubating.jar /home/hadoop/app/spark-alone/jars/`
    `cp metrics-core-2.2.0.jar /home/hadoop/app/spark-alone/jars/`

  - 在 hadoop03 拷贝：

    `scp hive-hbase-handler-1.1.0-cdh5.10.0.jar hadoop@hadoop01:/home/hadoop/app/spark-alone/jars`
    `scp mysql-connector-java-5.1.38.jar hadoop@hadoop01:/home/hadoop/app/spark-alone/jars`

- 在 hadoop03 启动 mysql 服务

  - 查看状态：`service mysqld status`
  - 启动：`service mysqld start`

- 在 hadoop01 启动 zookeeper 集群：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 在 hadoop01 启动 hadoop 集群

  - `sbin/start-dfs.sh`
  - `sbin/start-yarn.sh`
  - 在 hadoop02 启动 yarn rm实现高可用：`sbin/yarn-daemon.sh start resourcemanager`

- 在 hadoop03 启动 hive metastore 服务：`bin/hive --service metastore &`

- 在 hadoop01 启动 hbase 服务：`bin/start-hbase.sh`

- 在 hadoop01 启动 spark-shell：`spark-alone/bin/spark-shell`

- 在 scala>命令行输入：`spark.sql("select count(1) from sogoulogs").show`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/175.jpg" alt="image"  />