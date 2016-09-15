package edu.berkeley.cs.sdb.raptor

import scala.util.Try
import scala.util.parsing.combinator._

object ConfigParser extends JavaTokenParsers {
  case class VarReference(name: String)

  private def stripQuotes(s: String) =
    if (s.startsWith("\"") && s.endsWith("\"")) {
      s.substring(1, s.length - 1)
    } else {
      s
    }

  // Primitives used to specify deployment options or parameters
  def Path: Parser[String] = """(/|~/)?([a-zA-Z\._\-]+/)*[a-zA-Z\._\-]+""".r

  def URI: Parser[String] = """([a-zA-Z\._\-]+/)*[a-zA-Z\._\-]+(/\*|/\+)?""".r

  def MemAllocLiteral: Parser[String] = """[0-9]+[MmGg]""".r

  def ContainerName: Parser[String] = Path ~ opt(":" ~ ident) ^^ {
    case containerPath ~ None => containerPath
    case containerPath ~ Some(":" ~ tag) => containerPath + ":" + tag
  }

  // The right-hand side of any parameter
  def Value: Parser[String] = stringLiteral ^^ (stripQuotes(_)) |
    URI |
    Path |
    MemAllocLiteral |
    wholeNumber |
    floatingPointNumber

  def ValueSequence: Parser[String] = "[" ~> repsep(Value, ",") <~ opt(",") ~ "]" ^^ (_.mkString(", "))

  def Parameter: Parser[(String, String)] = ident ~ (":" | "=") ~ (Value | ident | ValueSequence) ^^ { case k ~ (":" | "=") ~ v => (k, v) }

  def ParameterSequence: Parser[Map[String, String]] = "{" ~> repsep(Parameter, ",") <~ opt(",") ~ "}" ^^ (_.toMap)

  def Entity: Parser[String] = "entity" ~> Path

  def SpawnpointList: Parser[Seq[String]] = "spawnpoints" ~ "[" ~> repsep(URI, ",") <~ opt(",") ~ "]"

  def DependencyList: Parser[Seq[String]] = "external" ~ "[" ~> repsep(ident | stringLiteral ^^ (stripQuotes(_)), ",") <~ opt(",") ~ "]"

  def DependencySpec: Parser[Seq[String]] = opt(DependencyList) ^^ {
    case None => Seq.empty[String]
    case Some(deps) => deps
  }

  def SpawnpointSpec: Parser[Either[String, Map[String, String]]] =
    "on" ~> (ident | stringLiteral ^^ (stripQuotes(_))) ^^ (Left(_)) |
    "where" ~> ParameterSequence ^^ (Right(_))

  def ServiceDeployment: Parser[Seq[Service]] = "container" ~ ContainerName ~ "as" ~ ident ~ "with" ~ ParameterSequence ~ SpawnpointSpec ^^
      { case "container" ~ imgName ~ "as" ~ svcName ~ "with" ~ params ~ spec =>
        spec match {
          case Left(spawnpointName) => Seq(Service(svcName, imgName, params, spawnpointName, Map.empty[String, String]))
          case Right(spawnpointParams) => Seq(Service(svcName, imgName, params, "", spawnpointParams))
        }
      }

  def ForComprehension: Parser[Seq[Service]] = "for" ~ ident ~ "in" ~ "[" ~ rep1sep(Value, ",") ~ "]" ~ "{" ~ ServiceDeployment ~ "}" ^^
      { case "for" ~ varName ~ "in" ~ "[" ~ varValues ~  "]" ~ "{" ~ Seq(service) ~ "}" => varValues.zipWithIndex.map { case (varValue, idx) =>
          val varReference = "${" + varName + "}"
          val svcName = if (service.name.contains(varReference)) {
            service.name.replace(varReference, varValue)
          } else {
            service.name + idx.toString
          }
          val imageName = service.imageName.replace(varReference, varValue)
          val params = service.params.map { case (k, v) => (k, v.replace(varReference, varValue)) }
          val spawnpointName = service.spawnpointName.replace(varReference, varValue)
          val constraints = service.constraints.map { case (k, v) => (k, v.replace(varReference, varValue)) }
          Service(svcName, imageName, params, spawnpointName, constraints)
        }
      }

  def SvcPath: Parser[Seq[Link]] = repsep(ident, "->") ^^ (_.sliding(2).map { case Seq(x, y) => Link(x, y) }.toSeq)

  def SvcGraph: Parser[Seq[Link]] = "dependencies" ~ "{" ~> repsep(SvcPath, ",") <~ opt(",") ~ "}" ^^ (_.flatten)

  def SvcGraphSpec: Parser[Seq[Link]] = opt(SvcGraph) ^^ {
    case None => Seq.empty[Link]
    case Some(links) => links
  }

  def DeploymentConfig: Parser[Deployment] = Entity ~ SpawnpointList ~ DependencySpec ~ rep1(ServiceDeployment | ForComprehension) ~ SvcGraphSpec ^^ {
    case ent ~ spawnpoints ~ dependencies ~ svcs ~ svcConns =>
      Deployment(ent, spawnpoints, dependencies, svcs.flatten, svcConns)
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
