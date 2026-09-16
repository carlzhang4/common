package common.connection

import chisel3._
import chisel3.util._
import common.storage._
import common.axi.HasLast
import common.ToZero

object XArbiter{
    def apply[T<:Data](num:Int)(gen:T, n:Int) = {
        Seq.fill(num)(Module(new XArbiter(gen,n)))
    }
    def apply[T<:Data](gen:T, n:Int, exportIdx:Boolean=false) = {
        Module(new XArbiter(gen,n,exportIdx))
    }
    def apply[T<:Data](seq:Seq[Int])(ins:Seq[DecoupledIO[T]],out:DecoupledIO[T]) = {
        val gen = chiselTypeOf(out.bits)
        def connect(node:XArbiter[T], seq:Seq[Int]):Seq[XArbiter[T]] = {
            val num = seq(0)
            val fanout = seq(1)
            val leaf_nodes = Seq.fill(num)(Module(new XArbiter(gen, fanout)))

            for(i<-0 until num){
                leaf_nodes(i).io.out	<> node.io.in(i)
            }
            if(seq.size>1){
                return leaf_nodes
            }else{
                return leaf_nodes.foldLeft(Seq[XArbiter[T]]())((s,a) => s++connect(a,seq.drop(1)))
            }
        }
        val node		= Module(new XArbiter(gen,seq(0)))
        val leaf_nodes	= connect(node,seq)
        node.io.out		<> out
        for(i <-0 until leaf_nodes.size){
            for(j <-0 until seq.last){
                val index	= i*seq.last+j
                ins(index)	<> leaf_nodes(i).io.in(j)
            }
        }
    }

    class XArbiter[T<:Data](val gen:T, val n:Int, exportIdx:Boolean=false) extends Module{
        val io = IO(new Bundle{
            val in = Vec(n,Flipped(Decoupled(gen)))
            val out = Decoupled(gen)
            val idx = if (exportIdx) {Some(Valid(UInt(log2Up(n).W)))} else None
        })
        val in	= {
            for(i<-0 until n)yield{
                val tmp = RegSlice(io.in(i))
                tmp
            }	
        }
        val out = Wire(Decoupled(gen))

        val grant_index		= GrantIndex(Cat(in.map(_.valid).reverse), out.fire())

        out.valid			:= 0.U
        out.bits			:= in(0).bits
        for(i <- 0 until n){
            in(i).ready	:=	0.U
            when(grant_index === i.U){
                in(i).ready		:= out.ready
                out.valid		:= in(i).valid
                out.bits 		:= in(i).bits
            }
        }
        io.out	<> RegSlice(out)

        // Export index
        if (exportIdx) {
            io.idx.get.valid	:= out.fire
            io.idx.get.bits		:= grant_index
        }
    }
}


object SerialArbiter{
    def apply[T<:HasLast](num:Int)(gen:T, n:Int) = {
        Seq.fill(num)(Module(new SerialArbiter(gen,n,false)))
    }
    def apply[T<:HasLast](num:Int)(gen:T, n:Int, exportIdx:Boolean=false) = {
        Seq.fill(num)(Module(new SerialArbiter(gen,n,exportIdx)))
    }
    def apply[T<:HasLast](gen:T, n:Int) = {
        Module(new SerialArbiter(gen,n,false))
    }
    def apply[T<:HasLast](gen:T, n:Int, exportIdx:Boolean) = {
        Module(new SerialArbiter(gen,n,exportIdx))
    }
    
    class SerialArbiter[T<:HasLast](val gen:T, val n:Int, exportIdx:Boolean=false) extends Module{
        val io = IO(new Bundle{
            val in = Vec(n, Flipped(Decoupled(gen)))
            val out = Decoupled(gen)
            val idx = if (exportIdx) {Some(Valid(UInt(log2Up(n).W)))} else None
        })

        val in	= {
            for(i<-0 until n)yield{
                val tmp = RegSlice(io.in(i))
                tmp
            }	
        }
        val out = Wire(Decoupled(gen))

        val grant_index		= GrantIndex(Cat(in.map(_.valid).reverse), out.fire() && out.bits.last===1.U)

        val is_head 		= RegInit(UInt(1.W),1.U)
        val idx				= Wire(UInt(log2Up(n).W))
        val last_idx		= RegInit(UInt(log2Up(n).W),0.U)

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

        // Export index
        if (exportIdx) {
            io.idx.get.valid	:= out.fire && is_head.asBool
            io.idx.get.bits		:= grant_index
        }
    }
}

object CompositeArbiter{
    def apply[TMeta<:Data,TData<:HasLast](genMeta:TMeta, genData:TData, n:Int, exportIdx:Boolean=false) = {
        Module(new CompositeArbiter(genMeta,genData,n,exportIdx))
    }

