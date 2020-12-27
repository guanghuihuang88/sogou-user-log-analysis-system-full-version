# 搜狗用户日志分析系统

## 项目需求分析与设计

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



## 项目环境搭建

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

- 配置网络

  - 输入`ifconfig`，暂时只有本地回环地址和子网掩码

  - `ping 127.0.0.1`能通，说明本机网卡和IP协议安装都没有问题

  - 在VMware查看网关地址，`ping 192.168.62.2`不通，说明网卡链路有问题

  - 配置网卡：`vi /etc/sysconfig/network-scripts/ifcfg-eth0`，修改`ONBOOT=yes`开启网卡

  - 重启网络服务：`service network restart`

  - 输入`ifconfig`，可查看到网卡eth0及IP地址，并且现在能ping通网关地址、IP地址、baidu

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

#### openssh-clients服务D

> 做免密码登录的时候需要用到这个服务

- 安装 openssh-clients 服务：`yum install -y openssh-clients`
- 





























