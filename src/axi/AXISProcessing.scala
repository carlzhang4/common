package common.axi

import chisel3._
import chisel3.util._
import common.storage._
import common._

object AXIStreamLShift {
    def apply(in: DecoupledIO[AXIS], offset: Int) = {
        val width   = in.bits.data.getWidth
        val shifter = Module(new AXIStreamLShift(offset, width))
        shifter.io.in <> in
        shifter.io.out
    }
}

class AXIStreamLShift (
    OFFSET  : Int,  // In bytes.
    WIDTH   : Int,  // In bits.
) extends Module {
    val io = IO(new Bundle{
        val in	= Flipped(Decoupled(new AXIS(WIDTH)))
        val out	= (Decoupled(new AXIS(WIDTH)))
    })

    val in  = RegSlice(io.in)

    val BEAT_TO_ADD = math.floor(OFFSET * 8 / WIDTH).toInt
    val BYTE_TO_ADD = OFFSET % (WIDTH / 8)

    val immediate   = Wire(Decoupled(new AXIS(WIDTH)))

    if (BEAT_TO_ADD == 0) {
        immediate.bits  := in.bits
        immediate.valid := in.valid
        in.ready        := immediate.ready
    } else {
        val cnt = RegInit(0.U(log2Up(BEAT_TO_ADD+1).W))
        when (in.fire) {
            when (in.bits.last.asBool) {
                cnt := 0.U
            }.elsewhen (cnt < BEAT_TO_ADD.U) {
                cnt := cnt + 1.U
            }
        }
        immediate.bits.data := Mux(cnt === BEAT_TO_ADD.U, in.bits.data, 0.U.asTypeOf(in.bits.data))
        immediate.bits.keep := Mux(cnt === BEAT_TO_ADD.U, in.bits.keep, -1.S((WIDTH/8).W).asUInt)
        immediate.bits.last := Mux(cnt === BEAT_TO_ADD.U, in.bits.last, 0.U)
        immediate.valid     := in.valid
        in.ready            := immediate.ready && (cnt === BEAT_TO_ADD.U)
    }

    if (BYTE_TO_ADD == 0) {
        io.out <> RegSlice(immediate)
    } else {
        val shifter = Module(new LSHIFT(BYTE_TO_ADD, WIDTH))

        shifter.io.in   <> RegSlice(immediate)
        io.out          <> RegSlice(shifter.io.out)
    }
}

object AXIStreamRShift {
    def apply(in: DecoupledIO[AXIS], offset: Int) = {
        val width   = in.bits.data.getWidth
        val shifter = Module(new AXIStreamRShift(offset, width))
        shifter.io.in <> in
        shifter.io.out
    }
}


class AXIStreamRShift (
    OFFSET  : Int,  // In bytes.
    WIDTH   : Int,  // In bits.
) extends Module {
	val io = IO(new Bundle{
		val in	= Flipped(Decoupled(new AXIS(WIDTH)))
		val out	= (Decoupled(new AXIS(WIDTH)))
	})

    val in  = RegSlice(io.in)

    val BEAT_TO_REMOVE  = math.floor(OFFSET * 8 / WIDTH).toInt
    val BYTE_TO_REMOVE  = OFFSET % (WIDTH / 8)

    val immediate   = Wire(Decoupled(new AXIS(WIDTH)))

    if (BEAT_TO_REMOVE == 0) {
        immediate.bits  := in.bits
        immediate.valid := in.valid
        in.ready        := immediate.ready
    } else {
        val cnt = RegInit(0.U(log2Up(BEAT_TO_REMOVE+1).W))
        when (in.fire) {
            when (in.bits.last.asBool) {
                cnt := 0.U
            }.elsewhen (cnt < BEAT_TO_REMOVE.U) {
                cnt := cnt + 1.U
            }
        }
        immediate.bits  := in.bits
        immediate.valid := in.valid && (cnt === BEAT_TO_REMOVE.U)
        in.ready        := immediate.ready
    }

    if (BYTE_TO_REMOVE == 0) {
        io.out <> RegSlice(immediate)
    } else {
        val shifter = Module(new RSHIFT(BYTE_TO_REMOVE, WIDTH))

        shifter.io.in   <> RegSlice(immediate)
        io.out          <> RegSlice(shifter.io.out)
    }
}

