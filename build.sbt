
scalaVersion := "2.13.1"

name := "lauda"
organization := "io.blue"
version := "1.0"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.10.2"
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.10.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.2"
libraryDependencies += "info.picocli" % "picocli" % "4.2.0"
libraryDependencies += "me.tongfei" % "progressbar" % "0.8.1"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"


assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter { f =>
    Array("ojdbc7.jar", "postgresql-42.2.11.jar").contains(
      f.data.getName
    )
  }
}