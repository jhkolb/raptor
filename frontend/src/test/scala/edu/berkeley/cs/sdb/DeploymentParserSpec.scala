package edu.berkeley.cs.sdb

import org.scalatest.FunSuite

import scala.io.Source

class DeploymentParserSpec extends FunSuite {
  test("Simple config file should parse successfully into deployment") {
    val configText = Source.fromURL(getClass.getResource("/test1.rpt")).mkString
    val parser = new ConfigParser()
    parser.parseAll(parser.deployment, configText) match {
      case parser.Success(result, _) => println(result)
      case parser.NoSuccess(cause, _) => fail(cause)
    }
  }
}
