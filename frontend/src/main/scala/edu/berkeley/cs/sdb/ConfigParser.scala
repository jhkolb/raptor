package edu.berkeley.cs.sdb

import scala.util.parsing.combinator._

class ConfigParser extends JavaTokenParsers {
  def path: Parser[String] = """/?([~a-zA-Z\._\-]+/)*[a-zA-Z\._\-]+""".r
  def entity = "entity" ~> path


  def URI: Parser[String] = """([a-zA-Z\._\-]+/)*[a-zA-Z\._\-]+(/\* | /\+)?""".r
  def spawnpointList: Parser[Seq[String]] = "spawnpoints" ~> "{" ~> rep1sep(URI, ",") <~ "}"


  def value: Parser[Any] = stringLiteral |
                           wholeNumber ^^ (_.toInt) |
                           floatingPointNumber ^^ (_.toDouble)

  def valueSequence: Parser[Seq[Any]] = "[" ~> rep1sep(value, ",") <~ "]"

  def parameter: Parser[(String, Any)] = ident ~ (":" | "=") ~ (ident | value | valueSequence) ^^ { case k ~ (":"|"=") ~ v => (k,v) }

  def parameterSequence: Parser[Seq[(String, Any)]] = "{" ~> repsep(parameter, ",") <~ "}"

  def spawnpointSpec: Parser[Either[String, Seq[(String, Any)]]] =
      "on" ~> (ident | stringLiteral) ^^ (Left(_))
      "where" ~> parameterSequence ^^ (Right(_))

  def serviceDeployment: Parser[Service] = "deploy" ~ path ~ "as" ~ ident ~ "with" ~ parameterSequence ~ spawnpointSpec ^^
      { case "deploy" ~ imgName ~ "as" ~ svcName ~ "with" ~ params ~ spec => Service(svcName, imgName, params, spec) }

  def svcGraph: Parser[Seq[(String, String)]] = repsep(ident, "->") ^^ (_.sliding(2).map { case List(x, y) => (x, y) }.toSeq)

  def deployment: Parser[Deployment] = entity ~ spawnpointList ~ rep(serviceDeployment) ~ rep(svcGraph) ^^
      { case ent ~ spawnpoints ~ svcs ~ connList => Deployment(ent, spawnpoints, svcs, connList.flatten) }
}