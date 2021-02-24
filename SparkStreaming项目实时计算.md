# Spark Streaming 项目实时分析

## 1 项目业务表建模

数据格式为：
访问时间\t用户 ID\t[查询词]\t该 URL 在返回结果中的排名\t用户点击的顺序号\t用户点击的 URL
其中，用户 ID 是根据用户使用浏览器访问搜索引擎时的 Cookie 信息自动赋值，即同一次使用浏览器输入的不同查询对应同一个用户 ID

- 启动 mysql 服务：`sudo service mysqld start`
- 登录 mysql 数据库（hadoop03）：`mysql -h hadoop03 -u hive -p`
- 查看所有数据库：`mysql> show databases;`
- 创建数据库 hive：`mysql> create database test;`
- 使用 test 数据库：`use test;`
- 使用 SQLyog 软件工具访问数据库

**业务表1**

> 根据业务一建表：
>
> 统计新闻话题总量：`select count(distinct keywords) from sogoulogs;`
> 统计新闻话题浏览量 topn 排名：`select keywords,count(*) as rank from sogoulogs group by keywords order by rank desc limit 10`

- 建表：
  `create table newscount (
  	name varchar(50) not null,
  	count int(11) not null
  );`

**业务表2**

> 根据业务二建表：
>
> 统计新闻浏览量最高的时段：`select substr(logtime,0,5),count(substr(logtime,0,5)) as counter from sogoulogs group by substr(logtime,0,5) order by counter desc limit 5;`

- 建表：
  `create table periodcount (
  	logtime varchar(50) not null,
  	count int(11) not null
  );`



## 2 Nc + Spark Streaming + MySQL 项目业务集成开发

### 编写业务代码

- maven 引入 mysql 和 spark-streaming依赖包：

  ```xml
  <dependency>
      <groupId>org.apache.spark</groupId>
      <artifactId>spark-streaming_2.11</artifactId>
      <version>2.3.0</version>
  </dependency>
  <dependency>
      <groupId>mysql</groupId>
      <artifactId>mysql-connector-java</artifactId>
      <version>5.1.29</version>
  </dependency>
  ```

