# Spark SQL 离线计算

## 1 Spark SQL 架构及运行原理

Spark SQL 是 Spark 的结构化数据处理模块

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/166.jpg" alt="image" style="zoom: 67%;" />

**何为结构化数据**

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/167.jpg" alt="image" style="zoom: 67%;" />

**Spark SQL 与 Spark Core 的关系**

Spark SQL 构建在 Spark Core 之上，专门用来处理结构化数据(不仅仅是 SQL)

Spark SQL 在 Spark Core 的基础上针对结构化数据处理进行很多优化和改进，简单来讲：

- Spark SQL 支持很多种结构化数据源，可以让你跳过复杂的读取过程，轻松从各种数据源中读取数
  据
- 当你使用 SQL 查询这些数据源中的数据并且只用到了一部分字段时，Spark SQL 可以智能地只扫描
  这些用到的字段，而不是像 SparkContext.hadoopFile 中那样简单粗暴地扫描全部数据

**Spark SQL 模块划分**

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/168.jpg" alt="image" style="zoom: 67%;" />

| 模块              | 功能                                                         |
| ----------------- | ------------------------------------------------------------ |
| core              | 处理数据的输入输出，从不同的数据源获取数据（RDD，Parquet，json等），将查询结果输出成 DataFrame |
| catalyst          | 处理查询语句的整个处理过程，包括解析、绑定、优化、物理计划等，说它是优化器，不如说是查询引擎 |
| hive              | 对 hive 数据的处理                                           |
| hive-ThriftServer | 提供 CLI 和 JDBC/ODBC 接口                                   |

**catalyst设计图**

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/169.jpg" alt="image" style="zoom: 80%;" />

类似于关系型数据库，SparkSQL 语句也是由 Projection（a1，a2，a3）、Data Source（tableA）、Filter（condition）组成，分别对应 sql 查询过程中的 Result、Data Source、Operation，也就是说
SQL 语句按 Result-->Data Source-->Operation 的次序来描述的

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/170.jpg" alt="image" style="zoom: 80%;" />

## 2 Spark SQL 编程

**依赖**

```xml
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.11</artifactId>
    <version>2.3.0</version>
    <scope>provided</scope>
</dependency>
```

### **编程入口**

Spark SQL 的切入点是 SparkSession类

```scala
import org.apache.spark.sql.SparkSession

val spark = SparkSession
	.builder()
	.appName("Spark SQL basic example")
	.config("spark.some.config.option", "some-value")
	.getOrCreate()

// For implicit conversions like converting RDDs to DataFrames
import spark.implicits._
```

spark2.0 之后的 SparkSeesion 就提供了 HiveQL查询、hive UDFs使用、从 hive表读取数据的支持。你不需
要安装hive就可以使用这些特性

### **服务架构**

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/171.jpg" alt="image" style="zoom: 50%;" />

### **DataFrame**

- DataFrame 是一种以 RDD 为基础的分布式数据集，类似与传统数据库中的二维表格
- DataFrame 与RDD的主要区别在于，前者有 schema 元数据信息，即 DataFrame 所表示的二维表数据集的每一列都带有名称和类型
- 使用 Spark SQL 得以洞察更多的结构信息，从而对藏于 DataFrame 背后的数据源以及作用与 DataFrame 之上的变换进行了针对性的优化，最终达到大幅提升运行时效率的目标
- 反观 RDD，由于无从得知所存数据的具体内部结构，Spark Core 只能在 Stage 层面进行简单的、通用的流水线优化

**DataFrame vs RDD**

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/172.jpg" alt="image" style="zoom: 67%;" />

- 提升执行效率
  - RDD API 是函数式的，强调不变性，在大部分场景下倾向于创建新对象而不是修改老对象。这一特
    点虽然带来了干净整洁的 API，却也使得 Spark 应用程序在运行期倾向于创建大量临时对象，对 GC 造
    成压力
  - Spark SQL 在框架内部已经在各种可能的情况下尽量重用对象，这样做虽然在内部会打破了不变性，
    但在将数据返回给用户时，还会重新转为不可变数据。利用 DataFrame API 进行开发，可以免费地
    享受到这些优化效果

- 减少数据读取
  - 分区剪枝便：当查询的过滤条件中涉及到分区列时，我们可以根据查询条件剪掉肯定不包含目标数
    据的分区目录，从而减少 IO
  - Spark SQL 还可以根据数据文件中附带的统计信息来进行剪枝。简单来说，在这类数据格式中，数据
    是分段保存的，每段数据都带有最大值、最小值、null值数量等 一些基本的统计信息。当统计信息表
    名某一数据段肯定不包括符合查询条件的目标数据时，该数据段就可以直接跳过（例如某整数列a某
    段的最大值为100，而查询条件要求a > 200）
  - Spark SQL 也可以充分利用 RCFile、ORC、Parquet 等列式存储格式的优势，仅扫描查询真正涉及的
    列，忽略其余列的数据

- 执行优化
  对于普通开发者而言，查询优化器的意义在于，即便是经验并不丰富的程序员写出的次优的查询，也可
  以被尽量转换为高效的形式予以执行

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/173.jpg" alt="image" style="zoom: 80%;" />

**DataFrame vs DataSet**

- DataSet 也是 Spark 弹性分布式数据集
- DataSet 可以认为是 DataFrame 的一个特例，主要区别是 Dataset 每一个 record 存储的是一个强类型值而不是一个 Row
- DataFrame 和 DataSet 可以相互转化，`df.as[ElementType]`这样可以把 DataFrame 转化为 DataSet，`ds.toDF()`这样可以把 DataSet 转化为 DataFrame

### RDD 与 DataFrame、DataSet 转换

- RDD 转 DataFrame

  ```scala
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
  ```

- RDD 转 DataSet

  ```scala
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
  ```

- **DataSet 转 DataFrame**

  ```scala
  /**
  * dataSet转DataFrame
  */
  def dataSetToDataFrame(spark:SparkSession):DataFrame = {
      // For implicit conversions like converting RDDs to DataFrames
      import spark.implicits._
      //读取数据
      val ds = spark.read
                  .textFile("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\wc.txt")
                  .flatMap(_.split("\\s+"))
                  .map((_,1))
  
      val df=ds.toDF("word","count")
      df
  }
  ```

- **DataSet 转 RDD**

  ```scala
  /**
  * dataSet 转 RDD
  */
  def dataSetToRDD(spark:SparkSession)={
      val rdd = spark.read
                      .textFile("D:\\大数据项目实战\\26SparkSQL离线计算\\数据集\\wc.txt")
                      .rdd
      rdd
  }
  ```

- **DataFrame 转 RDD**

  ```scala
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
  ```

  



