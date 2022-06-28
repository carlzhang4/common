package common.storage
import chisel3._
import chisel3.util._
import common.axi._

object AXIRegSlice{
	def apply(stage: Int)(upStream: AXI) = {
		val downStream = Wire(chiselTypeOf(upStream))

		val regSlice = Seq.fill(stage)(Module(new AXIRegSlice(
			upStream.ar.bits.addr.getWidth,
			upStream.r.bits.data.getWidth, 
			upStream.ar.bits.id.getWidth, 
			upStream.ar.bits.user.getWidth, 
			upStream.ar.bits.len.getWidth
		)))

		regSlice(0).io.upStream			<> upStream
		regSlice(stage-1).io.downStream	<> downStream

		var i = 0
		for (i <- 0 until stage-1) {
			regSlice(i).io.downStream   <> regSlice(i+1).io.upStream
		}

		downStream
	}

	def apply(upStream: AXI) = {
		val downStream = Wire(chiselTypeOf(upStream))

		val regSlice = Module(new AXIRegSlice(
			upStream.ar.bits.addr.getWidth,
			upStream.r.bits.data.getWidth, 
			upStream.ar.bits.id.getWidth, 
			upStream.ar.bits.user.getWidth, 
			upStream.ar.bits.len.getWidth
		))

		regSlice.io.upStream    <> upStream
		regSlice.io.downStream  <> downStream

		downStream
	}
	
	class AXIRegSlice(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int) extends Module() {
		val io = IO(new Bundle{
			val upStream = Flipped(new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
			val downStream = new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)
		})

		io.downStream.aw	<> RegSlice(io.upStream.aw)
		io.downStream.ar	<> RegSlice(io.upStream.ar)
		io.downStream.w		<> RegSlice(io.upStream.w)
		io.upStream.r		<> RegSlice(io.downStream.r)
		io.upStream.b		<> RegSlice(io.downStream.b)
	}
}

object RegSlice{
	def apply[T<:Data](upStream: DecoupledIO[T]) = {
		val downStream = Wire(chiselTypeOf(upStream))
		val queue = XQueue(chiselTypeOf(upStream.bits), 2, almostfull_threshold = 0)
		queue.io.in		<> upStream
		queue.io.out	<> downStream

		downStream
	}

	def apply[T<:Data](stage: Int)(upStream: DecoupledIO[T]) = {
		val downStream = Wire(chiselTypeOf(upStream))
		val queue = XQueue(stage)(chiselTypeOf(upStream.bits), 2)
		queue(0).io.in			<> upStream
		queue(stage-1).io.out	<> downStream

		var i = 0
		for (i <- 0 until stage-1) {
			queue(i).io.out   <> queue(i+1).io.in
		}

		downStream
	}
}