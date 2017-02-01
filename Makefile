raptor: main.go backend/*.go frontend/target/scala-2.11/frontend.jar
	go build

frontend/target/scala-2.11/frontend.jar: frontend/src/main/scala/edu/berkeley/cs/sdb/raptor/*.scala \
		  frontend/src/test/scala/edu/berkeley/cs/sdb/raptor/*.scala \
		  frontend/src/main/protobuf/raptor.proto
	cd frontend && sbt assembly

backend/raptor.pb.go:	backend/raptor.proto
	protoc --proto_path=backend/ --go_out=backend/ backend/raptor.proto

clean:
	rm -f raptor && cd frontend && sbt clean
