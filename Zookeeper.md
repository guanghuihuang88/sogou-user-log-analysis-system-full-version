# Zookeeper

## 1 Zookeeper概述

ZooKeeper 是一种为分布式应用所设计的高可用、高性能且一致的开源协调服务

- 它首先提供了分布式锁服务，由于 ZooKeeper 是开源的，后来者在分布式锁的基础上又提供了配置维护、组服务、分布式消息队列、分布式通知/协调等
- 它的目标就是封装好复杂易出错的关键服务，将简单易用的接口和性能高效、功能稳定的系统提供给用户

- Zookeeper 特点

  - 最终一致性：客户端无论连接到哪个Server，展示给它的都是同一个视图，这是 Zookeeper 最重要的性

    能

  - 可靠性：具有简单、健壮、良好的性能，如果消息message被一台服务器接受，那么它将被所有的服务器接受

  - 实时性：Zookeeper 保证客户端将在一个时间间隔范围内，获得服务器的更新信息或者服务器失效的信息。但由于网络延时等原因，Zookeeper 不能保证两个客户端能同时得到刚更新的数据，如果需要最新数据，应该在读数据之前调用`sync()`接口

  - 等待无关（wait-free）：慢的或者失效的 Client 不得干预快速的 Client 的请求，这就使得每个 Client 都能有效的等待

  - 原子性：更新操作要么成功，要么失败，没有中间状态

  - 顺序性：对于所有Server，同一消息发布顺序一致。它包括全局有序和偏序两种。

    - 全局有序是指如果在一台服务器上消息 a 在消息 b 前发布，则在所有Server上消息 a 都将在消息 b 前被发布

    - 偏序是指如果一个消息 b 在消息 a 后被同一个发送者发布，a 必将排在 b 前面

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/005.jpg" alt="image"  />



## 2 Zookeeper系统架构

Zookeeper（ZK）是一个由多个 Server 组成的集群，该集群有一个 Leader，多个 Follower。客户端可以连接任意 ZK 服务节点来读写数据

ZK 集群中每个 Server 都保存一份数据副本。ZK 使用简单的同步策略，通过以下两条基本保证来实现数据的一致性：1、全局串行化所有的写操作；2、保证同一客户端的指令被FIFO执行以及消息通知的FIFO。

所有的读请求由Zk Server 本地响应，所有的更新请求将转发给 Leader，由 Leader 实施

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/006.jpg" alt="image" style="zoom:80%;" />

ZK 通过复制来实现高可用性，只要 ZK 集群中半数以上的机器处于可用状态，它就能够提供服务。比如，在一个有5个节点的 ZK 集群中，每个Follower节点的数据都是 Leader 节点数据的副本，每个节点的数据视图都一样，这样就有5个节点提供 ZK 服务。并且 ZK 集群中任意2台机器出现故障，都可以保证 ZK 仍然对外提供服务，因为剩下的3台机器超过了半数

 ZK 会确保对 Znode 树的每一个修改都会被复制到超过半数的机器上。如果少于半数的机器出现故障，则最少有一台机器会保存最新的状态，那么这台机器就是我们的 Leader，其余的副本最终也会更新到这个状态。如果Leader挂了，由于其他机器保存了 Leader 的副本，那就可以从中选出一台机器作为新的 Leader 继续提供服务

### 角色

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/007.jpg" alt="image"  />

### 工作原理

Zookeeper 的核心是原子广播机制，这个机制保证了各个 server 之间的同步。实现这个机制的协议叫做 Zab 协议。Zab 协议有两种模式，它们分别是恢复模式和广播模式

- 恢复模式

   当服务启动或者在领导者崩溃后，Zab 就进入了恢复模式，当领导者被选举出来，且大多数 server 完成了和 leader 的状态同步以后，恢复模式就结束了。状态同步保证了 leader 和 server 具有相同的系统状态

