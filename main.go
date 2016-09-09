package main

import (
	"fmt"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/codegangsta/cli"
	"github.com/immesys/spawnpoint/objects"
	"github.com/jhkolb/raptor/backend"
	"github.com/mgutz/ansi"
)

const parserLocation = "frontend/target/scala-2.11/raptor-frontend-assembly-0.1.jar"
const tempOutputFile = "temp.protobuf"

func main() {
	app := cli.NewApp()
	app.Name = "raptor"
	app.Usage = "Configuration manager for Spawnpoint deployments"
	app.Version = "0.0.1 'Aviary'"

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
			},
		},
	}

	app.Run(os.Args)
}

func actionSubmit(c *cli.Context) error {
	inputFile := c.String("input")
	if inputFile == "" {
		fmt.Println("Missing 'input' parameter")
		os.Exit(1)
	}

	output, err := exec.Command("scala", parserLocation, inputFile, tempOutputFile).Output()
	if err != nil {
		fmt.Print(string(output))
		os.Exit(1)
	}

	logs, err := backend.DeployConfig(tempOutputFile, new(backend.FirstFitScheduler))
	os.Remove(tempOutputFile)
	if err != nil {
		fmt.Printf("%sDeployment failed: %v%s\n", ansi.ColorCode("red+b"), err, ansi.ColorCode("reset"))
		os.Exit(1)
	}

	fmt.Printf("%sDeployment complete, tailing logs. Ctrl-C to quit.%s\n",
		ansi.ColorCode("green+b"), ansi.ColorCode("reset"))
	for _, log := range logs {
		go tailLog(log)
	}
	for {
		time.Sleep(5 * time.Second)
	}
}

func tailLog(log chan *objects.SPLogMsg) {
	for logMsg := range log {
		tstring := time.Unix(0, logMsg.Time).Format("01/02 15:04:05")
		fmt.Printf("[%s] %s%s::%s > %s%s\n", tstring, ansi.ColorCode("blue+b"), logMsg.SPAlias,
			logMsg.Service, ansi.ColorCode("reset"), strings.Trim(logMsg.Contents, "\n"))
	}
}
