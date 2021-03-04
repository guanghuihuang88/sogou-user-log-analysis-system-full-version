package com.dsj.spark.testSpark

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object MyScalaWordCout {
  def main(args: Array[String]): Unit = {
    if(args.length<2){
      System.err.println("Usage:MyScalaWordCout <input> <output>")
      System.exit(1)
    }
    
    //输入路径
    val input = args(0)
    //输出路径
    val output = args(1)
    
    val conf = new SparkConf().setAppName("MyScalaWordCout")
    
    val sc = new SparkContext(conf)
    
    //读取数据
    val lines = sc.textFile(input)
    
    val resultRDD = lines.flatMap(_.split("\\s+")).map((_,1)).reduceByKey(_+_)
    
    //保存结果
    resultRDD.saveAsTextFile(output)
    
    sc.stop()
    
  }
}