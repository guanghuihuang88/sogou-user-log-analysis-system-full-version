#配置 jdk
export JAVA_HOME=/home/hadoop/app/jdk
#配置 hadoop 配置文件目录
export HADOOP_CONF_DIR=/home/hadoop/app/hadoop/etc/hadoop
#配置 hadoop 根目录
export HADOOP_HOME=/home/hadoop/app/hadoop
#spark master webui 端口，默认是 8080，跟 tomcat 冲突
SPARK_MASTER_WEBUI_PORT=8888
#配置 spark HA 配置
SPARK_DAEMON_JAVA_OPTS="-Dspark.deploy.recoveryMode=ZOOKEEPER -Dspark.deploy.zookeeper.url=hadoop01:2181,hadoop02:2181,hadoop03:2181 -Dspark.deploy.zookeeper.dir=/myspark"
#spark 配置文件目录
SPARK_CONF_DIR=/home/hadoop/app/spark/conf
#spark 日志目录
SPARK_LOG_DIR=/home/hadoop/data/spark/logs
#spark 进程 ip 文件保存位置
SPARK_PID_DIR=/home/hadoop/data/spark/logs