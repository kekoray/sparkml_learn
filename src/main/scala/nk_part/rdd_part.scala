package nk_part

import org.apache.log4j.{Level, Logger}
import org.apache.spark.util.AccumulatorV2
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable

/*
 * 
 * @ProjectName: lazada_production  
 * @program:    
 * @FileName: nk_part.rdd_part
 * @description:  TODO   
 * @version: 1.0   
 * *
 * @author: koray  
 * @create: 2021-09-09 11:53  
 * @Copyright (c) 2021,All Rights Reserved.
 */ object rdd_part {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)

    val conf = new SparkConf().setAppName("nk_part.rdd_part").setMaster("local[2]")
    val sc = new SparkContext(conf)

    //
    //    //=====================================================
    //    //=================  RDD创建   ========================
    //    //====================================================
    //
    //    // 1.通过本地集合直接创建,使用parallelize和makeRDD
    //    val seq = Seq(1, 2, 3, 4, 5)
    //    val rdd1: RDD[Int] = sc.parallelize(seq, 2)
    //    val rdd2: RDD[Int] = sc.makeRDD(seq, 2)
    //    rdd1.foreach(x => print(s"rdd1 : ${x}   "))
    //    rdd2.foreach(x => print(s"rdd2 : ${x}   "))
    //
    //
    //    // 2.通过读取外部数据集来创建
    //    /* a.访问本地文件,sc.textFile("file:///…")
    //       b.访问其他系统的文件,sc.textFile("hdfs://node-1:8020/dataset")   */
    //    val source: RDD[String] = sc.textFile("src/main/resources/wordcount.txt", 2)
    //    source.foreach(x => println(s"source : ${x}   "))
    //
    //
    //    // 3.通过其它的RDD衍生而来
    //    val rdd3: RDD[String] = source.flatMap(_.split(" "))
    //    rdd3.foreach(x => print(s"rdd3 : ${x}   "))
    //
    //
    //    //=====================================================
    //    //=================  RDD算子   ========================
    //    //====================================================
    //
    //    // ----------------  转换算子  -------------------------
    //
    //    // map
    //    sc.makeRDD(Seq(1, 2, 3, 4))
    //      .map(_ * 10)
    //      .foreach(println)
    //
    //    // flatmap
    //    sc.makeRDD(Seq("Hello lily", "Hello lucy", "Hello tim"))
    //      .flatMap(_.split(" "))
    //      .foreach(println)
    //
    //    // filter
    //    sc.makeRDD(Seq(1, 2, 3))
    //      .filter(_ > 2)
    //      .foreach(println)
    //
    //    // mapPartitions
    //    // mapPartitionsWithIndex
    //    // mapValues
    //    // sample
    //    // union
    //    // intersection
    //    // subtract
    //    // distinct
    //
    //
    //    // reducebykey
    //    sc.makeRDD(Seq(("a", 1), ("a", 1), ("b", 1)))
    //      .reduceByKey((x, y) => x + y)
    //      .foreach(println)
    //
    //    // groupByKey
    //    // combineByKey
    //    // aggregateByKey
    //    // foldByKey
    //    // join
    //    // cogroup
    //    // cartesian
    //    // sortBy
    //    // partitionBy
    //    // coalesce
    //    // repartition
    //    // repartitionAndSortWithinPartitions
    //
    //
    //    // ----------------  动作算子  -------------------------
    //
    //    // reduce
    //    // collect
    //    // count
    //    // first
    //    // take
    //    // takeSample
    //    // fold
    //    // saveAsTextFile
    //    // saveAsSequenceFile
    //    // countByKey
    //    // foreach
    //
    //
    //    //=====================================================
    //    //=================  RDD分区操作   =====================
    //    //=====================================================
    //
    //    // 查看分区数
    //    println(sc.makeRDD(Seq(1, 2, 3, 4)).map(_ * 10).partitions.size)
    //
    //    // 1.创建RDD时指定分区数
    //    println(sc.makeRDD(Seq(1, 2, 3, 4), 10).partitions.size) // 10
    //    // 2.通过coalesce算子指定
    //    println(sc.makeRDD(Seq(1, 2, 3, 4), 10).coalesce(5, shuffle = false).partitions.size) // 5
    //    // 3.通过repartition算子指定
    //    println(sc.makeRDD(Seq(1, 2, 3, 4), 10).repartition(3).partitions.size) // 3
    //
    //
    //    //=====================================================
    //    //==================  RDD容错  =========================
    //    //=====================================================
    //
    //    //----------------  缓存  ----------------
    //    val rdd = sc.makeRDD(Seq("a", "b", "c"))
    //      .map((_, 1))
    //      .reduceByKey((x, y) => x + y)
    //
    //    // cache等同于persist() ==> persist(StorageLevel.MEMORY_ONLY)
    //    rdd.cache()
    //
    //    // persist能够指定缓存的级别
    //    rdd.persist(StorageLevel.MEMORY_ONLY)
    //
    //    // 清理缓存
    //    rdd.unpersist()
    //
    //    //----------------  Checkpoint  ----------------
    //
    //    // 1.先设置Checkpoint的存储路径
    //    sc.setCheckpointDir("checkpoint")
    //
    //    // 2.开启Checkpoint
    //    rdd.checkpoint()

    //=====================================================
    //==================  分布式变量  ======================
    //=====================================================

    //----------------  全局累加器  ----------------
    // 支持数值型累加add()的分布式变量,默认值为0,遇到action算子触发
    val counter = sc.longAccumulator("counter")
    sc.makeRDD(Seq(1, 2, 3, 4)).foreach(counter.add(_))
    //    print(counter.value)


    //----------------  自定义累加器  ----------------
    val infoAccumulator = new InfoAccumulator()
    // 注册自定义累加器
    sc.register(infoAccumulator, "infos")
    sc.makeRDD(Seq("1", "2", "3", "4")).foreach(infoAccumulator.add)
    //    print(counter.value)


    //----------------  广播变量  ----------------
    // 1.创建广播变量
    val a = sc.broadcast(1)
    // 2.获取值
    println(a.value)
    // 3.销毁变量,释放内存空间
    a.destroy()

    // 唯一标识
    println(a.id)
    // 字符串表示
    println(a.toString())
    // 在Executor中异步的删除缓存副本
    a.unpersist()


  }

}


// ============================================================================
// ============================================================================
// ============================================================================

// 自定义累加器,继承AccumulatorV2,第一个参数是传入类型,第二个是输出类型
class InfoAccumulator extends AccumulatorV2[String, Set[String]] {

  // 创建可变集合用于收集累加值
  private val infos: mutable.Set[String] = mutable.Set()

  // 初始化累加器对象是否为空
  override def isZero: Boolean = {
    infos.isEmpty
  }

  // 拷贝创建一个新累加器对象
  override def copy(): AccumulatorV2[String, Set[String]] = {
    val infoAccumulator = new InfoAccumulator()
    infos.synchronized {
      infoAccumulator.infos ++= infos
    }
    infoAccumulator
  }

  // 重置累加器数据
  override def reset(): Unit = {
    infos.clear()
  }

  // 外部传入要累加的内容,在这个方法中进行累加
  override def add(v: String): Unit = {
    infos += v
  }

  // 累加器在进行累加的时候,可能每个分布式节点都有一个实例,在最后Driver端进行一次合并,把所有的实例的内容合并起来
  override def merge(other: AccumulatorV2[String, Set[String]]): Unit = {
    infos ++= other.value
  }

  // 提供给外部累加的结果
  override def value: Set[String] = {
    //需要返回一个不可变的集合,因为不能因为外部的修改而影响自身的值
    infos.toSet
  }
}