package edu.berkeley.cs.sdb.raptor

case class Service(name: String, imageName: String, params: Map[String, Any],
                   spawnpointSpec: Either[String, Map[String, Any]])