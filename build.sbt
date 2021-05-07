name := "spark-scd2"

version := "0.1"

scalaVersion := "2.11.9"
libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "2.7.4"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.4"
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.4"
libraryDependencies += "log4j" % "log4j" % "1.2.17"
libraryDependencies +="org.apache.spark" %% "spark-hive" % "2.4.4"


//configuration file dependency
