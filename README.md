# 搜狗用户日志分析系统

## 1 项目需求分析与设计

### 项目需求分析

- 采集用户海量浏览日志信息
- 实时统计分析 TopN 用户浏览最高的新闻话题
- 实时统计分析已经曝光的新闻话题总量
- 实时统计用户新闻浏览量最高的时间段

### 系统架构设计

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/001.jpg" alt="image" style="zoom:80%;" />

### 数据流程设计

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/002.jpg" alt="image" style="zoom:80%;" />

### 集群角色规划

|               | hadoop01 | hadoop02 | hadoop03 |
| ------------- | -------- | -------- | -------- |
| hdfs(nn)      | √        | √        |          |
| hdfs(dn)      | √        | √        | √        |
| yarn(rm)      | √        | √        |          |
| yarn(nm)      | √        | √        | √        |
| journalnode   | √        | √        | √        |
| zookeeper     | √        | √        | √        |
| hive          |          |          | √        |
| hbase(master) | √        | √        |          |
| hbase(rs)     | √        | √        | √        |
| flume         | √        | √        | √        |
| kafka         | √        | √        | √        |
| spark         |          |          | √        |
| hue           |          |          | √        |
| mysql         |          |          | √        |



## 2 项目环境搭建

### 虚拟机准备

> 准备三台centos6.5虚拟机，分别命名为hadoop01、hadoop02、hadoop03

#### 安装centos6.5

centos6.5镜像官网下载地址：https://vault.centos.org/6.5/isos/x86_64/CentOS-6.5-x86_64-bin-DVD1.iso

使用VMware15.5pro安装虚拟机

- 内存配置4G、处理器数1（每处理器内核数1）、网络适配器NAT、硬盘20G
- 安装centos6.5
  - 选择English版本
  - hostname命名为hadoop01
  - 选择use all space
  - 选择Minimal版，不需要图形界面

#### 配置并克隆hadoop01

