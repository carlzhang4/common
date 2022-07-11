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