class AXIStreamConcat (
    WIDTH   : Int
) extends Module {
    val io = IO(new Bundle{
        val inLow   = Flipped(Decoupled(new AXIS(WIDTH)))
        val inHigh  = Flipped(Decoupled(new AXIS(WIDTH)))
        val out     = Decoupled(new AXIS(WIDTH))
    })
    

    class AXISWithLen(val DATA_WIDTH : Int) extends  Bundle with HasLast {
        val data = Output(UInt(DATA_WIDTH.W))
        val len  = Output(UInt((log2Down(DATA_WIDTH)+1).W))
    }

    val inLowRaw    = RegSlice(io.inLow)
    val inHighRaw   = RegSlice(io.inHigh)

    val inHighRegSlice  = Module(new RegSlice(new AXISWithLen(WIDTH)))
    val inLowRegSlice   = Module(new RegSlice(new AXISWithLen(WIDTH)))

    inHighRegSlice.io.upStream.valid    := inHighRaw.valid
    inHighRegSlice.io.upStream.bits.data:= inHighRaw.bits.data
    inHighRegSlice.io.upStream.bits.last:= inHighRaw.bits.last
    inHighRegSlice.io.upStream.bits.len := parseKeepSignal(inHighRaw.bits.keep, WIDTH)
    inHighRaw.ready := inHighRegSlice.io.upStream.ready

    inLowRegSlice.io.upStream.valid     := inLowRaw.valid
    inLowRegSlice.io.upStream.bits.data := inLowRaw.bits.data
    inLowRegSlice.io.upStream.bits.last := inLowRaw.bits.last
    inLowRegSlice.io.upStream.bits.len  := parseKeepSignal(inLowRaw.bits.keep, WIDTH)
    inLowRaw.ready := inLowRegSlice.io.upStream.ready

    val inLow   = inLowRegSlice.io.downStream
    val inHigh  = inHighRegSlice.io.downStream

    val sLow :: sHigh :: sLast :: Nil = Enum(3)
    val state   = RegInit(sLow)

    val lowLen  = Wire(UInt((log2Down(WIDTH)+1).W))
    val highLen = Wire(UInt((log2Down(WIDTH)+1).W))
    val offset  = RegInit(0.U(log2Up(WIDTH).W))
    val tmpReg  = RegInit(0.U(WIDTH.W))

    lowLen  := Mux(inLow.valid, inLow.bits.len, 0.U)
    highLen := Mux(inHigh.valid, inHigh.bits.len, 0.U)

    val sumLen  = Wire(UInt((log2Down(WIDTH)+2).W))
    sumLen  := Cat(0.U(1.W), lowLen) + highLen

    when (state === sLow && ~inLow.bits.last.asBool) {
        // Case 1: Low data is not last.
        inLow.ready         := io.out.ready
        inHigh.ready        := false.B
        io.out.valid        := inLow.valid
        io.out.bits.data    := inLow.bits.data
        io.out.bits.keep    := -1.S((WIDTH/8).W).asUInt
        io.out.bits.last    := 0.U
    }.elsewhen (state === sLow/*  && inLow.bits.last.asBool*/ && (sumLen <= WIDTH.U)) {
        // Case 2: Low data and high data ends at the same beat.
        inLow.ready         := io.out.ready && inHigh.valid
        inHigh.ready        := io.out.ready && inLow.valid
        io.out.valid        := inLow.valid && inHigh.valid
        io.out.bits.data    := inHigh.bits.data
        for (i <- 1 until WIDTH) {
            when (lowLen === i.U) {
                io.out.bits.data    := Cat(inHigh.bits.data, inLow.bits.data(i-1, 0))
            }
        }
        io.out.bits.keep    := genKeepSignal(sumLen, WIDTH)
        io.out.bits.last    := 1.U
    }.elsewhen (state === sLow/*  && inLow.bits.last.asBool*/ && (lowLen === WIDTH.U)) {
        // Case 3: Last beat of low data and occupies full output space.
        inLow.ready         := io.out.ready
        inHigh.ready        := false.B
        io.out.valid        := inLow.valid
        io.out.bits.data    := inLow.bits.data
        io.out.bits.keep    := -1.S((WIDTH/8).W).asUInt
        io.out.bits.last    := 0.U
        tmpReg  := 0.U
        offset  := 0.U
        when (io.out.fire) {
            state   := sHigh
        }
    }.elsewhen (state === sLow/* && inLow.bits.last.asBool && (lowLen =/= WIDTH.U)*/) {
        // Case 4: Last beat of low data as well as first beat of high data.
        inLow.ready         := io.out.ready && inHigh.valid
        inHigh.ready        := io.out.ready && inLow.valid
        io.out.valid        := inLow.valid && inHigh.valid
        io.out.bits.data    := inHigh.bits.data
        for (i <- 1 until WIDTH) {
            when (lowLen === i.U) {
                io.out.bits.data    := Cat(inHigh.bits.data, inLow.bits.data(i-1, 0))
                tmpReg  := inHigh.bits.data(WIDTH-1, WIDTH-i)
            }
        }
        io.out.bits.keep    := -1.S((WIDTH/8).W).asUInt
        io.out.bits.last    := 0.U
        offset  := lowLen
        when (io.out.fire) {
            state   := sHigh
        }
    }.elsewhen (state === sHigh && ~inHigh.bits.last.asBool) {
        // Case 5: High data is not last.
        inLow.ready         := false.B
        inHigh.ready        := io.out.ready
        io.out.valid        := inHigh.valid
        io.out.bits.data    := inHigh.bits.data
        for (i <- 1 until WIDTH) {
            when (offset === i.U) {
                io.out.bits.data    := Cat(inHigh.bits.data, tmpReg(i-1, 0))
                tmpReg  := inHigh.bits.data(WIDTH-1, WIDTH-i)
            }
        }
        io.out.bits.keep    := -1.S((WIDTH/8).W).asUInt
        io.out.bits.last    := 0.U
    }.elsewhen (state === sHigh/* && inHigh.bits.last.asBool*/ && (offset + highLen > WIDTH.U)) {
        // Case 6: Last beat of high data and exceeds full output space, i.e., still a last beat remains.
        inLow.ready         := false.B
        inHigh.ready        := io.out.ready
        io.out.valid        := inHigh.valid
        io.out.bits.data    := inHigh.bits.data
        for (i <- 1 until WIDTH) {
            when (offset === i.U) {
                io.out.bits.data    := Cat(inHigh.bits.data, tmpReg(i-1, 0))
                tmpReg  := inHigh.bits.data(WIDTH-1, WIDTH-i)
            }
        }
        io.out.bits.keep    := -1.S((WIDTH/8).W).asUInt
        io.out.bits.last    := 0.U
        offset  := Cat(0.U(1.W), offset) + highLen - WIDTH.U
        when (io.out.fire) {
            state   := sLast
        }
    }.elsewhen (state === sHigh /*&& inHigh.bits.last.asBool && (offset + highLen <= WIDTH.U)*/) {
        // Case 7: Last beat of high data and fits in the output space.
        inLow.ready         := false.B
        inHigh.ready        := io.out.ready
        io.out.valid        := inHigh.valid
        io.out.bits.data    := inHigh.bits.data
        for (i <- 1 until WIDTH) {
            when (offset === i.U) {
                io.out.bits.data    := Cat(inHigh.bits.data, tmpReg(i-1, 0))
            }
        }
        io.out.bits.keep    := genKeepSignal(offset + highLen, WIDTH)
        io.out.bits.last    := 1.U
        when (io.out.fire) {
            state   := sLow
        }
    }.otherwise { /*state === sLast*/
        // Case 8: Last beat of data.
        inLow.ready         := false.B
        inHigh.ready        := false.B
        io.out.valid        := true.B
        io.out.bits.data    := tmpReg
        io.out.bits.keep    := genKeepSignal(offset, WIDTH)
        io.out.bits.last    := 1.U
        when (io.out.fire) {
            state   := sLow
        }
    }
}

