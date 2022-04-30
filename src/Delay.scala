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

        val shift_data = Reg(Vec(n,gen))
        val shift_valid = Reg(Vec(n,UInt(1.W)))

        val q = XQueue(gen, n+2048)

        shift_data(0)               := io.in.bits
        shift_valid(0)              := io.in.valid

		for(i <- 0 until (n-1)){ 
            shift_data(i+1)         := shift_data(i)
			shift_valid(i+1)        := shift_valid(i)
		}

        q.io.in.bits                := shift_data(n-1)
        q.io.in.valid               := shift_valid(n-1)
        io.in.ready                 := (q.io.count < 2000.U)

        q.io.out                       <> io.out

	}
}