    class CompositeArbiter[TMeta<:Data,TData<:HasLast](val genMeta:TMeta, val genData:TData, val n:Int, val exportIdx:Boolean=false)extends Module{
        val io = IO(new Bundle{
            val in_meta 	= Vec(n,Flipped(Decoupled(genMeta)))
            val in_data 	= Vec(n,Flipped(Decoupled(genData)))
            val out_meta	= Decoupled(genMeta)
            val out_data	= Decoupled(genData)
            val idx			= if (exportIdx) {Some(Valid(UInt(log2Up(n).W)))} else None
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

        val grant_index		= GrantIndex(Cat(in_meta.map(_.valid).reverse), out_data.fire()&&out_data.bits.last===1.U)
        
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

        // Export inner idx
        
        if (exportIdx) {
            io.idx.get.valid	:= out_meta.fire()
            io.idx.get.bits		:= grant_index
        }

    }
}

object CompositeReadArbiter{
    // Similar to CompositeArbiter, but the data input is flipped (for AXI read channels)

    def apply[TMeta<:Data,TData<:HasLast](genMeta:TMeta, genData:TData, n:Int, exportIdx:Boolean=false, maxOutstanding:Int=256) = {
        Module(new CompositeReadArbiter(genMeta,genData,n,exportIdx,maxOutstanding))
    }

    class CompositeReadArbiter[TMeta<:Data,TData<:HasLast](val genMeta:TMeta, val genData:TData, val n:Int, val exportIdx:Boolean=false, val maxOutstanding:Int=256)extends Module{
        val io = IO(new Bundle{
            val in_meta 	= Vec(n,Flipped(Decoupled(genMeta)))
            val in_data 	= Vec(n,Decoupled(genData))
            val out_meta	= Decoupled(genMeta)
            val out_data	= Flipped(Decoupled(genData))
            val idx			= if (exportIdx) {Some(Valid(UInt(log2Up(n).W)))} else None
        })

        dontTouch(io.in_meta)
        dontTouch(io.in_data)
        dontTouch(io.out_meta)
        dontTouch(io.out_data)

        val reqFifo = XQueue(UInt(log2Up(n).W), maxOutstanding)

        val inMeta	= Wire(Vec(n, Decoupled(genMeta)))
        val outMeta	= Wire(Decoupled(genMeta))
        val inData	= Wire(Vec(n, Decoupled(genData)))
        val outData	= Wire(Decoupled(genData))

        val outDataRegSlice	= Module(new RegSlice(genData))
        outDataRegSlice.io.upStream		<> io.out_data
        outDataRegSlice.io.downStream	<> outData
        io.out_meta <> RegSlice(outMeta)

        for (i <- 0 until n) {
            io.in_data(i)   <> RegSlice(inData(i))
            val regSliceInMeta  = Module(new RegSlice(genMeta))
            regSliceInMeta.io.upStream  <> io.in_meta(i)
            regSliceInMeta.io.downStream.ready := inMeta(i).ready && reqFifo.io.in.ready
            inMeta(i).valid := regSliceInMeta.io.downStream.valid && reqFifo.io.in.ready
            inMeta(i).bits  := regSliceInMeta.io.downStream.bits
        }

        val metaAbt = XArbiter(genMeta, n, exportIdx=true)

        metaAbt.io.in   <> inMeta
        outMeta         <> metaAbt.io.out

        reqFifo.io.in.valid := metaAbt.io.idx.get.valid
        reqFifo.io.in.bits  := metaAbt.io.idx.get.bits

        for (i <- 0 until n) {
            inData(i).bits  := outData.bits
            inData(i).valid := outData.valid && reqFifo.io.out.valid && (reqFifo.io.out.bits === i.U)
        }
        outData.ready := inData(reqFifo.io.out.bits).ready && reqFifo.io.out.valid
        
        reqFifo.io.out.ready := outData.fire && outData.bits.last.asBool
    }
}

object XArbiterWithInputId {
    def apply[T<:Data](num:Int)(gen:T, n:Int) = {
        Seq.fill(num)(Module(new XArbiterWithInputId(gen,n)))
    }
    def apply[T<:Data](gen:T, n:Int) = {
        Module(new XArbiterWithInputId(gen,n))
    }

    class XArbiterWithInputId[T<:Data](val gen:T, val n:Int) extends Module {
        val io = IO(new Bundle{
            val in = Vec(n,Flipped(Decoupled(gen)))
            val out = Decoupled(gen)
            val idx = Flipped(Decoupled(UInt(log2Up(n).W)))
        })
        val in = Wire(Vec(n, Decoupled(gen)))

        for (i<-0 until n) {
            val regSlice	= Module(new RegSlice(gen))
            regSlice.io.upStream	<> io.in(i)
            regSlice.io.downStream	<> in(i)
        }	

        val out = Wire(Decoupled(gen))

        io.idx.ready	:= in(io.idx.bits).valid & out.ready

        out.valid			:= 0.U
        out.bits			:= in(0).bits
        for(i <- 0 until n){
            in(i).ready	:=	0.U
            when(io.idx.bits === i.U){
                in(i).ready		:= out.ready & io.idx.valid
                out.valid		:= in(i).valid & io.idx.valid
                out.bits 		:= in(i).bits
            }
        }
        io.out	<> RegSlice(out)
    }
}