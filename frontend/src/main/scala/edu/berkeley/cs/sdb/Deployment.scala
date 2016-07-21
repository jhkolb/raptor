package edu.berkeley.cs.sdb

case class Deployment(entity: String, spawnPointUris: Seq[String], services: Seq[Service],
                      topology: Seq[(String, String)])
