// Code generated by protoc-gen-go.
// source: raptor.proto
// DO NOT EDIT!

/*
Package raptor is a generated protocol buffer package.

It is generated from these files:
	raptor.proto

It has these top-level messages
	Service
	Link
	Deployment
*/
package main

import proto "github.com/golang/protobuf/proto"
import fmt "fmt"
import math "math"

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion2 // please upgrade the proto package

type Service struct {
	Name           string            `protobuf:"bytes,1,opt,name=name" json:"name,omitempty"`
	ImageName      string            `protobuf:"bytes,2,opt,name=imageName" json:"imageName,omitempty"`
	Params         map[string]string `protobuf:"bytes,4,rep,name=params" json:"params,omitempty" protobuf_key:"bytes,1,opt,name=key" protobuf_val:"bytes,2,opt,name=value"`
	SpawnpointName string            `protobuf:"bytes,5,opt,name=spawnpointName" json:"spawnpointName,omitempty"`
	Constraints    map[string]string `protobuf:"bytes,6,rep,name=constraints" json:"constraints,omitempty" protobuf_key:"bytes,1,opt,name=key" protobuf_val:"bytes,2,opt,name=value"`
}

func (m *Service) Reset()                    { *m = Service{} }
func (m *Service) String() string            { return proto.CompactTextString(m) }
func (*Service) ProtoMessage()               {}
func (*Service) Descriptor() ([]byte, []int) { return fileDescriptor0, []int{0} }

func (m *Service) GetParams() map[string]string {
	if m != nil {
		return m.Params
	}
	return nil
}

func (m *Service) GetConstraints() map[string]string {
	if m != nil {
		return m.Constraints
	}
	return nil
}

type Link struct {
	Src  string `protobuf:"bytes,1,opt,name=src" json:"src,omitempty"`
	Dest string `protobuf:"bytes,2,opt,name=dest" json:"dest,omitempty"`
}

func (m *Link) Reset()                    { *m = Link{} }
func (m *Link) String() string            { return proto.CompactTextString(m) }
func (*Link) ProtoMessage()               {}
func (*Link) Descriptor() ([]byte, []int) { return fileDescriptor0, []int{1} }

type Deployment struct {
	Entity         string     `protobuf:"bytes,1,opt,name=entity" json:"entity,omitempty"`
	SpawnpointUris []string   `protobuf:"bytes,2,rep,name=spawnpointUris" json:"spawnpointUris,omitempty"`
	ExternalDeps   []string   `protobuf:"bytes,3,rep,name=externalDeps" json:"externalDeps,omitempty"`
	Services       []*Service `protobuf:"bytes,4,rep,name=services" json:"services,omitempty"`
	Topology       []*Link    `protobuf:"bytes,5,rep,name=topology" json:"topology,omitempty"`
}

func (m *Deployment) Reset()                    { *m = Deployment{} }
func (m *Deployment) String() string            { return proto.CompactTextString(m) }
func (*Deployment) ProtoMessage()               {}
func (*Deployment) Descriptor() ([]byte, []int) { return fileDescriptor0, []int{2} }

func (m *Deployment) GetServices() []*Service {
	if m != nil {
		return m.Services
	}
	return nil
}

func (m *Deployment) GetTopology() []*Link {
	if m != nil {
		return m.Topology
	}
	return nil
}

func init() {
	proto.RegisterType((*Service)(nil), "Service")
	proto.RegisterType((*Link)(nil), "Link")
	proto.RegisterType((*Deployment)(nil), "Deployment")
}

func init() { proto.RegisterFile("raptor.proto", fileDescriptor0) }

