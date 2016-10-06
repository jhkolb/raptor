package backend

import (
	"fmt"
	"strconv"

	"github.com/immesys/spawnpoint/objects"
)

type FirstFitScheduler struct{}

func (sched *FirstFitScheduler) schedule(deployment *Deployment,
	spawnpoints map[string]spawnpointInfo) (map[*Service]string, error) {

	placements := make(map[*Service]string)
	for _, service := range deployment.Services {
		rawMemAlloc := service.Params["memAlloc"]
		memAlloc, err := objects.ParseMemAlloc(rawMemAlloc)
		if err != nil {
			return nil, fmt.Errorf("Service %s specifies invalid memory allocation: %s", service.Name, rawMemAlloc)
		}

		rawCPUShares := service.Params["cpuShares"]
		cpuShares, err := strconv.ParseUint(rawCPUShares, 10, 64)
		if err != nil {
			return nil, fmt.Errorf("Service %s specifies invalid cpu shares: %s", service.Name, rawCPUShares)
		}

		if service.SpawnpointName != "" {
			relevantSp, ok := spawnpoints[service.SpawnpointName]
			if !ok {
				return nil, fmt.Errorf("Service %s references unkonwn spawnpoint %s", service.Name, service.SpawnpointName)
			} else if relevantSp.AvailableCPUShares < int64(cpuShares) {
				return nil, fmt.Errorf("Insufficient CPU shares on Spawnpoint %s for service %s (want %d, have %d)",
					service.SpawnpointName, service.Name, cpuShares, relevantSp.AvailableCPUShares)
			} else if relevantSp.AvailableMem < int64(memAlloc) {
				return nil, fmt.Errorf("Insufficient memory on Spawnpoint %s for service %s (want %d, have %d)",
					service.SpawnpointName, service.Name, memAlloc, relevantSp.AvailableMem)
			}

			placements[service] = relevantSp.Alias
		} else {
			for _, spInfo := range spawnpoints {
				if metadataMatches(service.Constraints, spInfo.Metadata) &&
					int64(memAlloc) <= spInfo.AvailableMem && int64(cpuShares) <= spInfo.AvailableCPUShares {
					placements[service] = spInfo.Alias
					break
				}
			}

			_, ok := placements[service]
			if !ok {
				return nil, fmt.Errorf("Could not satisfy metadata constraints for service %s", service.Name)
			}
		}
	}
	return placements, nil
}

func metadataMatches(required map[string]string, actual map[string]string) bool {
	for requiredKey, requiredVal := range required {
		actualVal, ok := actual[requiredKey]
		if !ok || actualVal != requiredVal {
			return false
		}
	}
	return true
}