- 在 eclipse 项目 testSpark 下新建包`com.dsj.spark.streaming`，在该包中新建 scala Object：`nc_sparkStreaming_mysql.scala` 和 `Constants.scala`

  ```scala
  package com.dsj.spark.streaming
  
  object Constants {
    var url:String  ="jdbc:mysql://192.168.62.203:3306/test"
    var userName:String ="hive"
    var passWord:String = "hive"
  }
  ```

  ```scala
  package com.dsj.spark.streaming
  import org.apache.spark.SparkConf
  import org.apache.spark.storage.StorageLevel
  import org.apache.spark.streaming.{ Seconds, StreamingContext }
  import java.sql.{ PreparedStatement, Connection, Statement, DriverManager }
  import java.sql.ResultSet
  /**
   * nc+Spark Streaming+MySQL集成开发
   */
  object nc_sparkStreaming {
    /**
     * 定义方法更新或者插入mysql数据库
     */
    def myFun(records: Iterator[(String, Int)]): Unit = {
      var conn: Connection = null
      var statement: Statement = null
      try {
        val url = Constants.url
        val userName = Constants.userName
        val passWord = Constants.passWord
        //conn属于长连接比较重，放在循环外面
        conn = DriverManager.getConnection(url, userName, passWord)
        records.foreach(t => {
          val name = t._1.replaceAll("[\\[\\]]","")
          val count = t._2
          //查询sql
          val querySql = "select 1 from newscount " +
            "where name = '" + name + "'"
  
          //更新sql
          val updateSql = "update newscount set " +
            "count = count+" + count + " where name = '" + name + "'"
  
          //插入sql
          val insertSql = "insert into newscount(name,count)" +
            "values('" + name + "'," + count + ")"
  
          //实例化Statement对象
          statement = conn.createStatement()
  
          //执行查询
          var resultSet = statement.executeQuery(querySql)
          //数据存在则更新，不存在则插入
          if (resultSet.next()) {
            statement.executeUpdate(updateSql)
          } else {
            statement.execute(insertSql)
          }
        })
      } catch {
        case e: Exception => e.printStackTrace()
      } finally {
        if (statement != null) {
          statement.close()
        }
        if (conn != null) {
          conn.close()
        }
      }
    }
  
    /**
     * 定义方法更新或者插入mysql数据库
     */
    def myFun2(records: Iterator[(String, Int)]): Unit = {
      var conn: Connection = null
      var statement: Statement = null
      try {
        val url = Constants.url
        val userName = Constants.passWord
        val passWord = Constants.passWord
        //conn属于长连接比较重，放在循环外面
        conn = DriverManager.getConnection(url, userName, passWord)
        records.foreach(t => {
          val logtime = t._1
          val count = t._2
          //查询sql
          val querySql = "select 1 from periodcount " +
            "where logtime = '" + logtime + "'"
  
          //更新sql
          val updateSql = "update periodcount set " +
            "count = count+" + count + " where logtime = '" + logtime + "'"
  
          //插入sql
          val insertSql = "insert into periodcount(logtime,count)" +
            "values('" + logtime + "'," + count + ")"
  
          //实例化Statement对象
          statement = conn.createStatement()
  
          //执行查询
          var resultSet = statement.executeQuery(querySql)
          //数据存在则更新，不存在则插入
          if (resultSet.next()) {
            statement.executeUpdate(updateSql)
          } else {
            statement.execute(insertSql)
          }
        })
      } catch {
        case e: Exception => e.printStackTrace()
      } finally {
        if (statement != null) {
          statement.close()
        }
        if (conn != null) {
          conn.close()
        }
      }
    }
    def main(args: Array[String]) {
      if (args.length < 2) {
        System.err.println("Usage: sogoulogsCount <hostname> <port>")
        System.exit(1)
      }
  
      // Create the context with a 1 second batch size
      val sparkConf = new SparkConf().setAppName("sogoulogsCount").setMaster("local[2]")
      val ssc = new StreamingContext(sparkConf, Seconds(5))
  
      // Create a socket stream on target ip:port and count the
      val lines = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)
  
      //无效数据过滤
      val filter = lines.map(_.split(",")).filter(_.length == 6)
  
      //统计新闻话题浏览量topn排行
      val newsCounts = filter.map(x => (x(2), 1)).reduceByKey(_ + _)
      newsCounts.print()
      newsCounts.foreachRDD(rdd => {
        //分区并行执行
        rdd.foreachPartition(myFun)
      })
  
      //统计新闻浏览量最高的时段
      val periodCounts = filter.map(x => (x(0), 1)).reduceByKey(_ + _)
      periodCounts.print()
      periodCounts.foreachRDD(rdd => {
        //分区并行执行
        rdd.foreachPartition(myFun2)
      })
      ssc.start()
      ssc.awaitTermination()
    }
  }
  ```

### 测试代码

- 在 hadoop01 打开 nc：`nc -lk 9999`
- 在 eclipse 中设置 run configuration 参数为 `hadoop01`和`9999`
- 运行`nc_sparkStreaming_mysql.scala`，并在 nc 命令行手动输入sogoulogs的数据
- 可在 MySQL test表中观察数据的更新

### 解决 MySQL 中文乱码问题

- 修改配置文件：`sudo vi /etc/my.cnf`

  ```
  [client]
  socket=/var/lib/mysql/mysql.sock
  default-character-set=utf8
  [mysqld]
  character-set-server=utf8
  datadir=/var/lib/mysql
  socket=/var/lib/mysql/mysql.sock
  user=mysql
  # Disabling symbolic-links is recommended to prevent assorted security risks
  symbolic-links=0
  [mysqld_safe]
  log-error=/var/log/mysqld.log
  pid-file=/var/run/mysqld/mysqld.pid
  ```

- 重启 mysql 服务乱码解决：`sudo service mysqld restart`

- 用 SQLyog 修改两个表中char列属性为utf-8

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/164.jpg" alt="image" style="zoom:80%;" />

## ⭐3 Flume + Kafka + Spark Streaming + MySQL 项目业务集成开发

> 使用之前编写的模拟脚本sogoulogs.sh，实时将 sogoulogs.log数据逐条输入到 test.log 文件中，该文件作为采集节点上 flume 的源，将数据聚合到聚合节点 上的flume，flume则将数据继续传到kafka，此时，通过编写的 sparkStreaming 代码，实时将 kafka 中的数据写入 Mysql 的两个业务表中

### 编写sparkStreaming代码

