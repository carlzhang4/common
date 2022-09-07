package common

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import firrtl.annotations.SingleTargetAnnotation
import firrtl.annotations.ReferenceTarget
import chisel3.experimental.{annotate,ChiselAnnotation}
import common.axi.HasLast

case class TestMetadataAnno(
		eles:Array[Element],
		num:Int,
		width:Int,
		offset:Int,
		target:ReferenceTarget) extends SingleTargetAnnotation[ReferenceTarget] {
	def duplicate(n: ReferenceTarget) = this //this.copy(n)

	def get_str(path:String,user_msg:String,meta:String) = {
		var msg = ""
		if(user_msg!=""){
			msg = "<"+user_msg+">"
		}//msg is empty or surrounded by <>
		var mods = path.split('.').drop(1)
		var str = msg+meta
		if(mods.last.startsWith("_")){
			if(msg==""){
				throw new Exception("When watching temperal variable, please add msg to recognize them")
			}
			mods 	= mods.dropRight(1)
			str		= "."+str
		}else if(meta == "[fire]" || meta == "[fireLast]"){
			mods 	= mods.dropRight(1)
			str		= "."+str
		}
		mods.mkString(".")+str
	}
	println(f"//Report width ${width}:")

	if(width==32){
		for(i<-0 until num){//generate mapping
			val index	= i+offset
			if(eles(i).fix_str != "" && !eles(i).fix_str.startsWith("_")){
				println(f"int ${eles(i).fix_str}%-60s    = ${index}%d;")
			}
		}
		for(i<-0 until num){
			val str		= get_str(eles(i).data.pathName, eles(i).msg, eles(i).meta)
			val index	= i+offset
			println("printf(\"" + f"${str}%-60s: " + "%u\\n\"" + f", bar[${index}%d]);")
		}
	}
	if(width==1){
		var bit_index = 0
		var index = offset
		for(i<-0 until num){
			val str		= get_str(eles(i).data.pathName, eles(i).msg, eles(i).meta)
			println("printf(\"" + f"${str}%-60s: " + "%u\\n\"" + f", (bar[${index}%d] >> ${bit_index}) & 1);")
			bit_index = bit_index + 1
			if(bit_index == 32){
				bit_index = 0
				index = index + 1
			}
		}
	}
	println()
}

class Element(var data:Data,var msg:String,var meta:String,var fix_str:String){

}

object Collector{
	def MAX_NUM = 512
	val widths	= Set(1,32)
	var eles	= widths.map(_->new Array[Element](MAX_NUM)).toMap
	var idxs	= collection.mutable.Map(widths.map(_->0).toSeq:_*)

	def add_signal(data:UInt,full_data:UInt,msg:String,meta:String="",fix_str:String="") = {
		val width = data.getWidth
		val unique_id = "report_w"+width+"_"+idxs(width)
		BoringUtils.addSource(data,unique_id,true,true)
		eles(width)(idxs(width)) = new Element(full_data,msg,meta,fix_str)
		idxs(width) = idxs(width)+1
		if(idxs(width) >= MAX_NUM){
			throw new Exception("Report number exceeds")
		}
	}

	def report(data:UInt, msg:String="", fix_str:String="") = {		
		val width = data.getWidth
		if(width==64){
			add_signal(data(63,32),data,msg,"[high]",fix_str+"_HIGH")
			add_signal(data(31,0),data,msg,"[low]",fix_str+"_LOW")
		}else if(widths.contains(width)){
			add_signal(data,data,msg,fix_str=fix_str)
		}else{
			throw new Exception("report width must be either 1/32/64") 
		}
	}

	def fire(data:DecoupledIO[Data],msg:String="") = {
		val counter 	= RegInit(UInt(32.W),0.U)
		when(data.fire()){
			counter		:= counter+1.U
		}
		add_signal(counter,data.valid,msg,"[fire]")
	}

	def fireLast(data:DecoupledIO[HasLast],msg:String="") = {
		val counter 	= RegInit(UInt(32.W),0.U)
		when(data.fire()&data.bits.last.asBool()){
			counter		:= counter+1.U
		}
		add_signal(counter,data.valid,msg,"[fireLast]")
	}

	def trigger(en:Bool,msg:String="") = {
		val t = RegInit(UInt(1.W),0.U)
		when(en){
			t	:= 1.U
		}.otherwise{
			t	:= t
		}
		add_signal(t,en,msg,"[trigger]")
	}

	def count(en:Bool, msg:String="", width:Int=32) = {
		val counter 	= RegInit(UInt(width.W),0.U)
		when(en){
			counter		:= counter+1.U
		}
		if(width==64){
			add_signal(counter(63,32),en,msg,"[cnt_high]")
			add_signal(counter(31,0),en,msg,"[cnt_low]")
		}else if(width==32){
			add_signal(counter,en,msg,"[cnt]")
		}else{
			throw new Exception("count width must be either 32/64") 
		}
	}

	def connect_to_status_reg(status_reg:Vec[UInt],offset:Int) = {
		var cur_offset = offset
		val sigs_32 = Wire(Vec(idxs(32),UInt(32.W)))
		ToZero(sigs_32)
		for(i<-0 until idxs(32)){
			val unique_id = "report_w32_"+i.toString
			BoringUtils.addSink(sigs_32(i),unique_id)
			status_reg(cur_offset+i)	:= sigs_32(i)
		}
		if(idxs(32) != 0){
			annotate(new ChiselAnnotation {
			def toFirrtl = TestMetadataAnno(
				eles(32),
				idxs(32),
				32,
				offset,
				eles(32)(0).data.toTarget)
			})	
		}

		cur_offset = cur_offset+idxs(32)
		val sigs_1 = Wire(Vec(Math.round_up(idxs(1),32),UInt(1.W)))
		ToZero(sigs_1)
		for(i<-0 until idxs(1)){
			val unique_id = "report_w1_"+i.toString
			BoringUtils.addSink(sigs_1(i),unique_id)
		}	
		for(i<-0 until Math.round_up(idxs(1),32)/32){
			status_reg(cur_offset+i)	:= sigs_1.asUInt()(i*32+31,i*32)
		}
		if(idxs(1)!=0){
			annotate(new ChiselAnnotation {
			def toFirrtl = TestMetadataAnno(
				eles(1),
				idxs(1),
				1,
				cur_offset,
				eles(1)(0).data.toTarget)
			})	
		}
	}
}