- 广播模式

   一旦 Leader 已经和多数的 Follower 进行了状态同步后，他就可以开始广播消息了，即进入广播状态。这时候当一个 Server 加入 ZooKeeper 服务中，它会在恢复模式下启动，发现 Leader，并和 Leader 进行状态同步。待到同步结束，它也参与消息广播。ZooKeeper 服务一直维持在 Broadcast 状态，直到 Leader 崩溃了或者 Leader 失去了大部分的 Followers 支持

Broadcast 模式极其类似于分布式事务中的 2pc（two-phrase commit 两阶段提交）：即 Leader 提起一个决议，由 Followers 进行投票，Leader 对投票结果进行计算决定是否通过该决议，如果通过执行该决议（事务），否则什么也不做

在广播模式下，ZooKeeper Server 会接受 Client 请求，所有的写请求都被转发给领导者，再由领导者将更新广播给跟随者。当半数以上的跟随者已经将修改持久化之后，领导者才会提交这个更新，然后客户端才会收到一个更新成功的响应。这个用来达成共识的协议被设计成具有原子性，因此每个修改要么成功要么失败

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/008.jpg" alt="image"  />



## 3 Zookeeper服务

Zookeeper提供了这么多服务，比如分布式锁、分布式队列、分布式通知与协调等，了解它具体如何实现的，对我们理解Zookeeper非常重要

- **数据结构**：Znode

- **原语**（操作）：在数据结构的基础上定义的一些原语，也就是该数据结构的一些操作

- **通知机制**：watcher，需要通过通知机制，将消息以网络形式发送给分布式应用程序

Zookeeper的相关服务主要通过数据结构 + 原语 + watcher 机制这3部分共同来实现

### Znode

Zookeeper 维护着一个树形层次结构，树中的节点被称为 znode，每个 znode 节点可以拥有子节点

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/009.jpg" alt="image" style="zoom: 50%;" />

Zookeeper 树中的 znode 兼具文件和目录双重特点，每个 znode 由三部分组成

- stat：为状态信息，描述该 Znode 的版本、权限等信息

- data：与该 Znode 相关的数据

- children：该 Znode 下的子节点

Znode有两种类型：短暂的和持久的。Znode的类型在创建时确定并且之后不能再修改

- 短暂节点（临时节点）：在创建短暂 znode 的客户端会话结束时，Zookeepe r会将该短暂 znode 删除。虽然每个短暂 znode 都会被绑定到一个客户端会话，但它们对所有的客户端还是可见的。短暂 znode 不可以有子节点，即使是短暂子节点

- 持久节点：持久 znode 不依赖与客户端会话，只有当客户端明确要删除该持久 znode 时才会被删除

### 原语（操作）

Zookeeper 维护着一个树形层次结构，树中的节点被称为 znode，每个 znode 节点可以拥有子节点

| 操作            | 描述                                |
| --------------- | ----------------------------------- |
| create          | 创建Znode（父Znode必须存在）        |
| delete          | 删除Znode（Znode没有子节点）        |
| exists          | 测试Znode是否存在，并获取他的元数据 |
| getACL/setACL   | 为Znode获取/设置ACL                 |
| getChildren     | 获取Znode所有子节点的列表           |
| getData/setData | 获取/设置Znode的相关数据            |
| sync            | 使客户端的Znode试图与ZooKeeper同步  |

### watcher

Zookeeper 可以在读操作`exists()`、`getChildren()`及`getData()`上设置观察。这些观察可以被写操作create、delete 和 setData 触发。当一个观察被触发时会产生一个观察事件，这个观察和触发它的操作共同决定了观察事件的类型

ZooKeeper 所管理的 watch 可以分为两类：

- 数据watch(data watch)：getData和exists负责设置数据watch，返回关于节点的数据信息

- 孩子watch(child watch)：getChildren负责设置孩子watch，返回孩子列表



## 4 Zookeeper集群部署

### 4.1 集群规划

