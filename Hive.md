# Hive 概述及其部署

## 1 Hive 大数据仓库

> 它是由 faceBook 开源，最初用于解决海量结构化的日志数据统计问题，它可以作为 ETL 工具；是构建在 Hadoop 之上的数据仓库，数据计算是 MapReduce，数据存储是 HDFS，适合离线数据处理
>
> 它定义了一种类 SQL 的查询语言—— HQL，它是将 HQL 转换为 MR 的语言翻译器

### Hive 与 Hadoop 之间的关系

- Hive 使用 HQL 作为查询接口层
- Hive 使用 MapReduce 作为执行层，统计分析数据
- Hive 使用 HDFS 作为底层存储层，存储海量数据

### Hive 与 关系型数据库之间的关系

| 查询语言     | HiveQL       | SQL                  |
| ------------ | ------------ | -------------------- |
| 数据存储位置 | HDFS         | Raw Device 或 本地FS |
| 数据格式     | 用户定义     | 系统决定             |
| 数据更新     | 不支持       | 支持                 |
| 索引         | 新版本有，弱 | 有                   |
| 执行         | MapReduce    | Executor             |
| 执行延迟     | 高           | 低                   |
| 可扩展性     | 高           | 低                   |
| 数据规模     | 大           | 小                   |

## 2 Hive 体系结构及原理

### 体系结构

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/112.jpg" alt="image"  />

### 用户接口

Hive对外提供了三种服务模式：即 Hive 命令行模式（CLI）、Hive 的 Web 模式（WUI）、Hive 的远程服务（Client）。下面介绍这些服务的用法

- Hive 命令行模式 ：`./hive`

  在终端输入hql语句与hive进行交互分析

- Hive Web 模式：

  Hive Web 界面的启动命令：`hive --service hwi`，通过浏览器访问 Hive，默认端口为 9999

- Hive 的远程服务

  远程服务（默认端口号 10000）启动方式命令如下，“nohup…&” 是 Linux 命令，表示命令在后台运行
  `nohup hive --service hiveserver2`

  Hive 远程服务通过 JDBC 等访问来连接 Hive ，这是程序员最需要的方式

### Driver原理

Hive 的 Driver 是 hive 的一个组件，负责解析和优化 HQL 语句，将其转换成一个 Hive Job（可以是 MapReduce，也可以是 Spark 等其他任务）并提交给 Hadoop 集群

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/113.jpg" alt="image"  />

SQL 转化为 MapReduce 任务的，整个编译过程分为六个阶段：

- Antlr 定义 SQL 的语法规则，完成 SQL 词法，语法解析，将 SQL 转化为抽象语法树 AST Tree
- 遍历 AST Tree，抽象出查询的基本组成单元 QueryBlock
- 遍历 QueryBlock，翻译为执行操作树 OperatorTree
- 逻辑层优化器进行 OperatorTree 变换，合并不必要的 ReduceSinkOperator，减少 shuffle 数据量
- 遍历 OperatorTree，翻译为 hive job 任务
- 物理层优化器进行 hive job 任务的变换，生成最终的执行计划

### Metastore 安装方式

Hive 将元数据存储在 RDBMS 中，一般常用 MySQL 和 Derby。默认情况下，Hive元数据保存在内嵌的 Derby 数据库中，只能允许一个会话连接，只适合简单的测试。实际生产环境中不适用， 为了支持多用户会话，则需要一个独立的元数据库，使用 MySQL 作为元数据库，Hive 内部对 MySQL 提供了很好的支持，配置一个独立的元数据库即可，第一次执行`./hive`的时候会初始化元数据

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/114.jpg" alt="image"  />

## 2 Hive 安装部署

|       | hadoop01 | hadoop02 | hadoop03 |
| ----- | -------- | -------- | -------- |
| hive  |          |          | √        |
| MySQL |          |          | √        |

### Mysql 安装

- yum 在线安装 mysql：`yum install mysql-server`

- 查看 mysql 服务状态：`service mysqld status`

- 启动 mysql 服务：`service mysqld start `

