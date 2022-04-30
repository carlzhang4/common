package common.storage

import common.Math
import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, Direction, requireIsChiselType}

class XConverterIO[T <: Data](private val gen: T) extends Bundle
{ 
	val in_clk = Input(Clock())
	val out_clk = Input(Clock())
	val rstn = Input(Bool())
	val in = Flipped(InIO(gen))
	val out = Flipped(OutIO(gen))
	// val count = Output(UInt(log2Ceil(entries + 1).W))
	// val almostfull = Output(UInt(1.W))
}

object XConverter{
	def apply[T<:Data](num:Int)(gen: T) = {
		Seq.fill(num)(Module(new XConverter(gen)))
	}

	def apply[T<:Data](gen: T) = {
		Module(new XConverter(gen))
	}

	def apply[T<:Data](gen: T, in_clk:Clock, rstn:Bool, out_clk:Clock) = {
		val cvt = Module(new XConverter(gen))
		cvt.io.in_clk	:= in_clk
		cvt.io.out_clk	:= out_clk
		cvt.io.rstn		:= rstn
		cvt
	}

	class XConverter[T<:Data](
		val gen:T,
	)extends RawModule(){
		val entries = 16

		val genType = if (compileOptions.declaredTypeMustBeUnbound) {
			requireIsChiselType(gen)
			gen
		} else {
			if (DataMirror.internal.isSynthesizable(gen)) {
				chiselTypeOf(gen)
			}else {
				gen
			}
		}
		val io = IO(new XConverterIO(genType))

		val width = Math.round_up((genType.getWidth),8)

		val fifo = Module(new SV_STREAM_FIFO(width,log2Up(entries),"auto","independent_clock"))

		fifo.io.s_clk		<> io.in_clk
		fifo.io.m_clk		<> io.out_clk
		fifo.io.reset_n		<> io.rstn

		fifo.io.in_data 	:= io.in.bits.asTypeOf(UInt())
		fifo.io.in_valid	<> io.in.valid
		fifo.io.in_ready	<> io.in.ready

		io.out.bits			:= fifo.io.out_data.asTypeOf(chiselTypeOf(io.out.bits))
		io.out.valid		:= fifo.io.out_valid
		fifo.io.out_ready	:= io.out.ready
		
		// io.count			:= fifo.io.wr_count
		// io.almostfull	:= fifo.io.wr_count >= (entries-almostfull_threshold).U

		
	}
}