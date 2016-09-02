package backend

import (
	docker "github.com/fsouza/go-dockerclient"
)

var dkrClient *docker.Client

func createOverlayNet(name string) error {
	var err error
	if dkrClient == nil {
		dkrClient, err = docker.NewClient("unix://var/run/docker.sock")
	}
	if err != nil {
		return err
	}

	_, err = dkrClient.CreateNetwork(docker.CreateNetworkOptions{
		Name:   name,
		Driver: "overlay",
	})
	if err != nil {
		return err
	}
	return nil
}