- 设置 mysql root 用户密码：

  刚安装完成，root 用户默认是没有密码的。第一次登陆输入以下命令：`mysql -u root -p`两次回车即可进入 mysql，输入如下命令设置 root 用户密码：`set password for root@localhost=password('root');`

- 创建 hive 账号

  - 创建 hive 用户：`create user 'hive' identified by 'hive';`

  - 将 mysql 所有权限授予 hive 用户：`grant all on *.* to 'hive'@'hadoop03' identified by 'hive';`

  - 刷新信息：`flush privileges;`

  - 查看 mysql 用户表 user：`select host,user,password from mysql.user;`

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/116.jpg" alt="image"  />

  - 更新用户信息（授权所有 ip 连接 mysql）：`update mysql.user set Host='%' where User = 'root' and Host='localhost'`

  - 删除 MySQL 用户信息（删除本地连接）

    `delete from mysql.user where user='root' and host='127.0.0.1';`

    `delete from mysql.user where host='hadoop03';`

  - 查看 mysql 用户表 user：`select host,user,password from mysql.user;`

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/115.jpg" alt="image"  />

- 创建 hive 数据库

  - 使用 hive 用户登录 mysql：`mysql -h hadoop03 -u hive -p `（密码为 hive）
  - 创建数据库 hive：`create database hive;`
  - 查看所有数据库：`show databases;`

### Hive 安装

> CDH 版本：http://archive-primary.cloudera.com/cdh5/cdh/5/

- 下载`hive-1.1.0-cdh5.10.0.tar.gz`版本的安装包，上传至 hadoop03 节点的`/home/hadoop/app`目录下

- 解压命令：`tar -zxvf hive-1.1.0-cdh5.10.0.tar.gz`

- 创建软连接：`ln -s hive-1.1.0-cdh5.10.0 hive`

### 修改配置文件

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

- 将`hive-log4j.properties.template`模板文件改为`hive-log4j.properties`，并修改日志目录

  ```properties
  hive.log.dir=/home/hadoop/app/hive/logs
  ```

- 将`hive-env.sh.template`模板文件改为`hive-env.sh`，并修改

  ```shell
  export HADOOP_HOME=/home/hadoop/app/hadoop
  export HIVE_CONF_DIR=/home/hadoop/app/hive/conf
  ```

- 修改`hive-site.xml`

  CDH 版本的 Hive 安装包解压后并没有`hive-site.xml`文件，可以从 Apache 版本中的解压文件获取`hive-default.xml.template`模板文件，然后改为`hive-site.xml`文件，并修改下面这些属性（用ctrl+f查找）

  另外，创建好`/home/hadoop/data/hive/iotmp`目录

  ```xml
  <configuration>
  	<property>
          <name>javax.jdo.option.ConnectionURL</name>
          <value>jdbc:mysql://hadoop03/metastore?createDatabaseIfNotExist=true</value>
  	</property>
  	<property>
          <name>javax.jdo.option.ConnectionDriverName</name>
          <value>com.mysql.jdbc.Driver</value>
  	</property>
  	<property>
   		<name>javax.jdo.option.ConnectionUserName</name>
  		<value>hive</value>
  	</property>
  	<property>
          <name>javax.jdo.option.ConnectionPassword</name>
          <value>hive</value>
  	</property>
      <property>
          <name>hive.exec.local.scratchdir</name>
          <value>/home/hadoop/data/hive/iotmp</value>
          <description>Local scratch space for Hive jobs</description>
    	</property>
    	<property>
          <name>hive.downloaded.resources.dir</name>
          <value>/home/hadoop/data/hive/iotmp</value>
          <description>Temporary local directory for added resources in the remote file system.</description>
    	</property>
      <property>
          <name>hive.querylog.location</name>
          <value>/home/hadoop/data/hive/iotmp</value>
          <description>Location of Hive run time structured log file</description>
    	</property>
  </configuration>
  ```

- 添加 mysql 驱动包

  下载`mysql-connector-java-5.1.38.jar`，将 mysql 驱动包拷贝到 hive 的 lib 目录下

### 启动 Hive

- 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 注意集群时钟必须同步：

  同步时间：`sudo ntpdate pool.ntp.org`
  查看时间：`date`

