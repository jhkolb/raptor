package edu.berkeley.cs.sdb

import scala.util.parsing.combinator._

class ConfigParser extends JavaTokenParsers {
  private def stripQuotes(s: String) =
    if (s.startsWith("\"") && s.endsWith("\"")) {
      s.substring(1, s.length - 2)
    } else {
      s
    }

  // Primitives used to specify deployment options or parameters
  def path: Parser[String] = """(/|~/)?([a-zA-Z\._\-]+/)*[a-zA-Z\._\-]+""".r
  def URI: Parser[String] = """([a-zA-Z\._\-]+/)*[a-zA-Z\._\-]+(/\*|/\+)?""".r
  def memAllocLiteral: Parser[String] = """[0-9]+[MmGg]""".r

  // The right-hand side of any parameter
  def value: Parser[Any] = stringLiteral ^^ (stripQuotes(_)) |
    URI |
    path |
    memAllocLiteral |
    wholeNumber ^^ (_.toInt) |
    floatingPointNumber ^^ (_.toDouble)

  def valueSequence: Parser[Seq[Any]] = "[" ~> repsep(value, ",") <~ opt(",") ~ "]"

  def parameter: Parser[(String, Any)] = ident ~ (":" | "=") ~ (value | ident | valueSequence) ^^
      { case k ~ (":" | "=") ~ v => (k,v) }

  def parameterSequence: Parser[Seq[(String, Any)]] = "{" ~> repsep(parameter, ",") <~ opt(",") ~ "}"

  def entity: Parser[String] = "entity" ~> path

  def spawnpointList: Parser[Seq[String]] = "spawnpoints" ~ "[" ~> repsep(URI, ",") <~ opt(",") ~ "]"

  def dependencyList: Parser[Seq[String]] = "external" ~ "[" ~> repsep(ident | stringLiteral ^^ (stripQuotes(_)), ",") <~ opt(",") ~ "]"
  def dependencySpec: Parser[Seq[String]] = opt(dependencyList) ^^ {
    case None => Seq.empty[String]
    case Some(deps) => deps
  }

  def spawnpointSpec: Parser[Either[String, Seq[(String, Any)]]] =
      "on" ~> (ident | stringLiteral ^^ (stripQuotes(_))) ^^ (Left(_)) |
      "where" ~> parameterSequence ^^ (Right(_))

  def serviceDeployment: Parser[Service] = "container" ~ path ~ "as" ~ ident ~ "with" ~ parameterSequence ~ spawnpointSpec ^^
      { case "container" ~ imgName ~ "as" ~ svcName ~ "with" ~ params ~ spec => Service(svcName, imgName, params, spec) }

  def svcPath: Parser[Seq[(String, String)]] = repsep(ident, "->") ^^ (_.sliding(2).toList.map { case Seq(x,y) => (x,y) })
  def svcGraph: Parser[Seq[(String, String)]] = "dependencies" ~ "{" ~> repsep(svcPath, ",") <~ opt(",") ~ "}" ^^ (_.flatten)
  def svcGraphSpec: Parser[Seq[(String, String)]] = opt(svcGraph) ^^ {
    case None => Seq.empty[(String, String)]
    case Some(svcConns) => svcConns
  }

  def deployment: Parser[Deployment] = entity ~ spawnpointList ~ dependencySpec ~ rep1(serviceDeployment) ~ svcGraphSpec ^^ {
    case ent ~ spawnpoints ~ dependencies ~ svcs ~ svcConns => Deployment(ent, spawnpoints, dependencies, svcs, svcConns)
  }
}