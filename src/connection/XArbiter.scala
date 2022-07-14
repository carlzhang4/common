package common.connection

import chisel3._
import chisel3.util._

object XArbiter{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new Arbiter(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new Arbiter(gen,n))
	}
}


object SerialArbiter{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new SerialArbiter(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new SerialArbiter(gen,n))
	}
	class SerialArbiter[T<:Data](val gen:T, val n:Int) extends Module{
		val io = IO(new Bundle{
			val in = Vec(n, Flipped(Decoupled(gen)))
			val last = Vec(n, Input(UInt(1.W)))
			val out = Decoupled(gen)
		})
		val req				= Cat(io.in.map(_.valid).reverse)//with reverse, port 0 is at the LSB
		val base			= RegInit(UInt(n.W),1.U)
		val double_req		= Cat(req,req)
		val double_grant	= double_req & ~(double_req-base)
		val grant			= double_grant(n-1,0) | double_grant(2*n-1,n)
		val grant_index		= OHToUInt(grant)

		val is_head 		= RegInit(UInt(1.W),1.U)
		val idx				= Wire(UInt(log2Up(n).W))
		val last_idx		= RegInit(UInt(log2Up(n).W),0.U)

		when(io.out.fire() && io.last(idx)===1.U){
			base			:= Cat(base(n-2,0),base(n-1))
		}

		when(is_head===1.U){
			idx				:= grant_index
		}.otherwise{
			idx 			:= last_idx
		}
		
		io.out.valid		:= 0.U
		io.out.bits			:= io.in(0).bits
		for(i <- 0 until n){
			io.in(i).ready	:=	0.U
			when(idx === i.U){
				io.in(i).ready	:= io.out.ready
				io.out.valid	:= io.in(i).valid
				io.out.bits 	:= io.in(i).bits
			}
		}
		when(io.out.fire() && io.last(idx)===1.U){
			is_head	:= 1.U
		}.elsewhen(io.out.fire()){
			is_head := 0.U
		}

		when(io.out.fire()){
			last_idx		:= idx
		}
	}
}
