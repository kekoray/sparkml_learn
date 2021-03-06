




 


    // *******************************************************************************************************************************
    // ********************************************************  spark-sql  ********************************************************** 
    // *******************************************************************************************************************************

    // 打印日志级别
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    
    // spark入口
    val spark = SparkSession.builder().master("local[*]").appName("dataFrame_op").getOrCreate()

    // 隐私转换
    import org.apache.spark.sql.functions._         // 作用于col,column
    import spark.implicits._                        // 作用于符号',$
    import spark.sql                                // 简写sql()



    // ---------------------  DS,DF,RDD转换  --------------------
    /*
      DF与DS关系:
          1.DataFrame就是Dataset[Row]
          2.DataFrame 是弱类型的操作; dataset是强类型的操作
          3.dataframe的范型只有Row; Dataset的范型可以是任意类型
          4.dataframe只能做到运行时类型检查; dataset可以做到编译时和运行时都检查

      应该场景:
          Spark-RDD主要用于处理非结构化数据和半结构化数据,且数据没有schema信息
          Spark-SQL主要用于处理结构化数据和带有schema的半结构化数据
     */

    // 1.RDD[T]转换为Dataset[T]/DataFrame
    val peopleRDD: RDD[People] = spark.sparkContext.makeRDD(Seq(People("zhangsan", 22), People("lisi", 15)))
    val peopleDS: Dataset[People] = peopleRDD.toDS()
    val peopleDF: DataFrame = peopleDS.toDF()

    // 2.Dataset[T]/DataFrame互相转换
    val peopleDF_fromDS: DataFrame = peopleDS.toDF()
    val peopleDS_fromDF: Dataset[People] = peopleDF.as[People]

    // 3.Dataset[T]/DataFrame转换为RDD[T]
    val RDD_fromDF: RDD[Row] = peopleDF.rdd
    val RDD_fromDS: RDD[People] = peopleDS.rdd




  
    // =======================================================================
    // =======================  DataFrame-创建方式  =======================
    // ***********************************************************************

    // ---------------------  1.通过隐式转换创建(toDF)  --------------------
    // rdd => df
    val dataRdd: RDD[(String, Int)] = spark.sparkContext.makeRDD(Seq(("tom", 11), ("tony", 33)))
    val rddDF: DataFrame = dataRdd.toDF()

    // Seq => df
    val SeqDF: DataFrame = Seq(("tom", 11), ("tony", 33)).toDF("name", "age")

    // 样例类 => df  (常用)
    val people: Seq[People] = Seq(People("tom", 11), People("tony", 33))
    val personDF: DataFrame = people.toDF()


    // ---------------------  2.通过createDataFrame创建  --------------------
    // 样例类 => df
    val df1: DataFrame = spark.createDataFrame(people)

    // rowRDD+schema => df
    val rdd: RDD[Row] = dataRdd.map(x => Row(x._1, x._2))
    val schema = StructType(List(StructField("name", StringType), StructField("age", IntegerType)))
    val df2: DataFrame = spark.createDataFrame(rdd, schema)


    // ---------------------  3.通过读取外部文件创建(read)  --------------------
    // read => df
    val df3: DataFrame = spark.read
      .format("csv")
      .option("header", true)
      .load("./文件路径")



    // =======================  DataFrame处理操作  =======================
    // A.命令式API操作
    df1.select().where("age > 18")

    // B.SQL操作 -- 注册一张临时表,然后用SQL操作这张临时表
    df1.createOrReplaceTempView("tmp_table")
    spark.sql("""select * from tmp_table""")





    // =======================================================================
    // =======================  DataFrame-读写数据操作  =======================
    // ***********************************************************************

    // ---------------------  1.读取外部数据源  --------------------
    // A.format+load
    val df4: DataFrame = spark.read
      .format("csv")
      .option("header", true)
      .load("./文件路径")

    // B.封装方法(csv,jdbc,json)
    val df5: DataFrame = spark.read
      .option("header", true)
      .csv("./文件路径")


    // ---------------------  2.写入外部数据源  --------------------
    /*
      读写模式(mode):
          SaveMode.Overwrite:       覆盖写入
          SaveMode.Append:          末尾写入
          SaveMode.Ignore:          若已经存在, 则不写入
          SaveMode.ErrorIfExists:   已经存在, 则报错
     */

    // A.format+save
    df1.repartition(1) // 当底层多文件时,重新分区只输出1个文件
      .write
      .mode("error")
      .format("json")
      .save("./文件路径")

    // B.封装方法(csv,jdbc,json)
    df2.write
      .mode(saveMode = "error") // 写入模式
      .option("encoding", "UTF-8") // 外部参数
      .partitionBy(colNames = "year", "month") // 文件夹分区操作,分区字段被指定后,再也不出现在数据中
      .json(path = "./文件路径")

    // C.saveAsTable
    df2.write
      .mode(SaveMode.Overwrite)
      .partitionBy(colNames = "year", "month") // 文件夹分区操作
      .bucketBy(numBuckets = 12, colName = "month") // 文件分桶+排序的组合操作,只能在saveAsTable中使用
      .sortBy(colName = "month")
      .saveAsTable("表名")





    // =======================================================================
    // =======================  DataFrame-缺失值处理  =======================
    // ***********************************************************************

    /*
      常见的缺失值有两种:
          1. Double.NaN类型的NaN和字符串对象null等特殊类型的值,也是空对象,一般使用drop,fill处理
          2. "Null", "NA", " "等为字符串类型的值,一般使用replace处理
     */

    val df: DataFrame = List((1, "kk", 18.0), (2, "", 25.0), (3, "UNKNOWN", Double.NaN), (4, "null", "NA"))
      .toDF("id", "name", "age")
    df.show()


    // ---------------------  1.删除操作  --------------------
    /*
       how选项:
          any ==> 处理的是当某行数据任意一个字段为空
          all ==> 处理的是当某行数据所有值为空
     */

    // A.默认为any,当某一列有NaN时就删除该行
    df.na.drop()
    df.na.drop("any")
    df.na.drop("any", List("id", "name"))

    // B.all,所有的列都是NaN时就删除该行
    df.na.drop("all")


    // ---------------------  2.填充操作  --------------------
    // 对包含null和NaN的所有列数据进行默认值填充,可以指定列;
    df.na.fill(0)
    df.na.fill(0, List("id", "name"))


    // ---------------------  3.替换操作  --------------------
    // 将包含'字符串类型的缺省值'的所有列数据替换成其它值,被替换的值和新值必须是同类型,可以指定列;
    // A.将“名称”列中所有出现的"UNKNOWN"替换为"unnamed"
    df.na.replace("name", Map("UNKNOWN" -> "unnamed"))

    // B.在所有字符串列中将所有出现的"UNKNOWN"替换为"unnamed"
    df.na.replace("*", Map("UNKNOWN" -> "unnamed"))


    // ---------------------  4.SQL操作  --------------------
    // A.使用函数直接转换非法的字符串,再用drop删除
    df.select('id, 'name,
      when('name === "NA", Double.NaN) // when+otherwise类似case when的效果
        .otherwise('age cast (DoubleType))
        .as("age"))
      .na.drop("any")
      .show()

    // B.使用where直接过滤  (常用)
    df.select('id, 'name, 'age)
      .where('name =!= "NA")
      .show





    // =======================================================================
    // =======================  DataFrame-多维分组聚合操作  ==================== 
    // ***********************************************************************

    // ---------------------  1.聚合操作  --------------------
    /*
      分组后聚合操作方式
          A.使用agg配合sql.functions函数进行聚合,这种方式能一次求多个聚合值,比较方便,常用这个!!
          B.使用GroupedDataset的API进行聚合,这种方式只能求一类聚合的值,不好用
     */

    val salesDF: DataFrame = Seq(("Beijing", 2016, 100), ("Beijing", 2017, 200), ("Shanghai", 2015, 50), ("Shanghai", 2016, 150), ("Guangzhou", 2017, 50))
      .toDF("city", "year", "amount")

    val groupedDF: RelationalGroupedDataset = salesDF.groupBy('city, 'year)

    // A.能一次求多个聚合值,还能指定别名  (常用)
    groupedDF
      .agg(sum("amount") as "sum_amount", avg("amount") as "avg_amount")
      .select('city, 'sum_amount, 'avg_amount)

    // B.只能求一类的聚合值,且不能指定别名
    groupedDF
      .sum("amount")


    // ---------------------  2.分组方式  --------------------
    /*
      多维分组聚合操作
          group by :       对查询的结果进行分组
          grouping sets :  对分组集中指定的组表达式的每个子集执行分组操作,                      [ group by A,B grouping sets((A,B),()) 等价于==>  group by A,B union 全表聚合 ]
          rollup :         对指定表达式集滚动创建分组集进行分组操作,最后再进行全表聚合,           [ rollup(A,B) 等价于==> group by A union group by A,B union 全表聚合]
          cube :           对指定表达式集的每个可能组合创建分组集进行分组操作,最后再进行全表聚合,   [ cube(A,B) 等价于==> group by A union group by B union group by A,B union 全表聚合]
     */

    // grouping sets操作
    salesDF.createOrReplaceTempView("sales")
    spark.sql(
      """select city,year,sum(amount) as sum_amount from sales
        |group by city,year grouping sets((city,year),())
        |order by city desc,year desc""".stripMargin)
    /*+---------+----+----------+
      |     city|year|sum_amount|
      +---------+----+----------+
      | Shanghai|2016|       150|
      | Shanghai|2015|        50|
      |Guangzhou|2017|        50|
      |  Beijing|2017|       200|
      |  Beijing|2016|       100|
      |     null|null|       550|
      +---------+----+----------+*/


    // rollup操作
    salesDF.rollup('city, 'year)
      .agg(sum("amount") as "sum_amount")
      .sort('city asc_nulls_last, 'year desc_nulls_last)


    spark.sql(
      """select city,year,sum(amount) as sum_amount from sales
        |group by city,year with rollup
        |order by city desc,year desc""".stripMargin)

    /*+---------+----+----------+
      |     city|year|sum_amount|
      +---------+----+----------+
      |  Beijing|2017|       200|
      |  Beijing|2016|       100|
      |  Beijing|null|       300|
      |Guangzhou|2017|        50|
      |Guangzhou|null|        50|
      | Shanghai|2016|       150|
      | Shanghai|2015|        50|
      | Shanghai|null|       200|
      |     null|null|       550|
      +---------+----+----------+*/


    // cube操作
    salesDF.cube('city, 'year)
      .agg(sum("amount") as "sum_amount")
      .sort('city asc_nulls_last, 'year desc_nulls_last)


    spark.sql(
      """select city,year,sum(amount) as sum_amount from sales
        |group by city,year with cube
        |order by city desc,year desc""".stripMargin)

    /*+---------+----+----------+
      |     city|year|sum_amount|
      +---------+----+----------+
      |  Beijing|2017|       200|
      |  Beijing|2016|       100|
      |  Beijing|null|       300|
      |Guangzhou|2017|        50|
      |Guangzhou|null|        50|
      | Shanghai|2016|       150|
      | Shanghai|2015|        50|
      | Shanghai|null|       200|
      |     null|2017|       250|
      |     null|2016|       250|
      |     null|2015|        50|
      |     null|null|       550|
      +---------+----+----------+*/





    // =======================================================================
    // =======================  DataFrame-join连接操作  ==================== 
    // ***********************************************************************
    /*
      join优化:
           1.Broadcast Hash Join ： 广播Join,或者叫Map端Join,适合一张较小的表(默认10M)和一张大表进行join
           2.Shuffle Hash Join :   适合一张小表和一张大表进行join,或者是两张小表之间的join
           3.Sort Merge Join ：    适合两张较大的表之间进行join
    */

    val person: DataFrame = Seq((0, "Lucy", 0), (1, "Lily", 0), (2, "Tim", 2), (3, "Danial", 0))
      .toDF("id", "name", "cityId")
    val cities: DataFrame = Seq((0, "Beijing"), (1, "Shanghai"), (2, "Guangzhou"))
      .toDF("id", "name")

    // join连接
    person.join(cities, person.col("id") === cities.col("id"))
      .select(person.col("id"), cities.col("name") as "city")

    // cross交叉连接(笛卡尔积)
    person.crossJoin(cities)
      .where(person.col("id") === cities.col("id"))

    // inner,left,left_outer,left_anti,left_semi,right,right_outer,outer,full,full_outer
    // left_anti操作是输出'左表独有的数据',如同 [left join + where t2.col is null] 的操作
    person.join(cities, person.col("id") === cities.col("id"), joinType = "left_anti")
    person.join(cities, person.col("id") === cities.col("id"), joinType = "left").where(cities.col("id") isNull)


    // ---------------------  1.广播Join操作  --------------------
    /*
      将小数据集分发给每一个Executor,让较大的数据集在Map端直接获取小数据集进行Join,这种方式是不需要进行Shuffle的,所以称之为Map端Join,或者广播join
      spark会自动实现Map端Join,依赖spark.sql.autoBroadcastJoinThreshold=10M(默认)参数,当数据集小于这个参数的大小时,会自动进行Map端Join操作
   */

    // 默认开启广播Join
    println(spark.conf.get("spark.sql.autoBroadcastJoinThreshold").toInt / 1024 / 1024)
    println(person.crossJoin(cities).queryExecution.sparkPlan.numberedTreeString)
    /*    00 BroadcastNestedLoopJoin BuildRight, Cross
          01 :- LocalTableScan [id#152, name#153, cityId#154]
          02 +- LocalTableScan [id#164, name#165  */

    // 关闭广播Join操作
    spark.conf.set("spark.sql.autoBroadcastJoinThreshold", -1)
    println(person.crossJoin(cities).queryExecution.sparkPlan.numberedTreeString)
    /*   00 CartesianProduct
         01 :- LocalTableScan [id#152, name#153, cityId#154]
         02 +- LocalTableScan [id#164, name#165]  */

    // 使用函数强制开启广播Join
    println(person.crossJoin(broadcast(cities)).queryExecution.sparkPlan.numberedTreeString)

    // sql版本的的广播Join,这是Hive1.6之前的写法,之后的版本自动识别小表
    spark.sql("""select /*+ MAPJOIN (rt) */ * from person cross join cities rt""")

    // sparkRDD版本的广播Join
    val personRDD: RDD[(Int, String, Int)] = spark.sparkContext.parallelize(Seq((0, "Lucy", 0), (1, "Lily", 0), (2, "Tim", 2), (3, "Danial", 3)))
    val citiesRDD: RDD[(Int, String)] = spark.sparkContext.parallelize(Seq((0, "Beijing"), (1, "Shanghai"), (2, "Guangzhou")))
    val citiesBroadcast: Broadcast[collection.Map[Int, String]] = spark.sparkContext.broadcast(citiesRDD.collectAsMap())
    personRDD.mapPartitions(
      iter => {
        val value = citiesBroadcast.value
        val result = for (x <- iter // 1.iter先赋值给x
                          if value.contains(x._3) // 2.再判断value中是否有x
                          )
          yield (x._1, x._2, value(x._3)) // 3.使用列表生成式yield生成列表
        result
      }
    ).collect().foreach(println)





    // =======================================================================
    // =======================  DataFrame-函数操作  ==================== 
    // ***********************************************************************
    val source: DataFrame = Seq(
      ("Thin", "Cell phone", 6000),
      ("Normal", "Tablet", 1500),
      ("Mini", "Tablet", 5500),
      ("Ultra thin", "Cell phone", 5000),
      ("Very thin", "Cell phone", 6000),
      ("Big", "Tablet", 2500),
      ("Bendable", "Cell phone", 3000),
      ("Foldable", "Cell phone", 3000),
      ("Pro", "Tablet", 4500),
      ("Pro2", "Tablet", 6500))
      .toDF("product", "category", "revenue")


    // ---------------------  1.UDF函数  --------------------
    // A.命令式注册UDF函数
    def toStr(revenue: Long): String = "$" + revenue // 1.定义方法
    val toStrUDF: UserDefinedFunction = udf(toStr _) // 2.注册UDF函数
    source.select(toStrUDF('revenue)).show()

    // B.sql方式注册UDF函数
    spark.udf.register("utf_func", (x: Int) => "$" + x)
    source.createOrReplaceTempView("table")
    spark.sql("select udf_str(revenue) from table").show()


    // ---------------------  2.窗口函数  --------------------
    /*
      1.排名函数
            row_number     不考虑数据重复性,依次连续打上标号 ==> [1 2 3 4]
            dense_rank     考虑数据重复性,重复的数据不会挤占后续的标号,是连续的 ==> [1 2 2 3]
            rank           排名函数,考虑数据重复性,重复的数据会挤占后续的标号 ==> [1 1 3 4]

      2.分析函数
            first          获取这个组第一条数据
            last           获取这个组最后一条数据
            lag            lag(field,n)获取当前数据的field列向前n条数据
            lead           lead(field,n)获取当前数据的field列向后n条数据

      3.聚合函数
            sum/avg..      所有的functions中的聚合函数都支持
     */

    // 1.定义窗口规则
    val window: WindowSpec = Window.partitionBy('category).orderBy('revenue.desc)

    // 2.定义窗口函数
    source.select('product, 'category, 'revenue, row_number() over window as "num").show()
    source.withColumn("num", row_number() over window).show()







    // =======================================================================
    // =======================  DataSet-常用操作  ============================= 
    // ***********************************************************************

    // ---------------------  1.有类型操作  --------------------
    // 不能直接拿到列进行操作,需要通过.的形式获取
    /*
      1.转换
          flatMap          处理对象是数据集中的每个元素,将一条数据转为一个数组
          map              处理对象是数据集中的每个元素,将一条数据转为另一种形式
          mapPartitions    处理对象是数据集中每个分区的iter迭代器
          transform        处理对象是整个数据集(Dataet),可以直接拿到Dataset的API进行操作
          as               类型转换
    */
    Seq("hello world", "hello pc").toDS().flatMap(_.split(" ")).show
    Seq(1, 2, 3, 4).toDS().map(_ * 10).show
    Seq(1, 2, 3, 4).toDS().mapPartitions(iter => iter.map(_ * 10)).show
    spark.range(5).transform(dataset => dataset.withColumn("doubled", 'id * 2))
    Seq("jark", "18").toDS().as[Person]


    /*
      2.过滤
          filter                按照条件过滤数据
      3.分组聚合
          groupByKeygrouByKey   算子的返回结果是KeyValueGroupedDataset,而不是一个Dataset,
                                所以必须要先经过KeyValueGroupedDataset中的方法进行聚合,再转回Dataset,才能使用Action得出结果;
      4.切分
          randomSplit            randomSplit会按照传入的权重随机将一个Dataset分为多个Dataset;数值的长度决定切分多少份;数组的数值决定权重
          sample                 sample会随机在Dataset中抽样.
      5.排序
          orderBy                指定多个字段进行排序,默认升序排序
          sort                   orderBy是sort的别名,功能是一样
      6.去重
          dropDuplicates         根据指定的列进行去重;不传入列名时,是根据所有列去重
          distinct               dropDuplicates()的别名,根据所有列去重
      7.分区
          coalesce         只能减少分区,会直接创建一个逻辑操作,并且设置Shuffle=false; 与RDD中coalesce不同
          repartitions     作用是一个是重分区到特定的分区数,另一个是按照某一列来分区; 类似于SQL中的distribute by
    */
    Seq(1, 2, 3).toDS().filter(_ > 2).show
    Seq((1, "k"), (2, "w"), (1, "p")).toDS().groupByKey(_._1).count().show
    spark.range(15).randomSplit(Array[Double](2, 3)).foreach(_.show)
    spark.range(15).sample(false, 0.5).show
    Seq(Person("zhangsan", 12), Person("lisi", 15)).toDS().orderBy('age.asc).show
    Seq(Person("zhangsan", 12), Person("lisi", 15)).toDS().sort('age.desc).show
    Seq(Person("zhangsan", 12), Person("lisi", 15)).toDS().dropDuplicates("name").show
    Seq(Person("zhangsan", 12), Person("lisi", 15)).toDS().distinct().show
    spark.range(15).coalesce(1).explain(true)
    spark.range(15).repartition(2).explain(true)


    /*
      8.集合操作
          except            求得两个集合的差集
          intersect         求得两个集合的交集
          union             求得两个集合的并集
          limit             限制结果集数量
     */
    val ds1 = spark.range(1, 10)
    val ds2 = spark.range(5, 15)
    ds1.except(ds2).show
    ds1.intersect(ds2).show
    ds1.union(ds2).show
    ds1.limit(3).show



    // ---------------------  2.无类型操作  --------------------
    // 可直接拿到列进行操作,使用函数功能的话就要导入隐式转换
    import org.apache.spark.sql.functions._

    /*
      1.选择
          select              用于选择某些列出现在结果集中
          selectExpr          使用expr函数的形式选择某些列出现在结果集中
      2.列操作
          withColumn          创建一个新的列或者修改原来的列
          withColumnRenamed   修改列名
      3.剪除
          drop                删掉某个列
      4.聚合
          groupBy             按照给定的行进行分组
    */

    val peopleDs: Dataset[Person] = Seq(Person("zhangsan", 12), Person("zhangsan", 8), Person("lisi", 15)).toDS()
    peopleDs.select(expr("count(age) as count")).show
    peopleDs.selectExpr("count(age) as count").show
    peopleDs.withColumn("random", expr("rand()")).show
    peopleDs.withColumnRenamed("name", "new_name").show
    peopleDs.drop('age).show
    peopleDs.groupBy('name).count().show





    // =======================================================================
    // =======================  DataSet-Column对象  =========================== 
    // ***********************************************************************
    // Column表示了Dataset中的一个列,也可以是表达式,作用于每一条数据
    // ---------------------  创建方式  --------------------
    val dataSet: Dataset[Person] = Seq(Person("zhangsan", 12), Person("zhangsan", 8), Person("lisi", 15)).toDS()

    import org.apache.spark.sql.functions._ // 作用于col,column
    import spark.implicits._ // 作用于符号',$

    // 1.创建Column对象
    dataSet
      .select('name) // 常用
      .select($"name")
      .select(col("name"))
      .select(column("name"))
      .where('age > 0)
      .where("age > 0") // 常用

    // 2.创建关联此Dataset的Column对象
    dataSet.col("addCol")
    dataSet.apply("addCol2")
    dataSet("addCol2")


    // ---------------------  常用操作  --------------------
    // 1.类型转换
    dataSet.select('age.as[String])

    // 2.创建别名
    dataSet.select('name.as("other_name"))

    // 3.添加列
    dataSet.withColumn("double_age", 'age * 2)

    // 4.模糊查找
    dataSet.select('name.like("apple"))

    // 5.是否存在指定列
    dataSet.select('name.isin("a", "b"))

    // 6.正反排序
    dataSet.sort('age.asc)
    dataSet.sort('age.desc)






    // ====================================================================
    // ===========================  整合读写JSON文件  =====================
    // ***********************************************************************


    // -------------  1.json转为rdd[T]: 利用JSON-API解析成Map类型数据,再封装到样例类中  --------------------
    val sc = spark.sparkContext
    // A.rdd读取文件
    val jsonRDD: RDD[String] = sc.textFile("src/main/resources/item.jsonl")
    // B.使用Scala中有自带JSON库解析,返回对象为Some(map: Map[String, Any])
    val jsonParseRDD: RDD[Option[Any]] = jsonRDD.map(JSON.parseFull(_)) // Some(Map(payDate -> 2020-11-30 19:50:42, ... ))
    // C.将Some数据转换为Map类型
    val jsonMapRDD: RDD[Map[String, Any]] = jsonParseRDD.map(
      r => r match {
        case Some(map: Map[String, Any]) => map
        case _ => null
      }
    )
    // D.将数据封装到样例类中
    val PayRdd: RDD[Pay] = jsonMapRDD.map(x => Pay(x("amount").toString, x("memberType").toString, x("orderNo").toString, x("payDate").toString, x("productType").toString))
    PayRdd.foreach(println(_)) // Pay(40000.0,105,7E84FF304B45455894999A3FD9449093,2020-11-30 19:50:42,88.0)


    // -------------  2.json转为DataFrame: 利用sparkSQL的json方法  --------------------
    val jsonDF: DataFrame = spark.read.json("src/main/resources/item.jsonl")
    jsonDF.show()


    // -------------  3.写入json文件  --------------------
    // 当底层有多个文件时,repartition重新分区只输出1个文件
    jsonDF.repartition(1)
      .write.json("src/main/resources/output/json")






    // =======================================================================
    // ===========================  整合hive  ================================
    // ***********************************************************************

    // spark整合hive的连接配置
    val spark: SparkSession = SparkSession.builder().master("local[*]").appName("accessHive")
      .config("spark.sql.warehouse.dir", "hdfs://cdh1:8020/user/hive/warehouse") // 设置WareHouse的位置
      .config("hive.metastore.uris", "thrift://cdh1:9083") // 设置MetaStore的位置
      .enableHiveSupport() // 开启Hive支持
      .config("hive.exec.dynamic.partition.mode", "nonstrict") // 设置动态分区模式
      .getOrCreate()

    // 隐私转换
    import org.apache.spark.sql.functions._
    import spark.implicits._


    // =======================  创建操作  =======================
    spark.sql("USE spark_test")
    val createSql =
      """Create External Table If Not Exists student
        |( name String,
        |  age  Int,
        |  gpa  Decimal(5,2)
        |) Comment '学生表'
        |  Partitioned By (
        |    dt String Comment '日期分区字段{"format":"yyyy-MM-dd"}')
        |  Row Format Delimited
        |    Fields Terminated By '\t'
        |    Lines Terminated By '\n'
        |  Stored As textfile
        |  Location '/dataset/hive'
        |  Tblproperties ("orc.compress" = "SNAPPY")""".stripMargin
    spark.sql(createSql)


    // =======================  写入操作  =======================
    val data = Array(("张三", 21, 2.1), ("李四", 16, 1.2), ("王五", 18, 5.3))
    val dataDF: DataFrame = spark.createDataset(data).toDF("name", "age", "gpa")
    val result: Dataset[Row] = dataDF.select("name", "age", "gpa").where("age > 20")

    // 设置dt分区字段 -- lit()用于创建自定义值的列
    val resultDF: DataFrame = result.withColumn("dt", lit(LocalDate.now().toString.substring(0, 7)))
    resultDF.show()

    /* ---------------------  1.insertInto模式  ------------------------------
      insertInto模式是按照数据位置顺序插入数据,前提是要设置动态分区模式 [config("hive.exec.dynamic.partition.mode", "nonstrict")]
      若操作的是分区表,不用指定partitionBy(),会自动获取分区字段,从而插入对应分区的数据;
      注意!! :  如果操作过[overwrite+saveAsTable]后,则会无法找到分区字段,overwrite+insertInto就会覆盖全表;
        A. overwrite + insertInto : 在不影响其他分区数据的情况下,只覆盖指定分区的数据;
        B. append + insertInto : 在表末尾追加增量数据
    */
    // 常用操作,相当于(Inset Overwrite .. Partition ..)
    resultDF.write
      .mode("overwrite")
      .insertInto("spark_test.student")

    /* ---------------------  2.saveAsTable模式  ------------------------------
    A. overwrite + saveAsTable:
        1.表存在,schema字段数相同,会按照新schema字段位置,覆盖全表插入数据
        2.表不存在,或者表存在且schema字段数不相同,则会按照新schema进行重新建表并插入数据
    B. append + saveAsTable:
        1.表存在且表已有数据,直接在表末尾追加增量数据
        2.表存在且表无数据,报错并提议使用insertInto
        3.不存在,自动建表并插入数据
    C. error + saveAsTable:
        1.只要表存在,就抛出异常
        2.不存在,自动建表并插入数据
    D. ignore + saveAsTable:
        1.只要表存在,无论有无数据,都无任何操作
        2.表不存在,自动建表并插入数据
    */
    resultDF.write
      .mode("append")
      .partitionBy("dt")
      .saveAsTable("spark_test.student")


    // =======================  查询操作  =======================
    spark.sql("select * from student").show()
    spark.table("student").show()

    // 简写方式
    import spark.sql
    sql("select * from student").show()

    spark.stop()








    // =======================================================================
    // ===========================  整合MySQL  ================================
    // ***********************************************************************


    // =======================  读取操作  =======================
    // 1.jdbc连接的配置文件
    val properties = new Properties()
    properties.put("user", "root")
    properties.put("password", "123456")
    properties.put("useSSL", "false")

    // 2.需要URL,table,配置文件缺一不可,其中table项可写子查询语句
    spark.read.jdbc("jdbc:mysql://192.168.100.216:3306/spark_test", "student", properties).show()
    spark.read.jdbc("jdbc:mysql://192.168.100.216:3306/spark_test", "(select * from student where age > 20 ) as tab", properties).show()


    // =======================  写入操作  =======================
    // 1.设置schema表头,才能写对应字段写入MySQL中
    val mysql_schema = StructType(
      List(StructField("id", IntegerType),
        StructField("name", StringType),
        StructField("age", IntegerType),
        StructField("gpa", DoubleType)))

    // 2.读取csv文件
    val csvDF: DataFrame = spark.read
      .option("delimiter", "\t")
      .schema(mysql_schema)
      .csv("src/main/resources/student.csv")

    // 4.保存到MySQL表中
    csvDF.write.format("jdbc")
      .mode("overwrite")
      .option("url", "jdbc:mysql://192.168.100.216:3306/spark_test")
      .option("dbtable", "student")
      .option("user", "root")
      .option("password", "123456")
      .option("useSSL", "false") // 关闭SSL认证
      .option("partitionColumn", "id") // 按照指定列进行分区,只能设置类型为数值的列
      // 确定步长的参数,lowerBound-upperBound之间的数据均分给每一个分区,小于lowerBound的数据分给第一个分区,大于upperBound的数据分给最后一个分区
      .option("lowerBound", 1)
      .option("upperBound", 60)
      .option("numPartitions", 10) // 分区数量
      .save()








    // =======================================================================
    // ===========================  整合Redis  ================================
    // ***********************************************************************


    // 整合redis的配置
    val spark: SparkSession = SparkSession.builder().master("local[*]").appName("accessRedis")
      .config("spark.redis.host", "192.168.100.189")
      .config("spark.redis.port", "6379")
      .config("spark.redis.db", "1")
      .config("spark.redis.auth", "96548e1f-0440-4e48-8ab9-7bb1e3d45238")
      .config("redis.timeout", "2000")
      .config("spark.port.maxRetries", "1000")
      .getOrCreate()
    val sc: SparkContext = spark.sparkContext

    // redisAPI的隐式转换
    import spark.implicits._
    import com.redislabs.provider.redis._


    // =======================  读写操作  =======================
    // KV处理的是string类型数据

    // 获取相匹配的key
    sc.fromRedisKeys(Array("customer_gender")).collect().foreach(println) // 搞不懂什么用处
    sc.fromRedisKeyPattern("customer*").collect().foreach(println)

    // 根据key获取对应的value值
    sc.fromRedisKV("customer_gender").collect().foreach(println)
    sc.fromRedisKV("customer*").collect().foreach(println)
    sc.fromRedisList("customer_phone").collect().foreach(println)
    sc.fromRedisHash("gps").collect().foreach(println)
    sc.fromRedisHash("user:1000").collect().foreach(println)
    sc.fromRedisSet("user:1000:*").collect().foreach(println)
    sc.fromRedisSet(Array("user:1000:email", "user:1000:phones")).collect().foreach(println)


    // =======================  写入操作  =======================
    sc.toRedisLIST(Seq("lili", "or", "zz").toDS().rdd, "list_name")
    sc.toRedisHASH(Seq(("1", "lili"), ("2", "mimi")).toDS().rdd, "hash_name")


    // =======================  hash表操作  =======================
    val df: DataFrame = Seq(("John", 30), ("Peter", 45)).toDF("name", "age")

    // sql形式写入,但是不能select读取
    spark.sql(
      """CREATE TEMPORARY VIEW person2
        |(name STRING, age INT)
        |USING org.apache.spark.sql.redis
        |OPTIONS (table 'person2', key.column 'name')""".stripMargin)
    spark.sql("""INSERT INTO TABLE person2 VALUES ('John', 63),('Peter', 65)""")

    // hash表写入
    df.write
      .format("org.apache.spark.sql.redis")
      .mode("overwrite")
      .option("table", "person") // 指定一级key名 [ person:随机数 ==> (name,John) (age,30) ]
      .option("key.column", "name") // 指定二级key名  [ person:John ==> (age,30) ]
      .save()
    /*127.0.0.1:6379[1]> keys 'person*'
            1) "person:Peter"
            2) "person:John"
      127.0.0.1:6379[1]> hgetall 'person:Peter'
            1) "age"
            2) "45"                 */

    // hash表读取
    spark.read
      .format("org.apache.spark.sql.redis")
      .option("table", "person")
      .option("key.column", "name")
      .load()
      .show()
    /*+-----+---+
      | name|age|
      +-----+---+
      |Peter| 45|
      | John| 30|
      +-----+---+*/







    // =======================================================================
    // ===========================  整合Hbase  ================================
    // ***********************************************************************
    /*  TableInputFormat/TableOutputFormat是org.apache.hadoop.hbase.mapreduce包下  */


    val spark = SparkSession.builder().appName("accessHbase").master("local[*]")
      // .enableHiveSupport()
      .getOrCreate()
    val sc: SparkContext = spark.sparkContext
    import spark.implicits._

    // hbase的配置
    val hbaseConf = HBaseConfiguration.create()
    hbaseConf.set("hbase.zookeeper.quorum", "192.168.100.216")
    hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")
    //在IDE中设置此项为true，避免出现"hbase-default.xml"版本不匹配的运行时异常
    hbaseConf.set("hbase.defaults.for.version.skip", "true")



    // =======================  管理操作  =======================
    // 获取admina管理员
    val hbaseConn = ConnectionFactory.createConnection(hbaseConf)
    val admin = hbaseConn.getAdmin

    // 查表
    println(admin.listTableNames().toList)

    // 删除表
    admin.deleteTable(TableName.valueOf("table_name"))



    // =======================  读取数据  =======================
    // 设置查询的表名
    hbaseConf.set(TableInputFormat.INPUT_TABLE, "person")
    val hBaseRDD: RDD[(ImmutableBytesWritable, Result)] = spark.sparkContext.newAPIHadoopRDD(hbaseConf,
      classOf[TableInputFormat],
      classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
      classOf[org.apache.hadoop.hbase.client.Result]).cache()

    // 遍历结果
    hBaseRDD.map({ case (_, result) =>
      // 获取行键
      val key = Bytes.toString(result.getRow)
      // 通过列族和列名获取值
      val name = Bytes.toString(result.getValue("name".getBytes, "china".getBytes))
      (key, name)
    }).toDF("id", "name").show(false)




    // =======================  写入数据  =======================

    // ---------------------  saveAsNewAPIHadoopDataset-AP方式插入  --------------------
    val table_name = "person3"

    // 判断表是否存在,不存在则创建表
    if (!admin.tableExists(TableName.valueOf(table_name))) {
      val table_desc = TableDescriptorBuilder.newBuilder(TableName.valueOf(table_name))
      //指定列簇,不需要创建列
      val col_desc = ColumnFamilyDescriptorBuilder.newBuilder("info".getBytes()).build()
      table_desc.setColumnFamily(col_desc)
      admin.createTable(table_desc.build())
    }

    // 设置写入的表名
    hbaseConf.set(TableOutputFormat.OUTPUT_TABLE, table_name)
    val job = Job.getInstance(hbaseConf)
    job.setOutputKeyClass(classOf[ImmutableBytesWritable])
    job.setOutputValueClass(classOf[Result])
    job.setOutputFormatClass(classOf[TableOutputFormat[ImmutableBytesWritable]])

    val data = sc.makeRDD(Array("1,Jack,M,26", "2,Rose,M,17")).map(_.split(","))
    val dataRDD = data.map(arr => {
      // 设置行健
      val put = new Put(Bytes.toBytes(arr(0)))
      // 设置列族和列名和值
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"), Bytes.toBytes(arr(1)))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("gender"), Bytes.toBytes(arr(2)))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("age"), Bytes.toBytes(arr(3).toInt))
      (new ImmutableBytesWritable, put)
    })

    // 数据写入hbase
    dataRDD.saveAsNewAPIHadoopDataset(job.getConfiguration)



    // ---------------------  BulkLoad方式批量插入  --------------------
    /*
        BulkLoad原理是先利用mapreduce在hdfs上生成相应的HFlie文件,然后再把HFile文件导入到HBase中,以此来达到高效批量插入数据
        如果hdfs使用9000端口,会报错 [Protocol message end-group tag did not match expected tag]
      */

    val table_name = "person5"
    val table: Table = hbaseConn.getTable(TableName.valueOf(table_name))

    // 判断表是否存在,不存在则创建表
    if (!admin.tableExists(TableName.valueOf(table_name))) {
      val table_desc = TableDescriptorBuilder.newBuilder(TableName.valueOf(table_name))
      //指定列簇,不需要创建列
      val col_desc = ColumnFamilyDescriptorBuilder.newBuilder("info".getBytes()).build()
      table_desc.setColumnFamily(col_desc)
      admin.createTable(table_desc.build())
    }

    // 设置写入的表名
    hbaseConf.set(TableOutputFormat.OUTPUT_TABLE, table_name)
    val job = Job.getInstance(hbaseConf)
    job.setMapOutputKeyClass(classOf[ImmutableBytesWritable])
    job.setMapOutputValueClass(classOf[KeyValue])
    job.setOutputFormatClass(classOf[HFileOutputFormat2])
    HFileOutputFormat2.configureIncrementalLoad(job, table, hbaseConn.getRegionLocator(TableName.valueOf(table_name)))

    // 生成HFlie文件
    val data = sc.makeRDD(Array("1,Jack,26", "2,Rose,17")).map(_.split(","))
    val dataRDD: RDD[(ImmutableBytesWritable, KeyValue)] = data.map(x => (DigestUtils.md5Hex(x(0)).substring(0, 3) + x(0), x(1), x(2)))
      .sortBy(_._1)
      .flatMap(x => {
        val listBuffer = new ListBuffer[(ImmutableBytesWritable, KeyValue)]
        val kv1: KeyValue = new KeyValue(Bytes.toBytes(x._1), Bytes.toBytes("info"), Bytes.toBytes("name"), Bytes.toBytes(x._2 + ""))
        val kv2: KeyValue = new KeyValue(Bytes.toBytes(x._1), Bytes.toBytes("info"), Bytes.toBytes("age"), Bytes.toBytes(x._3 + ""))
        listBuffer.append((new ImmutableBytesWritable, kv2))
        listBuffer.append((new ImmutableBytesWritable, kv1))
        listBuffer
      })

    // 判断hdfs上文件是否存在，存在则删除
    val filePath = "hdfs://192.168.100.216:8020/tmp/hbaseBulk"
    val output: Path = new Path(filePath)
    val hdfs: FileSystem = FileSystem.get(URI.create(filePath), new Configuration())
    if (hdfs.exists(output)) {
      hdfs.delete(output, true)
    }

    // 数据写入hbase
    dataRDD.saveAsNewAPIHadoopFile(filePath, classOf[ImmutableBytesWritable], classOf[KeyValue], classOf[HFileOutputFormat2], job.getConfiguration)
    val bulkLoader = new LoadIncrementalHFiles(hbaseConf)
    bulkLoader.doBulkLoad(new Path(filePath), admin, table, hbaseConn.getRegionLocator(TableName.valueOf(table_name)))
    hdfs.close()
    table.close()



    // ---------------------  Phoenix方式插入  --------------------
    // (测试操作没过,报错)

    //spark读取phoenix返回DataFrame的第一种方式
    val rdf = spark.read
      .format("jdbc")
      .option("driver", "org.apache.phoenix.jdbc.PhoenixDriver")
      .option("url", "jdbc:phoenix:192.168.100.216:2181")
      .option("dbtable", "person5")
      .load()
    val rdfList = rdf.collect()
    for (i <- rdfList) {
      println(i.getString(0) + " " + i.getString(1) + " " + i.getString(2))
    }
    rdf.printSchema()

    //spark读取phoenix返回DataFrame的第二种方式
    val df = spark.read
      .format("org.apache.phoenix.spark")
      .options(Map("table" -> "person5", "zkUrl" -> "192.168.100.216:2181"))
      .load()
    df.printSchema()
    val dfList = df.collect()
    for (i <- dfList) {
      println(i.getString(0) + " " + i.getString(1) + " " + i.getString(2))
    }

    //spark DataFrame写入phoenix,需要先建好表
    df.write
      .format("org.apache.phoenix.spark")
      .mode(SaveMode.Overwrite)
      .options(Map("table" -> "PHOENIXTESTCOPY", "zkUrl" -> "jdbc:phoenix:192.168.187.201:2181"))
      .save()



    // =======================  SparkSQL操作HBase  =======================
    // (测试操作没过,报错)
    /*
       1.jar包需要自己匹配加到maven中
                <!-- https://mvnrepository.com/artifact/com.hortonworks/shc-core -->
                <dependency>
                    <groupId>com.hortonworks</groupId>
                    <artifactId>shc-core</artifactId>
                    <version>1.1.0-2.1-s_2.11</version>
                </dependency>
        2.需要开启enableHiveSupport()支持
     */

    import spark.sql
    // hbase的schema
    val catalog =
      s"""{
         |    "table":{"namespace":"default", "name":"person3"},
         |    "rowkey":"id",
         |    "columns":{
         |    "id":{"cf":"rowkey", "col":"id", "type":"int"},
         |    "name":{"cf":"info", "col":"name", "type":"string"},
         |    "biaoqian":{"cf":"info", "col":"biaoqian", "type":"string"},
         |    "age":{"cf":"info",  "col":"age", "type":"int"}
         |    }
         |    }""".stripMargin

    val dataDF = sc.makeRDD(Seq(("1", "Jack", "M", "26"), ("2", "Rose", "M", "17"))).toDF()
    dataDF.show()
    dataDF.write.options(
      Map(HBaseTableCatalog.tableCatalog -> catalog, HBaseTableCatalog.newTable -> "4"))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .mode("overwrite")
      .save()

    val read = spark.read
      .options(Map(HBaseTableCatalog.tableCatalog -> catalog))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .load()
    read.createOrReplaceTempView("table")
    sql("select * from read").show()









    // *******************************************************************************************************************************
    // ********************************************************  spark-RDD  ********************************************************** 
    // *******************************************************************************************************************************



