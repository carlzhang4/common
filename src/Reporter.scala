package common

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

abstract class Reporter(unique_prefix:String="default"){
	def MAX_NUM = 32
	var msgs = new Array[String](MAX_NUM)
	var cur_idx = 0
	var data_width = 0
	var OFFSET = 0

	def report(data:Data,msg:String)={
		if(cur_idx == 0){
			data_width = data.getWidth
			OFFSET = MAX_NUM/32*data_width
		}else{
			if(data_width != data.getWidth){
				println("Error, data_width not match")
			}
		}
		val unique_id = unique_prefix+"_report_"+cur_idx.toString
		BoringUtils.addSource(data,unique_id,true,true)
		msgs(cur_idx) = msg
		cur_idx = cur_idx + 1
		if(cur_idx>=MAX_NUM){
			println("Error, report number exceeds")
		}
	}

	def get_reports(sigs:Seq[Data])={
		for(i<-0 until cur_idx){
			val unique_id = unique_prefix+"_report_"+i.toString
			BoringUtils.addSink(sigs(i),unique_id)
		}
	}

	def print_msgs()={
		println(unique_prefix+f"Repoter (width=${data_width}, MAX_NUM=${MAX_NUM}):")
		println(f"int offset_${unique_prefix}%sReporter = offset;")
		if(data_width == 1){
			var bit_index = 0
			var reg_index = 0
			for(i<-0 until cur_idx){
				println("printf(\"" + f"${msgs(i)}%-60s:" + "%d\\n\"" + f", (bar[${reg_index}%d+offset_${unique_prefix}%sReporter] >> ${bit_index}) & 1);")
				bit_index = bit_index + 1
				if(bit_index == 32){
					bit_index = 0
					reg_index = reg_index + 1
				}
			}
			println(f"offset+=${MAX_NUM/32};")
		}else{
			for(i<-0 until cur_idx){
				println("printf(\"" + f"${msgs(i)}%-60s:" + "%d\\n\"" + f", bar[${i}%d+offset_${unique_prefix}%sReporter]);")
			}
			println(f"offset+=${MAX_NUM};")
		}
		println()
	}
}

abstract class XCounter(unique_prefix:String="DefaultCounterReporter"){
	def MAX_NUM = 32
	var msgs = new Array[String](MAX_NUM)
	var cur_idx = 0
	var OFFSET = MAX_NUM/32*32

	def record(en:Bool, msg:String)={
		val unique_id	= unique_prefix+"_counter_"+cur_idx.toString
		val counter 	= RegInit(UInt(32.W),0.U)
		when(en){
			counter		:= counter+1.U
		}
		BoringUtils.addSource(counter,unique_id,true,true)
		msgs(cur_idx) = msg
		cur_idx = cur_idx + 1
		if(cur_idx>=MAX_NUM){
			println("Error, report number exceeds")
		}
	}

	def get_counters(counters:Seq[UInt])={
		for(i<-0 until cur_idx){
			val unique_id = unique_prefix+"_counter_"+i.toString
			BoringUtils.addSink(counters(i),unique_id)
		}
		cur_idx
	}

	def print_msgs()={
		println(unique_prefix+f"Counter (width=32, MAX_NUM=${MAX_NUM}):")
		println(f"int offset_${unique_prefix}%sCounter = offset;")
		for(i<-0 until cur_idx){
			println("printf(\"" + f"${msgs(i)}%-60s:" + "%d\\n\"" + f", bar[${i}%d+offset_${unique_prefix}%sCounter]);")
		}
		println(f"offset+=${MAX_NUM};")
		println()
	}
}