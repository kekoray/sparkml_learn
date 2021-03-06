package spark_ml.kn

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.{DataFrame, SparkSession}

/*
 * 
 * @ProjectName: lazada_production  
 * @program: spark_ml
 * @FileName: _03_Features_ETS
 * @description:  TODO
 * @version: 1.0
 * *
 * @author: koray
 * @create: 2021-12-21 11:51
 * @Copyright (c) 2021,All Rights Reserved.
 */ object _03_Features_ETS {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    val spark = SparkSession.builder().appName("_03_Features_ETS").master("local[*]").getOrCreate()
    val sc = spark.sparkContext

    //=================  1.Feature Extractors  ==================
    /*
    特征抽取,主要是针对文本数据、图像数据、视频数据等需要结构化数据的抽取;
    一般我们都是处理MySQL中结构化的数据;
    在sparkMllib中仅提供了文本抽取的API;
     */

    //=========================================================
    //=================  2.Feature Transformers  ==================
    //=========================================================
    /*
     特征转化
     1.类别型数据的数值化
           (a).标签编码  -->  StringIndexer,IndexToString
           (b).独热编码  -->  OneHotEncoder
     2.数值型数据的归一化和标准化
          (a).归一化  -->  MinMaxScaler,MaxAbsScaler
          (b).标准化  -->  StandardScaler
     3.连续值数据的离散化
          (a).二值化操作  -->  Binarizer
          (b).分桶操作    -->  Bucketizer
     4.特征组合
          (a).向量汇编器  -->  VectorAssembler

     */

    /* =================  1.类别型数据的数值化  ================
        (a).labelencoder标签编码---如果原始数据是有顺序的情况下,使用StringIndexer实现,如abc,123等;
        (b).onehotencoder独热编码---如果原始数据没有顺序的,使用OneHotEncoder实现,如男女,大小等;
     */

    val data = spark.createDataFrame(Seq((0, "a"), (1, "b"), (2, "c"), (3, "a"), (4, "a"), (5, "c"))).toDF("id", "category")

    // -------------- (a).标签编码 ------------------
    // 使用StringIndexer将类别型数据转换为以标签索引为主的数值型数据,索引在[0,numLabels)中,按标签频率排序,最频繁的标签为0索引;
    // setInputCol() 输入列,需要转化的schema
    // setOutputCol() 输出列,用户自定义
    val indexer: StringIndexer = new StringIndexer().setInputCol("category").setOutputCol("categoryIndexer")
    // 在特征工程中需要fit形成model在使用tranforam进行转化,model保存了数据转化前后的映射关系
    val indexerModel: StringIndexerModel = indexer.fit(data)
    val indexerResultDF: DataFrame = indexerModel.transform(data)
    indexerResultDF.show(false)
    /*
          +---+--------+---------------+
          |id |category|categoryIndexer|
          +---+--------+---------------+
          |0  |a       |0.0            |
          |1  |b       |2.0            |
          |2  |c       |1.0            |
          |3  |a       |0.0            |
          |4  |a       |0.0            |
          |5  |c       |1.0            |
          +---+--------+---------------+
     */

    // 对预测值进行原数据映射
    // setInputCol() 输入列,需要映射预测的schema
    // setOutputCol() 输出列,用户自定义
    // setLabels() 映射关系,model保存了数据转化前后的映射关系
    val indexToString: IndexToString = new IndexToString().setInputCol("categoryIndexer").setOutputCol("beforeIndex").setLabels(indexerModel.labels)
    val indexToStringResult: DataFrame = indexToString.transform(indexerResultDF)
    indexToStringResult.show(false)
    /*
        +---+--------+---------------+-----------+
        |id |category|categoryIndexer|beforeIndex|
        +---+--------+---------------+-----------+
        |0  |a       |0.0            |a          |
        |1  |b       |2.0            |b          |
        |2  |c       |1.0            |c          |
        |3  |a       |0.0            |a          |
        |4  |a       |0.0            |a          |
        |5  |c       |1.0            |c          |
        +---+--------+---------------+-----------+
     */

    // -------------- (b).独热编码 ------------------
    // 将一列类别索引映射到一列二进制向量,每行最多有一个单值,表示输入类别索引,不过已在spark2.3.0中弃用,并将在3.0.0中删除;
    // 首先要先将数据转化为标签索引数据,再使用独热编码;
    val encoder: OneHotEncoder = new OneHotEncoder().setInputCol("categoryIndexer").setOutputCol("oheIndex").setDropLast(false)
    val oheResult = encoder.transform(indexerResultDF)
    oheResult.show(false)
    /*
        +---+--------+---------------+-------------+
        |id |category|categoryIndexer|oheIndex     |
        +---+--------+---------------+-------------+
        |0  |a       |0.0            |(3,[0],[1.0])|  -->  (1.0, 0.0, 0.0)   <--  稀疏向量,只存非0值
        |1  |b       |2.0            |(3,[2],[1.0])|  -->  (0.0, 0.0, 1.0)
        |2  |c       |1.0            |(3,[1],[1.0])|  -->  (0.0, 1.0, 0.0)
        |3  |a       |0.0            |(3,[0],[1.0])|
        |4  |a       |0.0            |(3,[0],[1.0])|
        |5  |c       |1.0            |(3,[1],[1.0])|
        +---+--------+---------------+-------------+
     */


    // =================  2.数值型数据的归一化和标准化  ================
    /*
      (a).归一化操作,即在具备不同量纲的前提下,通过归一化操作能够将所有的数据归一化到[0,1]或[-1,1]的区间,从而降低因为量纲对模型带来的影响;
          MinMaxScaler:  (当前的值-当前列的最小值)/(当前列的最大值-当前列的最小值),可以将数据归一化到[最小值,最大值]=[0,1]区间
          MaxAbsScaler:  (当前的值)/max(abs(当前列的取值)),可以将数据归一化到[-1,1]区间

      (b).标准化操作,因为某些算法需要数据呈现为标准正态分布,所以需要对数据进行标准化
          StandSclaer:  (当前的值-均值)/方差,适合于非标准正态分布或者正态分布的数据转化为标准正态分布的数据
     */

    val df3 = spark.createDataFrame(Seq(
      (0, Vectors.dense(1.0, 0.5, -1.0)),
      (1, Vectors.dense(2.0, 1.0, 1.0)),
      (2, Vectors.dense(4.0, 10.0, 2.0))
    )).toDF("id", "features")

    // -------------- (a).归一化操作 ------------------
    val minMaxDF = new MinMaxScaler().setInputCol("features").setOutputCol("MinMaxfeatures").fit(df3).transform(df3)
    val maxAbsDF = new MaxAbsScaler().setInputCol("features").setOutputCol("MaxAbsfeatures").fit(df3).transform(df3)
    minMaxDF.show(false)
    maxAbsDF.show(false)
    /*
          +---+--------------+-----------------------------------------------------------+
          |id |features      |MinMaxfeatures                                             |
          +---+--------------+-----------------------------------------------------------+          (当前的值-当前列的最小值)/(当前列的最大值-当前列的最小值)
          |0  |[1.0, 0.5, -1.0]|[0.0, 0.0, 0.0]                                              |      (1-1)/(4-1) = 0
          |1  |[2.0, 1.0, 1.0] |[0.3333333333333333, 0.05263157894736842, 0.6666666666666666]|      (2-1)/(4-1) = 0.3333333333333333
          |2  |[4.0, 10.0, 2.0]|[1.0, 1.0, 1.0]                                              |      (4-1)/(4-1) = 1
          +---+--------------+-----------------------------------------------------------+


          +---+--------------+----------------+
          |id |features      |MaxAbsfeatures  |
          +---+--------------+----------------+         (当前的值)/max(abs(当前列的取值))
          |0  |[1.0, 0.5, -1.0]|[0.25, 0.05, -0.5]|     1/4 = 0.25
          |1  |[2.0, 1.0, 1.0] |[0.5, 0.1, 0.5]   |     2/4 = 0.5
          |2  |[4.0, 10.0, 2.0]|[1.0, 1.0, 1.0]   |     4/4 = 1
          +---+--------------+----------------+
          */


    // -------------- (b).标准化操作 ------------------
    val standardDF = new StandardScaler().setInputCol("features").setOutputCol("Standardfeatures").fit(df3).transform(df3)
    standardDF.show(false)
    /*
          +---+--------------+------------------------------------------------------------+
          |id |features      |Standardfeatures                                            |
          +---+--------------+------------------------------------------------------------+
          |0  |[1.0,0.5,-1.0]|[0.6546536707079771,0.09352195295828246,-0.6546536707079771]|
          |1  |[2.0,1.0,1.0] |[1.3093073414159542,0.18704390591656492,0.6546536707079771] |
          |2  |[4.0,10.0,2.0]|[2.6186146828319083,1.8704390591656492,1.3093073414159542]  |
          +---+--------------+------------------------------------------------------------+
     */


    // =================  3.连续值数据的离散化  ================
    /*
      离散化的原因是因为连续性数据是不符合某些算法的要求的,比如决策树;
         (a).二值化操作(Binarizer),只能划分成2种类别的数据
         (b).分桶操作(Bucketizer),可以划分成多种类别的数据
     */

    val df1 = spark.createDataFrame(Array((0, 0.1), (1, 8.0), (2, 0.2), (3, -2.0), (4, 0.0))).toDF("label", "feature")

    // -------------- (a).二值化操作 ------------------
    val binarizerDF = new Binarizer().setInputCol("feature").setOutputCol("feature_binarizer").setThreshold(0.5).transform(df1)
    binarizerDF.show(false)
    /*
        +-----+-------+-----------------+
        |label|feature|feature_binarizer|
        +-----+-------+-----------------+
        |0    |0.1    |0.0              |
        |1    |8.0    |1.0              |
        |2    |0.2    |0.0              |
        |3    |-2.0   |0.0              |
        |4    |0.0    |0.0              |
        +-----+-------+-----------------+
     */


    // -------------- (b).分桶操作 ------------------
    // 分箱条件(正无穷 -> 0 -> 10 -> 正无穷)
    val splits = Array(Double.NegativeInfinity, 0, 5, Double.PositiveInfinity)
    val bucketizerDF = new Bucketizer().setInputCol("feature").setOutputCol("feature_bucketizer").setSplits(splits).transform(df1)
    bucketizerDF.show(false)
    /*
        +-----+-------+------------------+
        |label|feature|feature_bucketizer|
        +-----+-------+------------------+
        |0    |0.1    |1.0               |
        |1    |8.0    |2.0               |
        |2    |0.2    |1.0               |
        |3    |-2.0   |0.0               |
        |4    |0.0    |1.0               |
        +-----+-------+------------------+
     */


    // =================  4.特征组合  ================
    // 特征组合就是将指定的多列的Array数组组合成一个向量列,并且输入列的值将按照指定的顺序组合成一个向量;
    val df2 = spark.createDataFrame(Seq((0, 18, 1.0, Vectors.dense(0.0, 10.0, 0.5), 1.0))).toDF("id", "hour", "mobile", "userFeatures", "clicked")
    val VectorAssemblerDF = new VectorAssembler().setInputCols(Array("hour", "mobile", "userFeatures", "clicked")).setOutputCol("features").transform(df2)
    VectorAssemblerDF.printSchema()
    VectorAssemblerDF.show(false)
    /*
        root
         |-- id: integer (nullable = false)
         |-- hour: integer (nullable = false)
         |-- mobile: double (nullable = false)
         |-- userFeatures: vector (nullable = true)
         |-- clicked: double (nullable = false)
         |-- features: vector (nullable = true)


        +---+----+------+--------------+-------+---------------------------+
        |id |hour|mobile|userFeatures  |clicked|features                   |
        +---+----+------+--------------+-------+---------------------------+
        |0  |18  |1.0   |[0.0,10.0,0.5]|1.0    |[18.0,1.0,0.0,10.0,0.5,1.0]|
        +---+----+------+--------------+-------+---------------------------+
     */


    //=========================================================
    //=================  3.Feature Selectors  ==================
    //=========================================================
    /*
      特征选择:
          1.df.select()直接获取特征列
          2.卡方验证选择: 使用具有分类特征的标签数据,根据卡方验证方法,选择出与标签列最相关的特征列;
     */
    val dataDF2: DataFrame = spark.createDataFrame(Seq(
      (7, Vectors.dense(0.0, 0.0, 18.0, 1.0), 1.0),
      (8, Vectors.dense(0.0, 1.0, 12.0, 0.0), 0.0),
      (9, Vectors.dense(1.0, 0.0, 15.0, 0.1), 0.0)
    )).toDF("id", "features", "clicked")


    // 卡方验证选择
    // setFeaturesCol()  特征列
    // setLabelCol()     标签列
    // setOutputCol()    输出列
    // setNumTopFeatures()  选择出指定数量的卡方值最高的特征列
    val chiSqSelector = new ChiSqSelector().setFeaturesCol("features").setLabelCol("clicked").setOutputCol("chisquareFeatures").setNumTopFeatures(2)
    chiSqSelector.fit(dataDF2).transform(dataDF2).show(false)
    /*
        +---+------------------+-------+-----------------+
        |id |features          |clicked|chisquareFeatures|
        +---+------------------+-------+-----------------+
        |7  |[0.0,0.0,18.0,1.0]|1.0    |[18.0,1.0]       |
        |8  |[0.0,1.0,12.0,0.0]|0.0    |[12.0,0.0]       |
        |9  |[1.0,0.0,15.0,0.1]|0.0    |[15.0,0.1]       |
        +---+------------------+-------+-----------------+
     */


    //=========================================================
    //============  4.Feature Dimensionality Reduction  =======
    //=========================================================
    /*
      特征降维:
          PCA主成分分析,是一种无监督线性数据转换技术,主要作用是特征降维,即把具有相关性的高维变量转换为线性无关的低维变量,减少数据集的维数,同时保持数据集对方差贡献最大的特征;
          所以当数据集在不同维度上的方差分布不均匀的时候,PCA最有用;

        原理:
           所谓的主成分分析,不过是在高维的空间中寻找一个低维的正交坐标系;
           比如说在三维空间中寻找一个二维的直角坐标系,那么这个二维的直角坐标系就会构成一个平面,将三维空间中的各个点在这个二维平面上做投影,就得到了各个点在二维空间中的一个表示,由此数据点就从三维降成了二维.

        API用法:
             PCA算法转换的是特征向量,需要将降维的字段转为向量字段
             setK() 主成分的数量,必须小于等于降维字段个数
     */
    val pca: PCA = new PCA().setInputCol("features").setOutputCol("features_pca").setK(2)
    val pcaDF = pca.fit(dataDF2).transform(dataDF2)
    pcaDF.show(false)
    /*
       +---+------------------+-------+----------------------------------------+
      |id |features          |clicked|features_pca                            |
      +---+------------------+-------+----------------------------------------+
      |7  |[0.0,0.0,18.0,1.0]|1.0    |[17.681719144883022,-0.5910509417080694]|
      |8  |[0.0,1.0,12.0,0.0]|0.0    |[11.517306699961809,-0.5866681457448389]|
      |9  |[1.0,0.0,15.0,0.1]|0.0    |[14.61657922764673,0.5984520165752177]  |
      +---+------------------+-------+----------------------------------------+
     */


  }

}
