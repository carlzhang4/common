package common

import chisel3._
import chisel3.util._
import common.storage._

class BFloat16ToFixed (VEC_LEN : Int = 32, SCALE_FACTOR : Int = 12) extends Module {

    val MIN_BIAS = 127 - SCALE_FACTOR

    class BFloat16 extends Bundle {
        val sign    = UInt(1.W)
        val exp     = UInt(8.W)
        val frac    = UInt(7.W)
    }

    class Cycle1Data extends Bundle {
        val data        = Vec(VEC_LEN, new BFloat16)
        val underflow   = Vec(VEC_LEN, Bool())
        val overflow    = Vec(VEC_LEN, Bool())
    }

    val io = IO(new Bundle{
        val in  = Flipped(Decoupled(UInt((16*VEC_LEN).W)))
        val out = Decoupled(UInt((16*VEC_LEN).W))
    })

    val cycle0data  = Wire(Vec(VEC_LEN, new BFloat16))

    // Cycle 1 : Detect overflow and underflow
    
    for (i <- 0 until VEC_LEN) {
        cycle0data(i).sign  := io.in.bits(i*16+15)
        cycle0data(i).exp   := io.in.bits(i*16+14, i*16+7)
        cycle0data(i).frac  := io.in.bits(i*16+6 , i*16  )
    }

    val cycle1data  = Module(new RegSlice(new Cycle1Data))

    cycle1data.io.upStream.valid    := io.in.valid
    for (i <- 0 until VEC_LEN) {
        cycle1data.io.upStream.bits.data(i)         := cycle0data(i)
        cycle1data.io.upStream.bits.overflow(i)     := cycle0data(i).exp >= (MIN_BIAS+14).U(8.W)
        cycle1data.io.upStream.bits.underflow(i)    := cycle0data(i).exp < MIN_BIAS.U(8.W)
    }
    io.in.ready := cycle1data.io.upStream.ready

    // Cycle 2 : Extract data for of/uf, calculate the shift offset.

    val cycle2data  = Module(new RegSlice(new Cycle1Data))

    cycle2data.io.upStream.valid    := cycle1data.io.downStream.valid
    cycle1data.io.downStream.ready  := cycle2data.io.upStream.ready
    for (i <- 0 until VEC_LEN) {
        cycle2data.io.upStream.bits.overflow(i)  := cycle1data.io.downStream.bits.overflow(i)
        cycle2data.io.upStream.bits.underflow(i) := cycle1data.io.downStream.bits.underflow(i)
        when (cycle1data.io.downStream.bits.overflow(i)) {    // Overflow : Send INT_MAX
            when (cycle1data.io.downStream.bits.data(i).sign === 1.U(1.W)) {
                cycle2data.io.upStream.bits.data(i) := 0x8000.U(16.W).asTypeOf(new BFloat16())
            }.otherwise {
                cycle2data.io.upStream.bits.data(i) := 0x7fff.U(16.W).asTypeOf(new BFloat16())
            }
        }.elsewhen (cycle1data.io.downStream.bits.underflow(i)) { // Underflow : Clear to 0
            cycle2data.io.upStream.bits.data(i) := 0.U(16.W).asTypeOf(new BFloat16())
        }.otherwise {   // Calculate shift offset and inverse bits.
            cycle2data.io.upStream.bits.data(i).sign := cycle1data.io.downStream.bits.data(i).sign
            cycle2data.io.upStream.bits.data(i).exp  := cycle1data.io.downStream.bits.data(i).exp - MIN_BIAS.U(8.W)
            when (cycle2data.io.upStream.bits.data(i).sign === 1.U) {
                cycle2data.io.upStream.bits.data(i).frac := ~cycle1data.io.downStream.bits.data(i).frac
            }.otherwise {
                cycle2data.io.upStream.bits.data(i).frac := cycle1data.io.downStream.bits.data(i).frac
            }
        }
    }

    // Cycle 3 : Shift bits. 

    val cycle3data  = Module(new RegSlice(Vec(VEC_LEN, UInt(16.W))))