> 官网参考地址：http://spark.apache.org/docs/2.3.0/streaming-kafka-0-10-integration.html

- maven 引入 spark-streaming-kafka 依赖包：

  ```xml
  <dependency>
      <groupId>org.apache.spark</groupId>
      <artifactId>spark-streaming-kafka-0-10_2.11</artifactId>
      <version>2.3.0</version>
  </dependency>
  ```

- 在 eclipse 项目 testSpark 的包`com.dsj.spark.streaming`中新建 scala Object：`kafka_sparkStreaming.scala` 

  ```scala
  package com.dsj.spark.streaming
  import org.apache.spark.SparkConf
  import org.apache.spark.storage.StorageLevel
  import org.apache.spark.streaming.{ Seconds, StreamingContext }
  import java.sql.{ PreparedStatement, Connection, Statement, DriverManager }
  import java.sql.ResultSet
  import org.apache.kafka.clients.consumer.ConsumerRecord
  import org.apache.kafka.common.serialization.StringDeserializer
  import org.apache.spark.streaming.kafka010._
  import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
  import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
  
  /**
   * kafka+spark Streaming+mysql集成开发
   */
  object kafka_sparkStreaming {
    /**
     * 定义方法更新或者插入mysql数据库
     */
    def myFun(records: Iterator[(String, Int)]): Unit = {
      var conn: Connection = null
      var statement: Statement = null
      try {
        val url = Constants.url
        val userName = Constants.userName
        val passWord = Constants.passWord
        //conn属于长连接比较重，放在循环外面
        conn = DriverManager.getConnection(url, userName, passWord)
        records.foreach(t => {
          val name = t._1.replaceAll("[\\[\\]]","")
          val count = t._2
          //查询sql
          val querySql = "select 1 from newscount " +
            "where name = '" + name + "'"
  
          //更新sql
          val updateSql = "update newscount set " +
            "count = count+" + count + " where name = '" + name + "'"
  
          //插入sql
          val insertSql = "insert into newscount(name,count)" +
            "values('" + name + "'," + count + ")"
  
          //实例化Statement对象
          statement = conn.createStatement()
  
          //执行查询
          var resultSet = statement.executeQuery(querySql)
          //数据存在则更新，不存在则插入
          if (resultSet.next()) {
            statement.executeUpdate(updateSql)
          } else {
            statement.execute(insertSql)
          }
        })
      } catch {
        case e: Exception => e.printStackTrace()
      } finally {
        if (statement != null) {
          statement.close()
        }
        if (conn != null) {
          conn.close()
        }
      }
    }
  
    /**
     * 定义方法更新或者插入mysql数据库
     */
    def myFun2(records: Iterator[(String, Int)]): Unit = {
      var conn: Connection = null
      var statement: Statement = null
      try {
        val url = Constants.url
        val userName = Constants.passWord
        val passWord = Constants.passWord
        //conn属于长连接比较重，放在循环外面
        conn = DriverManager.getConnection(url, userName, passWord)
        records.foreach(t => {
          val logtime = t._1
          val count = t._2
          //查询sql
          val querySql = "select 1 from periodcount " +
            "where logtime = '" + logtime + "'"
  
          //更新sql
          val updateSql = "update periodcount set " +
            "count = count+" + count + " where logtime = '" + logtime + "'"
  
          //插入sql
          val insertSql = "insert into periodcount(logtime,count)" +
            "values('" + logtime + "'," + count + ")"
  
          //实例化Statement对象
          statement = conn.createStatement()
  
          //执行查询
          var resultSet = statement.executeQuery(querySql)
          //数据存在则更新，不存在则插入
          if (resultSet.next()) {
            statement.executeUpdate(updateSql)
          } else {
            statement.execute(insertSql)
          }
        })
      } catch {
        case e: Exception => e.printStackTrace()
      } finally {
        if (statement != null) {
          statement.close()
        }
        if (conn != null) {
          conn.close()
        }
      }
    }
    def main(args: Array[String]) {
      // Create the context with a 1 second batch size
      val sparkConf = new SparkConf().setAppName("sogoulogsCount").setMaster("local[2]")
      val ssc = new StreamingContext(sparkConf, Seconds(5))
  
      // Create a socket stream on target ip:port and count the
      //val lines = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)
      
      
      /**
       * 第一列：kafka集群地址
       * 第二列：key默认序反序列化类
       * 第三列：value默认序反序列化类
       * 第四列：消费者组
       * 第五列：偏移量earliest latest
       * 第六列：是否自动提交偏移量
       */
      val kafkaParams = Map[String, Object](
        "bootstrap.servers" -> "hadoop01:9092,hadoop02:9092,hadoop03:9092",
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        "group.id" -> "sogoulogs",
        "auto.offset.reset" -> "earliest",
        "enable.auto.commit" -> (true: java.lang.Boolean))
  
      //消费kafka topic数据
      val topics = Array("sogoulogs")
      //订阅kafka topic数据
      val stream = KafkaUtils.createDirectStream[String, String](
        ssc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams))
        
        
      //提取日志记录
      val lines = stream.map(record => record.value);
      //无效数据过滤
      val filter = lines.map(_.split(",")).filter(_.length == 6)
  
      //统计新闻话题浏览量topn排行
      val newsCounts = filter.map(x => (x(2), 1)).reduceByKey(_ + _)
      newsCounts.print()
      newsCounts.foreachRDD(rdd => {
        //分区并行执行
        rdd.foreachPartition(myFun)
      })
  
      //统计新闻浏览量最高的时段
      val periodCounts = filter.map(x => (x(0), 1)).reduceByKey(_ + _)
      periodCounts.print()
      periodCounts.foreachRDD(rdd => {
        //分区并行执行
        rdd.foreachPartition(myFun2)
      })
      ssc.start()
      ssc.awaitTermination()
    }
  }
  ```

