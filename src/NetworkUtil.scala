package common

import chisel3._
import chisel3.util._
import math.pow
import common.storage.XRam

// A module that might be helpful for building reliable networks.
// Since the arrival of network packets might be out-of-order, we
// often need to do reordering of packets, where requires counting
// the consecutive acknowledged packets. 
// io.in passes the sequence number of the received packet,
// and io.ack indicates the next expected sequence number.

class AckCounter (
    BUFFER_SIZE     : Int
) extends Module {
    assert(BUFFER_SIZE >= 8, 
        "BUFFER_SIZE should be no less than 8."
    )
    assert(pow(2, log2Ceil(BUFFER_SIZE)).toInt == BUFFER_SIZE, 
        "BUFFER_SIZE of LatencyBucket should be exponential of 2."
    )

    val IN_LEN  = log2Up(BUFFER_SIZE)

    val io = IO(new Bundle {
        val in  = Flipped(Valid(UInt(IN_LEN.W)))
        val ack = Output(UInt(IN_LEN.W))
    })

    val ackRamA = XRam(UInt(1.W), BUFFER_SIZE / 4, latency = 1, memory_type="distributed")
    val ackRamB = XRam(UInt(1.W), BUFFER_SIZE / 4, latency = 1, memory_type="distributed")

    ackRamA.io.addr_a       := io.in.bits(IN_LEN-2, 1)
    ackRamA.io.data_in_a    := ~io.in.bits(IN_LEN-1)
    ackRamA.io.wr_en_a      := io.in.valid && (io.in.bits(0) === 0.U(1.W))
    ackRamB.io.addr_a       := io.in.bits(IN_LEN-2, 1)
    ackRamB.io.data_in_a    := ~io.in.bits(IN_LEN-1)
    ackRamB.io.wr_en_a      := io.in.valid && (io.in.bits(0) === 1.U(1.W))

    val ackPtr      = RegInit(0.U(IN_LEN.W))
    val nextAckPtr  = ackPtr + 1.U(IN_LEN.W)

    ackRamA.io.addr_b   := Mux(ackPtr(0) === 0.U(1.W), ackPtr(IN_LEN-2, 1), nextAckPtr(IN_LEN-2, 1))
    ackRamB.io.addr_b   := ackPtr(IN_LEN-2, 1)

    when (ackPtr(0) === 0.U(1.W) && ackRamA.io.data_out_b =/= ackPtr(IN_LEN-1)) {
        ackPtr  := ackPtr + 1.U(IN_LEN.W)
    }.elsewhen(ackPtr(0) === 1.U(1.W) && ackRamB.io.data_out_b =/= ackPtr(IN_LEN-1)) {
        ackPtr  := ackPtr + 1.U(IN_LEN.W)
    }

    io.ack  := ackPtr
}