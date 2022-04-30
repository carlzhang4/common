package common.connection

import chisel3._
import chisel3.util._

object SimpleRouter{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new SimpleRouter(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new SimpleRouter(gen,n))
	}

	class SimpleRouter[T<:Data](val gen:T, val n:Int)extends Module{
		val io = IO(new Bundle{
			val in		= Flipped(Decoupled(gen))
			val out		= Vec(n,Decoupled(gen))
			val idx		= Input(UInt(log2Up(n).W))
		})

		io.in.ready		:= 0.U
		for(i<-0 until n){
			io.out(i).bits		:= io.in.bits
			io.out(i).valid		:= 0.U
			when(io.idx === i.U){
				io.out(i).valid	:= io.in.valid
				io.in.ready		:= io.out(i).ready
			}
		}
	}
}

object SerialRouter{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new SerialRouter(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new SerialRouter(gen,n))
	}
	class SerialRouter[T<:Data](val gen:T, val n:Int)extends Module{
		val io = IO(new Bundle{
			val in		= Flipped(Decoupled(gen))
			val out		= Vec(n,Decoupled(gen))
			val idx		= Input(UInt(log2Up(n).W))
			val last	= Input(UInt(1.W))
			val last_o	= Output(UInt(1.W))
		})
		io.last_o		:= io.last

		val is_head 		= RegInit(UInt(1.W),1.U)
		val idx				= RegInit(UInt(log2Up(n).W),0.U)

		when(io.in.fire() && io.last===1.U){
			is_head		:= 1.U
		}.elsewhen(io.in.fire()){
			is_head		:= 0.U
		}

		when(is_head === 1.U){
			idx			:= io.idx
		}

		io.in.ready		:= 0.U
		for(i<-0 until n){
			io.out(i).bits		:= io.in.bits
			io.out(i).valid		:= 0.U
			when(is_head === 1.U){
				when(io.idx === i.U){
					io.out(i).valid	:= io.in.valid
					io.in.ready		:= io.out(i).ready
				}
			}.otherwise{
				when(idx === i.U){
					io.out(i).valid	:= io.in.valid
					io.in.ready		:= io.out(i).ready
				}
			}
			
		}
	}
}





