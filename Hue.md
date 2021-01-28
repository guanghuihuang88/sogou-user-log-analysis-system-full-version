# Hue 概述及其部署

## 1 Hue 数据可视化分析

> Hue 是一个开源的 Apache Hadoop UI 系统，最早是由 Cloudera Desktop 演化而来，由 Cloudera 贡献给开源社区，它是基于 Python Web 框架 Django 实现的。通过使用 Hue 我们可以在浏览器端的 Web 控制台上与 Hadoop 集群进行交互来分析处理数据，例如操作 HDFS 上的数据，运行 MapReduce Job 等等

## 2 Hue 安装配置

> Hue 下载地址：http://archive-primary.cloudera.com/cdh5/cdh/5/

- 下载`hue-3.9.0-cdh5.10.0.tar.gz`版本，然后上传至 hadoop03 节点的`/home/hadoop/app`目录下

- 解压：`tar -zxvf hue-3.9.0-cdh5.10.0.tar.gz`

- 创建软链接：`ln -s hue-3.9.0-cdh5.10.0/ hue`

- 安装依赖包：

  `yum install gcc g++ libxml2-devel libxslt-devel cyrus-sasl-devel cyrus-sasl-gssapi mysql-devel python-devel python-setuptools sqlite-devel ant libsasl2-dev libsasl2-modules-gssapi-mit libkrb5-dev libtidy-0.99-0 mvn libldap2-dev gmp-devel libffi-devel openssl-devel openldap-devel gcc-c++`

- 编译：

  - `cd /home/hadoop/app/hue`
  - `make apps`

- 编译成功：

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/123.jpg" alt="image"  />

- 修改`hue.ini`配置：`vi hue/desktop/conf/hue.ini`

  ```ini
  #秘钥查找：
  secret_key=jFE93j;2[290-eiw.KEiwN2s3['d;/.q[eIW^y#e=+Iei*@Mn<qW5o
  #host port
  http_host=hadoop03
  http_port=8888
  #时区
  time_zone=Asia/Shanghai
  ```

-  修改`desktop.db`权限：`sudo chmod o+w hue/desktop/desktop.db`

- 启动 hue 服务

  - 用 root 用户将 hue 整个目录授权给 hadoop 用户：`chown -R hadoop:hadoop hue-3.9.0-cdh5.10.0`
  - 用 hadoop 用户启动：`hue-3.9.0-cdh5.10.0/build/env/bin/supervisor`

  - 如果报错：`KeyError: "Couldn't get user id for user hue"`
    解决办法：`adduser hue`

- 查看 hue web 界面

  访问地址：http://hadoop03:8888/
  首次登陆设置任意用户名和密码作为超级用户，需要你记住，这里使用用户名：hue 密码：hue

## 3 Hue 与 HDFS 集成开发

- 在 hadoop01 修改`core-site.xml`配置文件：

  ```xml
  <property>
      <name>hadoop.proxyuser.hue.hosts</name>
      <value>*</value>
  </property>
  <property>
      <name>hadoop.proxyuser.hue.groups</name>
      <value>*</value>
  </property>
  ```

- 将`core-site.xml`配置文件分发到其他节点：`deploy.sh core-site.xml /home/hadoop/app/hadoop-2.6.0-cdh5.10.0/etc/hadoop slave`

- 在 hadoop03 修改`hue.ini`配置：`vi hue/desktop/conf/hue.ini`

  ```ini
  fs_defaultfs=hdfs://mycluster
  webhdfs_url=http://hadoop01:50070/webhdfs/v1
  hadoop_hdfs_home=/home/hadoop/app/hadoop
  hadoop_bin=/home/hadoop/app/hadoop/bin
  hadoop_conf_dir=/home/hadoop/app/hadoop/etc/hadoop
  ```

- 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

- 注意集群时钟必须同步：

  同步时间：`sudo ntpdate pool.ntp.org`
  查看时间：`date`

- 启动 HDFS：`sbin/start-dfs.sh`

- 重启 Hue：`hue-3.9.0-cdh5.10.0/build/env/bin/supervisor`

- 如果遇到错误：`hue Failed to access filesystem root`
  解决：修改 hdfs 根目录权限`bin/hdfs dfs -chown -R hadoop:hadoop /`

- 访问地址：http://hadoop03:8888/filebrowser/

## 4 Hue 与 yarn 集成开发