- Zookeeper安装模式有三种： 
  - 单机模式：Zookeeper 只运行在一台服务器上，适合测试环境
  - 伪集群模式：一台物理机上运行多个 Zookeeper 实例，适合测试环境
  - 分布式集群模式：Zookeeper 运行于一个集群中，适合生产环境

- 集群安装步骤：
  1. 集群规划
  2. 环境准备
  3. JDK 安装
  4. Zookeeper 安装

- 主机规划

  |           | hadoop01 | hadoop02 | hadoop03 |
  | --------- | -------- | -------- | -------- |
  | Zookeeper | √        | √        | √        |

- 软件规划

  | 软件      | 版本                             | 位数 |
  | --------- | -------------------------------- | ---- |
  | JDK       | 1.8                              | 64   |
  | Centos    | 6.5                              | 64   |
  | Zookeeper | zookeeper-3.4.5-cdh5.10.0.tar.gz |      |
  | Hadoop    | hadoop-2.6.0-cdh5.10.0.tar.gz    |      |

- 用户规划

  | 节点名称 | 用户组 | 用户   |
  | -------- | ------ | ------ |
  | hadoop01 | hadoop | hadoop |
  | hadoop02 | hadoop | hadoop |
  | hadoop03 | hadoop | hadoop |

- 目录规划

  | 名称         | 路径               |
  | ------------ | ------------------ |
  | 所有软件目录 | /home/hadoop/app   |
  | 脚本目录     | /home/hadoop/tools |
  | 数据目录     | /home/hadoop/data  |

### 4.2 环境准备

#### 时钟同步

- `cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime`
- 下载安装 ntp：`yum install ntp`
- 同步时间：`ntpdate pool.ntp.org`
- 查看时间：`date`

#### Hosts 文件配置

配置集群所有节点 ip 与 hostname 的映射关系：`vi /etc/hosts`

```
192.168.62.201 hadoop01
192.168.62.202 hadoop02
192.168.62.203 hadoop03
```

#### 关闭防火墙

- 查看防火墙状态：`service iptables status`

- 永久关闭防火墙：`chkconfig iptables off`

- 临时关闭防火墙：`service iptables stop`

#### SSH 免密码登录

- 首先每个节点分别配置好 ssh 免密码登录，下面以 hadoop01 节点为例

  - 切换到 hadoop 用户根目录：
  - `mkdir .ssh`
  - `ssh-keygen -t rsa`
  - 进入.ssh 文件：`cd .ssh`
  - `cat id_rsa.pub >> authorized_keys`
  - 退回到根目录
  - `chmod 700 .ssh`
  - `chmod 600 .ssh/*`
  - `ssh hadoop01`

- 将 hadoop02 和 hadoop03 的公钥`id_ras.pub`拷贝到 hadoop01 中的`authorized_keys`文件中

  - 三个节点：`su hadoop`
  - 分别在 hadoop02 和 hadoop03 上执行：`cat ~/.ssh/id_rsa.pub | ssh hadoop@hadoop01 'cat >> ~/.ssh/authorized_keys'`
  - 在 hadoop01 上验证发送成功：`cat ~/.ssh/authorized_keys `

  - 然后将 hadoop01 中的`authorized_keys`文件分发到 hadoop02 和 hadoop03 节点上面

    `scp -r ~/.ssh/authorized_keys hadoop@hadoop02:~/.ssh/`

    `scp -r ~/.ssh/authorized_keys hadoop@hadoop03:~/.ssh/`

    然后 hadoop01、hadoop02 和 hadoop03 就可以免密码互通

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/010.jpg" alt="image"  />

#### 集群脚本准备

> 脚本只需在 hadoop01 上配置，之后对它的运用默认是在 hadoop01 上，其作用就是辅助 hadoop01 同步操作到 hadoop02、hadoop03

- 创建脚本存放目录：`mkdir /home/hadoop/tools`