class AXIWidthConversion (
    IN_WIDTH    : Int,
    OUT_WIDTH   : Int
) extends Module {
    val io = IO(new Bundle{
        val in     = Flipped(Decoupled(AXIS(IN_WIDTH)))
        val out    = Decoupled(AXIS(OUT_WIDTH))
    })

    val in      = RegSlice(io.in)

    val core    = Module(new WidthConversion(IN_WIDTH = IN_WIDTH, OUT_WIDTH = OUT_WIDTH))

    core.io.in.bits.counter := parseKeepSignal(in.bits.keep, 512)
    core.io.in.bits.data    := in.bits.data
    core.io.in.bits.last    := in.bits.last
    core.io.in.valid        := in.valid
    in.ready                := core.io.in.ready

    io.out.bits.keep        := genKeepSignal(core.io.out.bits.counter, 512)
    io.out.bits.data        := core.io.out.bits.data
    io.out.bits.last        := core.io.out.bits.last
    io.out.valid            := core.io.out.valid
    core.io.out.ready       := io.out.ready
}


        
// Auxiliary private interface for processing. 
class DataStreamWithCounterLast (
    DATA_WIDTH:     Int = 512,
    COUNTER_WIDTH:  Int = 16
) extends HasLast {
    val data = UInt(DATA_WIDTH.W)
    val counter = UInt(COUNTER_WIDTH.W)
}

