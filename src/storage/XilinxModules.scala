package common.storage

import chisel3._
import chisel3.util._


class SV_STREAM_FIFO(DATA_WIDTH:Int, DEPTH_BIT:Int=8, RAM_TYPE:String="auto", CLOCKING_MODE:String="common_clock")extends BlackBox(
	Map(
		"DATA_WIDTH"->DATA_WIDTH,
		"DEPTH_BIT"->DEPTH_BIT,
		"RAM_TYPE"->RAM_TYPE,
		"CLOCKING_MODE"->CLOCKING_MODE,
	)
){
	val io = IO(new Bundle {
		val m_clk                       = Input(Clock())
		val s_clk                       = Input(Clock())
		val reset_n                     = Input(UInt(1.W))

		val in_data                     = Input(UInt(DATA_WIDTH.W))
		val in_valid                    = Input(UInt(1.W))
		val in_ready                    = Output(UInt(1.W))
		val out_data                    = Output(UInt(DATA_WIDTH.W))
		val out_valid                   = Output(UInt(1.W))
		val out_ready                   = Input(UInt(1.W))
		val rd_count                    = Output(UInt((DEPTH_BIT+1).W))
		val wr_count                    = Output(UInt((DEPTH_BIT+1).W))
	})
	
}


class SV_RAM(DATA_WIDTH:Int, DEPTH_BIT:Int=8, MEMORY_TYPE:String="auto", LATENCY:Int=2, USE_MUSK:Int=0, WRITE_MODE:String="no_change")extends BlackBox(
	Map(
		"DATA_WIDTH"->DATA_WIDTH,
		"DEPTH_BIT"->DEPTH_BIT,
		"MEMORY_TYPE"->MEMORY_TYPE,
		"LATENCY"->LATENCY,
		"USE_MUSK"->USE_MUSK,
		"WRITE_MODE"->WRITE_MODE,
	)
){
	val io = IO(new Bundle {
		val clk                         = Input(Clock())
		val usr_rst                     = Input(UInt(1.W))
		val addr_a                      = Input(UInt(DEPTH_BIT.W))
		val addr_b                      = Input(UInt(DEPTH_BIT.W))
		val wr_en_a                     = Input(UInt((DATA_WIDTH/8).W))
		val data_in_a                   = Input(UInt(DATA_WIDTH.W))
		val data_out_a                  = Output(UInt(DATA_WIDTH.W))
		val data_out_b                  = Output(UInt(DATA_WIDTH.W))
	})
	
}