- 编写脚本配置文件和分发文件：`cd ~/tools`

  - `vi deploy.conf`

    ```
    hadoop01,master,all,zookeeper,namenode,datanode,
    hadoop02,slave,all,zookeeper,namenode,datanode,
    hadoop03,slave,all,zookeeper,datanode,
    ```

  - 编写shell脚本：`vi deploy.sh`

    ```
    #!/bin/bash
    if [ $# -lt 3 ]
    then
      echo "Usage: ./deploy.sh srcFile(or Dir) descFile(or Dir) MachineTag"
      echo "Usage: ./deploy.sh srcFile(or Dir) descFile(or Dir) MachineTag confFile"
      exit
    fi
    
    src=$1
    dest=$2
    tag=$3
    
    if [ 'a'$4'a' == 'aa' ]
    then
      confFile=/home/hadoop/tools/deploy.conf
    else
      confFile=$4
    fi
    
    if [ -f $confFile ]
    then
      if [ -f $src ]
      then
        for server in `cat $confFile | grep -v '^#'|grep ','$tag','|awk -F',' '{print $1}'`
        do
          scp $src $server":"${dest}
        done
      elif [ -d $src ]
      then
        for server in `cat $confFile | grep -v '^#'|grep ','$tag','|awk -F',' '{print $1}'`
        do 
          scp -r $src $server":"${dest}
        done
      else
        echo "Error: No source file exist"
      fi
    else
      echo "Error: Please assign config file or run deploy.sh command with deploy.conf in same directory"
    fi
    ```

  - `vi runRemoteCmd.sh`

    ```shell
    #!/bin/bash
    
    if [ $# -lt 2 ]
    then
      echo "Usage: ./runRemoteCmd.sh Command MachineTag"
      echo "Usage: ./runRemoteCmd.sh Command MachineTag confFile"
      exit
    fi
    
    cmd=$1
    tag=$2
    if [ 'a'$3'a' == 'aa' ]
    then
      confFile=/home/hadoop/tools/deploy.conf
    else
      confFile=$3
    fi
    
    if [ -f $confFile ]
    then
      for server in `cat $confFile | grep -v '^#'|grep ','$tag','|awk -F',' '{print $1}'`
      do
        echo "*************************server******************************"
        ssh $server "source ~/.bashrc; $cmd"
      done
    else
      echo "Error: Please assign config file or run deploy.sh command with deploy.conf in same directory"
    fi
    ```

- 给脚本添加执行权限：（文件名会变色）

  - `chmod u+x deploy.sh`

  - `chmod u+x runRemoteCmd.sh`

- 配置脚本环境变量：`vi ~/.bashrc`

  ```
  PATH=/home/hadoop/tools:$PATH
  export PATH
  ```

  保存环境变量：`source ~/.bashrc`

- 使用`deploy.sh`脚本：分发文件`1.txt`至slave节点（即 hadoop02、hadoop03）

  `deploy.sh ~/1.txt /home/hadoop/ slave`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/011.jpg" alt="image"  />

- 使用`runRemoteCmd.sh`脚本：批量创建各个节点相应目录

  `runRemoteCmd.sh "mkdir /home/hadoop/app" all`

  `runRemoteCmd.sh "mkdir /home/hadoop/data" all`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/012.jpg" alt="image"  />



### 4.3 JDK 安装

> Zookeeper 是由 Java 编写，运行在 JVM，所以需要提前安装 JDK 运行环境，本项目我们选择jdk8
>
> 使用 hadoop 用户执行如下步骤

#### 下载、解压 JDK

- 官网下载`jdk-8u51-linux-x64.tar.gz`，并用 Xftp 上传至`/home/hadoop/app`
- 在`/home/hadoop/app`目录下：`tar -zxvf jdk-8u51-linux-x64.tar.gz`
- 删除安装包：`sudo rm -rf jdk-8u51-linux-x64.tar.gz`
- 创建软链接：`ln -s jdk1.8.0_51 jdk`

#### 配置环境变量

> 本项目使用方式二

