package edu.berkeley.cs.sdb.raptor

import org.scalatest.FunSuite

import scala.io.Source

class DeploymentParserSyntaxSpec extends FunSuite {
  val parser = new ConfigParser

  private def ensureParseSuccess(fileName: String) = {
    val configText = Source.fromURL(getClass.getResource("/" + fileName)).mkString
    assert(parser.parseAll(parser.deployment, configText).successful)
  }

  private def ensureParseFailure(fileName: String) = {
    val configText = Source.fromURL(getClass.getResource("/" + fileName)).mkString
    assert(!parser.parseAll(parser.deployment, configText).successful)
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

  test("Config file missing entity") {
    ensureParseFailure("syntax/test4.rpt")
  }

  test("Config file missing target spawnpoint list") {
    ensureParseFailure("syntax/test5.rpt")
  }

  test("Config file missing container deployment list") {
    ensureParseFailure("syntax/test6.rpt")
  }
}
