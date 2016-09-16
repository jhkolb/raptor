# Raptor

Raptor lets you easily deploy distributed applications on top of
[Spawnpoint](https://github.com/immesys/spawnpoint). These applications are
instantiated as compositions of Docker containers that are then executed and
monitored by Spawnpoint instances. While Spawnpoint is primarily aimed at
services that communicate over [BOSSWAVE](https://github.com/immesys/bw2),
helpful features are available for native communication as well.

## Constructing an Application
First, write each component of your application as a Spawnpoint service. Consult
the Spawnpoint documentation for assistance. Once you are ready to deploy your
application's containers on top of a collection of Spawnpoint instances, you
will need to write a Raptor configuration.

Raptor currently expects applications to be expressed in a simple configuration
DSL. It is very similar to Spawnpoint's YAML configuration files. Every
configuration must contain a particular sequence of elements, detailed below.

### Entity
You must declare the BOSSWAVE entity that will be used to deploy your containers
on the relevant Spawnpoint instances. This is expressed as the absolute path
to a file containing the desired entity. For example:
```
entity /home/oski/bosswave/keys/appDev.ent
```

### Spawnpoints
Each configuration file requires a list of URIs that identify the Spawnpoints
on which containers may be deployed. You are free to include BOSSWAVE-style
wildcards in these URIs. For example:
```
spawnpoints [jkolb/spawnpoint/alpha, 410.dev/spawnpoint/pi/*]
```

### Container
Each of the application's containers is declared in a block with the following
elements:

* The Docker image to use for the container
* A name for the container (this must be unique)
* Spawnpoint configuration parameters such as BOSSWAVE entity, the command to
  run upon container start, etc. You *must* include the memory allocation
  (`memAlloc`) and CPU share (`cpuShares`) parameters. See Spawnpoint's
  documentation for a complete list of parameters.
* Either an alias specifically identifying the Spawnpoint on which the container
  should run or a list of metadata constraints that must be satisfied in order
  for a Spawnpoint to be eligible to run the container.

These blocks take the form:
```
container <image name> as <service name> with {
  <param key>: <param value>,
  <param key>: <param value>,
  ...
} on <spawnpoint alias>
```
when the intent is to run on a specific Spawnpoint, or
```
container <image name> as <service name> with {
  <param key>: <param value>,
  <param key>: <param value>,
  ...
} where {
  <metadata key> = <metadata value>,
  <metadata key> = <metadata value>,
}
```
when the intent is to run on a Spawnpoint satisfying certain metadata
constraints.

Here is a complete example:
```
container immesys/spawnpoint:amd64 as demosvc with {
  entity: /home/oski/bosswave/keys/appDev.ent,
  source: "git+http://github.com/jhkolb/demosvc",
  build: ["go get -d", "go build -o demosvc"],
  run: ["./demosvc", 100],
  memAlloc: 512M,
  cpuShares: 1024,
  includedFiles: [params.yml],
} on alpha
```

### `for` Comprehensions
To avoid repetitive configuration details, Raptor allows you to express multiple
containers that differ by a single aspect as a `for` comprehension. This takes
the form:
```
for <variable name> in [<var value 1>, <var value 2>, ...] {
  <container block>
}
```

Inside the container block, any occurrence of the loop variable (escaped with a
`$` and enclosing braces) is replaced with each of the values inside the value
list producing one distinct container for each value. For example, to easily
deploy two instances of `demosvc` on Spawnpoints `alpha` and `beta`:
```
for dest in [alpha, beta] {
  container jhkolb/spawnpoint:amd64 as demosvc with {
      entity: /home/oski/bosswave/keys/appDev.ent,
      source: "git+http://github.com/jhkolb/demosvc",
      build: ["go get -d", "go build -o demosvc"],
      run: ["./demosvc", 100],
      memAlloc: 512M,
      cpuShares: 1024,
      includedFiles: [params.yml],
  } on ${dest}
}
```
Note that the container name will be slightly modified if necessary within a
`for` comprehension to preserve uniqueness. This is done by appending a
numerical index to each container's name. The preceding `for` comprehension
will emit containers named `demsovc1` and `demosvc2`.

You may also embed variables in larger strings inside `for` comprehensions. For
example:
```
for str in [eapolis, essota] {
  container jhkolb/spawnpoint:amd64 as minn${str}Svc with {
    ...
  } on alpha
}
```
This deploys two containers, named `minneapolisSvc` and `minnesotaSvc` on
Spawnpoint `alpha`.

### tl;dr Putting it all Together
Here is a full example configuration file.
```
entity /home/oski/bosswave/keys/appDev.ent

spawnpoints [scratch.ns/spawnpoint/*, 410.dev/spawnpoint/pi/*,]

container jhkolb/spawnpoint:amd64 as demosvc with {
    entity: /home/oski/bosswave/keys/appDev.ent,
    source: "git+http://github.com/jhkolb/demosvc",
    build: ["go get -d", "go build -o demosvc"],
    run: ["./demosvc", 100],
    memAlloc: 512M,
    cpuShares: 1024,
    includedFiles: [params.yml],
} on alpha

for dest in [beta, gamma] {
  container kfchen/vision_frontend:latest as frontend with {
    entity: /home/oski/bosswave/keys/vision.ent,
    run: [./vision_server],
    memAlloc: 2G,
    cpuShares: 2048,
  } on dest
}
```

## Deploying an Application
Once you have written a Raptor DSL configuration, you are ready to launch your
application. Parsing a configuration file and launching the necessary Spawnpoint
containers is the job of the `raptor` binary.

Given a Raptor configuration file named `deploy.rpt`, execute the command
```
$ raptor submit -i deploy.rpt
```
to submit your configuration to they system and launch your application. At this
point, you may see a (hopefully descriptive) error message if your configuration
file does not pass the validation process.

Otherwise, as with Spawnpoint, the `raptor` utility will tail the logs for all
containers included in your application until `<Ctrl>-C` is pressed.
