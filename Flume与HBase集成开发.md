# Flume+HBase 集成开发

> 相关配置文件可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6

- 启动 Zookeeper 集群：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 注意集群时钟必须同步：

  - 同步时间：`sudo ntpdate pool.ntp.org`
  - 查看时间：`date`

- 启动 HDFS：`sbin/start-dfs.sh`

- 在 hadoop01 启动 HBase（在哪个节点启动 HBase，哪个节点就是 master 角色）：`bin/start-hbase.sh`

- 修改配置文件`flume-env.sh`配置 Flume 环境变量

  ```shell
  export HADOOP_HOME=/home/hadoop/app/hadoop
  export HBASE_HOME=/home/hadoop/app/hbase
  ```


## 1 Flume+HBase 最简集成

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/096.jpg" alt="image" style="zoom: 33%;" />

> 按照官方文档中配置 HBase Sink：User Guide —> Configuration —> Flume Sink —> AsyncHBaseSink

- 在 hadoop01 配置`flume-hbase-simple.properties`

  这里使用`test.log`数据，需要把之前在 hadoop02 创建的`/home/hadoop/data/flume/logs/test.log`拷贝到 hadoop01

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = execSource
  agent.channels = memoryChannel
  agent.sinks = hbaseSink
  
  # For each one of the sources, the type is defined
  agent.sources.execSource.type = exec
  agent.sources.execSource.channels = memoryChannel
  agent.sources.execSource.command = tail -F /home/hadoop/data/flume/logs/test.log
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.hbaseSink.type = asynchbase
  agent.sinks.hbaseSink.table  = wordcount
  agent.sinks.hbaseSink.columnFamily  = frequency
  agent.sinks.hbaseSink.serializer = org.apache.flume.sink.hbase.SimpleAsyncHbaseEventSerializer
  agent.sinks.hbaseSink.channel = memoryChannel
  ```

- 进入`bin/hbase shell`创建 HBase 测试表

  - `disable 'wordcount'`
  - `drop 'wordcount'`
  - `create 'wordcount','frequency'`

- 重新开一个 hadoop01 会话启动 flume

  启动 flume 监控日志文件，并将数据发送到 HBase 数据库

  `bin/flume-ng agent -n agent -c conf -f conf/flume-hbase-simple.properties -Dflume.root.logger=INFO,console`

- 在进入`bin/hbase shell`的会话中执行`scan 'wordcount'`，可以看到传入 HBase 的数据

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/097.jpg" alt="image"  />

## 2 Flume+Kafka 集成配置

> 搜狗用户日志数据较复杂，数据格式有六列，因此需要修改 Flume 源码

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/099.jpg" alt="image" style="zoom: 33%;" />

- 这里使用`sogoulogs.log`数据，进入`bin/hbase shell`创建 HBase 测试表
  - `disable 'sogoulogs'`
  - `drop 'sogoulogs'`
  - `create 'sogoulogs','info'`

### 修改配置文件

- 在 hadoop01 创建配置文件`flume-hbase-costum.properties`，注意：

  - payloadColumn 分别对应搜狗数据的六个列：

    `访问时间\t用户ID\t[查询词]\t该URL在返回结果中的排名\t用户点击的顺序号\t用户点击的URL`

  - serializer 使用下一节再flume源码基础上修改后的 jar包`org.apache.flume.sink.hbase.DsjAsyncHbaseEventSerializer`

  ```properties
  # The configuration file needs to define the sources, 
  # the channels and the sinks.
  # Sources, channels and sinks are defined per agent, 
  # in this case called 'agent'
  
  agent.sources = execSource
  agent.channels = memoryChannel
  agent.sinks = hbaseSink
  
  # For each one of the sources, the type is defined
  agent.sources.execSource.type = exec
  agent.sources.execSource.channels = memoryChannel
  agent.sources.execSource.command = tail -F /home/hadoop/data/flume/logs/test.log
  
  # Each channel's type is defined.
  agent.channels.memoryChannel.type = memory
  agent.channels.memoryChannel.capacity = 100
  
  # Each sink's type must be defined
  agent.sinks.hbaseSink.type = asynchbase
  agent.sinks.hbaseSink.table  = sogoulogs
  agent.sinks.hbaseSink.columnFamily  = info
  agent.sinks.hbaseSink.zookeeperQuorum = hadoop01:2181,hadoop02:2181,hadoop03:2181
  agent.sinks.hbaseSink.serializer.payloadColumn = logtime,uid,keywords,resultno,clickno,url
  agent.sinks.hbaseSink.znodeParent = /hbase
  agent.sinks.hbaseSink.serializer = org.apache.flume.sink.hbase.DsjAsyncHbaseEventSerializer
  agent.sinks.hbaseSink.channel = memoryChannel
  ```

### Eclipse 开发 Flume 源码

> 相关源码包及其开发后的jar包可在我的github项目仓库克隆：https://github.com/guanghuihuang88/sogou-user-log-analysis-system/tree/master/%E6%BA%90%E7%A0%81%E5%8C%85

- 打开 Eclipse 导入 Flume 源码`flume-ng-hbase-sink`，右键import —> Maven —> Existing Maven Projects —> Browse 选择文件夹：

  `sogou-user-log-analysis-system\源码包\Flume\apache-flume-1.7.0-src\apache-flume-1.7.0-src\flume-ng-sinks\flume-ng-hbase-sink`

- 导入后可能报错，建议直接百度解决，参考如下博客：

  https://blog.csdn.net/huoyunshen88/article/details/40657895

  https://www.cnblogs.com/asderx/p/6541945.html

### ⭐flume-ng-hbase-sink 源码修改

- 因为 payloadColumn 对应六个列，需要修改 flume 源码：`DsjAsyncHbaseEventSerializer`类

  ```java
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DsjAsyncHbaseEventSerializer.class);//借助log4j生成日志辅助调试，等程序跑通了，再把logger删去即可
  
  @Override
  public List<PutRequest> getActions() {
      List<PutRequest> actions = new ArrayList<PutRequest>();
      if (payloadColumn != null) {
          byte[] rowKey;
          try {
              //logger.info("entry DsjAsyncHbaseEventSerializer");
              //logger.info("payload="+new String(this.payload));
              //logger.info("payloadColumn="+new String(this.payloadColumn));
              String[] columns = new String(this.payloadColumn).split(",");
              String[] values = new String(this.payload).split(",");
              //logger.info("columns size="+columns.length);
              //logger.info("values size = "+values.length);
              if(columns.length != values.length){
                  return actions;
              }
  
  
              String logtime = values[0].toString();
  
              String uid = values[1].toString();
  
              rowKey = SimpleRowKeyGenerator.getDsjRowKey(uid, logtime);
              ///logger.info("rowkey="+rowKey);
              for(int i=0;i<columns.length;i++){
                  byte[] colColumn = columns[i].getBytes();
                  byte[] colValue = values[i].getBytes(Charsets.UTF_8);
                  PutRequest putRequest =  new PutRequest(table, rowKey, cf,
                                                          colColumn, colValue);
                  actions.add(putRequest);
                  //logger.info("插入数据"+i);
              }
  
          } catch (Exception e) {
              throw new FlumeException("Could not get row key!", e);
          }
      }
      return actions;
  }
  ```

- 自定义 rowkey：修改`SimpleRowKeyGenerator`类，添加方法`getDsjRowKey`

  ```java
  /**
     * 自定义rowkey
     * @param uid
     * @param logtime
     * @return
     * @throws UnsupportedEncodingException
     */
  public static byte[] getDsjRowKey(String uid,String logtime) throws UnsupportedEncodingException {
      return (uid + logtime + String.valueOf(System.currentTimeMillis())).getBytes("UTF8");
  }
  ```

- 在项目路径 用 cmd 通过 Maven 导出 jar包`flume-ng-hbase-sink-1.7.0.jar`：`mvn clean package`

### 自定义代码部署

> Flume 官方提供了一种简单部署自定义代码的方式：plugins.d 框架。plugins.d 目录是自动添加到 Flume 环境变量的，因此不需要明确添加到 FLUME_CLASSPATH 环境变量中。对于每个自定义组件，可以在 plugins.d 目录中创建一个新的子目录（名字可以随意），在每个子目录中，Flume 会有三个目录：
>
> - Lib ：该目录包含了实际的 JAR 文件，JAR 文件包含了插件的类和可以进入配置文件的 Builder，通过Flume 进行实例化
> - Libext：该目录包含了插件依赖的内部依赖
> - Native：该目录包含了任何需要通过 Java Native Interface 加载的本地库

- hadoop01 进入 Flume 的`/lib/`目录，将默认的hbase jar包`flume-ng-hbase-sink-1.7.0.jar`改名

  `mv flume-ng-hbase-sink-1.7.0.jar flume-ng-hbase-sink-1.7.0.jar.bak`

- 在 Flume `/`目录下创建

  - `plugins.d/Serializer/lib`：并将修改的jar包`flume-ng-hbase-sink-1.7.0.jar`放到该路径下

### 测试运行

- 先清空`/home/hadoop/data/flume/logs/test.log`中的数据：

  `cat /dev/null > /home/hadoop/data/flume/logs/test.log`

- 启动 flume 监控日志文件，并将数据发送到 HBase 数据库

  `bin/flume-ng agent -n agent -c conf -f conf/flume-hbase-custom.properties -Dflume.root.logger=INFO,console`

- 此时 HBase 中 sogou 表还没有数据

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/098.jpg" alt="image"  />

- 随便复制一条 `sogoulogs.log` 的数据到`test.log`文件

  `echo “00:09:40,2739469928397069,[汶川震后十分钟],8,29,nv.qianlong.com/33530/2008/05/16/2400@4444820.htm” >> test.log`

- 在启动后的 Flume 会话中可以看到数据插入信息：

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/100.jpg" alt="image"  />

- 进入`bin/hbase shell`的会话中执行`scan 'sogoulogs'`，可以看到传入 HBase 的数据

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/101.jpg" alt="image"  />

  

  

















