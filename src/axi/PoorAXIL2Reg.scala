package common.axi

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, requireIsChiselType}
import common._
import common.storage._
import common.ToZero

class PoorAXIL2Reg[T<:AXI](private val gen:T, depth:Int, width:Int, enable_ila:Int=0) extends Module{
	val shift = Math.log2(width/8)
	val io = IO(new Bundle{
		val axi = Flipped(gen)	
		val reg_control = Output(Vec(depth,UInt(width.W))) 
		val reg_status = Input(Vec(depth,UInt(width.W)))
	})
	val reg_control = Reg(Vec(depth,UInt(width.W))) 

	val reg_status = Reg(Vec(depth,UInt(width.W))) 

	for(i<- 0 until depth){
		reg_status(i)				:= io.reg_status(i)
		io.reg_control(i)			:= reg_control(i)
	}

	val wire_control = Wire(Vec(2*depth,UInt(width.W))) 
	for(i<- 0 until depth){
		wire_control(depth+i)				:= reg_status(i)
		wire_control(i)						:= reg_control(i)
	}

	ToZero(io.axi.r.bits)
	ToZero(io.axi.b.bits)
	io.axi.b.valid := 1.U

	val sIDLE :: sWORK :: Nil = Enum(2)
	val s_rd = RegInit(sIDLE)
	val s_wr = RegInit(sIDLE)

	val r = io.axi.r
	val ar = io.axi.ar
	val w = io.axi.w
	val aw = io.axi.aw

	val r_addr = Reg(chiselTypeOf(ar.bits.addr))
	val w_addr = Reg(chiselTypeOf(ar.bits.addr))

	ar.ready	:= (s_rd === sIDLE)
	r.valid		:= (s_rd === sWORK)
	r.bits.data	:= wire_control(r_addr)
	switch(s_rd){
		is(sIDLE){
			when(ar.fire()){
				r_addr			:= ar.bits.addr >> shift.U
				s_rd			:= sWORK
			}
		}
		is(sWORK){
			when(r.fire()){
				s_rd			:= sIDLE
			}
		}
	}

	aw.ready	:= (s_wr === sIDLE)
	w.ready		:= (s_wr === sWORK)
	when(w.fire()){
		reg_control(w_addr)	:= w.bits.data
	}
	switch(s_wr){
		is(sIDLE){
			when(aw.fire()){
				w_addr			:= aw.bits.addr >> shift.U
				s_wr			:= sWORK
			}
		}
		is(sWORK){
			when(w.fire()){
				s_wr			:= sIDLE
			}
		}
	}

	if(enable_ila == 1){
		class ila_wr(seq:Seq[Data]) extends BaseILA(seq)
		val mod2 = Module(new ila_wr(Seq(
			io.axi.aw.valid,
			io.axi.aw.ready,
			io.axi.aw.bits.addr,
			io.axi.w.valid,
			io.axi.w.ready,
			io.axi.w.bits.data,
			io.axi.w.bits.last,
			io.axi.w.bits.strb,
			w_addr,
			s_wr,
		)))
		mod2.connect(clock)

		class ila_rd(seq:Seq[Data]) extends BaseILA(seq)
		val mod3 = Module(new ila_rd(Seq(
			io.axi.ar.valid,
			io.axi.ar.ready,
			io.axi.ar.bits.addr,
			io.axi.r.valid,
			io.axi.r.ready,
			io.axi.r.bits.data,
			r_addr,
			s_rd,
		)))
		mod3.connect(clock)
	}
}