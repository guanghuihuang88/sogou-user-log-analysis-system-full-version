# Spark 快速入门

## 1 Spark 大数据计算引擎

> Spark 是一个用来实现快速而通用的集群计算的平台。
>
> 在速度方面， Spark 扩展了广泛使用的 MapReduce 计算模型，而且高效地支持更多计算模式，包括交互式查询和流处理。 在处理大规模数据集时，速度是非常重要的。速度快就意味着我们可以进行交互式的数据操作， 否则我们每次操作就需要等待数分钟甚至数小时
> Spark 的一个主要特点就是能够在内存中进行计算， 因而更快。不过即使是必须在磁盘上进行的复杂计算， Spark 依然比 MapReduce 更加高效

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/126.jpg" alt="image" style="zoom:80%;" />

- Spark Core
  - Spark Core 实现了Spark 的基本功能，包含任务调度、内存管理、错误恢复、与存储系统交互等模块
  - Spark Core 中还包含了对弹性分布式数据集 RDD（ resilient distributed dataset）的 API 定义。 RDD 表示分布在多个计算节点上可以并行操作的元素集合，是Spark 主要的编程抽象。 Spark Core 提供了创建和操作这些集合的多个 API
- Spark SQL
  - Spark SQL 是Spark 用来操作结构化数据的程序包
  - 使用Hive SQL
  - 多数据源（Hive表、Parquet、JSON等）
  - SQL与RDD编程结合
  - 从Shark到Spark SQL
- Spark Streaming
  - Spark 提供的对实时数据进行流式计算的组件
  - 微批处理(Storm、Flink)—从批处理到流处理

- Spark Mllib
  - Spark 提供的包含常见机器学习（ML）功能的库
  - 分类、回归、聚类、协同过滤等
  - 模型评估、数据导入等额外的支持功能
  - Mahout(Runs on distributed Spark, H2O, and Flink)

- GraphX
  - GraphX是Spark 提供的图计算和图挖掘的库。
  - 与Spark Streaming 和Spark SQL 类似，GraphX 也扩展了Spark 的RDD API，能用来创建一个顶点和边都包含任意属性的有向图
  - GraphX还支持针对图的各种计算和常见的图算法

|          | spark     | Storm | hadoop      |
| -------- | --------- | ----- | ----------- |
| 流式计算 | Streaming | Storm | 无          |
| 批计算   | Core      | 无    | Map/reduce  |
| 图计算   | GraphX    | 无    | 无          |
| 机器学习 | MLlib     | 无    | Mahout      |
| SQL      | DataFrame | 无    | Drill、hive |

## 2 Spark 最简安装

> Apache 版本下载地址：http://spark.apache.org/downloads.html
> CDH 版本下载地址：http://archive-primary.cloudera.com/cdh5/cdh/5/

- 下载：这里选择`spark-2.3.0-bin-hadoop2.6.tgz`，以及`scala-2.11.8.tgz `

- 解压：`tar -zxvf spark-2.3.0-bin-hadoop2.6.tgz`以及`tar -zxvf scala-2.11.8.tgz`

- 创建软连接：`ln -s spark-2.3.0-bin-hadoop2.6/ spark`以及`ln -s scala-2.11.8/ scala`

- 配置 scala 环境变量：`vi ~/.bashrc`，并使其生效：`source ~/.bashrc`

  ```shell
  SCALA_HOME=/home/hadoop/app/scala
  PATH=$JAVA_HOME/bin:$SCALA_HOME/bin:
  ```

- 查看 scala 版本：`scala -version`

- 进入 spark：`bin/spark-shell`，实现 wordcount 功能（退出：`:quit`）

  - `val line = sc.textFile("/home/hadoop/app/spark/test.txt")`
  - `val lines = line.flatMap(_.split("\t"))`
  - `val mapword = lines.map((_,1))`
  - `val reduceword = mapword.reduceByKey(_+_)`
  - `reduceword.collect().foreach(println)`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/127.jpg" alt="image" style="zoom:80%;" />

## 3 基于 Eclipse Spark 程序开发

### 开发 Java 版本 Wordcount

