package edu.berkeley.cs.sdb.raptor

import org.scalatest.FunSuite

import scala.io.Source

class DeploymentParserSyntaxSpec extends FunSuite {
  private def ensureParseSuccess(fileName: String) = {
    val configText = Source.fromURL(getClass.getResource("/" + fileName)).mkString
    assert(ConfigParser.parseAll(ConfigParser.DeploymentConfig, configText).successful)
  }

  private def ensureParseFailure(fileName: String) = {
    val configText = Source.fromURL(getClass.getResource("/" + fileName)).mkString
    assert(!ConfigParser.parseAll(ConfigParser.DeploymentConfig, configText).successful)
  }

  test("Minimal config file") {
    ensureParseSuccess("syntax/test0.rpt")
  }

  test("Config file with external services") {
    ensureParseSuccess("syntax/test1.rpt")
  }

  test("Config file with service graph") {
    ensureParseSuccess("syntax/test2.rpt")
  }

  test("Full config file") {
    ensureParseSuccess("syntax/test3.rpt")
  }

  test("Config file missing target spawnpoint list") {
    ensureParseFailure("syntax/test5.rpt")
  }

  test("Config file missing container deployment list") {
    ensureParseFailure("syntax/test6.rpt")
  }

  test("Config file with useless for comprehension") {
    ensureParseSuccess("syntax/test7.rpt")
  }

  test("Config file with for comprehension influencing param value") {
    ensureParseSuccess("syntax/test8.rpt")
  }

  test("Config file with for comprehension influencing container name") {
    ensureParseSuccess("syntax/test9.rpt")
  }

  test("Config file with for comprehension influencing destination") {
    ensureParseSuccess("syntax/test10.rpt")
  }
}
