package common.connection

import chisel3._
import chisel3.util._

object Connection{
	def one2many(one:DecoupledIO[Data]) (many:DecoupledIO[Data]*)	= {
		one.ready	:= many.map(_.ready).reduce(_ & _)
		many.map(t => t.valid := one.fire())
	}

	def many2one(many:DecoupledIO[Data]*)(one:DecoupledIO[Data])	= {
		one.valid	:= many.map(_.valid).reduce(_ & _)
		many.map(t => t.ready := one.fire())
	}

	def one2one(one:DecoupledIO[Data])(two:DecoupledIO[Data])		= {
		one.valid	<> two.valid
		one.ready	<> two.ready
	}

	def limit(in:DecoupledIO[Data], out:DecoupledIO[Data], en_cycles:UInt,total_cycles:UInt) = {
		val reg_count	= RegInit(UInt(32.W),0.U)
		val en			= Wire(Bool())
		when(reg_count<en_cycles){
			en			:= true.B
		}.otherwise{
			en			:= false.B
		}
		when(reg_count+1.U>=total_cycles){
			reg_count	:= 0.U
		}.otherwise{
			reg_count	:= reg_count+1.U
		}
		out.bits		:= in.bits
		out.valid		:= in.valid	& en
		in.ready		:= out.ready & en
	}
		
}