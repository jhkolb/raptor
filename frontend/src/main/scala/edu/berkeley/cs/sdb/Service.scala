package edu.berkeley.cs.sdb

case class Service(name: String, imageName: String, params: Map[String, Any],
                   spawnpointSpec: Either[String, Map[String, Any]])