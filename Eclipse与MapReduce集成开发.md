# Eclipse与MapReduce集成开发

## 1 JDK\Eclipse\Maven安装

> 本项目安装的 JDK1.8 版本（略）

- 验证JDK安装是否成功：`java -version`

> 本项目安装的 maven-3.3.9 版本

- 可以到官网下载需要的 maven 版本，下载地址：http://maven.apache.org/docs/history.html
- 官网下载对应 maven 版本后，解压到本地的一个目录即可
- 配置 maven 环境变量
  - 在 path 的变量值最后加上`%MAVEN_HOME%\bin`
  - 新建系统变量：变量名：`MAVEN_HOME`，变量值：`C:\Program Files\apache-maven-3.3.9`
- 验证 maven 安装是否成功：`mvn -version`

## 2 Eclipse集成Maven

- 打开 Eclipse 点击 window>prferences 之后会弹出；选中 add 添加按钮，添加 maven 安装路径

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/044.jpg" alt="image" style="zoom:80%;" />

- 连接本地仓库：找到你的 maven 解压文件夹的`conf`子文件夹（`C:\Program Files\apache-maven-3.3.9\conf`）然后编辑 `settings.xml`

  可以修改 localRepository 本地仓库目录，默认是在`${user.home}/.m2/repository`目录下

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/045.jpg" alt="image"  />

- 点击 window->preferences，搜索 maven 选中 User Setting

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/046.jpg" alt="image" style="zoom: 80%;" />

- Ok，到此 maven 配置完毕

## 3 Eclipse构建Maven项目

- new Maven project

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/047.jpg" alt="image" style="zoom: 80%;" />

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/048.jpg" alt="image" style="zoom: 80%;" />

- 选择要构建的骨架 maven-archetype-quickstart

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/049.jpg" alt="image" style="zoom: 80%;" />

- GroupID 是项目组织唯一的标识符，实际对应 JAVA 的包的结构，是 main 目录里 java 的目录结构
  ArtifactID 就是项目的唯一的标识符，实际对应项目的名称，就是项目根目录的名称

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/050.jpg" alt="image" style="zoom: 80%;" />

- 点击 finish 完成 maven 项目构建，运行示例程序 app

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/051.jpg" alt="image" style="zoom: 80%;" />

- 更改默认 jre 为 1.8版本

  - 点击 window->preferences，搜索 jdk

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/052.jpg" alt="image" style="zoom: 80%;" />

  - 右击项目名 -> Build Path -> Configure Build Path

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/053.jpg" alt="image" style="zoom: 80%;" />

  - 修改完成

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/054.jpg" alt="image"  />



## 4 Eclipse集成开发MapReduce

### 编写 Wordcount

- 拷贝 Hadoop 官网 Wordcount 源码：
  http://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html#Example:_WordCount_v1.0
- new 一个类 wordcount.java，复制 Wordcount 源码

###  添加依赖包

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

- 通过 maven 自动下载 Wordcount 所需要的依赖包：http://mvnrepository.com/

- 在 pom.xml 中添加如下依赖

  ```xml
  <dependency>
      <groupId>org.apache.hadoop</groupId>
      <artifactId>hadoop-common</artifactId>
      <version>2.6.0</version>
      <scope>provided</scope>
  </dependency>
  
  <dependency>
      <groupId>org.apache.hadoop</groupId>
      <artifactId>hadoop-client</artifactId>
      <version>2.6.0</version>
      <scope>provided</scope>
  </dependency>
  
  <dependency>
      <groupId>jdk.tools</groupId>
      <artifactId>jdk.tools</artifactId>
      <version>1.7</version>
      <scope>system</scope>
      <systemPath>${JAVA_HOME}/lib/tools.jar</systemPath>
  </dependency>
  ```

###  添加 log4j

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

- 拷贝`log4j.properties`文件

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/055.jpg" alt="image"  />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/056.jpg" alt="image"  />

### hadoop2.6补丁

> 此处需要用到本项目提前准备的`hadoop2.6补丁`文件夹，将其拷贝到window下合适的目录下，并配置hadoop环境变量

- 在 Window 下配置 hadoop 环境变量

  - 在 Path 中添加`%HADOOP_HOME%\bin`
  - 新建系统变量`HADOOP_HOME`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/057.jpg" alt="image" style="zoom:80%;" />

- 拷贝`hadoop2.6补丁\hadoop-2.6.0\bin`下的`winutils.exe`和`hadoop.dll`两个文件到`C:\Windows\System32`路径下

### 本地运行 Wordcount

- 右键—>Run Configuration，配置 main class 为`com.hadoop.hadoop_test.WordCount`

- 配置 Program arguments 参数：`输入路径 输出路径`（注意，输出路径名为不存在的目录`out/`）

  这里，我们临时创建文件`wc.txt`作为输入文件

  ```txt
  hadoop	hadoop	hadoop
  java	java	java
  spark	spark	spark
  ```

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/058.jpg" alt="image" style="zoom:80%;" />

- 在输出路径下可以看到结果：

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/059.jpg" alt="image" style="zoom:80%;" />

### 项目打包

- Export 方式打包（对单个文件打包）

  - 右击项目名，export—> Java —> JAR file
  - 仅选择需要打包的类
  - 输入打包路径并命名

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/060.jpg" alt="image" style="zoom:80%;" />

- Maven 方式打包（对整个项目打包）

  - 找到项目的类路径（`F:\Mycode\Eclipse\Workshop\hadoop-test`）

  - 打开 cmd，cd 到项目路径下，执行`mvn clean package`

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/061.jpg" alt="image" style="zoom:80%;" />

  - 在项目的`target/`路径下，可以找到打包后的整个项目

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/062.jpg" alt="image" style="zoom:80%;" />

### 集群运行 Wordcount

> 这里汇总集群启动的相关代码：
>
> - 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`
> - 启动 HDFS：`sbin/start-dfs.sh`
> - 启动 YARN：`sbin/start-yarn.sh`
>   - （在 hadoop02 上）启动备用节点：`sbin/yarn-daemon.sh start resourcemanager`

- 将之前两种打包方式的 jar 包利用 Xftp 上传到 hadoop01 节点的`~/app/hadoop`路径下，并分别测试两个 jar 包是否可以成功执行：（注意`com.hadoop.hadoop_test`包名+`WordCount`类名）
  - 命令：`bin/hadoop jar wc.jar com.hadoop.hadoop_test.WordCount /test/wc.txt /test/out1`
  - 命令：`bin/hadoop jar hadoop-test-0.0.1-SNAPSHOT.jar com.hadoop.hadoop_test.WordCount
    /test/wc.txt /test/out2`
  
  - 在命令中省略包名的方法（略）





















