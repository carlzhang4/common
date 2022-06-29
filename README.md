# common
General modules in Chisel

**Table of Contents**  
- [common](#common)
  - [Buses And Interfaces](#buses-and-interfaces)
    - [AXI Interface Descripton](#axi-interface-descripton)
    - [AXI Stream Interface Descripton](#axi-stream-interface-descripton)
  - [Modules Description](#modules-description)
    - [Root Package](#root-package)
      - [BUF Wrappers](#buf-wrappers)
      - [Delay](#delay)
      - [BaseILA](#baseila)
      - [BaseVIO](#basevio)
      - [LEADING_ZERO_COUNTER](#leading_zero_counter)
      - [Reporter](#reporter)
      - [ToZero](#tozero)
    - [AXI Subpackage](#axi-subpackage)
      - [Connect_AXI](#connect_axi)
      - [AXI2Reg](#axi2reg)
      - [PoorAXIL2Reg](#pooraxil2reg)
    - [Connection Subpackage](#connection-subpackage)
      - [SimpleRouter](#simplerouter)
      - [SerialRouter](#serialrouter)
      - [Switch](#switch)
      - [XArbiter](#xarbiter)
      - [SerialArbiter](#serialarbiter)
    - [Storage Subpackage](#storage-subpackage)
      - [RegSlice](#regslice)
      - [AXIRegSlice](#axiregslice)
      - [XConverter](#xconverter)
      - [XPacketQueue](#xpacketqueue)
      - [XQueue](#xqueue)
      - [XRam](#xram)
  - [Elaboration and Benchmarking](#elaboration-and-benchmarking)

## Buses And Interfaces

### AXI Interface Descripton

### AXI Stream Interface Descripton

## Modules Description

### Root Package

#### BUF Wrappers

#### Delay

#### BaseILA

`BaseILA` helps to instantiate a Xilinx ILA in Chisel, thus to monitor signals and buses.

**Usage**

Example code: 
```scala
class ila_name(seq:Seq[Data]) extends BaseILA(seq)
val inst = Module(new ila_name(Seq(	
    input1,
    input2,
    ...
)))
inst.connect(clock)
```
Before using ILA, a derived class starts with `ila` should be defined. Signals to be monitored should be wrapped in `Seq` and passed as argument. Each input can either be signal or bus. Inputs of different types can be mixed. 
An ILA should connect to a free running clock.
About tcl generation, see [here](#elaboration-and-benchmarking).


#### BaseVIO

#### LEADING_ZERO_COUNTER

#### Reporter

#### ToZero

### AXI Subpackage

#### Connect_AXI

#### AXI2Reg

#### PoorAXIL2Reg

### Connection Subpackage

#### SimpleRouter

#### SerialRouter

#### Switch

#### XArbiter

#### SerialArbiter

### Storage Subpackage

#### RegSlice

`RegSlice` module can helps to insert an pipeline stage on a `DecoupledIO` bus. It can be used to improve timing on long routes, while introducing latency.  

**Usage**

`downStream <> RegSlice(upStream)`

Insert an 1-stage pipeline between an upstream (who sends data) interface and a downstream (who receives data) interface. Both `upStream` and `downStream` should derive from `DecoupledIO` class and have the same type. 

`downStream <> RegSlice(stage)(upStream)`

Insert a multiple-stage pipeline between upstream and downstream. `Stage` is a scala `Int` which indicates the latency. Multiple-stage pipelines are often used in cross SLR routes or some long routes.

#### AXIRegSlice

`AXIRegSlice` module can helps to insert an pipeline stage on an `AXI` bus. It can be used to improve timing on long routes, while introducing latency. This module implements `RegSlice` on each AXI channel.

**Usage**

`downStream <> AXIRegSlice(upStream)`

Insert an 1-stage pipeline between an upstream (who sends AW/AR requests) interface and a downstream (who receives AW/AR requests) interface. Both `upStream` and `downStream` should be `AXI` and have exact same arguments. 

`downStream <> AXIRegSlice(stage)(upStream)`

Insert a multiple-stage pipeline between upstream and downstream. `Stage` is a scala `Int` which indicates the latency. Multiple-stage pipelines are often used in cross SLR routes or some long routes.

#### XConverter

#### XPacketQueue

#### XQueue

#### XRam

## Elaboration and Benchmarking