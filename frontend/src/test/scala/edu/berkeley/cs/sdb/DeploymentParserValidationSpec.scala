package edu.berkeley.cs.sdb

import org.scalatest.FunSuite

import scala.io.Source

class DeploymentParserValidationSpec extends FunSuite {
  val parser = new ConfigParser

  private def ensureValidationSuccess(fileName: String): Unit = {
    val configText = Source.fromURL(getClass.getResource("/" + fileName)).mkString
    val parsedDep = parser.parseAll(parser.deployment, configText)
    assert(parsedDep.successful)
    assert(ConfigParser.validate(parsedDep.get).isEmpty)
  }

  private def ensureValidationFailure(fileName: String): Unit = {
    val configText = Source.fromURL(getClass.getResource("/" + fileName)).mkString
    val parsedDep = parser.parseAll(parser.deployment, configText)
    assert(ConfigParser.validate(parsedDep.get).isDefined)
  }

  test("Minimal config file") {
    ensureValidationSuccess("validation/test0.rpt")
  }

  test("Config file with external dependencies") {
    ensureValidationSuccess("validation/test1.rpt")
  }

  test("Config file with service graph") {
    ensureValidationSuccess("validation/test2.rpt")
  }

  test("Fully featured config file") {
    ensureValidationSuccess("validation/test3.rpt")
  }

  test("Service missing entity parameter") {
    ensureValidationFailure("validation/test4.rpt")
  }

  test("Service missing memAlloc parameter") {
    ensureValidationFailure("validation/test5.rpt")
  }

  test("Service missing cpuShares parameter") {
    ensureValidationFailure("validation/test6.rpt")
  }

  test("Topology references unknown service") {
    ensureValidationFailure("validation/test7.rpt")
  }
}