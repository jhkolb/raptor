package main

import (
	"fmt"
	"io/ioutil"
	"os"

	"github.com/golang/protobuf/proto"
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

func main() {
	if len(os.Args) != 2 {
		fmt.Printf("Usage: %s <input file>\n", os.Args[0])
		os.Exit(1)
	}

	deployment, err := readProtoFile(os.Args[1])
	if err != nil {
		fmt.Printf("Failed to read input file: %v\n", err)
		os.Exit(1)
	} else {
		fmt.Printf("%+v\n", deployment)
	}
}