- 方式一：修改/etc/profile 文件

  如果你的计算机仅仅作为开发使用时推荐使用这种方法，因为所有用户的 shell 都有权使用这些环境变量，但是可能会给系统带来安全性问题。 这里是针对所有的用户的，所有的 shell，`vi /etc/profile`

  ```
  JAVA_HOME=/home/hadoop/app/jdk
  CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
  PATH=$JAVA_HOME/bin:/home/hadoop/tools:$PATH
  export JAVA_HOME CLASSPATH PATH
  ```

- 方式二：修改 .bashrc 文件

  这种方法更为安全，它可以把使用这些环境变量的权限控制到用户级别，对某一个特定的用户，如果你需要给某个用户权限使用这些环境变量，你只需要修改其个人用户主目录下的.bashrc 文件就可以了`vi ~/.bashrc`

  ```
  ##已包含了之前为脚本配置的环境变量，可直接覆盖
  JAVA_HOME=/home/hadoop/app/jdk
  CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
  PATH=$JAVA_HOME/bin:/home/hadoop/tools:$PATH
  export JAVA_HOME CLASSPATH PATH
  ```

  使配置文件生效：`source ~/.bashrc `

  检查 JDK 是否安装成功：`java -version `

#### JDK 安装包同步到其他节点

通过脚本命令：`deploy.sh jdk1.8.0_51 /home/hadoop/app/ slave`将 jdk安装包同步到其他节点，然后重复上述步骤完成各个节点的 jdk 安装



### 4.4 Zookeeper 安装

> 本项目统一选择 cdh5.10.0 版本，包括后面 hadoop 也下载这个版本
>
> - Apache 版本下载地址：https://archive.apache.org/dist/
>
> - CDH 版本下载地址：http://archive-primary.cloudera.com/cdh5/cdh/5/
>
> 使用 hadoop 用户执行如下步骤

#### 下载、解压 Zookeeper 

- 官网下载：`zookeeper-3.4.5-cdh5.10.0.tar.gz`，用 Xftp 上传至`/home/hadoop/app`
- 在`/home/hadoop/app`目录下：`tar -zxvf zookeeper-3.4.5-cdh5.10.0.tar.gz`
- 删除安装包：`sudo rm -rf zookeeper-3.4.5-cdh5.10.0.tar.gz`
- 创建软链接：`ln -s zookeeper-3.4.5-cdh5.10.0 zookeeper`

#### 修改 zoo.cfg 配置文件

> zoo.cfg 配置文件可在我的[github项目仓库](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/%E9%85%8D%E7%BD%AE%E6%96%87%E4%BB%B6/Zookeeper/zoo.cfg)克隆

- 用Xftp将`zoo.cfg`上传到`/home/hadoop/app/zookeeper/conf`

#### 安装目录同步到其他节点

- 安装目录分发到其他节点：`deploy.sh zookeeper-3.4.5-cdh5.10.0/ /home/hadoop/app/ slave`

- 并分别创建软连接：`ln -s zookeeper-3.4.5-cdh5.10.0 zookeeper`

#### 创建规划的目录

- 在 hadoop01 用`runRemoteCmd.sh`脚本同步创建目录：

  `runRemoteCmd.sh "mkdir -p /home/hadoop/data/zookeeper/zkdata" all`

  `runRemoteCmd.sh "mkdir -p /home/hadoop/data/zookeeper/zkdatalog" all`

#### 修改每个节点服务编号

分别到各个节点，进入`/home/hadoop/data/zookeeper/zkdata`目录，创建文件`myid`，里面的内容分别写入一个编号：1、2、3

#### 测试运行

- 启动 Zookeeper：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh start" all`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/013.jpg" alt="image"  />

- 查看 Zookeeper 进程：`runRemoteCmd.sh "jps" all`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/014.jpg" alt="image"  />

- 查看 Zookeeper 状态：`runRemoteCmd.sh "/home/hadoop/app/zookeeper/bin/zkServer.sh status" all`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/015.jpg" alt="image"  />

#### 查看并操作 znode

- `bin/zkCli.sh`