var fileDescriptor0 = []byte{
	// 328 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x09, 0x6e, 0x88, 0x02, 0xff, 0x94, 0x92, 0xdf, 0x4a, 0xc3, 0x30,
	0x14, 0xc6, 0xe9, 0xba, 0xd5, 0xed, 0x6c, 0xc8, 0x38, 0x0c, 0xa9, 0xc3, 0x8b, 0x59, 0x44, 0x76,
	0x31, 0x7a, 0xa1, 0x37, 0xfe, 0x01, 0x6f, 0xd4, 0x3b, 0x11, 0xa9, 0xf8, 0x00, 0x71, 0x86, 0x11,
	0xd6, 0x26, 0x21, 0x89, 0xd3, 0x3e, 0x9a, 0x8f, 0xe0, 0x5b, 0x99, 0x64, 0xed, 0xb6, 0xee, 0xce,
	0xab, 0x9e, 0x7c, 0xe7, 0x7c, 0xbf, 0x70, 0xbe, 0x14, 0x06, 0x8a, 0x48, 0x23, 0x54, 0x2a, 0x95,
	0x30, 0x22, 0xf9, 0x6d, 0xc1, 0xc1, 0x2b, 0x55, 0x2b, 0x36, 0xa7, 0x88, 0xd0, 0xe6, 0xa4, 0xa0,
	0x71, 0x30, 0x09, 0xa6, 0xbd, 0xcc, 0xd7, 0x78, 0x02, 0x3d, 0x56, 0x90, 0x05, 0x7d, 0x76, 0x8d,
	0x96, 0x6f, 0x6c, 0x05, 0x9c, 0x41, 0x24, 0x89, 0x22, 0x85, 0x8e, 0xdb, 0x93, 0x70, 0xda, 0xbf,
	0x18, 0xa5, 0x15, 0x2b, 0x7d, 0xf1, 0xf2, 0x23, 0x37, 0xaa, 0xcc, 0xaa, 0x19, 0x3c, 0x87, 0x43,
	0x2d, 0xc9, 0x17, 0x97, 0x82, 0x71, 0xe3, 0x81, 0x1d, 0x0f, 0xdc, 0x53, 0xf1, 0x16, 0xfa, 0x73,
	0xc1, 0xb5, 0x51, 0xc4, 0x2a, 0x3a, 0x8e, 0x3c, 0xfa, 0x78, 0x83, 0xbe, 0xdf, 0xf6, 0xd6, 0xfc,
	0xdd, 0xe9, 0xf1, 0x35, 0xf4, 0x77, 0xee, 0xc6, 0x21, 0x84, 0x4b, 0x5a, 0x56, 0x2b, 0xb9, 0x12,
	0x47, 0xd0, 0x59, 0x91, 0xfc, 0xb3, 0xde, 0x66, 0x7d, 0xb8, 0x69, 0x5d, 0x05, 0xe3, 0x3b, 0x18,
	0xee, 0xb3, 0xff, 0xe3, 0x4f, 0x66, 0xd0, 0x7e, 0x62, 0x7c, 0xe9, 0x3c, 0x5a, 0xcd, 0x6b, 0x8f,
	0x2d, 0x5d, 0xb2, 0x1f, 0x54, 0x9b, 0xca, 0xe2, 0xeb, 0xe4, 0x27, 0x00, 0x78, 0xa0, 0x32, 0x17,
	0x65, 0x41, 0xb9, 0xc1, 0x23, 0x88, 0xec, 0x87, 0x99, 0xfa, 0xae, 0xea, 0xd4, 0x0c, 0xed, 0x4d,
	0x31, 0x6d, 0x21, 0x61, 0x33, 0x34, 0xa7, 0x62, 0x02, 0x03, 0xfa, 0x6d, 0xa8, 0xe2, 0x24, 0xb7,
	0x54, 0x1d, 0x87, 0x7e, 0xaa, 0xa1, 0xe1, 0x19, 0x74, 0xf5, 0x3a, 0xc4, 0xfa, 0xc1, 0xba, 0x75,
	0xaa, 0xd9, 0xa6, 0x83, 0xa7, 0xd0, 0x35, 0x42, 0x8a, 0x5c, 0x2c, 0x4a, 0xfb, 0x40, 0x6e, 0xaa,
	0x93, 0xba, 0xbd, 0xb2, 0x8d, 0xfc, 0x1e, 0xf9, 0x9f, 0xe7, 0xf2, 0x2f, 0x00, 0x00, 0xff, 0xff,
	0xc1, 0xec, 0xfc, 0xde, 0x4c, 0x02, 0x00, 0x00,
}
