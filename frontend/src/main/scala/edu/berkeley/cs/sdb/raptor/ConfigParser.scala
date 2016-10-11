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

  def ImageName: Parser[String] = Path ~ opt(":" ~ ident) ^^ {
    case containerPath ~ None => containerPath
    case containerPath ~ Some(":" ~ tag) => containerPath + ":" + tag
  }

  def simpleName: Parser[String] = """[a-zA-Z0-9_]+""".r

  def VarSubstitution: Parser[String] = """[a-zA-Z0-9\._\-]*\$\{[a-zA-Z0-9]+\}[a-zA-Z0-9\._\-]*""".r

  def Value: Parser[String] = stringLiteral ^^ (stripQuotes(_)) |
    URI |
    Path |
    MemAllocLiteral |
    wholeNumber |
    floatingPointNumber |
    VarSubstitution

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
    "on" ~> (simpleName | VarSubstitution) ^^ (Left(_)) |
    "where" ~> ParameterSequence ^^ (Right(_))

  def ServiceDeployment: Parser[Seq[Service]] = "container" ~ ImageName ~ "as" ~ (simpleName | VarSubstitution) ~ "with" ~ ParameterSequence ~ SpawnpointSpec ^^
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

  private def extractContainerVars(s: Service): Seq[String] = {
    val varPattern = """\$\{[a-zA_Z0-9]+\}""".r
    val nameVar = varPattern.findFirstIn(s.name)
    val imageNameVar = varPattern.findFirstIn(s.imageName)
    val spawnpointNameVar = varPattern.findFirstIn(s.spawnpointName)
    val paramVars = s.params.values.map(varPattern.findFirstIn(_))
    val constraintVars = s.constraints.values.map(varPattern.findFirstIn(_))

    Seq(nameVar, imageNameVar, spawnpointNameVar).flatten ++ paramVars.flatten ++ constraintVars.flatten
  }

  def validate(deployment: Deployment): Option[String] = {
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

    val undefinedVars = deployment.services.flatMap(extractContainerVars(_))
    if (undefinedVars.nonEmpty) {
      return Some(s"Variable ${undefinedVars.head} is not defined")
    }
    None
  }
}
