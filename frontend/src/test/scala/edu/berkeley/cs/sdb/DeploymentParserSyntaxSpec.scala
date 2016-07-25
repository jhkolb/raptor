package edu.berkeley.cs.sdb

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
    ensureParseSuccess("test0.rpt")
  }

  test("Config file with external services") {
    ensureParseSuccess("test1.rpt")
  }

  test("Config file with service graph") {
    ensureParseSuccess("test2.rpt")
  }

  test("Full config file") {
    ensureParseSuccess("test3.rpt")
  }

  test("Config file missing entity") {
    ensureParseFailure("test4.rpt")
  }

  test("Config file missing target spawnpoint list") {
    ensureParseFailure("test5.rpt")
  }

  test("Config file missing container deployment list") {
    ensureParseFailure("test6.rpt")
  }
}
