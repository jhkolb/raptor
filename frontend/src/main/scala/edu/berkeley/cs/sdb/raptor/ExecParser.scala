package edu.berkeley.cs.sdb.raptor

import java.io.{File, FileOutputStream}

import scala.io.Source

object ExecParser {
  def main(args: Array[String]) = {
    if (args.length != 2) {
      println("Usage: ExecParser <input file> <output file>")
      System.exit(1)
    }

    val inputFileName = args(0)
    val rawInput = Source.fromFile(inputFileName).mkString
    ConfigParser.parseAll(ConfigParser.DeploymentConfig, rawInput) match {
      case ConfigParser.Failure(msg, _) =>
        println("Syntax error in config file: " + msg)
        System.exit(1)
      case ConfigParser.Success(deployment, _) =>
        ConfigParser.validate(deployment) match {
          case Some(err) =>
            println("Invalid configuration file: " + err)
            System.exit(1)
          case None =>
            val outputFile = new File(args(1))
            val fos = new FileOutputStream(outputFile)
            deployment.writeTo(fos)
        }
    }
  }
}
