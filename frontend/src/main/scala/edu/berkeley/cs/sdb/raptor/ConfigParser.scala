package edu.berkeley.cs.sdb.raptor

import scala.util.Try
import scala.util.parsing.combinator._

object ConfigParser extends JavaTokenParsers {
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
  def value: Parser[String] = stringLiteral ^^ (stripQuotes(_)) |
    URI |
    path |
    memAllocLiteral |
    wholeNumber |
    floatingPointNumber

  def valueSequence: Parser[String] = "[" ~> repsep(value, ",") <~ opt(",") ~ "]" ^^ ("[" + _.mkString(", ") + "]")

  def parameter: Parser[(String, String)] = ident ~ (":" | "=") ~ (value | ident | valueSequence) ^^ { case k ~ (":" | "=") ~ v => (k, v) }

  def parameterSequence: Parser[Map[String, String]] = "{" ~> repsep(parameter, ",") <~ opt(",") ~ "}" ^^ (_.toMap)

  def entity: Parser[String] = "entity" ~> path

  def spawnpointList: Parser[Seq[String]] = "spawnpoints" ~ "[" ~> repsep(URI, ",") <~ opt(",") ~ "]"

  def dependencyList: Parser[Seq[String]] = "external" ~ "[" ~> repsep(ident | stringLiteral ^^ (stripQuotes(_)), ",") <~ opt(",") ~ "]"

  def dependencySpec: Parser[Seq[String]] = opt(dependencyList) ^^ {
    case None => Seq.empty[String]
    case Some(deps) => deps
  }

  def spawnpointSpec: Parser[Either[String, Map[String, String]]] =
    "on" ~> (ident | stringLiteral ^^ (stripQuotes(_))) ^^ (Left(_)) |
    "where" ~> parameterSequence ^^ (Right(_))

  def serviceDeployment: Parser[Service] = "container" ~ path ~ "as" ~ ident ~ "with" ~ parameterSequence ~ spawnpointSpec ^^
      { case "container" ~ imgName ~ "as" ~ svcName ~ "with" ~ params ~ spec =>
        spec match {
          case Left(spawnpointName) => Service(svcName, imgName, params, spawnpointName, Map.empty[String, String])
          case Right(spawnpointParams) => Service(svcName, imgName, params, "", spawnpointParams)
        }
      }

  def svcPath: Parser[Seq[Link]] = repsep(ident, "->") ^^ (_.sliding(2).map { case Seq(x, y) => Link(x, y) }.toSeq)

  def svcGraph: Parser[Seq[Link]] = "dependencies" ~ "{" ~> repsep(svcPath, ",") <~ opt(",") ~ "}" ^^ (_.flatten)

  def svcGraphSpec: Parser[Seq[Link]] = opt(svcGraph) ^^ {
    case None => Seq.empty[Link]
    case Some(links) => links
  }

  def deployment: Parser[Deployment] = entity ~ spawnpointList ~ dependencySpec ~ rep1(serviceDeployment) ~ svcGraphSpec ^^ {
    case ent ~ spawnpoints ~ dependencies ~ svcs ~ svcConns => Deployment(ent, spawnpoints, dependencies, svcs, svcConns)
  }

  private def findMissingParam(paramName: String, services: Seq[Service]): Option[String] = {
    services.find(!_.params.contains(paramName)) match {
      case None => None
      case Some(svc) => Some(svc.name)
    }
  }

  private def validMemAlloc(memAlloc: String): Boolean = {
    memAlloc.matches("\\d+[MmGg]")
  }

  def validate(deployment: Deployment): Option[String] = {
    val noEntName = findMissingParam("entity", deployment.services)
    if (noEntName.isDefined) {
      return Some("Description of service " + noEntName.get + " does not specify entity")
    }

    val noMemName = findMissingParam("memAlloc", deployment.services)
    if (noMemName.isDefined) {
      return Some("Description of service " + noMemName.get + " does not specify memory allocation")
    }
    val invalidMemSvc = deployment.services.find { svc => !validMemAlloc(svc.params("memAlloc")) }
    if (invalidMemSvc.isDefined) {
      return Some(s"Service ${invalidMemSvc.get.name} specifies invalid memory allocation")
    }

    val noCpuName = findMissingParam("cpuShares", deployment.services)
    if (noCpuName.isDefined) {
      return Some("Description of service " + noCpuName.get + " does not specify CPU shares")
    }
    val invalidCpuSvc = deployment.services.find { svc => Try(svc.params("cpuShares").toInt).isFailure }
    if (invalidCpuSvc.isDefined) {
      return Some(s"Service ${invalidCpuSvc.get.name} specifies invalid CPU share number")
    }

    val unknownLink = deployment.topology.find { case Link(from, to) =>
      (!deployment.services.map(_.name).contains(from) && !deployment.externalDeps.contains(from)) ||
        (!deployment.services.map(_.name).contains(to) && !deployment.externalDeps.contains(to))
    }
    if (unknownLink.isDefined) {
      return Some(s"Dependency ${unknownLink.get.src} -> ${unknownLink.get.dest} references unknown service")
    }
    None
  }
}
