//import org.apache.spark
//import org.apache.spark.rdd.RDD
//import org.apache.spark.sql
//import org.apache.spark.sql.{DataFrame, Dataset, Row, SaveMode, SparkSession}
//
///*
// *
// * @ProjectName: lazada_production
// * @program:
// * @FileName: sparksql_part
// * @description:  TODO
// * @version: 1.0
// * *
// * @author: koray
// * @create: 2021-10-14 10:15
// * @Copyright (c) 2021,All Rights Reserved.
// */
//object sparksql_part {
//  def main(args: Array[String]): Unit = {
//    val spark: SparkSession = new sql.SparkSession.Builder()
//      .master("local[6]")
//      .appName("sql-task")
//      .getOrCreate()
//
//
//
//
//
//    //====================================================
//    //=================  DS,DF,RDD转换   ==================
//    //====================================================
//
//    // 隐式转换
//    import spark.implicits._
//
//    // RDD[T]转换为Dataset[T]/DataFrame
//    val peopleRDD: RDD[People] = spark.sparkContext.makeRDD(Seq(People("zhangsan", 22), People("lisi", 15)))
//    val peopleDS: Dataset[People] = peopleRDD.toDS()
//    val peopleDF: DataFrame = peopleDS.toDF()
//
//    // Dataset[T]/DataFrame互相转换
//    val peopleDF_fromDS: DataFrame = peopleDS.toDF()
//    val peopleDS_fromDF: Dataset[People] = peopleDF.as[People]
//
//    // Dataset[T]/DataFrame转换为RDD[T]
//    val RDD_fromDF: RDD[Row] = peopleDF.rdd
//    val RDD_fromDS: RDD[People] = peopleDS.rdd
//
//
//
//
//    //====================================================
//    //=================  Dataset   =======================
//    //====================================================
//
//
//    //====================================================
//    //=================  DataFrame   =====================
//    //====================================================
//
//    // ----------------   读取外部数据源  ------------------------
//
//    // 1.format+load方式
//    val df1: DataFrame = spark.read
//      .format("csv")
//      .option("header", true)
//      .load("./文件路径")
//
//
//    // 2.封装方法(csv,jdbc,json)
//    val df2: DataFrame = spark.read
//      .option("header", true)
//      .csv("./文件路径")
//
//
//
//    // ----------------   写入数据  ------------------------
//
//    // 读写模式:
//    // SaveMode.Overwrite: 覆盖写入
//    // SaveMode.Append: 末尾写入
//    // SaveMode.Ignore: 若已经存在, 则不写入
//    // SaveMode.ErrorIfExists: 已经存在, 则报错
//
//
//    // 1.format+save方式
//    df1.repartition(1) // 当底层多文件时,重新分区只输出1个文件
//      .write
//      .mode("error")
//      .format("json")
//      .save("./文件路径")
//
//
//    // 2.封装方法(csv,jdbc,json)
//    df2.write
//      // 写入模式
//      .mode(saveMode = "error")
//      // 外部参数
//      .option("encoding", "UTF-8")
//      // 文件夹分区操作,分区字段被指定后,再也不出现在数据中
//      .partitionBy(colNames = "year", "month")
//      // 存储路径
//      .json(path = "./文件路径")
//
//
//    // 3.写入表
//    df2.write
//      .mode(SaveMode.Overwrite)
//      // 文件夹分区操作
//      .partitionBy(colNames = "year", "month")
//      // 文件分桶+排序的组合操作,只能在saveAsTable中使用
//      .bucketBy(numBuckets = 12, colName = "month")
//      .sortBy(colName = "month")
//      .saveAsTable("表名")
//
//
//
//    //====================================================
//    //=================  SparkSQL 整合 Hive   ============
//    //====================================================
//
//
//
//
//
//
//
//
//  }
//
//  case class People(name: String, age: Int)
//
//
//}
