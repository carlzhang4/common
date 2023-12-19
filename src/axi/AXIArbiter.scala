package common.axi

import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.connection._

object AXIArbiter {
    def apply(n : Int, shape : AXI) = {
        Module(new AXIArbiter(
			shape.ar.bits.addr.getWidth,
			shape.r.bits.data.getWidth, 
			shape.ar.bits.id.getWidth, 
			shape.ar.bits.user.getWidth, 
			shape.ar.bits.len.getWidth,
            n
        ))
    }

    class AXIArbiter(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int, n: Int) extends Module {
        val io = IO(new Bundle {
            val in      = Vec(n, Flipped(new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)))
            val out     = new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)
        })

        // For write channel. just use composite arbiter.

        val wrAbt = CompositeArbiter(
            new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH),
            new AXI_DATA_W(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH),
            n,
            exportIdx = true
        )

        for (i <- 0 until n) {
            wrAbt.io.in_meta(i) <> io.in(i).aw
            wrAbt.io.in_data(i) <> io.in(i).w
        }
        io.out.aw   <> wrAbt.io.out_meta
        io.out.w    <> wrAbt.io.out_data

        // Response channel. Assume no more than 256 outstanding W transactions

        val inResp      = Wire(Vec(n, Decoupled(new AXI_BACK(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH))))
        val outResp     = Wire(Decoupled(new AXI_BACK(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH)))

        for (i <- 0 until n) {
            val regSlice = Module(new RegSlice(new AXI_BACK(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH)))
            regSlice.io.upStream    <> inResp(i)
            regSlice.io.downStream  <> io.in(i).b
        }

        {
            val regSlice = Module(new RegSlice(new AXI_BACK(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH)))
            regSlice.io.upStream    <> io.out.b
            regSlice.io.downStream  <> outResp
        }

        val respFifo    = XQueue(UInt(log2Up(n).W), 256)
        
        respFifo.io.in.valid    := wrAbt.io.idx.get.valid
        respFifo.io.in.bits     := wrAbt.io.idx.get.bits

        for (i <- 0 until n) {
            inResp(i).bits      := outResp.bits
            inResp(i).valid     := outResp.valid && respFifo.io.out.valid && (respFifo.io.out.bits === i.U)
        }
        outResp.ready   := inResp(respFifo.io.out.bits).ready
        respFifo.io.out.ready   := outResp.fire

        // Read channel. Max 256 outstanding transactions supported

        val readFifo    = XQueue(UInt(log2Up(n).W), 256)

        val inReadAddr  = Wire(Vec(n, Decoupled(new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))))
        val outReadAddr = Wire(Decoupled(new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)))
        val inRead      = Wire(Vec(n, Decoupled(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH))))
        val outRead     = Wire(Decoupled(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH)))

        {
            val regSlice = Module(new RegSlice(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH)))
            regSlice.io.upStream    <> io.out.r
            regSlice.io.downStream  <> outRead
        }
        io.out.ar   <> RegSlice(outReadAddr)
        for (i <- 0 until n) {
            io.in(i).r      <> RegSlice(inRead(i))
            val regSlice = Module(new RegSlice(new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)))
            regSlice.io.upStream    <> io.in(i).ar
            regSlice.io.downStream.ready    := inReadAddr(i).ready && readFifo.io.in.ready
            inReadAddr(i).valid := regSlice.io.downStream.valid && readFifo.io.in.ready
            inReadAddr(i).bits  := regSlice.io.downStream.bits
        }

        val arAbt = XArbiter(
            new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH),
            n,
            exportIdx = true
        )

        arAbt.io.in <> inReadAddr
        outReadAddr <> arAbt.io.out

        readFifo.io.in.valid    := arAbt.io.idx.get.valid
        readFifo.io.in.bits     := arAbt.io.idx.get.bits
        
        for (i <- 0 until n) {
            inRead(i).bits  := outRead.bits
            inRead(i).valid := outRead.valid && readFifo.io.out.valid && (readFifo.io.out.bits === i.U)
        }

        outRead.ready   := inRead(readFifo.io.out.bits).ready && readFifo.io.out.valid

        readFifo.io.out.ready   := outRead.fire && outRead.bits.last.asBool
    }
}