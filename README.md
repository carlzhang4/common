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
      - [LEADING\_ZERO\_COUNTER](#leading_zero_counter)
      - [Reporter](#reporter)
      - [ToZero](#tozero)
      - [Floating Point](#floating-point)
    - [AXI Subpackage](#axi-subpackage)
      - [Connect\_AXI](#connect_axi)
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

#### Floating Point

Floating point-related modules handles conversion of different floating point formats. Up to now we only support conversion between int16 and bf16. More diverse format support is comming soon. 

The floating point modules converts data format in a fully-pipelined stream-based manner. Each cycle they accept a number vector and output a number vector. Current modules typically have 2 parameters, which is defined as below.

| Parameter    | Description |
|--------------|-------------|
| VEC_LEN      | To save resource, each module is able to convert a vector of multiple numbers in a cycle. VEC_LEN defines the length of the vector. For instance, a int16 to bf16 module of VEC_LEN 32 accept data stream of width 16*32=512. |
| SCALE_FACTOR | A floating-point number may multiply a factor and then floored into a integer. That's how scale factor works. In hardware, a scale factor should be a power of 2. Scale factor in this module represents the exponent. For example, if SCALE_FACTOR is set to 12, then the actual scale factor is 2^12=4096. |

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