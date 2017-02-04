package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"

	"github.com/immesys/spawnpoint/objects"
	"github.com/jhkolb/raptor/backend"
	"github.com/mgutz/ansi"
	"github.com/urfave/cli"
)

const tempOutputFile = "temp.protobuf"

var parserLocation string

func main() {
	app := cli.NewApp()
	app.Name = "raptor"
	app.Usage = "Configuration manager for Spawnpoint deployments"
	app.Version = "0.0.3 'Aviary'"

	app.Commands = []cli.Command{
		{
			Name:   "submit",
			Usage:  "Submit a deployment configuration file",
			Action: actionSubmit,
			Flags: []cli.Flag{
				cli.StringFlag{
					Name:  "input, i",
					Usage: "set the input configuration file",
					Value: "",
				},
				cli.StringFlag{
					Name:   "entity, e",
					Usage:  "set the entity key file",
					EnvVar: "BW2_DEFAULT_ENTITY",
				},
			},
		},
	}

	// This is an ugly (and hopefully temporary) hack
	parserLocation = os.Getenv("GOPATH") + "/src/github.com/jhkolb/raptor/frontend/target/scala-2.11/frontend.jar"
	app.Run(os.Args)
}

func actionSubmit(c *cli.Context) error {
	entityFile := c.String("entity")
	if entityFile == "" {
		fmt.Println("Missing 'entity' parameter")
		os.Exit(1)
	}

	inputFile := c.String("input")
	if inputFile == "" {
		fmt.Println("Missing 'input' parameter")
		os.Exit(1)
	}

	inputFileLoc, _ := filepath.Abs(expandTilde(inputFile))

	rawOutput, err := exec.Command("scala", parserLocation, inputFileLoc, tempOutputFile).CombinedOutput()
	output := string(rawOutput)
	if err != nil {
		if output != "" {
			fmt.Print(output)
		} else {
			fmt.Println(err)
		}
		os.Exit(1)
	}

	err = backend.DeployConfig(entityFile, tempOutputFile, new(backend.FirstFitScheduler))
	os.Remove(tempOutputFile)
	if err != nil {
		fmt.Printf("%sDeployment failed: %v%s\n", ansi.ColorCode("red+b"), err, ansi.ColorCode("reset"))
		os.Exit(1)
	}

	fmt.Printf("%sDeployment complete, continuing to tail logs. Ctrl-C to quit.%s\n",
		ansi.ColorCode("green+b"), ansi.ColorCode("reset"))
	for {
		time.Sleep(5 * time.Second)
	}
}

func tailLog(log chan *objects.SPLogMsg) {
	for logMsg := range log {
		backend.PrintLogMsg(logMsg)
	}
}

func expandTilde(path string) string {
	homeDir := os.Getenv("HOME")
	return strings.Replace(path, "~", homeDir, -1)
}
