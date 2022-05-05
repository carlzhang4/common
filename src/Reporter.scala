package common

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

abstract class Reporter{
	def MAX_NUM = 32
	var msgs = new Array[String](MAX_NUM)
	var cur_idx = 0

	def report(cond:Bool,msg:String)={
		val unique_id = "report_"+cur_idx.toString
		BoringUtils.addSource(cond,unique_id,true,true)
		msgs(cur_idx) = msg
		cur_idx = cur_idx + 1
		if(cur_idx>=MAX_NUM){
			println("Error, report number exceeds")
		}
	}

	def get_reports(sigs:Seq[Bool])={
		for(i<-0 until cur_idx){
			val unique_id = "report_"+i.toString
			BoringUtils.addSink(sigs(i),unique_id)
		}
	}

	def print_msgs()={
		for(i<-0 until cur_idx){
			println("Report "+i.toString+":"+msgs(i))
		}
	}
}