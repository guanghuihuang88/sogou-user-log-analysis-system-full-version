package com.dsj.spark.sql
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.sql._
/**
 * rdd dataframe dataset 数据相互转换
 */
object rddDataFrameDataSet {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("topn")
      .master("local[2]")
      .getOrCreate()
    /**
     * rdd转DataFrame
     */
    //    val df = rddToDataFrame(spark)
    //    df.printSchema()
    //    df.select("_1", "_2").show()

    /**
     * rdd 转DataSet
     */
    //      val ds =rddToDataSet(spark)
    //      ds.createOrReplaceTempView("wordcount")//创建一个本地临时视图
    //      spark.sql("select word,count(1) from wordcount group by word").show()
    /**
     * ds 转 df
     */
    //      val df = dataSetToDataFrame(spark)
    //      df.select("word", "count").groupBy("word").count().show()
    /**
     *  dataSet 转 RDD
     */
    //        val fileRDD = dataSetToRDD(spark)
    //        val wordCounts = fileRDD.flatMap(_.split("\\s+"))
    //                            .map((_,1))
    //                            .reduceByKey(_+_)
    //                            .map(x =>(x._2,x._1))
    //                            .sortByKey(false)
    //                            .map(x =>(x._2,x._1))
    //        wordCounts.collect().foreach(println)
    /**
     * dataFrame 转 RDD
     */
    val jsonRDD = dataFrameToRDD(spark)
    jsonRDD.foreach(println)

  }


  /**
   * dataFrame 转 RDD
   */
  case class Person(name: String, age: Int)
  def dataFrameToRDD(spark:SparkSession)={
    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._
    val people = spark.read
      .json("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\people.json")
    people.printSchema()
    people.createOrReplaceTempView("people")

    val teenagers =spark.sql("select name from people where age >=13 and age<=19")
    teenagers.rdd
  }
  /**
   * dataSet 转 RDD
   */
  def dataSetToRDD(spark:SparkSession)={
    val rdd = spark.read
      .textFile("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\wc.txt")
      .rdd
    rdd
  }

  /**
   * dataSet转DataFrame
   */
  def dataSetToDataFrame(spark:SparkSession):DataFrame = {
    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._
    //读取数据
    val ds = spark
      .read
      .textFile("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\wc.txt")
      .flatMap(_.split("\\s+"))
      .map((_,1))

    val df=ds.toDF("word","count")
    df
  }
  /**
   * rdd 转DataSet
   */
  case class wordcount(word:String,count:Int)
  def rddToDataSet(spark:SparkSession):Dataset[wordcount] ={
    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._
    //读取数据
    val ds = spark
      .sparkContext
      .textFile("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\wc.txt")
      .flatMap(_.split("\\s+"))
      .map((_,1))
      .map(x =>(wordcount(x._1,x._2))).toDS()
    ds
  }
  /**
   * rdd转DataFrame
   */
  def rddToDataFrame(spark:SparkSession):DataFrame = {

    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._

    //读取数据
    val fileRDD = spark.sparkContext.textFile("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\wc.txt")

    val wordCountsRDD = fileRDD.flatMap(_.split("\\s+"))
      .map((_,1))
      .reduceByKey(_+_)
      .map(x =>(x._2,x._1))
      .sortByKey(false)
      .map(x =>(x._2,x._1))

    //rdd转DataFrame
    val df = wordCountsRDD.toDF()
    df
  }
}