- 打开之前创建的 hadoop-test 项目，创建新的包`com.spark`

- maven 依赖

  ```xml
  <!-- https://mvnrepository.com/artifact/org.apache.spark/spark-core -->
  <dependency>
  <groupId>org.apache.spark</groupId>
  <artifactId>spark-core_2.11</artifactId>
  <version>2.3.0</version>
  </dependency>
  ```

- maven 打包插件

  ```xml
  <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>2.4.1</version>
      <executions>
      	<!-- Run shade goal on package phase -->
          <execution>
              <phase>package</phase>
              <goals>
                  <goal>shade</goal>
              </goals>
              <configuration>
                  <transformers>
                      <!-- add Main-Class to manifest file -->
                      <transformer
                      implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer"
                      >
                          <!--<mainClass>com.spark</mainClass>-->
                      </transformer>
                  </transformers>
                  <createDependencyReducedPom>false</createDependencyReducedPom>
              </configuration>
           </execution>
      </executions>
  </plugin>
  ```

- Java 版本 Wordcount

  ```java
  package com.spark;
  
  import java.util.Arrays;
  import java.util.Iterator;
  
  import org.apache.spark.SparkConf;
  import org.apache.spark.api.java.JavaPairRDD;
  import org.apache.spark.api.java.JavaRDD;
  import org.apache.spark.api.java.JavaSparkContext;
  import org.apache.spark.api.java.function.FlatMapFunction;
  import org.apache.spark.api.java.function.Function2;
  import org.apache.spark.api.java.function.PairFunction;
  
  import scala.Tuple2;
  
  
  public class MyJavaWordCount {
  		
  	public static void main(String[] args) {
  		//参数检查
  		if(args.length < 2) {
  			System.err.println("Usage: MyJavaWordCount input output");
  			System.exit(1);
  		}
  		
  		//输入输出路径
  		String inputPath = args[0];
  		String outputPath = args[1];
  		
  		//创建SparkContext
  		SparkConf conf = new SparkConf().setAppName("MyJavaWordCount");
  		JavaSparkContext jsc = new JavaSparkContext(conf);
  		
  		//读取数据
  		JavaRDD<String> inputRDD = jsc.textFile(inputPath);
  		
  		//flatmap扁平化操作
  		JavaRDD<String> words = inputRDD.flatMap(new FlatMapFunction<String, String>() {
  			private static final long serialVersionUID = 1L;
  
  			@Override
  			public Iterator<String> call(String line) throws Exception {
  				// TODO Auto-generated method stub
  				return Arrays.asList(line.split("\t")).iterator();
  			}
  		});
  		
  		//map 操作
  		JavaPairRDD<String, Integer> pairRDD = words.mapToPair(new PairFunction<String, String, Integer>(){
  			private static final long serialVersionUID = 1L;
  
  			@Override
  			public Tuple2<String, Integer> call(String word) throws Exception {
  				// TODO Auto-generated method stub
  				return new Tuple2<String, Integer>(word, 1);
  			}
  		});
  		
  		//reduce 操作
  		JavaPairRDD<String, Integer> result = pairRDD.reduceByKey(new Function2<Integer, Integer, Integer>(){
  			private static final long serialVersionUID = 1L;
  
  			@Override
  			public Integer call(Integer x, Integer y) throws Exception {
  				// TODO Auto-generated method stub
  				return x+y;
  			}
  		});
  		
  		//保存结果
  		result.saveAsTextFile(outputPath);
  		
  		//关闭jsc
  		jsc.close();
  		
  	}
  }
  ```

- 将代码打包`javaWordCount.jar`，上传到 hadoop01 路径`~/app/spark/`下

- 通过 spark-submit 提交 spark Wordcount 作业

  `bin/spark-submit --master local[2] --class (包+类)名 jar包名 输入路径 输出路径`

  如执行：`bin/spark-submit --master local[2] --class com.spark.MyJavaWordCount javaWordCount.jar /home/hadoop/app/spark/test.txt /home/hadoop/app/spark/out`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/128.jpg" alt="image" style="zoom:80%;" />

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/129.jpg" alt="image" style="zoom:80%;" />

### 开发 Scala 版本 Wordcount

