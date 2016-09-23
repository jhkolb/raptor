package backend

import (
	"fmt"
	"io/ioutil"
	"regexp"
	"strconv"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/immesys/spawnpoint/objects"
	"github.com/immesys/spawnpoint/spawnclient"
	uuid "github.com/satori/go.uuid"
)

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

func DeployConfig(configFile string, sched Scheduler) ([]chan *objects.SPLogMsg, error) {
	deployment, err := readProtoFile(configFile)
	if err != nil {
		return nil, err
	}
	spawnClient, err := spawnclient.New("", deployment.Entity)
	if err != nil {
		err = fmt.Errorf("Failed to initialize spawn client: %v", err)
		return nil, err
	}

	allSpawnpoints := make(map[string]spawnpointInfo)
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
		_, rawMd, err := spawnClient.Inspect(spawnpoint.URI)
		if err != nil {
			return nil, err
		}

		metadata := make(map[string]string)
		for key, mdTup := range rawMd {
			if time.Now().Sub(time.Unix(0, mdTup.Timestamp)) < objects.MetdataCutoff {
				metadata[key] = mdTup.Value
			}
		}
		spawnpoint.Metadata = metadata
		allSpawnpoints[alias] = spawnpoint
	}

	placement, err := sched.schedule(deployment, allSpawnpoints)
	if err != nil {
		return nil, err
	}

	// Set up overlay network for apps that want to use normal sockets
	netName := uuid.NewV4().String()
	// TODO: By leaving this up to Spawnd instances, there is a race condition

	logs := make([]chan *objects.SPLogMsg, len(placement))
	for service, spAlias := range placement {
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
			AptRequires:   service.Params["aptRequires"],
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
		log, err := spawnClient.DeployService(&config, relevantSp.URI, service.Name)
		if err != nil {
			return nil, err
		}

		logs = append(logs, log)
	}
	return logs, nil
}
