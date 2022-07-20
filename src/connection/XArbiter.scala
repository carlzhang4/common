package common.connection

import chisel3._
import chisel3.util._
import common.storage.RegSlice
import common.axi.HasLast
import common.ToZero

object XArbiter{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new Arbiter(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new Arbiter(gen,n))
	}
}


object SerialArbiter{
	def apply[T<:HasLast](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new SerialArbiter(gen,n)))
	}
	def apply[T<:HasLast](gen:T, n:Int) = {
		Module(new SerialArbiter(gen,n))
	}
	class SerialArbiter[T<:HasLast](val gen:T, val n:Int) extends Module{
		val io = IO(new Bundle{
			val in = Vec(n, Flipped(Decoupled(gen)))
			val out = Decoupled(gen)
		})

		val in	= {
			for(i<-0 until n)yield{
				val tmp = RegSlice(io.in(i))
				tmp
			}	
		}
		val out = Wire(Decoupled(gen))

		val req				= Cat(in.map(_.valid).reverse)//with reverse, port 0 is at the LSB
		val base			= RegInit(UInt(n.W),1.U)
		val double_req		= Cat(req,req)
		val double_grant	= double_req & ~(double_req-base)
		val grant			= double_grant(n-1,0) | double_grant(2*n-1,n)
		val grant_index		= OHToUInt(grant)

		val is_head 		= RegInit(UInt(1.W),1.U)
		val idx				= Wire(UInt(log2Up(n).W))
		val last_idx		= RegInit(UInt(log2Up(n).W),0.U)

		when(out.fire() && out.bits.last===1.U){
			base			:= Cat(base(n-2,0),base(n-1))
		}

		when(is_head===1.U){
			idx				:= grant_index
		}.otherwise{
			idx 			:= last_idx
		}
		
		out.valid			:= 0.U
		out.bits			:= in(0).bits
		for(i <- 0 until n){
			in(i).ready	:=	0.U
			when(idx === i.U){
				in(i).ready		:= out.ready
				out.valid		:= in(i).valid
				out.bits 		:= in(i).bits
			}
		}
		when(out.fire() && out.bits.last===1.U){
			is_head	:= 1.U
		}.elsewhen(out.fire()){
			is_head := 0.U
		}

		when(out.fire()){
			last_idx		:= idx
		}
		io.out	<> RegSlice(out)
	}
}

object CompositeArbiter{
	def apply[TMeta<:Data,TData<:HasLast](genMeta:TMeta, genData:TData, n:Int) = {
		Module(new CompositeArbiter(genMeta,genData,n))
	}

	class CompositeArbiter[TMeta<:Data,TData<:HasLast](val genMeta:TMeta, val genData:TData, val n:Int)extends Module{
		val io = IO(new Bundle{
			val in_meta 	= Vec(n,Flipped(Decoupled(genMeta)))
			val in_data 	= Vec(n,Flipped(Decoupled(genData)))
			val out_meta	= Decoupled(genMeta)
			val out_data	= Decoupled(genData)
		})

		val in_meta	= {
			for(i<-0 until n)yield{
				val tmp = RegSlice(io.in_meta(i))
				tmp
			}	
		}
		val in_data	= {
			for(i<-0 until n)yield{
				val tmp = RegSlice(io.in_data(i))
				tmp
			}	
		}

		val out_meta = Wire(Decoupled(genMeta))
		val out_data = Wire(Decoupled(genData))

		val req				= Cat(in_meta.map(_.valid).reverse)//with reverse, port 0 is at the LSB
		val base			= RegInit(UInt(n.W),1.U)
		val double_req		= Cat(req,req)
		val double_grant	= double_req & ~(double_req-base)
		val grant			= double_grant(n-1,0) | double_grant(2*n-1,n)
		val grant_index		= OHToUInt(grant)

		val last_idx		= RegInit(UInt(log2Up(n).W),0.U)

		val sFirst :: sMiddle :: Nil = Enum(2)
		val state 	= RegInit(sFirst)
		switch(state){
			is(sFirst){
				last_idx		:= grant_index
				when(out_meta.fire()){
					when(out_data.fire && out_data.bits.last===1.U){
						state 		:= sFirst
					}.otherwise{
						state 		:= sMiddle
					}
				}
			}
			is(sMiddle){
				when(out_data.fire() && out_data.bits.last===1.U){
					state		:= sFirst
				}
			}
		}
		when(out_data.fire()){
			base		:= Cat(base(n-2,0),base(n-1))
		}

		out_meta.valid			:= 0.U
		ToZero(out_meta.bits)
		out_data.valid			:= 0.U
		ToZero(out_data.bits)
		for(i<-0 until n){
			in_meta(i).ready	:= 0.U
			in_data(i).ready	:= 0.U
			when(state===sFirst && grant_index === i.U){
				in_meta(i).ready	:= out_meta.ready
				out_meta.valid		:= in_meta(i).valid
				out_meta.bits		:= in_meta(i).bits

				in_data(i).ready	:= out_data.ready & out_meta.fire()
				out_data.valid		:= in_data(i).valid & out_meta.fire()
				out_data.bits		:= in_data(i).bits
			}.elsewhen(state===sMiddle && last_idx === i.U){
				in_data(i).ready	:= out_data.ready
				out_data.valid		:= in_data(i).valid
				out_data.bits		:= in_data(i).bits
			}
		}
		io.out_meta	<> RegSlice(out_meta)
		io.out_data	<> RegSlice(out_data)

	}
}