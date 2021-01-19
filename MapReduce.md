# MapReduce分布式计算框架

> MapReduce 源之于 Google 的 MapReduce 论文
>
> - 2004年12月份，谷歌发表了关于分布式计算框架 MapReduce 的论文
> - Nutch 的开发人员根据该论文实现了自己的 MapReduce 分布式计算框架

## 1 MapReduce 概述

> MapReduce 是一个使用简单的软件框架，基于它写出来的应用程序能够运行在由上千个商用机器组成的大型集群上，并以一种可靠容错式并行处理TB级别的数据集

总的来说：MapReduce 是面向大规模数据并行处理的计算模型、框架和平台。具体包含如下3个层面的含义

- MapReduce 是一个并行程序的设计模型与方法
- MapReduce 是一个并行程序运行的软件框架
- MapReduce 是一个基于集群的高性能并行计算平台

优点：

- 易于编程
- 良好的扩展性
- 高容错性
- 适合PB级别以上海量数据的离线处理

缺点：

- 实时计算
- 流式计算
- DAG计算

### 基本设计思想

> 面向大规模数据处理，MapReduce 有以下三个层面上的基本设计思想

1. 分而治之：对付大数据并行处理采用"分而治之"的设计思想

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/033.jpg" alt="image" style="zoom:80%;" />

2. 抽象成模型：把函数式编程思想构建成抽象模型—Map 和 Reduce

   <img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/034.jpg" alt="image" style="zoom:80%;" />

3. 上升到构架：以统一构架为程序员隐藏系统底层细节

   - 计算任务的自动划分和调度
   - 数据的自动化分布存储和划分
   - 处理数据与计算任务的同步
   - 结果数据的收集整理（sorting，combining，partitioning等）
   - 系统通信、负载平衡、计算性能优化处理
   - 处理系统节点出错检测和失效恢复

## 2 MapReduce 编程模型

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/035.jpg" alt="image" style="zoom:80%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/036.jpg" alt="image"  />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/037.jpg" alt="image" style="zoom:80%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/038.jpg" alt="image"  />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/039.jpg" alt="image"  />

## 3 MapReduce 案例分析

> 典型的mapreduce编程模型所适合解决的问题：

- 业务场景：有大量的文件，每个文件里面存储的都是单词
- 业务需求：统计所有文件中每个单词出现的次数
- 解决思路：先分别统计出每个文件中各个单词出现的次数；然后，再累加不同文件中同一个单词出现次数

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/040.jpg" alt="image" style="zoom:80%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/041.jpg" alt="image" style="zoom:80%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/042.jpg" alt="image" style="zoom:80%;" />

<img src="https://hexo.oss-cn-beijing.aliyuncs.com/%E9%A1%B9%E7%9B%AE/%E6%90%9C%E7%8B%97%E7%94%A8%E6%88%B7%E6%97%A5%E5%BF%97%E5%88%86%E6%9E%90%E7%B3%BB%E7%BB%9F/043.jpg" alt="image" style="zoom:80%;" />



