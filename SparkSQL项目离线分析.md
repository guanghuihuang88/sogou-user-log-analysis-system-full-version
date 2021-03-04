# Spark SQL 项目离线分析

## 1 业务建模

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

  ```mysql
  create table newscount (
  	name varchar(50) not null,
  	count int(11) not null
  );
  ```

**业务表2**

> 根据业务二建表：
>
> 统计新闻浏览量最高的时段：`select substr(logtime,0,5),count(substr(logtime,0,5)) as counter from sogoulogs group by substr(logtime,0,5) order by counter desc limit 5;`

- 建表：

  ```mysql
  create table periodcount (
  	logtime varchar(50) not null,
  	count int(11) not null
  );
  ```

## 2 业务核心代码实现

> 下面的 spark 业务代码，对指定的 sogoulogs数据集文件进行 sql 查询，并将结果写入 Mysql 业务表中，然后用 mysql 查询语句对业务进行离线分析

- 定义数据库连接参数

  ```scala
  package com.dsj.spark.sql
  /**
   * 定义数据连接参数
   */
  object Constants {
    var url:String  ="jdbc:mysql://192.168.62.203:3306/test"
    var userName:String ="hive"
    var passWord:String = "hive"
  }
  ```

- sparl 业务代码

  ```scala
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
  ```

- MySQL 业务离线分析

  ```mysql
  #统计新闻话题浏览量 topn 排名
  select * from newscount order by count desc LIMIT 10
  #统计各时段浏览量 topn 排名
  select * from periodcount order by count desc LIMIT 10
  ```

  