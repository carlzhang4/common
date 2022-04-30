package common.axi

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, requireIsChiselType}
import common._
import common.storage._
import common.ToZero

class AXI2Reg[T<:AXI](private val gen:T, depth:Int, width:Int, enable_ila:Int=0) extends Module{
	require(width%8 == 0)
	val genType = if (compileOptions.declaredTypeMustBeUnbound) {
		requireIsChiselType(gen)
		gen
	} else {
		if (DataMirror.internal.isSynthesizable(gen)) {
			chiselTypeOf(gen)
		}else {
			gen
		}
	}
	val io = IO(new Bundle{
		val axi = Flipped(genType)
		val reg_control = Output(Vec(depth,UInt(width.W))) 
		val reg_status = Input(Vec(depth,UInt(width.W)))
	})

	val reg_control = Reg(Vec(2*depth,UInt(width.W)))  //val reg_control = RegInit(VecInit(Seq.fill(512)(0.U(32.W))))

	for(i<- 0 until depth){
		reg_control(depth+i)		:= io.reg_status(i)
		io.reg_control(i)			:= reg_control(i)
	}

	//b
	ToZero(io.axi.b.bits)
	io.axi.b.valid := 1.U

	//w and aw
	val q_aw = XQueue(chiselTypeOf(io.axi.aw.bits), 2, almostfull_threshold = 0)
	q_aw.io.in <> io.axi.aw
	q_aw.io.out.ready := 0.U

	val q_w = XQueue(chiselTypeOf(io.axi.w.bits), 2, almostfull_threshold = 0)
	q_w.io.in <> io.axi.w
	q_w.io.out.ready 	:= 0.U

	val offset_w = RegInit(0.U(32.W))
	val addr_w = q_aw.io.out.bits.addr

	when(q_aw.io.out.valid){
		q_w.io.out.ready := 1.U
	}

	when(q_w.io.out.fire()){
		offset_w	:= offset_w+1.U
		when(q_w.io.out.bits.last.asBool()){
			offset_w	:= 0.U
			q_aw.io.out.ready := 1.U
		}
		reg_control(addr_w+offset_w) := q_w.io.out.bits.data
	}

	//ar and r
	val q_ar = XQueue(chiselTypeOf(io.axi.ar.bits), 2, almostfull_threshold = 0)
	q_ar.io.in <> io.axi.ar
	q_ar.io.out.ready := 0.U

	val q_r = XQueue(chiselTypeOf(io.axi.r.bits), 2, almostfull_threshold = 0)
	q_r.io.out <> io.axi.r
	q_r.io.in.valid 	:= 0.U
	ToZero(q_r.io.in.bits) //r.resp = 0.U for normal access ok

	val offset_r = RegInit(0.U(32.W))
	val addr_r = q_ar.io.out.bits.addr

	when(q_ar.io.out.valid){
		q_r.io.in.valid := 1.U
	}
	val is_last = offset_r===q_ar.io.out.bits.len
	when(q_r.io.in.fire()){
		offset_r	:= offset_r+1.U
		when(is_last){
			offset_r			:= 0.U
			q_ar.io.out.ready	:= 1.U
		}
	}
	q_r.io.in.bits.data	:= reg_control(addr_r+offset_r)
	q_r.io.in.bits.last	:= is_last

	if(enable_ila == 1){
		class ila_axi2reg(seq:Seq[Data]) extends BaseILA(seq)
		val mod2 = Module(new ila_axi2reg(Seq(
			q_aw.io.out.valid,
			q_aw.io.out.ready,
			q_aw.io.out.bits.addr,
			offset_w,
			io.axi.w.valid,
			io.axi.w.ready,
			io.axi.w.bits.data,
			io.axi.w.bits.last,
			io.axi.w.bits.strb,
		)))
		mod2.connect(clock)

		class ila_ar(seq:Seq[Data]) extends BaseILA(seq)
		val mod3 = Module(new ila_ar(Seq(
			q_ar.io.out.valid,
			q_ar.io.out.ready,
			q_ar.io.out.bits.addr,
			io.axi.r.valid,
			io.axi.r.ready,
			io.axi.r.bits.data,
		)))
		mod3.connect(clock)
	}
	
}