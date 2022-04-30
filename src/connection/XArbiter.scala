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

		val is_head 		= RegInit(UInt(1.W),1.U)

		val grant_idx		= Wire(UInt(log2Up(n).W))

		val idx				= Wire(UInt(log2Up(n).W))

		val last_idx		= RegInit(UInt(log2Up(n).W),0.U)

		grant_idx			:= 0.U

		val shifts = Reg(Vec(n, UInt(log2Up(n).W)))
		when(reset.asBool){
			for(i <- 0 until n){
				shifts(i)	:= i.U
			}
		}.otherwise{
			for(i <- 1 until n){
				shifts(i)	:= shifts(i-1)
			}
			shifts(0)		:= shifts(n-1)
		}

		for(i <- 0 until n){
			when(io.in(shifts(i)).valid === 1.U){
				grant_idx 	:= shifts(i)
			}
		}
		when(is_head===1.U){
			idx				:= grant_idx
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
