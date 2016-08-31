package backend

import "github.com/immesys/spawnpoint/objects"

type spawnpointInfo struct {
	objects.SpawnPoint
	Metadata map[string]string
}

type Scheduler interface {
	schedule(*Deployment, map[string]spawnpointInfo) (map[*Service]string, error)
}
