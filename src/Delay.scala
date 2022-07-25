package common

import common.storage._
import chisel3._
import chisel3.util._

object Delay{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new Delay(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new Delay(gen,n))
	}
	class Delay[T<:Data](val gen:T, val n:Int)extends Module{
		val io = IO(new Bundle{
			val in		= Flipped(Decoupled(gen))
			val out		= Decoupled(gen)
		})
		io.out			<> RegSlice(n)(io.in)
	}
}