- 在 hadoop03 修改`hue.ini`配置：`vi hue/desktop/conf/hue.ini`

  ```ini
  resourcemanager_host=hadoop01
  resourcemanager_port=8032
  submit_to=True
  resourcemanager_api_url=http://hadoop01:8088
  proxy_api_url=http://hadoop01:8088
  history_server_api_url=http://hadoop01:19888
  ```

- 启动 YARN：`sbin/start-yarn.sh`

- （在 hadoop02 上）启动备用节点的 RM：`sbin/yarn-daemon.sh start resourcemanager`

- 重启 Hue：`hue-3.9.0-cdh5.10.0/build/env/bin/supervisor`

## 5 Hue 与 Hive 集成开发

- 在 hadoop03 修改`hue.ini`配置：`vi hue/desktop/conf/hue.ini`

  ```ini
  hive_server_host=hadoop03
  hive_server_port=10000
  hive_conf_dir=/home/hadoop/app/hive/conf
  ```

- 启动 MySQL：`sudo service mysqld start`

- 启动hiveserver2：前台启动：`bin/hive --service hiveserver2`

- 重启 Hue：`hue-3.9.0-cdh5.10.0/build/env/bin/supervisor`

- 如果点击 hive 遇到错误：`Could not start SASL: Error in sasl_client_start (-4) SASL(-4): no mechanism available: No worthy mechs found`
  解决：`yum install cyrus-sasl-plain`

- 如果点击 hive 遇到错误：`database is locked`

  - root 登录 MySQL：

    - 创建数据库 hue：`create database hue default character set utf8 default collate utf8_general_ci;`

    - 授予用户 hue 权限（密码 hue）：

      `grant all on hue.* to 'hue'@'%' identified by 'hue';`

      `grant all on hue.* to 'hue'@'hadoop03' identified by 'hue';`

  - 修改`hue.ini`配置：`vi hue/desktop/conf/hue.ini`

    ```ini
    engine=mysql
    host=hadoop03
    port=3306
    user=hue
    password=hue
    name=hue
    ```

  - 完成以上的这个配置，启动Hue,通过浏览器访问，会发生错误，原因是mysql数据库没有被初始化`DatabaseError: (1146, "Table 'hue.desktop_settings' doesn't exist")`

    - 执行：`hue/build/env/bin/hue syncdb`，`hue/build/env/bin/hue migrate`
    - 执行完以后，可以在mysql中看到，hue相应的表已经生成
    - 启动hue, 能够正常访问了

- 在 hadoop01 启动 HBase：`bin/start-hbase.sh`

- 访问地址：http://hadoop03:8888/notebook/editor?type=hive

- 输入查询：`select * from sogoulogs limit 10;`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/124.jpg" alt="image"  />

- 在上一章，我们已经通过 flume 采集搜狗用户日志数据并保存到 HBase 中了，现在可以用 Hue 可视化查询

  - 统计总记录数：`select count(*) from sogoulogs;`

  - 统计新闻话题总量（对 keywords 去重）：`select count(distinct keywords) from sogoulogs;`

  - 统计新闻话题浏览量 topn：`select keywords,count(*) as rank from sogoulogs group by keywords order by rank desc limit 10;`

  - 统计新闻浏览量最高的时段：`select substr(logtime,0,5),count(substr(logtime,0,5)) as counter from sogoulogs group by substr(logtime,0,5) order by counter desc limit 5;`

## 6 Hue 与 HBase 集成开发

- 在 hadoop03 修改`hue.ini`配置：`vi hue/desktop/conf/hue.ini`

  ```ini
  hbase_clusters=(Cluster|hadoop01:9090)
  hbase_conf_dir=/home/hadoop/app/hbase/conf
  ```

- 在 hadoop01 启动 HBase 集群：`bin/start-hbase.sh`

- 在 hadoop01 HBase 中启动 thrift 服务：`bin/hbase-daemon.sh start thrift`

- 重启 hue 服务：`bin/supervisor`

- 访问地址：http://hadoop03:8888/hbase/#Cluster

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/125.jpg" alt="image"  />

## 7 Hue 与 mysql集成开发

- 在 hadoop03 修改`hue.ini`配置：`vi hue/desktop/conf/hue.ini`

  ```ini
  nice_name="My SQL DB"
  name=metastore
  engine=mysql
  host=hadoop03
  port=3306
  user=hive
  password=hive
  ```

- 重启 hue 服务：`bin/supervisor`