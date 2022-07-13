package common

import chisel3._
import chisel3.util._

object ToZero{
	// def apply[T<:Bundle](x:T):Unit={
	// 	x	:= 0.U.asTypeOf(x)
	// }

	def apply[T<:Data](x:T):Unit={
		x	:= 0.U.asTypeOf(x)
	}

	def apply[T<:Data](v:Vec[T]):Unit={
		v.foreach(x => apply(x))
	}
}

object ToAllOnes{
	def apply[T<:Data](x:T):Unit={
		x	:= -1.S(x.getWidth.W).asTypeOf(x)
	}
}

object XCounter{
	def record_signals_asyn(is_high:Bool,is_reset:Bool,cur_clock:Clock)={
		withClockAndReset(cur_clock,is_reset){
			val count = RegInit(UInt(32.W),0.U)
			when(is_high){
				count	:= count+1.U
			}
			count
		}
	}

	def record_signals_sync(is_high:Bool)={
		val count = RegInit(UInt(32.W),0.U)
		when(is_high){
			count	:= count+1.U
		}
		count
	}
}