    cycle3data.io.upStream.valid    := cycle2data.io.downStream.valid
    cycle2data.io.downStream.ready  := cycle3data.io.upStream.ready
    for (i <- 0 until VEC_LEN) {
        when (cycle2data.io.downStream.bits.overflow(i)) {
            cycle3data.io.upStream.bits(i) := cycle2data.io.downStream.bits.data(i).asUInt
        }.elsewhen (cycle2data.io.downStream.bits.underflow(i)) {
            cycle3data.io.upStream.bits(i) := cycle2data.io.downStream.bits.data(i).asUInt
        }.otherwise {
            cycle3data.io.upStream.bits(i) := 0.U(16.W)
            when (cycle2data.io.downStream.bits.data(i).exp === 0.U(16.W)) {
                cycle3data.io.upStream.bits(i) := Cat(
                    Fill(15, cycle2data.io.downStream.bits.data(i).sign),
                    ~cycle2data.io.downStream.bits.data(i).sign
                )
            }
            for (j <- 1 until 8) {
                when (cycle2data.io.downStream.bits.data(i).exp === j.U(16.W)) {
                    cycle3data.io.upStream.bits(i) := Cat(
                        Fill(15-j, cycle2data.io.downStream.bits.data(i).sign),
                        ~cycle2data.io.downStream.bits.data(i).sign,
                        cycle2data.io.downStream.bits.data(i).frac(6, 7-j)
                    )
                }
            }
            for (j <- 8 until 15) {
                when (cycle2data.io.downStream.bits.data(i).exp === j.U(16.W)) {
                    cycle3data.io.upStream.bits(i) := Cat(
                        Fill(15-j, cycle2data.io.downStream.bits.data(i).sign),
                        ~cycle2data.io.downStream.bits.data(i).sign,
                        cycle2data.io.downStream.bits.data(i).frac,
                        0.U((j-7).W)
                    )
                }
            }
        }
    }

    // Cycle 4 : For negative number...

    val cycle4data  = Module(new RegSlice(Vec(VEC_LEN, UInt(16.W))))

    cycle4data.io.upStream.valid    := cycle3data.io.downStream.valid
    cycle3data.io.downStream.ready  := cycle4data.io.upStream.ready

    for (i <- 0 until VEC_LEN) {
        when (cycle3data.io.downStream.bits(i)(15) === 1.U) {
            cycle4data.io.upStream.bits(i) := (
                cycle3data.io.downStream.bits(i) + 1.U(15.W)
            )
        }.otherwise {
            cycle4data.io.upStream.bits(i) := cycle3data.io.downStream.bits(i)
        }
    }

    // Output.

    io.out.valid    := cycle4data.io.downStream.valid
    io.out.bits     := cycle4data.io.downStream.bits.asUInt
    cycle4data.io.downStream.ready  := io.out.ready
}

class FixedToBFloat16 (VEC_LEN : Int = 32, SCALE_FACTOR : Int = 12) extends Module {

    val MIN_BIAS = 127 - SCALE_FACTOR

    class BFloat16 extends Bundle {
        val sign    = UInt(1.W)
        val exp     = UInt(8.W)
        val frac    = UInt(7.W)
    }

    class Cycle1Data extends Bundle {
        val data        = Vec(VEC_LEN, UInt(16.W))
        val underflow   = Vec(VEC_LEN, Bool())
        val overflow    = Vec(VEC_LEN, Bool())
    }

    class Cycle2Data extends Bundle {
        val data        = Vec(VEC_LEN, UInt(16.W))
        val leadingZero1= Vec(VEC_LEN, UInt(3.W))
        val leadingZero2= Vec(VEC_LEN, UInt(3.W))
        val leadingZero3= Vec(VEC_LEN, UInt(3.W))
        val underflow   = Vec(VEC_LEN, Bool())
        val overflow    = Vec(VEC_LEN, Bool())
    }

    class Cycle3Data extends Bundle {
        val data        = Vec(VEC_LEN, UInt(16.W))
        val leadingZero = Vec(VEC_LEN, UInt(4.W))
        val underflow   = Vec(VEC_LEN, Bool())
        val overflow    = Vec(VEC_LEN, Bool())
    }

