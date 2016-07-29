name := "raptor-frontend"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test"

mainClass in assembly := Some("edu.berkeley.cs.sdb.raptor.ExecParser")