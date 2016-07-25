package edu.berkeley.cs.sdb

/**
  * A collection of Spawnpoint services that form a cohesive deployment
  * @param entity The Bosswave entity to use for container deployment to Spawnpoint instances
  * @param spawnPointUris Bosswave base URIs for the Spawnpoints to deploy to. May identify particular
  *                       spawnpoint instances or express a base URI under which multiple Spawnpoints
  *                       reside.
  * @param externalDeps External services that this deployment requires to function.
  * @param services A list of specifications for the services to be deployed.
  * @param topology The relationships between the deployment's services, i.e. an expression of
  *                 which services rely upon each other to function properly.
  */
case class Deployment(entity: String, spawnPointUris: Seq[String], externalDeps: Seq[String], services: Seq[Service],
                      topology: Seq[(String, String)])
