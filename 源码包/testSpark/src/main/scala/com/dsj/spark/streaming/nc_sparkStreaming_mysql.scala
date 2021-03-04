package com.dsj.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}


object nc_sparkStreaming_mysql {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: sogoulogs <hostname> <port>")
      System.exit(1)
    }

    // Create the context with a 1 second batch size
    val sparkConf = new SparkConf().setAppName("sogoulogs").setMaster("local[2]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    // Create a socket stream on target ip:port and count the
    // words in input stream of \n delimited text (e.g. generated by 'nc')
    // Note that no duplication in storage level only for running locally.
    // Replication necessary in distributed scenario for fault tolerance.
    val lines = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)
    
    //无效数据过滤
    val filter = lines.map(_.split(",")).filter(_.length == 6)
    
    //统计新闻话题浏览量 topn
    val newsCounts = filter.map(x => (x(2), 1)).reduceByKey(_ + _)
    newsCounts.print()
    
    //统计所有时段新闻浏览量
    val periodCounts = filter.map(x => (x(0), 1)).reduceByKey(_ + _)
    periodCounts.print()
    
    ssc.start()
    ssc.awaitTermination()
  }
}