> 注意，centos6官方已经不再支持yum源，参考[我的另一篇博客](http://guanghuihuang.cn/2020/12/28/Tips/centos6%20yum%E6%BA%90%E4%B8%8D%E8%83%BD%E4%BD%BF%E7%94%A8/)更换yum源

- 配置网络

  - 输入`ifconfig`，暂时只有本地回环地址和子网掩码

  - `ping 127.0.0.1`能通，说明本机网卡和IP协议安装都没有问题

  - 在VMware查看网关地址，`ping 192.168.62.2`不通，说明网卡链路有问题

  - 配置网卡：`vi /etc/sysconfig/network-scripts/ifcfg-eth0`，修改`ONBOOT=yes`开启网卡

  - 重启网络服务：`service network restart`

  - 输入`ifconfig`，可查看到网卡eth0及IP地址，并且现在能ping通网关地址、IP地址、baidu

    若ping不通百度：则`vi /etc/resolv.conf`，添加`nameserver 8.8.8.8`

  - 静态IP配置：`vi /etc/sysconfig/network-scripts/ifcfg-eth0`：

    ```
    BOOTPROTO=static
    IPADDR=192.168.62.201
    NETMASK=255.255.255.0
    GATEWAY=192.168.62.2
    ```

  - 重启网络服务：`service network restart`

- (完整)克隆hadoop01，分别命名为hadoop02、hadoop03，以hadoop02为例配置：

  - 删除网卡信息：`vi /etc/udev/rules.d/70-persistent-net.rules`

    删除eth0的网卡，并将eth1网卡修改为eth0，并复制下其物理地址`00:0c:29:4b:2c:c5`

    <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/003.jpg" alt="image"  />

  - 配置网卡：`vi /etc/sysconfig/network-scripts/ifcfg-eth0`，修改`HWADDR`和`IPADDR`
  - 修改主机名：`vi /etc/sysconfig/network`
  - 重启`reboot`

- 安装Xshell，远程登陆三台虚拟机

### Linux配置

#### 用户配置

- 创建用户组：`groupadd hadoop`

- 创建用户：`useradd -g hadoop(用户组) hadoop(用户)`，hadoop用户目录会出现在`home/`下

- 设置用户密码：`passwd hadoop`

- 设置hadoop用户sudo权限：在root用户下输入`visudo`，在文件末尾添加`hadoop ALL=(ALL) NOPASSWD: ALL`

  验证hadoop用户拥有sudo权限：在hadoop用户下输入`sudo visudo`成功免密打开

#### 防火墙配置

- 临时性关闭：`service iptables stop`
- 永久性关闭：`chkconfig iptables off`，重启才能生效
- 查看防火墙状态：`service iptables status`

#### openssh-clients服务

> 做免密码登录的时候需要用到这个服务

- 安装 openssh-clients 服务：`yum install -y openssh-clients`

#### 主机名与 IP 映射

一般情况下通过主机名是无法访问虚拟机的，需要通过 IP 地址才可以访问虚拟机。但是 IP地址不容易记住，这时可以通过配置主机名与 IP 地址的映射关系，从而可以通过主机名来访问虚拟机

- `vi /etc/hosts`

  ```
  192.168.62.201 hadoop01
  192.168.62.202 hadoop02
  192.168.62.203 hadoop03
  ```

#### SSH 免密码登录

> SSH 是一个可以在应用程序中提供安全通信的一个协议，通过 SSH 可以安全地进行网络数据传输，它的主要原理就是利用非对称加密体系，对所有待传输的数据进行加密，保证数据在传输时不被恶意破坏、泄露或者篡改。但是 hadoop 使用 ssh 主要不是用来进行数据传输的，hadoop 主要是在启动和停止的时候需要主节点通过 SSH 协议将从节点上面的进程启动或停止。也就是说如果不配置 SSH 免密码登录对 hadoop 的正常使用也没有任何影响，只是在启动和停止 hadoop 的时候需要输入每个从节点的用户名的密码就可以了，但是我们可以想象一下，当集群规模比较大的时候，比如上百台，如果每次都要输入每个从节点的密码，那肯定是比较麻烦点，所以这种方法肯定是不可取的，所以我们要进行 SSH 免密码的配置，而且目前远程管理环境中最常使用的也是 SSH（Secure Shell）

- 为 hadoop 用户创建 SSH 免密码登录。在 hadoop 用户下，切换到 hadoop 用户的家目录

- 创建.ssh 目录：`mkdir .ssh`

- 生成秘钥：`ssh-keygen -t rsa `

- 将公钥 copy 到认证文件：`cp id_rsa.pub authorized_keys`

- 为.ssh 赋予权限：退回到 hadoop 的家目录，为.ssh 赋予权限

  - `chmod 700 .ssh`
  - `chmod 600 .ssh/*`

- 验证 ssh 免密码登录：用 ssh 登录 hadoop01,第一次登录需要输入 yes,第二次以后就不用输入密码了，如果能达到这个效果就表示 SSH 免密码登录设置成功，登陆的时候用 ssh hadoop01 这个命令

  `ssh hadoop01`

  <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/004.jpg" alt="image"  />

## 3 Zookeeper 概述及其部署

参见文档[Zookeeper](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/Zookeeper.md)，可直接看第4节部署

## 4 HDFS 概述及其部署

参见文档[HDFS](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/HDFS.md)，可直接看第4节部署

## 5 YARN 概述及其部署

参见文档[YARN](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/YARN.md)，可直接看第5节部署

## 6 MapReduce 概述

参见文档[MapReduce](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/MapReduce.md)

## 7 Eclipse 与 MapReduce 集成开发 (利用Maven)

参见文档[Eclipse 与 MapReduce 集成开发](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/Eclipse%E4%B8%8EMapReduce%E9%9B%86%E6%88%90%E5%BC%80%E5%8F%91.md)

## 8 HBase 概述及其部署

参见文档[HBase](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/HBase.md)，可直接看第4节部署

## 9 Kafka 概述及其部署

参见文档[Kafka](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/Kafka.md)，可直接看第4节部署

## 10 Flume 概述及其部署

参见文档[Flume](https://github.com/guanghuihuang88/sogou-user-log-analysis-system/blob/master/Flume.md)，可直接看第3节部署





