// Width conversion core. Do not directly use!
class WidthConversion (
    IN_WIDTH    : Int,
    OUT_WIDTH   : Int
) extends Module {

    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new DataStreamWithCounterLast(IN_WIDTH, log2Down(IN_WIDTH) + 1)))
        val out = Decoupled(new DataStreamWithCounterLast(OUT_WIDTH, log2Down(OUT_WIDTH) + 1))
    })
    
    val in  = RegSlice(io.in)

    val tmpReg1 = VivadoMarkDontTouch(RegInit(0.U(OUT_WIDTH.W)))
    val tmpReg2 = VivadoMarkDontTouch(RegInit(0.U(OUT_WIDTH.W))) // Duplicate signal for better timing!
    val inBase1 = VivadoMarkDontTouch(RegInit(0.U(log2Up(IN_WIDTH).W)))
    val inBase2 = VivadoMarkDontTouch(RegInit(0.U(log2Up(IN_WIDTH).W)))
    val inBase3 = VivadoMarkDontTouch(RegInit(0.U(log2Up(IN_WIDTH).W)))
    val outBase1= VivadoMarkDontTouch(RegInit(0.U(log2Up(OUT_WIDTH).W)))
    val outBase2= VivadoMarkDontTouch(RegInit(0.U(log2Up(OUT_WIDTH).W)))
    val outBase3= VivadoMarkDontTouch(RegInit(0.U(log2Up(OUT_WIDTH).W)))


    // tmp1 : Next outBase
    val tmp1Width   = math.max(log2Up(IN_WIDTH), log2Up(OUT_WIDTH)) + 3
    val tmp1    = Wire(UInt(tmp1Width.W))
    tmp1    := Cat(0.U(3.W), outBase1) + in.bits.counter - inBase1

    // tmp2 : Next Cat(tmpReg, io.out.bits.data) 
    val tmp2    = Wire(UInt((2*OUT_WIDTH).W))
    tmp2    := Cat(0.U((2*OUT_WIDTH).W), in.bits.data) >> inBase2
    for (i <- 1 until OUT_WIDTH) {
        when (outBase2 === i.U) {
            tmp2 := Cat(in.bits.data, tmpReg1(i-1, 0))
        }
    }

    // tmp3 : Next inBase
    val tmp3    = Wire(UInt(tmp1Width.W))
    tmp3    := Cat(0.U(3.W), inBase3) + OUT_WIDTH.U(tmp1Width.W) - outBase3

    // tmp4 : For better timing, same as tmp2.
    val tmp4    = Wire(UInt((2*OUT_WIDTH).W))
    tmp4    := Cat(0.U((2*OUT_WIDTH).W), in.bits.data) >> inBase2
    for (i <- 1 until OUT_WIDTH) {
        when (outBase2 === i.U) {
            tmp4 := Cat(in.bits.data, tmpReg2(i-1, 0))
        }
    }
    
    io.out.bits.data    := tmp2(OUT_WIDTH-1, 0)

    // Another awful case analysis. 

    // 1. Adjust ready and valid signals, and io.out.bits.keep
    when (in.bits.last.asBool && tmp1 <= OUT_WIDTH.U(tmp1Width.W)) {
        // Case 1: Last beat of input, output is not full.
        io.out.valid    := in.valid
        in.ready        := io.out.ready
        io.out.bits.counter := tmp1
        io.out.bits.last    := 1.U
    }.elsewhen (!in.bits.last.asBool && tmp1 < OUT_WIDTH.U(tmp1Width.W)) {
        // Case 2: Not last beat of input, output is not full.
        io.out.valid    := 0.U
        in.ready     := io.out.ready
        io.out.bits.counter := 0.U(64.W)
        io.out.bits.last    := 0.U
    }.elsewhen (!in.bits.last.asBool && tmp1 === OUT_WIDTH.U(tmp1Width.W)) {
        // Case 3: Not last beat of input, output is just full.
        io.out.valid    := in.valid
        in.ready        := io.out.ready
        io.out.bits.counter := OUT_WIDTH.U
        io.out.bits.last    := 0.U
    }.elsewhen (!in.bits.last.asBool && tmp1 > OUT_WIDTH.U(tmp1Width.W) && tmp1 < (2*OUT_WIDTH).U(tmp1Width.W)) {
        // Case 4: Not last beat of input, data exceeds output but does not exceed register.
        io.out.valid    := in.valid
        in.ready        := io.out.ready
        io.out.bits.counter := OUT_WIDTH.U
        io.out.bits.last    := 0.U
    }.elsewhen (in.bits.last.asBool && tmp1 > OUT_WIDTH.U(tmp1Width.W) && tmp1 < (2*OUT_WIDTH).U(tmp1Width.W)) {
        // Case 5: Last beat of input, data exceeds output but does not exceed register.
        io.out.valid    := in.valid
        in.ready        := 0.U
        io.out.bits.counter := OUT_WIDTH.U
        io.out.bits.last    := 0.U
    }.otherwise {
        // Case 6: Data exceeds both output and register.
        io.out.valid    := in.valid
        in.ready        := 0.U
        io.out.bits.counter := OUT_WIDTH.U
        io.out.bits.last    := 0.U
    }

    // 2. Adjust the data registers.
    when (io.out.ready && in.valid) {
        when (in.bits.last.asBool && tmp1 <= OUT_WIDTH.U(tmp1Width.W)) {
            // Case 1: Last beat of input, output is not full.
            tmpReg1 := 0.U
            tmpReg2 := 0.U
            inBase1 := 0.U
            inBase2 := 0.U
            inBase3 := 0.U
            outBase1:= 0.U
            outBase2:= 0.U
            outBase3:= 0.U
        }.elsewhen (!in.bits.last.asBool && tmp1 < OUT_WIDTH.U(tmp1Width.W)) {
            // Case 2: Not last beat of input, output is not full.
            tmpReg1 := io.out.bits.data
            tmpReg2 := io.out.bits.data
            inBase1 := 0.U
            inBase2 := 0.U
            inBase3 := 0.U
            outBase1:= tmp1
            outBase2:= tmp1
            outBase3:= tmp1
        }.elsewhen (!in.bits.last.asBool && tmp1 === OUT_WIDTH.U(tmp1Width.W)) {
            // Case 3: Not last beat of input, output is just full.
            tmpReg1 := 0.U
            tmpReg2 := 0.U
            inBase1 := 0.U
            inBase2 := 0.U
            inBase3 := 0.U
            outBase1:= 0.U
            outBase2:= 0.U
            outBase3:= 0.U
        }.elsewhen (!in.bits.last.asBool && tmp1 > OUT_WIDTH.U(tmp1Width.W) && tmp1 < (2*OUT_WIDTH).U(tmp1Width.W)) {
            // Case 4: Not last beat of input, data exceeds output but does not exceed register.
            tmpReg1 := tmp4(2*OUT_WIDTH-1, OUT_WIDTH)
            tmpReg2 := tmp4(2*OUT_WIDTH-1, OUT_WIDTH)
            inBase1 := 0.U
            inBase2 := 0.U
            inBase3 := 0.U
            outBase1:= (tmp1 - OUT_WIDTH.U(tmp1Width.W))
            outBase2:= (tmp1 - OUT_WIDTH.U(tmp1Width.W))
            outBase3:= (tmp1 - OUT_WIDTH.U(tmp1Width.W))
        }.elsewhen (in.bits.last.asBool && tmp1 > OUT_WIDTH.U(tmp1Width.W) && tmp1 < (2*OUT_WIDTH).U(tmp1Width.W)) {
            // Case 5: Last beat of input, data exceeds output but does not exceed register.
            tmpReg1 := 0.U
            tmpReg2 := 0.U
            inBase1 := tmp3
            inBase2 := tmp3
            inBase3 := tmp3
            outBase1:= 0.U
            outBase2:= 0.U
            outBase3:= 0.U
        }.otherwise {
            // Case 6: Data exceeds both output and register.
            tmpReg1 := 0.U
            tmpReg2 := 0.U
            inBase1 := tmp3
            inBase2 := tmp3
            inBase3 := tmp3
            outBase1:= 0.U
            outBase2:= 0.U
            outBase3:= 0.U
        }
    }
}

