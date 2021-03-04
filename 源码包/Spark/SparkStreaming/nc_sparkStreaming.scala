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