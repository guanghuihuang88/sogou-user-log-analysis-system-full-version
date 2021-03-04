package com.dsj.spark.sql
import org.apache.spark.sql._
import org.apache.spark.{SparkConf, SparkContext}
import java.sql.Connection
import java.sql.Statement
import java.sql.DriverManager
object sparksql_mysql {
  case class sogoulogs(logtime:String,uid:String,keywords:String,resultno:String,clickno:String,url:String)

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName("sogoulogs")
      .master("local[2]")
      .getOrCreate()
    import spark.implicits._

    val fileRDD = spark
      .sparkContext
      .textFile("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\sogoulogs.log")
    val ds = fileRDD.map(line =>line.split(",")).map(t =>sogoulogs(t(0),t(1),t(2),t(3),t(4),t(5))).toDS()

    ds.createTempView("sogoulogs")
    //统计每个新闻浏览量
    val newsCount=spark.sql("select keywords as name,count(keywords) as count from sogoulogs group by keywords")
    newsCount.show()

    newsCount.rdd.foreachPartition(myFun)

    //统计每个时段新闻浏览量
    val  periodCount=spark.sql("select logtime,count(logtime) as count from sogoulogs group by logtime")
    periodCount.show()
    periodCount.rdd.foreachPartition(myFun2)
  }

  /**
   * 新闻浏览量数据插入mysql
   */
  def myFun(records:Iterator[Row]): Unit = {
    var conn:Connection = null
    var statement:Statement = null

    try{
      val url = Constants.url
      val userName:String = Constants.userName
      val passWord:String = Constants.passWord

      //conn长连接
      conn = DriverManager.getConnection(url, userName, passWord)

      records.foreach(t => {

        val name = t.getAs[String]("name").replaceAll("[\\[\\]]", "")
        val count = t.getAs[Long]("count").asInstanceOf[Int]
        print(name+"@"+count+"***********************************")

        val sql = "select 1 from newscount "+" where name = '"+name+"'"

        val updateSql = "update newscount set count = count+"+count+" where name ='"+name+"'"

        val insertSql = "insert into newscount(name,count) values('"+name+"',"+count+")"
        //实例化statement对象
        statement = conn.createStatement()

        //执行查询
        var resultSet = statement.executeQuery(sql)

        if(resultSet.next()){
          print("*****************更新******************")
          statement.executeUpdate(updateSql)
        }else{
          print("*****************插入******************")
          statement.execute(insertSql)
        }

      })
    }catch{
      case e:Exception => e.printStackTrace()
    }finally{
      if(statement !=null){
        statement.close()
      }

      if(conn !=null){
        conn.close()
      }

    }

  }

  /**
   * 时段浏览量数据插入mysql数据
   */
  def myFun2(records:Iterator[Row]): Unit = {
    var conn:Connection = null
    var statement:Statement = null

    try{
      val url = Constants.url
      val userName:String = Constants.userName
      val passWord:String = Constants.passWord

      //conn
      conn = DriverManager.getConnection(url, userName, passWord)

      records.foreach(t => {

        val logtime = t.getAs[String]("logtime")
        val count = t.getAs[Long]("count").asInstanceOf[Int]
        print(logtime+"@"+count+"***********************************")

        val sql = "select 1 from periodcount "+" where logtime = '"+logtime+"'"

        val updateSql = "update periodcount set count = count+"+count+" where logtime ='"+logtime+"'"

        val insertSql = "insert into periodcount(logtime,count) values('"+logtime+"',"+count+")"
        //实例化statement对象
        statement = conn.createStatement()

        //执行查询
        var resultSet = statement.executeQuery(sql)

        if(resultSet.next()){
          print("*****************更新******************")
          statement.executeUpdate(updateSql)
        }else{
          print("*****************插入******************")
          statement.execute(insertSql)
        }

      })
    }catch{
      case e:Exception => e.printStackTrace()
    }finally{
      if(statement !=null){
        statement.close()
      }

      if(conn !=null){
        conn.close()
      }

    }

  }
}