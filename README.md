# common

General-purpose Chisel modules shared by the projects in this repository.

This folder mainly provides:

- bus/interface definitions used across the design,
- reusable pipeline, queue, RAM, and CDC helpers,
- arbitration/routing utilities for `DecoupledIO` streams,
- AXI / AXI-Stream helpers,
- debug wrappers such as ILA / VIO.

Most modules are written as small building blocks. In normal use, you instantiate them inside your own module and connect them with `<>`.

**Table of Contents**
- [common](#common)
  - [Source Layout](#source-layout)
  - [Buses and Interfaces](#buses-and-interfaces)
    - [AXI](#axi)
    - [AXI Stream](#axi-stream)
  - [Root Package](#root-package)
    - [BUF wrappers](#buf-wrappers)
    - [Delay](#delay)
    - [BaseILA](#baseila)
    - [BaseVIO](#basevio)
    - [LEADING\_ZERO\_COUNTER](#leading_zero_counter)
    - [ToZero / ToAllOnes / Init](#tozero--toallones--init)
    - [ResetSync](#resetsync)
    - [Floating Point](#floating-point)
    - [DebugBridge](#debugbridge)
    - [Collector](#collector)
    - [OffsetGenerator](#offsetgenerator)
    - [Header Processing](#header-processing)
    - [StreamShift](#streamshift)
    - [CommonPins](#commonpins)
    - [LatencyBucket](#latencybucket)
    - [AckCounter](#ackcounter)
    - [Math helpers](#math-helpers)
  - [AXI Subpackage](#axi-subpackage)
    - [AXIArbiter](#axiarbiter)
    - [AXIRouter](#axirouter)
    - [AXIClockWidthConverter](#axiclockwidthconverter)
    - [AXI Stream Processing](#axi-stream-processing)
  - [Connection Subpackage](#connection-subpackage)
    - [SimpleRouter](#simplerouter)
    - [SerialRouter](#serialrouter)
    - [XArbiter](#xarbiter)
    - [SerialArbiter](#serialarbiter)
    - [ProducerConsumer](#producerconsumer)
  - [Storage Subpackage](#storage-subpackage)
    - [RegSlice](#regslice)
    - [AXIRegSlice](#axiregslice)
    - [XConverter](#xconverter)
    - [XPacketQueue](#xpacketqueue)
    - [XQueue](#xqueue)
    - [XRam](#xram)

## Source Layout

- `common`: general helpers and debug utilities,
- `common.axi`: AXI / AXIS data structures and AXI-related modules,
- `common.connection`: routers and arbiters for `DecoupledIO`,
- `common.storage`: queues, register slices, RAMs, and clock-domain converters.

## Buses and Interfaces

### AXI

The `common.axi.AXI` bundle defines a full AXI memory-mapped interface with the five standard channels:

- `aw`: write address,
- `w`: write data,
- `b`: write response,
- `ar`: read address,
- `r`: read data.

Typical construction:

```scala
import common.axi._

val m_axi = new AXI(ADDR_WIDTH = 64, DATA_WIDTH = 512, ID_WIDTH = 4, USER_WIDTH = 1, LEN_WIDTH = 8)
```

Useful helpers on `AXI` and its channel bundles:

- `init()`: generic zero-initialization for a master-side AXI bundle.
- `hbm_init()`: initialize an AXI bundle for HBM-style ports.
- `noc_init()`: initialize fields for NoC-facing AXI ports.
- `qdma_init()`: initialize fields for the QDMA slave bridge.
- `mark_intf(name)`: attach Vivado interface attributes to the bundle.

**Usage notes**

1. Call an initialization helper first.
2. Assign only the fields you really drive afterwards.
3. For HBM or QDMA ports, prefer the matching specialized initializer.

Example:

```scala
io.m_axi.hbm_init()
io.m_axi.aw.valid := sendWrite
io.m_axi.aw.bits.addr := writeAddr
io.m_axi.aw.bits.len := burstLen
io.m_axi.w.valid := writeValid
io.m_axi.w.bits.data := writeData
io.m_axi.w.bits.strb := Fill(32, 1.U(1.W))
io.m_axi.w.bits.last := writeLast
io.m_axi.b.ready := true.B
```

### AXI Stream

The `common.axi.AXIS(width)` bundle is a lightweight AXI-Stream style payload with:

- `data`: stream payload,
- `keep`: byte enables,
- `last`: packet boundary.

It is normally wrapped inside `Decoupled(...)`.

Example:

```scala
import common.axi._

val in  = Flipped(Decoupled(new AXIS(512)))
val out = Decoupled(new AXIS(512))
```

Modules such as `SerialRouter`, `SerialArbiter`, and `XPacketQueue` depend on the `last` field to preserve packet boundaries.

## Root Package

### BUF wrappers

`Buf.scala` contains BlackBox wrappers for common Xilinx clock and IO primitives, such as:

- `IBUF`,
- `BUFG`,
- `IBUFDS`,
- `IBUFDS_GTE4`,
- `IBUFDS_GTME5`,
- `MMCME4_ADV_Wrapper`.

Use them when your top-level module needs to connect board pins or vendor clocking resources. For more details, please refer to Xilinx's documents and Vivado language templates.

Example:

```scala
val sysClkBuf = BUFG(io.sysClk)
val refClk = IBUFDS(io.refclk_p, io.refclk_n)
```

### Delay

`Delay` is a convenience wrapper around `RegSlice(stage)` for `DecoupledIO` streams.

**Usage**

```scala
import common.Delay

val delayed = Delay(io.in, 4)
io.out <> delayed
```

This inserts a 4-stage ready/valid pipeline while preserving back-pressure.

You can also instantiate the module form directly:

```scala
val delay = Delay(UInt(32.W), 2)
delay.io.in <> io.in
io.out <> delay.io.out
```

Use `Delay` when you want extra latency for timing closure but do not want to write repeated `RegSlice(...)` chains by hand.

### BaseILA

`BaseILA` helps instantiate a Xilinx ILA from Chisel so that internal signals can be probed in hardware.

**Usage**

```scala
class ila_top(seq: Seq[Data]) extends BaseILA(seq)

val ila = Module(new ila_top(Seq(
  io.in.fire,
  io.out.bits,
  state
)))
ila.connect(clock)
```

Rules:

- the derived class name must start with `ila`,
- pass all probe signals as a `Seq[Data]`,
- connect the ILA to a free-running clock,
- keep the probe list stable after elaboration.

The wrapper also emits metadata that is later consumed by the post-elaboration flow to generate Vivado helper files.

> [!IMPORTANT]
> When instantiating ILA module, please check
> * Whether the class name starts with `ila`.
> * Whether the module has `connect` call.

### BaseVIO

`BaseVIO` is the matching wrapper for a Xilinx VIO.

It exports hardware-controlled values back into Chisel signals, which is useful for debug switches, manual triggers, and runtime configuration knobs.

**Usage**

```scala
class vio_top(seq: Seq[Data]) extends BaseVIO(seq)

val vioEnable = Wire(Bool())
val vioValue  = Wire(UInt(32.W))

val vio = Module(new vio_top(Seq(vioEnable, vioValue)))
vio.connect(clock)
```

Rules are the same as for `BaseILA`, except the derived class name must start with `vio`.

### LEADING_ZERO_COUNTER

`LEADING_ZERO_COUNTER(WIDTH)` counts the number of leading zero bits in an unsigned input.

**Interface**

- input: `io.data`,
- output: `io.result`.

If the input is all-zero, the module sets the MSB of the result to indicate that special case.

Example:

```scala
val lzc = Module(new LEADING_ZERO_COUNTER(16))
lzc.io.data := someUInt
val leadingZeros = lzc.io.result
```

This block is mainly useful in normalization logic, format conversion, and simple math datapaths.

### ToZero / ToAllOnes / Init

These helpers reduce boilerplate when initializing buses and aggregates.

- `ToZero(x)`: drive any Chisel `Data` to zero.
- `ToAllOnes(x)`: drive any Chisel `Data` to all ones.
- `Init(x: DecoupledIO[T])`: initialize a decoupled interface according to its direction.

Examples:

```scala
ToZero(io.out.bits)
io.out.valid := false.B

ToAllOnes(mask)

Init(io.someDecoupled)
```

These are especially handy when building AXI / AXIS control logic with many default assignments.

### ResetSync

`ResetSync` retimes an asynchronous reset into a given clock domain.

**Usage**

```scala
val rstSync = ResetSync(clk = io.userClk, rstIn = io.extReset)
```

Or module form:

```scala
val rst = Module(new ResetSync(PIPE_LEN = 4))
rst.io.clk := io.userClk
rst.io.rstIn := io.extReset
val userReset = rst.io.rstOut
```

Use this when a reset is generated outside the local clock domain and must be safely synchronized before use.

> [!NOTE]
> This module still has timing bug and relies on `set_false_path` constraints. Please fix it if you are interested in solving this.

### Floating Point

`FloatingPoint.scala` currently contains two stream-oriented converters:

- `BFloat16ToFixed`,
- `FixedToBFloat16`.

Both operate on `Decoupled(UInt(...))` vectors and are fully pipelined.

| Parameter | Description |
|-----------|-------------|
| `VEC_LEN` | Number of 16-bit values processed per cycle. |
| `SCALE_FACTOR` | Exponent of a power-of-two scale factor used during conversion. |

Example:

```scala
val cvt = Module(new BFloat16ToFixed(VEC_LEN = 32, SCALE_FACTOR = 12))
cvt.io.in <> bf16Stream
fixedStream <> cvt.io.out
```

Practical notes:

- the input and output width is always `16 * VEC_LEN`,
- use these modules only for packed 16-bit lanes,
- the modules already contain internal pipeline stages, so they are suitable for high-throughput datapaths.

### DebugBridge

`DebugBridge` is a BlackBox wrapper for the Xilinx debug bridge IP. Generally for designs that enables partial reconfiguration. Please see [this repo](https://github.com/RC4ML/rc4ml_static) for more details about using partial reconfiguration for UltraScale+ projects with QDMA.

Important constraints from the implementation:

- instantiate it in the top module,
- the top module should be a `MultiIOModule`,
- no data-path wiring is needed beyond the generated BSCAN IO bundle.

Example:

```scala
val dbg = DebugBridge(clk = clock, IP_CORE_NAME = "DebugBridge")
dbg.getTCL()
```

`getTCL()` prints the Vivado `create_ip` commands for the corresponding debug bridge instance.

### Collector

`Collector` is a lightweight runtime instrumentation helper for exposing counters and trigger signals through status registers.

It is intended for board-level debug rather than simulation-only tracing.

Common patterns:

- `Collector.report(x, msg)`: export a 1-bit / 32-bit / 64-bit signal,
- `Collector.fire(stream, msg)`: count successful `fire()` events on a stream,
- `Collector.fireLast(stream, msg)`: count completed packets on `HasLast` streams,
- `Collector.trigger(cond, msg)`: latch a trigger once it becomes true,
- `Collector.count(cond, msg)`: accumulate cycles while a condition is true.

After registering signals, connect them to status registers:

```scala
// Call this in arbitrary modules.
Collector.report(statusReg, "status")
Collector.fire(io.out, "out_stream")
// Call this only in top module with QDMA/CIPS, and below all codes with io.status!
Collector.connect_to_status_reg(io.status, offset = 512)
```

> [!IMPORTANT]
> You must call `connect_to_status_reg` in top module, and below any code that connects status registers. 

### OffsetGenerator

`OffsetGenerator` produces a repeated address-offset pattern from four parameters:

- `num`: number of outer repetitions,
- `range`: distance between consecutive outer positions,
- `step`: inner stride,
- `en`: update enable.

Example:

```scala
val offset = OffsetGenerator(
  num = 4.U,
  range = 4096.U,
  step = 64.U,
  en = advance
)
```

This is useful for simple multi-buffer or tiled address generation patterns.

### Header Processing

`HeaderProcessing.scala` provides two stream helpers for packet formats that store a fixed-width header in the first bytes of an AXI-Stream payload.

- `AddHeader`: prepend a metadata header to a payload stream,
- `SplitHeader`: extract the header and forward the remaining payload.

Example:

```scala
val addHeader = AddHeader(UInt(128.W), new AXIS(512), header_width = 16)
addHeader.io.inMeta <> metaIn
addHeader.io.inData <> dataIn
dataOut <> addHeader.io.outData
```

Use the matching `header_width` in bytes on both insertion and extraction paths.

### StreamShift

`StreamShift.scala` contains low-level `LSHIFT` and `RSHIFT` modules for byte-wise shifting of AXI-Stream packets.

These blocks are used internally by `AddHeader`, `SplitHeader`, and the higher-level AXI stream shifters.

In most designs, prefer the wrappers in `AXISProcessing.scala`:

- `AXIStreamLShift`,
- `AXIStreamRShift`.

Use `LSHIFT` / `RSHIFT` directly only when you explicitly need byte-granular packet realignment inside `common`-style AXIS datapaths.

### CommonPins

`CommonPins.scala` provides reusable top-level board pin bundles. We define CommonPins in common library rather than their own library due to the need of partial reconfiguration workflow.

Currently included bundles:

- `CMACPin`: GT reference clock and transceiver lanes for CMAC-like ports,
- `DDRPin`: grouped DDR external pins.

These bundles are mainly for top-level IO declarations, for example:

```scala
val io = IO(new Bundle {
  val cmac = new CMACPin
})
```

### LatencyBucket

`LatencyBucket` measures operation latency and accumulates the results into histogram buckets.

The module tracks `start` / `end` events, computes the latency in cycles, and increments the corresponding bucket.

Typical use cases include memory latency measurement and end-to-end pipeline profiling.

Example:

```scala
val bucket = Module(new LatencyBucket(
  BUCKET_SIZE = 64,
  LATENCY_STRIDE = 16,
  MAX_INFLIGHT = 256
))
bucket.io.enable := profileEnable
bucket.io.start := req.fire
bucket.io.end := resp.fire
bucket.io.bucketRdId := readBucketId
```

Notes:

- `LATENCY_STRIDE` must be a power of two,
- reading `bucketValue` has pipeline latency,
- use `resetBucket` when you want to clear accumulated histogram state.

### AckCounter

`AckCounter` helps with ordered acknowledgement tracking for out-of-order packet arrival.

It accepts packet sequence numbers through `io.in` and advances `io.ack` once all previous sequence IDs have been observed.

Example:

```scala
val ackCounter = Module(new AckCounter(BUFFER_SIZE = 256))
ackCounter.io.in.valid := pktDone
ackCounter.io.in.bits := seqId
val nextExpected = ackCounter.io.ack
```

This is particularly useful in network or transport-style reorder logic.

### Math helpers

`Math.scala` contains a few small integer helpers used across the repository:

- `Math.round_up(x, m)`,
- `Math.pow2(x)`,
- `Math.log2(x)`.

These are convenience functions for elaboration-time calculations, such as FIFO widths, aligned sizes, and memory geometry.

## AXI Subpackage

### AXIArbiter

`AXIArbiter(n, shape)` merges `n` AXI initiators into a single AXI target-facing port.

It uses composite arbitration for the write address/data channels, plus response FIFOs to route `b` and `r` channels back to the original requester.

Example:

```scala
val arb = AXIArbiter(4, io.in(0))
for (i <- 0 until 4) {
  arb.io.in(i) <> masters(i)
}
target <> arb.io.out
```

Use it when multiple internal masters share one downstream memory or MMIO interface.

### AXIRouter

`AXIRouter(n, shape)` routes one AXI initiator to one of `n` downstream AXI targets.

Routing decisions are controlled by:

- `io.wrIdx` for AW/W/B traffic,
- `io.rdIdx` for AR/R traffic.

Example:

```scala
val rt = AXIRouter(4, io.in)
rt.io.in <> host
rt.io.wrIdx := writeSel
rt.io.rdIdx := readSel
for (i <- 0 until 4) {
  slaves(i) <> rt.io.out(i)
}
```

Use it when address decoding or system-level routing is done outside the AXI bundle itself.

### AXIClockWidthConverter

`AXIClockWidthConverter` connects a master AXI port and a slave AXI port that differ in both clock domain and data width.

The helper performs:

- clock-domain crossing with `XConverter`,
- write/read data width conversion,
- automatic `len` and `size` adaptation.

Usage is side-effect based:

```scala
AXIClockWidthConverter(
  m_gen = io.mAxi,
  m_clk = mClk,
  m_rstn = mRstn,
  s_gen = io.sAxi,
  s_clk = sClk,
  s_rstn = sRstn
)
```

This is the preferred helper when width conversion and CDC are both required.

### AXI Stream Processing

`AXISProcessing.scala` contains several reusable AXI-Stream datapath helpers:

- `AXIStreamLShift`: insert a byte offset at the front of a packet,
- `AXIStreamRShift`: remove a byte offset from the front of a packet,
- `AXIStreamConcat`: concatenate two AXIS packet streams,
- `AXIStreamWidthConversion`: convert AXIS width with better timing behavior,
- `AXIStreamWidthConversionAlter`: older direct version with worse timing.

Examples:

```scala
val shifted = AXIStreamLShift(io.in, offset = 16)
io.out <> shifted
```

```scala
val cvt = Module(new AXIStreamWidthConversion(IN_WIDTH = 256, OUT_WIDTH = 512))
cvt.io.in <> in256
out512 <> cvt.io.out
```

For new designs, prefer `AXIStreamWidthConversion` over `AXIStreamWidthConversionAlter`.

## Connection Subpackage

These modules operate on `DecoupledIO` streams and help build multi-source / multi-destination datapaths.

### SimpleRouter

`SimpleRouter(gen, n)` routes each incoming beat to one of `n` outputs according to `io.idx`.

**Usage**

```scala
val router = SimpleRouter(UInt(32.W), 4)
router.io.in <> io.in
router.io.idx := targetPort
io.outs <> router.io.out
```

Use it when every beat is independent and may be routed separately.

### SerialRouter

`SerialRouter(genWithLast, n)` routes a packet stream while keeping all beats of the same packet on the same output.

The selected output is sampled on the first beat and held until a beat with `last := 1.U` is transferred.

**Usage**

```scala
val router = SerialRouter(new AXIS(512), 4)
router.io.in <> io.packetIn
router.io.idx := packetDest
```

Use `SerialRouter` for packetized streams such as AXI-Stream frames.

### XArbiter

`XArbiter(gen, n)` arbitrates `n` inputs into one output.

**Usage**

```scala
val arb = XArbiter(UInt(64.W), 4)
for (i <- 0 until 4) {
  arb.io.in(i) <> inputs(i)
}
io.out <> arb.io.out
```

If you pass `exportIdx = true`, the arbiter also reports which input won:

```scala
val arb = XArbiter(UInt(64.W), 4, exportIdx = true)
```

This is useful when the data path needs a sideband owner ID.

The file also contains a tree-style helper overload for building larger arbiters from a sequence of fan-ins.

### SerialArbiter

`SerialArbiter` is the packet-preserving version of `XArbiter`.

It chooses a source on the first beat and keeps granting that source until `last` is seen.

Use it when arbitrating framed AXI-Stream traffic or any `HasLast` payload.

The same source file also contains:

- `CompositeArbiter`: for paired metadata + payload streams,
- `XArbiterWithInputId`: when the winning input is provided by an external ID stream.

### ProducerConsumer

`ProducerConsumer(gen, n)` distributes one input stream across `n` consumers in a round-robin style driven by downstream readiness.

Example:

```scala
val pc = ProducerConsumer(UInt(32.W), 4)
pc.io.in <> in
for (i <- 0 until 4) {
  outs(i) <> pc.io.out(i)
}
```

This is useful for work distribution when any ready consumer may accept the next item.

## Storage Subpackage

### RegSlice

`RegSlice` inserts a one-stage ready/valid pipeline on a `DecoupledIO` stream.

**Usage**

```scala
downStream <> RegSlice(upStream)
```

For multiple stages:

```scala
downStream <> RegSlice(3)(upStream)
```

Use it to break long timing paths while preserving handshake semantics.

> [!NOTE]
> When using RegSlice, downstream port must be left-handed and upstream port must be right-handed.

### AXIRegSlice

`AXIRegSlice` applies `RegSlice` to every AXI channel.

**Usage**

```scala
io.mem <> AXIRegSlice(io.host)
```

or with multiple stages:

```scala
io.mem <> AXIRegSlice(2)(io.host)
```

Both ends must have the same AXI parameterization.

> [!WARNING]
> This module seems to have bugs that prevent elaborating.

### XConverter

`XConverter` is a clock-domain crossing FIFO for generic `DecoupledIO` payloads.

**Usage**

```scala
val cdc = XConverter(UInt(128.W), inClk, inRstn, outClk)
cdc.io.in <> inStream
outStream <> cdc.io.out
```

There is also an overload that directly converts a stream:

```scala
val outStream = XConverter(inStream, inClk, inRstn, outClk)
```

For AXI buses, use `XAXIConverter` to insert CDC FIFOs on all channels.

### XPacketQueue

`XPacketQueue(width, entries)` is an AXI-Stream FIFO that preserves packet boundaries through `tlast`.

**Usage**

```scala
val q = XPacketQueue(width = 512, entries = 64)
q.io.in <> inAxis
outAxis <> q.io.out
```

Extra outputs:

- `io.count`: current occupancy,
- `io.almostfull`: back-pressure hint based on `almostfull_threshold`.

Use this when buffering packetized AXIS traffic.

### XQueue

`XQueue(gen, entries)` is the standard FIFO helper for arbitrary `DecoupledIO` payloads.

**Usage**

```scala
val q = XQueue(UInt(64.W), entries = 16)
q.io.in <> producer
consumer <> q.io.out
```

Options:

- `almostfull_threshold`: controls when `io.almostfull` is asserted,
- `packet_fifo`: forwarded to the vendor FIFO wrapper for packet-style behavior.

Implementation detail:

- for small depths (`entries <= 32`), it uses Chisel `Queue`,
- for larger depths, it switches to the vendor FIFO wrapper.

This makes `XQueue` the default buffering primitive for most streams in this codebase.

### XRam

`XRam` is a dual-port RAM wrapper based on Xilinx XPM memory.

**Constructor**

```scala
val ram = XRam(UInt(512.W), entries = 1024, memory_type = "block", latency = 2)
```

**Main ports**

- `addr_a`, `wr_en_a`, `data_in_a`, `data_out_a`: port A, usually read/write,
- `addr_b`, `data_out_b`: port B, usually read-only.

Example:

```scala
val ram = XRam(UInt(32.W), entries = 1024, memory_type = "auto", latency = 1)
ram.io.addr_a := wrAddr
ram.io.wr_en_a := wrEn
ram.io.data_in_a := wrData
ram.io.addr_b := rdAddr
val rdData = ram.io.data_out_b
```

Useful parameters:

- `memory_type`: `"auto"`, `"block"`, or `"distributed"`,
- `latency`: read latency of the XPM wrapper,
- `use_musk`: enable byte write masking,
- `initFile`: optional XPM init file.

Use `XRam` when you want a synthesis-friendly RAM wrapper without writing vendor-specific boilerplate in every design.