- 在 eclipse 中安装Scala 插件：help->eclipse marketplace->搜索scala->install

- 新建 Maven project 参考我的另一篇博客：[Eclipse与MapReduce集成开发](http://www.guanghuihuang.cn/2021/01/20/%E5%A4%A7%E6%95%B0%E6%8D%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/%E3%80%90%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F%E3%80%91(%E5%85%AD)%EF%BC%9AEclipse%E4%B8%8EMapReduce%E9%9B%86%E6%88%90%E5%BC%80%E5%8F%91/)

  - Group Id：`com.dsj.spark`
  - Artifact Id：`testSpark`

  - Package：`com.dsj.spark.testSpark`

- 修改项目 jdk 为1.8
- 为项目增加 scala 特性：右击项目 -> configure -> add scala nature
- 新建 scala 目录：右击项目-> new -> source folder：`src/main/scala`

- 在`src/main/scala`目录下新建包：new -> package：`com.dsj.spark.testSpark`

- 在`com.dsj.spark.testSpark`包中新建 scala 类：new -> others -> scala object：`Test`

- 通过 maven 引入 spark 依赖包

  ```xml
  <!-- https://mvnrepository.com/artifact/org.apache.spark/spark-core -->
  <dependency>
      <groupId>org.apache.spark</groupId>
      <artifactId>spark-core_2.11</artifactId>
      <version>2.3.0</version>
  </dependency>
  ```

- 添加 scala 库

  ```xml
  <dependency>
      <groupId>org.scala-lang</groupId>
      <artifactId>scala-library</artifactId>
      <version>${scala.version}</version>
  </dependency>
  ```

- 添加 scala 版本

  ```xml
  <properties>
  	<scala.version>2.11.8</scala.version>
  </properties>
  ```

- 添加打包插件

  ```xml
  <build>
  		<sourceDirectory>src/main/scala</sourceDirectory>
  		<testSourceDirectory>src/test/scala</testSourceDirectory>
  		<plugins>
  			<plugin>
  				<groupId>org.scala-tools</groupId>
  				<artifactId>maven-scala-plugin</artifactId>
  				<executions>
  					<execution>
  						<goals>
  							<goal>compile</goal>
  							<goal>testCompile</goal>
  						</goals>
  					</execution>
  				</executions>
  				<configuration>
  					<scalaVersion>${scala.version}</scalaVersion>
  					<args>
  						<arg>-target:jvm-1.5</arg>
  					</args>
  				</configuration>
  			</plugin>
  			<plugin>
  				<groupId>org.apache.maven.plugins</groupId>
  				<artifactId>maven-eclipse-plugin</artifactId>
  				<configuration>
  					<downloadSources>true</downloadSources>
  					<buildcommands>
  						<buildcommand>ch.epfl.lamp.sdt.core.scalabuilder</buildcommand>
  					</buildcommands>
  					<additionalProjectnatures>
  						<projectnature>ch.epfl.lamp.sdt.core.scalanature</projectnature>
  					</additionalProjectnatures>
  					<classpathContainers>
  						<classpathContainer>org.eclipse.jdt.launching.JRE_CONTAINER</classpathContainer>
  						<classpathContainer>ch.epfl.lamp.sdt.launching.SCALA_CONTAINER</classpathContainer>
  					</classpathContainers>
  				</configuration>
  			</plugin>
  		</plugins>
  	</build>
  	<reporting>
  		<plugins>
  			<plugin>
  				<groupId>org.scala-tools</groupId>
  				<artifactId>maven-scala-plugin</artifactId>
  				<configuration>
  					<scalaVersion>${scala.version}</scalaVersion>
  				</configuration>
  			</plugin>
  		</plugins>
  	</reporting>
  ```

- 测试项目打包，进入项目根目录cmd：`mvn clean package`

- scala 版本 Wordcount

  ```scala
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
  ```

- 通过 maven 对项目打包（也可以使用 export 导出方式）上传到spark节点（hadoop01）

- 通过 spark-submit 提交 spark Wordcount 作业：`bin/spark-submit --master local[2] --class 类名 包名 输入路径 输出路径`

  





