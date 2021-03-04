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