### 测试代码

- 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 注意集群时钟必须同步：

  同步时间：`sudo ntpdate pool.ntp.org`
  查看时间：`date`

- 每个节点分别启动 Kafka 集群：`bin/kafka-server-start.sh config/server.properties`

- 在聚合节点 hadoop01 

  - 编写 flume 配置文件`data_flume-kafka.properties`上传到`~/shell/conf`路径
  - 编写 flume 启动脚本`start_data_flume_kafka.sh`上传到 hadoop01的`~/shell/bin`路径

  ```properties
  agent.sources = avroSource
  agent.channels = memoryChannel
  agent.sinks = kafkaSink
  
  # For each one of the sources, the type is defined
  agent.sources.avroSource.type = avro
  agent.sources.avroSource.bind = 0.0.0.0
  agent.sources.avroSource.port = 1234
  agent.sources.avroSource.channels = memoryChannel
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 1000000
  agent.channels.memoryChannel.keep-alive = 60
  
  # Each sink's type must be defined
  agent.sinks.kafkaSink.type = org.apache.flume.sink.kafka.KafkaSink
  agent.sinks.kafkaSink.kafka.topic = sogoulogs
  agent.sinks.kafkaSink.kafka.bootstrap.servers = hadoop01:9092,hadoop02:9092,hadoop03:9092
  agent.sinks.kafkaSink.channel = memoryChannel
  ```

  ```shell
  #!/bin/sh
  
  home=$(cd `dirname $0`; cd ..; pwd)
  
  . ${home}/bin/common.sh
  
  ${flume_home}/bin/flume-ng agent \
  --conf ${conf_home} \
  -f ${conf_home}/data-flume-kafka.properties -n agent >> ${logs_home}/flume-hadoop01.log 2>&1 &
  
  echo $! > ${logs_home}/flume-hadoop01.pid
  ```

- 在采集节点 hadoop02，hadoop03 
  - 清空 test.log 文件：`cat /dev/null > test.log `
  - 使用之前编写好的 flume 配置文件`flume-conf.properties`
  - 使用之前编写好的 flume 启动脚本：`start_flume_hadoop02.sh`和`start_flume_hadoop03.sh`

- 先在聚合节点 hadoop01 启动 flume：`~/shell/bin`路径执行`./start_data_flume_kafka.sh `
- 然后在eclipse中运行 `kafka_sparkStreaming.scala` 代码，观察到 sparkStreeaming 开始持续监听
- 最后在采集节点 hadoop02，hadoop03 启动 flume：`~/shell/bin`路径执行`start_flume_hadoop02.sh`和`start_flume_hadoop03.sh`

- eclipse 控制台中能观察到有计算结果出现

- SQLyog 中可以看到数据已写入数据表中

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/165.jpg" alt="image" style="zoom:80%;" />