    class Cycle4Data extends Bundle {
        val data        = Vec(VEC_LEN, new BFloat16)
    }

    object CountLeadingZero5 {
        def apply(in: UInt) = {
            assert(in.getWidth == 5)

            val leadingZero = Wire(UInt(3.W))

            when (in(4, 0) === 0.U(5.W)) {
                leadingZero := 5.U(3.W)
            }.elsewhen (in(4, 1) === 0.U(4.W)) {
                leadingZero := 4.U(3.W)
            }.elsewhen (in(4, 2) === 0.U(3.W)) {
                leadingZero := 3.U(3.W)
            }.elsewhen (in(4, 3) === 0.U(3.W)) {
                leadingZero := 2.U(3.W)
            }.elsewhen (in(4) === 0.U(1.W)) {
                leadingZero := 1.U(3.W)
            }.otherwise {
                leadingZero := 0.U(3.W)
            }

            leadingZero
        }
    }

    val io = IO(new Bundle{
        val in  = Flipped(Decoupled(UInt((16*VEC_LEN).W)))
        val out = Decoupled(UInt((16*VEC_LEN).W))
    })

    val cycle0data  = Wire(Vec(VEC_LEN, UInt(16.W)))
    
    for (i <- 0 until VEC_LEN) {
        cycle0data(i)   := io.in.bits(i*16+15, i*16)
    }

    // Cycle 1 : For negative number, 2's implement to positive

    val cycle1data  = Module(new RegSlice(new Cycle1Data))

    io.in.ready := cycle1data.io.upStream.ready
    cycle1data.io.upStream.valid    := io.in.valid

    for (i <- 0 until VEC_LEN) {
        when (cycle0data(i)(15) === 1.U) {
            cycle1data.io.upStream.bits.data(i) := Cat(
                1.U(1.W),
                ~(cycle0data(i)(14, 0) - 1.U(15.W))
            )
        }.otherwise {
            cycle1data.io.upStream.bits.data(i) := cycle0data(i)
        }

        when (cycle1data.io.upStream.bits.data(i)(14, 0) === 0x7fff.U(15.W)) {
            cycle1data.io.upStream.bits.overflow(i)     := true.B
            cycle1data.io.upStream.bits.underflow(i)    := false.B
        }.elsewhen(cycle0data(i) === 0.U) {
            cycle1data.io.upStream.bits.underflow(i)    := true.B
            cycle1data.io.upStream.bits.overflow(i)     := false.B
        }.otherwise {
            cycle1data.io.upStream.bits.overflow(i)     := false.B
            cycle1data.io.upStream.bits.underflow(i)     := false.B
        }
    }

    // Cycle 2 : Count leading 0 phase 1, split to 5/5/5

    val cycle2data  = Module(new RegSlice(new Cycle2Data))

    cycle2data.io.upStream.valid    := cycle1data.io.downStream.valid
    cycle1data.io.downStream.ready  := cycle2data.io.upStream.ready
    cycle2data.io.upStream.bits.data        := cycle1data.io.downStream.bits.data
    cycle2data.io.upStream.bits.overflow    := cycle1data.io.downStream.bits.overflow
    cycle2data.io.upStream.bits.underflow   := cycle1data.io.downStream.bits.underflow

    for (i <- 0 until VEC_LEN) {
        when (cycle1data.io.downStream.bits.overflow(i) || cycle1data.io.downStream.bits.underflow(i)) {
            cycle2data.io.upStream.bits.leadingZero1(i) := 0.U
            cycle2data.io.upStream.bits.leadingZero2(i) := 0.U
            cycle2data.io.upStream.bits.leadingZero3(i) := 0.U
        }.otherwise {
            cycle2data.io.upStream.bits.leadingZero1(i) := CountLeadingZero5(cycle1data.io.downStream.bits.data(i)(14, 10))
            cycle2data.io.upStream.bits.leadingZero2(i) := CountLeadingZero5(cycle1data.io.downStream.bits.data(i)(9, 5))
            cycle2data.io.upStream.bits.leadingZero3(i) := CountLeadingZero5(cycle1data.io.downStream.bits.data(i)(4, 0))
        }
    }

    // Cycle 3 : Count leading 0 phase 2

