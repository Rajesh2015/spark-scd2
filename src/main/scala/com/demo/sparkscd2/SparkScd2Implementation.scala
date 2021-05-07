package com.demo.sparkscd2

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

import org.apache.spark.sql.types.{BooleanType, DateType, IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.functions._

object SparkScd2Implementation {
  def main(args: Array[String]): Unit = {
    val sparkSession=SparkSession.builder().master("local[*]").appName("scd2").getOrCreate()
    sparkSession.sparkContext.setLogLevel("ERROR")
    //Sample Source Data
    val sourcelist = List(
    Row(1, "Spark"),
    Row(2, "PySpark!"),
    Row(4, "Scala!")
    )
    val schema_source = StructType(List(
      StructField("src_id", IntegerType, true),
    StructField("src_attr", StringType, true)
    ))

    val sourceDf=sparkSession.createDataFrame(sparkSession.sparkContext.parallelize(sourcelist),schema_source)

//sourceDf.show()

    //Sample TargetDataFrame
    val targetList=List(Row(1, "Hello!", false, false,"2020-01-01", "2021-1-31"),
    Row(1, "Hadoop", true, false, "2019-01-01", "9999-12-31"),
    Row(2, "Hadoop with Java", true, false,
      "2019-02-01", "9999-12-31"),
    Row(3, "old system", true, false,
      "2019-02-01", "9999-12-31"))
    val schema_target = StructType(List(
      StructField("id", IntegerType, true),
    StructField("attr", StringType, true),
    StructField("is_current", BooleanType, true),
    StructField("is_deleted", BooleanType, true),
    StructField("start_date", StringType, true),
    StructField("end_date", StringType, true)))
    val targetDf=sparkSession.createDataFrame(sparkSession.sparkContext.parallelize(targetList),schema_target)

   // targetDf.show()
   var maxdate  = LocalDate.parse("9999-12-31", DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString

    val format = new SimpleDateFormat("dd-MM-yyyy")

    val todayDate = format.format(Calendar.getInstance().getTime()).toString
    val newsourcedf=sourceDf.withColumn("src_start_date", lit(
      todayDate)).withColumn("src_end_date", lit(maxdate))

import sparkSession.implicits._
    val joinDataDf=targetDf.join(newsourcedf,targetDf("id")===newsourcedf("src_id")
      and targetDf("end_date")===newsourcedf("src_end_date") ,"full")
    val enrichedActionDf=joinDataDf.withColumn("action",when(joinDataDf("attr")=!=joinDataDf("src_attr"),"Upsert")
      .when(joinDataDf("src_attr").isNull and joinDataDf("is_current"),"Delete")
      .when(joinDataDf("id").isNull,"Insert").otherwise("NoAction"))

    val column_names = Seq("id", "attr", "is_current",
    "is_deleted", "start_date", "end_date")
   val df_NoAction= enrichedActionDf.filter($"action"==="NoAction").select(column_names.map(c=>col(c)):_*)

    val df_insert= enrichedActionDf.filter($"action"==="Insert")
      .select(enrichedActionDf("src_id") as "id", enrichedActionDf("src_attr") as "attr" ,lit(true) as "is_current",lit(false) as "is_deleted",
        enrichedActionDf("src_start_date") as "start_date",enrichedActionDf("src_end_date") as "end_date")
    val df_delete= enrichedActionDf.filter($"action"==="Delete").withColumn("end_date",lit(todayDate)).withColumn("is_deleted",lit(true)).withColumn("is_current",lit(false))
      .select(column_names.map(c=>col(c)):_*)
val df_upsert_1=enrichedActionDf.filter($"action"==="Upsert").withColumn("end_date",enrichedActionDf("src_start_date")).withColumn("is_current",lit(false))
    .select(column_names.map(c=>col(c)):_*)
    val df_upsert_2=enrichedActionDf.filter($"action"==="Upsert").withColumn("is_current",lit(true)).withColumn("is_deleted",lit(false))
      .select($"id",$"src_attr" as "attr",lit(true) as "is_current",lit(false) as "is_deleted",$"src_start_date" as "start_date",$"src_end_date" as "end_date")

    //Union All
    //Ordered by to make it more clear it's not required in implementation
    val final_merged_df=df_NoAction.union(df_insert).union(df_delete).union(df_upsert_1).union(df_upsert_2).orderBy("id")
    final_merged_df.show()

    sparkSession.close()


  }

}
