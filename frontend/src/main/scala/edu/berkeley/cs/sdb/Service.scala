package edu.berkeley.cs.sdb

case class Service(name: String, imageName: String, params: Seq[(String, Any)],
                   spawnpointSpec: Either[String, Seq[(String, Any)]])