    val cycle3data  = Module(new RegSlice(new Cycle3Data))

    cycle3data.io.upStream.valid    := cycle2data.io.downStream.valid
    cycle2data.io.downStream.ready  := cycle3data.io.upStream.ready
    cycle3data.io.upStream.bits.data        := cycle2data.io.downStream.bits.data
    cycle3data.io.upStream.bits.overflow    := cycle2data.io.downStream.bits.overflow
    cycle3data.io.upStream.bits.underflow   := cycle2data.io.downStream.bits.underflow

    for (i <- 0 until VEC_LEN) {
        when (cycle2data.io.downStream.bits.leadingZero1(i) =/= 5.U(3.W)) {
            cycle3data.io.upStream.bits.leadingZero(i) := cycle2data.io.downStream.bits.leadingZero1(i)
        }.otherwise {
            when (cycle2data.io.downStream.bits.leadingZero2(i) =/= 5.U(3.W)) {
                cycle3data.io.upStream.bits.leadingZero(i) := 5.U(4.W) + Cat(0.U(1.W), cycle2data.io.downStream.bits.leadingZero2(i))
            }.otherwise {
                cycle3data.io.upStream.bits.leadingZero(i) := 10.U(4.W) + Cat(0.U(1.W), cycle2data.io.downStream.bits.leadingZero3(i))
            }
        }
    }

    // Cycle 4 : Shift bits, calculate exponents.

    val cycle4data  = Module(new RegSlice(new Cycle4Data))

    cycle4data.io.upStream.valid    := cycle3data.io.downStream.valid
    cycle3data.io.downStream.ready  := cycle4data.io.upStream.ready

    for (i <- 0 until VEC_LEN) {
        when (cycle3data.io.downStream.bits.underflow(i)) { // Underflow, fill all 0's
            cycle4data.io.upStream.bits.data(i).sign    := 0.U(1.W)
            cycle4data.io.upStream.bits.data(i).exp     := 0.U(8.W)
            cycle4data.io.upStream.bits.data(i).frac    := 0.U(7.W)
        }.elsewhen (cycle3data.io.downStream.bits.overflow(i)) { // Overflow, add to INT_MAX
            cycle4data.io.upStream.bits.data(i).sign    := cycle3data.io.downStream.bits.data(i)(15)
            cycle4data.io.upStream.bits.data(i).exp     := (MIN_BIAS+15).U(8.W)
            cycle4data.io.upStream.bits.data(i).frac    := 0.U(7.W)
        }.otherwise {
            cycle4data.io.upStream.bits.data(i).sign    := cycle3data.io.downStream.bits.data(i)(15)
            cycle4data.io.upStream.bits.data(i).exp     := (MIN_BIAS+14).U(8.W) - cycle3data.io.downStream.bits.leadingZero(i)
            cycle4data.io.upStream.bits.data(i).frac    := 0.U(7.W)
            for (j <- 0 until 8) {
                when (cycle3data.io.downStream.bits.leadingZero(i) === j.U(4.W)) {
                    cycle4data.io.upStream.bits.data(i).frac := cycle3data.io.downStream.bits.data(i)(13-j, 7-j)
                }
            }
            for (j <- 8 until 14) {
                when (cycle3data.io.downStream.bits.leadingZero(i) === j.U(4.W)) {
                    cycle4data.io.upStream.bits.data(i).frac := Cat(
                        cycle3data.io.downStream.bits.data(i)(13-j, 0),
                        0.U((j-7).W)
                    )
                }
            }
            when (cycle3data.io.downStream.bits.leadingZero(i) === 14.U(4.W)) {
                cycle4data.io.upStream.bits.data(i).frac := 0.U(7.W)
            }
        }
    }

    // Output.

    val cycle5data = Wire(Vec(VEC_LEN, UInt(16.W)))
    for (i <- 0 until VEC_LEN) {
        cycle5data(i)   := cycle4data.io.downStream.bits.data(i).asUInt
    }

    io.out.valid    := cycle4data.io.downStream.valid
    io.out.bits     := cycle5data.asUInt()
    cycle4data.io.downStream.ready  := io.out.ready
}