// Miscellaneous functions.

object genKeepSignal {
    def apply(numBits: UInt, outLen: Int) = {
        assert(outLen % 8 == 0)
        val numBitsE    = Cat(0.U(1.W), numBits)
        val numBitsMsb  = log2Down(outLen) + 1
        val keepMsb     = outLen/8
        val keep        = Wire(UInt((outLen/8).W))
        keep := -1.S((outLen/8).W).asUInt

        for (i <- 0 until 64) {
            when (numBitsE(numBitsMsb-1, 3) === i.U && numBitsE(2, 0) === 0.U(3.W)) {
                val shift   = (keepMsb-i).U(7.W)
                keep := -1.S((outLen/8).W).asUInt >> shift
            }.elsewhen(numBitsE(numBitsMsb-1, 3) === i.U && numBitsE(2, 0) =/= 0.U(3.W)) {
                val shift   = (keepMsb-1-i).U(7.W)
                keep := -1.S((outLen/8).W).asUInt >> shift
            }
        }

        keep
    }
}

object parseKeepSignal {
    def apply(keep: UInt, inLen: Int) = {
        assert(inLen % 8 == 0)
        val inLenMsb    = log2Down(inLen) + 1
        val keepMsb     = inLen/8
        val lz          = Wire(UInt((inLenMsb-3).W))
        lz  := PriorityEncoder(Reverse(keep))
        val numBits     = Wire(UInt((inLenMsb+2).W))

        numBits := Cat((inLen/8).U((inLenMsb-3).W) - lz, 0.U(3.W))

        numBits
    }
}