- 启动 HDFS：`sbin/start-dfs.sh`

- 启动 YARN：`sbin/start-yarn.sh`

- 启动 Hive：

  - 如果单独配置的 Metastore，得先启动 metastore 服务：`bin/hive --service metastore`
  - 启动 Hive：`bin/hive`

### 测试 Hive

- 通过 hive 服务创建表：

  `CREATE TABLE stu(id INT,name STRING) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t';`

- 查询表：`show tables`

- 加载数据到 hive 表：

  `load data local inpath '/home/hadoop/app/hive/stu.txt' into table stu;`

- 查询 hive 表数据：`select * from stu;`

## 3 Hive 图像界面配置运行

> 下载对应版本的 hive 源码包`hive-1.1.0-cdh5.10.0-src.tar.gz`，上传至 hadoop03 (规划节点)节点的`/home/hadoop/software`目录下，注意不要和`hive-1.1.0-cdh5.10.0.tar.gz`包解压后的 hive 文件夹放在一起，因为解压后文件重名会造成覆盖

- 进入`/home/hadoop/software/hive-1.1.0-cdh5.10.0/hwi/web`目录，执行`jar cvf hive-hwi-1.0.0.war ./*`

- 将生成的jar包`hive-hwi-1.0.0.war`拷贝到`/home/hadoop/app/hive/lib/`

- 从 jdk 中拷贝一个jar包到 hive：`cp ~/app/jdk/lib/tools.jar ~/app/hive/lib/`

- 修改`hive-site.xml`配置文件

  ```xml
  <property>
      <name>hive.hwi.war.file</name>
      <value>lib/hive-hwi-1.0.0.war</value>
      <description>This sets the path to the HWI war file, relative to ${HIVE_HOME}.</description>
  </property>
  <property>
      <name>hive.hwi.listen.host</name>
      <value>0.0.0.0</value>
      <description>This is the host address the Hive Web Interface will listen on</description>
  </property>
  <property>
      <name>hive.hwi.listen.port</name>
      <value>9999</value>
      <description>This is the port the Hive Web Interface will listen on</description>
  </property>
  ```

- 启动 hive web 模式：`bin/hive --service hwi`

  通过浏览器访问 Hive，默认端口为 9999：http://hadoop03:9999/hwi/

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/117.jpg" alt="image"  />

## 4 Hive 实操

###  HiveServer1

HiveServer 是一种可选服务，允许远程客户端可以使用各种编程语言向 Hive 提交请求并检索结果。HiveServer 是建立在 Apache ThriftTM（http://thrift.apache.org/）之上的，因此有时会被称为 Thrift Server，这可能会导致混乱，因为新服务 HiveServer2 也是建立在 Thrift 之上的．自从引入 HiveServer2 后，HiveServe r也被称为HiveServer1

HiveServer 无法处理来自多个客户端的并发请求.这实际上是 HiveServer 导出的 Thrift 接口所施加的限制，也不能通过修改 HiveServer 源代码来解决

HiveServer2 对 HiveServer 进行了重写，来解决这些问题，从 Hive 0.11.0 版本开始。建议使用 HiveServer2。从 Hive1.0.0 版本（以前称为0.14.1版本）开始 HiveServer 开始被删除，请切换到 HiveServer2 即可

###  HiveServer2

HiveServer2(HS2) 是一种能使客户端执行 Hive 查询的服务。 HiveServer2 是 HiveServer1 的改进版，HiveServer1 已经被废弃。HiveServer2 可以支持多客户端并发和身份认证。旨在为开放 API客户端（如 JDBC 和 ODBC）提供更好的支持
HiveServer2 单进程运行，提供组合服务，包括基于 Thrift 的 Hive 服务（TCP 或 HTTP）和用于 Web UI 的 Jetty Web 服务器
基于 Thrift 的 Hive 服务是 HiveServer2 的核心，负责维护 Hive 查询（例如，从 Beeline）。Thrift 是构建跨平台服务的 RPC 框架

### HiveServer 与 HiveServer2 联系与区别

联系：

