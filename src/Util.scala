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

object ValidCounter{
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

case class Timer(start:UInt,end:UInt){
	private val reg_time				= RegInit(UInt(64.W),0.U)
	private val reg_latency				= RegInit(UInt(64.W),0.U)
	private val reg_cnt_start			= RegInit(UInt(32.W),0.U)
	private val reg_cnt_end				= RegInit(UInt(32.W),0.U)
	private val reg_en					= RegInit(UInt(1.W),0.U)

	when(start===1.U){
		reg_cnt_start			:= reg_cnt_start+1.U
	}.otherwise{
		reg_cnt_start			:= reg_cnt_start
	}

	when(end===1.U){
		reg_cnt_end				:= reg_cnt_end+1.U
	}.otherwise{
		reg_cnt_end				:= reg_cnt_end
	}

	when(start===1.U){
		reg_en					:= 1.U
	}.otherwise{
		reg_en					:= reg_en
	}

	when(reg_en===1.U){
		reg_time				:= reg_time+1.U
	}

	when(start===1.U && end===1.U){
		reg_latency				:= reg_latency
	}.elsewhen(start===1.U){
		reg_latency				:= reg_latency - reg_time
	}.elsewhen(end===1.U){
		reg_latency				:= reg_latency + reg_time
	}.otherwise{
		reg_latency				:= reg_latency
	}	

	val latency					= reg_latency
	val cnt_start				= reg_cnt_start
	val cnt_end					= reg_cnt_end
	val time					= reg_time
}