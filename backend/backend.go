package backend

import (
	"fmt"
	"io/ioutil"
	"regexp"
	"strconv"

	"github.com/golang/protobuf/proto"
	"github.com/immesys/spawnpoint/objects"
	"github.com/immesys/spawnpoint/spawnclient"
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

func DeployConfig(configFile string) ([]chan *objects.SPLogMsg, error) {
	deployment, err := readProtoFile(configFile)
	if err != nil {
		return nil, err
	}
	spawnClient, err := spawnclient.New("127.0.0.1:28589", deployment.Entity)
	if err != nil {
		err = fmt.Errorf("Failed to initialize spawn client: %v", err)
		return nil, err
	}

	allSpawnpoints := make(map[string]objects.SpawnPoint)
	for _, uri := range deployment.SpawnpointUris {
		spawnpoints, err := spawnClient.Scan(uri)
		if err == nil {
			for _, spawnpoint := range spawnpoints {
				allSpawnpoints[spawnpoint.Alias] = spawnpoint
			}
		}
	}

	// Ensure that all service configs reference a valid spawnpoint -- no partial deployments
	for _, service := range deployment.Services {
		if _, ok := allSpawnpoints[service.SpawnpointName]; !ok {
			err = fmt.Errorf("Service %s references unknown spawnpoint %s", service.Name, service.SpawnpointName)
			return nil, err
		}
	}

	var logs []chan *objects.SPLogMsg
	for _, service := range deployment.Services {
		build := paramStringToSlice(&service.Params, "build")
		run := paramStringToSlice(&service.Params, "run")
		volumes := paramStringToSlice(&service.Params, "volumes")
		includedDirs := paramStringToSlice(&service.Params, "includedDirs")
		includedFiles := paramStringToSlice(&service.Params, "includedFiles")
		cpuShares, err := strconv.ParseUint(service.Params["cpuShares"], 10, 64)
		if err != nil {
			return nil, err
		}

		config := objects.SvcConfig{
			ServiceName:   service.Name,
			Entity:        service.Params["entity"],
			Container:     service.ImageName,
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
		}

		relevantSp := allSpawnpoints[service.SpawnpointName]
		log, err := spawnClient.DeployService(&config, relevantSp.URI, service.Name)
		if err != nil {
			return nil, err
		}

		logs = append(logs, log)
	}
	return logs, nil
}