- 两者都允许远程客户端使用多种编程语言。通过 HiveServer 或者 HiveServer2，客户端可以在不启动 CLI 的情况下对 Hive 中的数据进行操作，允许远程客户端使用多种编程语言如 java，python 等向 hive 提交请求取回结果（从hive0.15 起就不再支持 hiveserver 了）
- HiveServer 或者 HiveServer2 都是基于 Thrift 的，但 HiveSever 有时被称为 Thrift server，而 HiveServer2 却不会。既然已经存在 HiveServer，为什么还需要 HiveServer2 呢？这是因为 HiveServer 不能处理多于一个客户端的并发请求，这是由于 HiveServer 使用的 Thrift 接口所导致的限制，不能通过修改 HiveServer 的代码修正。因此在 Hive-0.11.0 版本中重写了 HiveServer 代码得到了 HiveServer2，进而解决了该问题。 HiveServer2 支持多客户端的并发和认证，为开放 API 客户端（如 JDBC、ODBC）提供更好的支持

区别：

|             | Connection URL                        | Driver Class                           |
| ----------- | ------------------------------------- | -------------------------------------- |
| HiveServer2 | jdbc:hive2://:localhost:10000/default | org.apache.hive.jdbc.HiveDriver        |
| HiveServer1 | jdbc:hive://:localhost:10000/default  | org.apache.hadoop.hive.jdbc.HiveDriver |

###  JDBC 连接 HiveServer2

- Maven 引入依赖包

  ```xml
  <dependency>
      <groupId>org.apache.hive</groupId>
      <artifactId>hive-jdbc</artifactId>
      <version>1.0.0</version>
  </dependency>
  ```

- 连接 hiveserver2 相关参数

  ```java
  package com.hive;
  
  public class HiveConfigurationConstants {
  	public static final String CONFIG_DRIVERNAME = "org.apache.hive.jdbc.HiveDriver";
  	public static final String CONFIG_URL = "jdbc:hive2://hadoop03:10000/default";
  	public static final String CONFIG_USER = "hive";
  	public static final String CONFIG_PASSWORD = "hive";
  }
  ```

- Java核心代码如下所示：

  ```java
  package com.hive;
  
  import java.sql.Connection;
  import java.sql.DriverManager;
  import java.sql.ResultSet;
  import java.sql.SQLException;
  import java.sql.Statement;
  
  public class JDBCToHiveServer2 {
  
  	public static void main(String[] args) throws SQLException {
  		// TODO Auto-generated method stub
  		// 通过反射机制获取驱动程序
  		try {
  			Class.forName(HiveConfigurationConstants.CONFIG_DRIVERNAME);
  			// 建立连接
  			Connection conn = DriverManager.getConnection(HiveConfigurationConstants.CONFIG_URL,	HiveConfigurationConstants.CONFIG_USER,HiveConfigurationConstants.CONFIG_PASSWORD);
  			// 创建statement
  			Statement stmt = conn.createStatement();
  			// 准备SQL脚本
  			String sql = "select * from stu";
  			// statement执行脚本
  			ResultSet res = stmt.executeQuery(sql);
  			// 处理结果集
  			while (res.next()) {
  				System.out.println(res.getString(1)+"@"+res.getString(2));
  			}
  		} catch (ClassNotFoundException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
  	}
  }
  ```

- 启动hiveserver2：

  - 前台启动：`bin/hive --service hiveserver2`
  - 后台启动：`nohup bin/hive --service hiveserver2 &`

###  beeline 连接 HiveServer2

HiveServer2 支持一个新的命令行 Shell，称为 Beeline，它是基于 SQLLine CLI 的 JDBC 客户端。是从 Hive 0.11版本引入的，Hive 新的命令行客户端工具
Beeline 支持嵌入模式(embedded mode)和远程模式(remote mode)。在嵌入式模式下，运行嵌入式的 Hive(类似Hive CLI)，而远程模式可以通过 Thrift 连接到独立的 HiveServer2 进程上。从 Hive 0.14 版本开始，Beeline使用 HiveServer2 工作时，它也会从 HiveServer2 输出日志信息到 STDERR

（略）









