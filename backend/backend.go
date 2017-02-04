package backend

import (
	"errors"
	"fmt"
	"io/ioutil"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/immesys/spawnpoint/objects"
	"github.com/immesys/spawnpoint/spawnclient"
	"github.com/mgutz/ansi"
	uuid "github.com/satori/go.uuid"
)

const successPrefix = "[SUCCESS]"
const failurePrefix = "[FAILURE]"

const serviceCutoff = 10 * time.Second

func readProtoFile(name string) (*Deployment, error) {
	rawBytes, err := ioutil.ReadFile(name)
	if err != nil {
		return nil, err
	}

	var deployment Deployment
	if err = proto.Unmarshal(rawBytes, &deployment); err != nil {
		return nil, err
	}
	return &deployment, nil
}

func paramStringToSlice(params *map[string]string, name string) []string {
	paramVal, ok := (*params)[name]
	if ok {
		return regexp.MustCompile(",\\s*").Split(paramVal, -1)
	}
	return nil
}

func lenientBoolParse(s string) bool {
	b, err := strconv.ParseBool(s)
	if err != nil {
		return false
	}
	return b
}

func DeployConfig(entity string, configFile string, sched Scheduler) error {
	deployment, err := readProtoFile(configFile)
	if err != nil {
		return err
	}
	// Use top-level entity as default if one isn't specified for a service
	for _, service := range deployment.Services {
		_, ok := service.Params["entity"]
		if !ok {
			service.Params["entity"] = entity
		}
	}

	spawnClient, err := spawnclient.New("", entity)
	if err != nil {
		err = fmt.Errorf("Failed to initialize spawn client: %v", err)
		return err
	}

	allSpawnpoints := make(map[string]spawnpointInfo)
	existingServices := make(map[string]string)
	for _, uri := range deployment.SpawnpointUris {
		spawnpoints, err := spawnClient.Scan(uri)
		if err == nil {
			for _, spawnpoint := range spawnpoints {
				if spawnpoint.Good() {
					allSpawnpoints[spawnpoint.Alias] = spawnpointInfo{SpawnPoint: spawnpoint}
				}
			}
		}
	}

	for alias, spawnpoint := range allSpawnpoints {
		svcs, rawMd, err := spawnClient.Inspect(spawnpoint.URI)
		if err != nil {
			return err
		}

		metadata := make(map[string]string)
		for key, mdTup := range rawMd {
			if time.Now().Sub(time.Unix(0, mdTup.Timestamp)) < objects.MetadataCutoff {
				metadata[key] = mdTup.Value
			}
		}
		spawnpoint.Metadata = metadata
		allSpawnpoints[alias] = spawnpoint

		for _, service := range svcs {
			if time.Now().Sub(service.LastSeen) < serviceCutoff {
				hostAlias := service.HostURI[strings.LastIndex(service.HostURI, "/")+1:]
				existingServices[service.Name] = hostAlias
			}
		}
	}

	placement, err := sched.schedule(deployment, allSpawnpoints)
	if err != nil {
		return err
	}
	ordering := topologicalSort(deployment)

	// Set up overlay network for apps that want to use normal sockets
	netName := uuid.NewV4().String()
	// TODO: By leaving this up to Spawnd instances, there is a race condition

	for _, service := range ordering {
		alias, ok := existingServices[service.Name]
		if ok {
			fmt.Printf("%sService %s is already running on spawnpoint \"%s\", skipping deployment%s\n",
				ansi.ColorCode("yellow+b"), service.Name, alias, ansi.ColorCode("reset"))
			continue
		}

		spAlias := placement[service]
		build := paramStringToSlice(&service.Params, "build")
		run := paramStringToSlice(&service.Params, "run")
		volumes := paramStringToSlice(&service.Params, "volumes")
		includedDirs := paramStringToSlice(&service.Params, "includedDirs")
		includedFiles := paramStringToSlice(&service.Params, "includedFiles")
		cpuShares, _ := strconv.ParseUint(service.Params["cpuShares"], 10, 64)

		config := objects.SvcConfig{
			ServiceName:   service.Name,
			Entity:        service.Params["entity"],
			Image:         service.ImageName,
			Build:         build,
			Source:        service.Params["source"],
			Run:           run,
			MemAlloc:      service.Params["memAlloc"],
			CPUShares:     cpuShares,
			Volumes:       volumes,
			IncludedFiles: includedFiles,
			IncludedDirs:  includedDirs,
			AutoRestart:   lenientBoolParse(service.Params["autoRestart"]),
			RestartInt:    service.Params["restartInt"],
			OverlayNet:    netName,
		}

		relevantSp := allSpawnpoints[spAlias]
		log, err := spawnClient.DeployService(service.asString(), &config, relevantSp.URI, service.Name)
		if err != nil {
			return err
		}
		for msg := range log {
			PrintLogMsg(msg)
			if strings.HasPrefix(msg.Contents, successPrefix) {
				go func() {
					for msg := range log {
						PrintLogMsg(msg)
					}
				}()
				break
			} else if strings.HasPrefix(msg.Contents, failurePrefix) {
				errMsg := msg.Contents[strings.Index(msg.Contents, "]")+2:]
				return errors.New(errMsg)
			}
		}
	}
	return nil
}

func topologicalSort(deployment *Deployment) [](*Service) {
	var orderingByName []string
	adjList := createAdjacencyList(deployment.Topology)
	visited := make(map[string]bool)
	for srcNode := range *adjList {
		if !visited[srcNode] {
			visit(srcNode, adjList, &visited, &orderingByName)
		}
	}

	services := make(map[string]*Service)
	finalOrdering := make([](*Service), len(orderingByName))
	for _, service := range deployment.Services {
		services[service.Name] = service
	}
	for i, name := range orderingByName {
		finalOrdering[i] = services[name]
		delete(services, name)
	}

	// Some services may not have been included in the dependency graph
	for _, service := range services {
		finalOrdering = append(finalOrdering, service)
	}
	return finalOrdering
}

func createAdjacencyList(links [](*Link)) *map[string]([]string) {
	adjList := make(map[string]([]string))
	for _, link := range links {
		adjList[link.Src] = append(adjList[link.Src], link.Dest)
	}
	return &adjList
}

func visit(node string, adjList *map[string]([]string), visited *map[string]bool, ordering *[]string) {
	if !(*visited)[node] {
		(*visited)[node] = true
		for _, dest := range (*adjList)[node] {
			visit(dest, adjList, visited, ordering)
		}
		*ordering = append(*ordering, node)
	}
}
