package common.connection

import chisel3._
import chisel3.util._
import common.storage.RegSlice
import common.axi.HasLast

object XArbiter{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new Arbiter(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new Arbiter(gen,n))
	}
}


object SerialArbiter{
	def apply[T<:HasLast](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new SerialArbiter(gen,n)))
	}
	def apply[T<:HasLast](gen:T, n:Int) = {
		Module(new SerialArbiter(gen,n))
	}
	class SerialArbiter[T<:HasLast](val gen:T, val n:Int) extends Module{
		val io = IO(new Bundle{
			val in = Vec(n, Flipped(Decoupled(gen)))
			val out = Decoupled(gen)
		})

		val in	= {
			for(i<-0 until n)yield{
				val tmp = RegSlice(io.in(i))
				tmp
			}	
		}
		val out = Wire(Decoupled(gen))

		val req				= Cat(in.map(_.valid).reverse)//with reverse, port 0 is at the LSB
		val base			= RegInit(UInt(n.W),1.U)
		val double_req		= Cat(req,req)
		val double_grant	= double_req & ~(double_req-base)
		val grant			= double_grant(n-1,0) | double_grant(2*n-1,n)
		val grant_index		= OHToUInt(grant)

		val is_head 		= RegInit(UInt(1.W),1.U)
		val idx				= Wire(UInt(log2Up(n).W))
		val last_idx		= RegInit(UInt(log2Up(n).W),0.U)

		when(out.fire() && out.bits.last===1.U){
			base			:= Cat(base(n-2,0),base(n-1))
		}

		when(is_head===1.U){
			idx				:= grant_index
		}.otherwise{
			idx 			:= last_idx
		}
		
		out.valid			:= 0.U
		out.bits			:= in(0).bits
		for(i <- 0 until n){
			in(i).ready	:=	0.U
			when(idx === i.U){
				in(i).ready		:= out.ready
				out.valid		:= in(i).valid
				out.bits 		:= in(i).bits
			}
		}
		when(out.fire() && out.bits.last===1.U){
			is_head	:= 1.U
		}.elsewhen(out.fire()){
			is_head := 0.U
		}

		when(out.fire()){
			last_idx		:= idx
		}
		io.out	<> RegSlice(out)
	}
}
