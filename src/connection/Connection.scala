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
}