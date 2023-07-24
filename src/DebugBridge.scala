package common

import chisel3._
import chisel3.util._

/*
 * To use debug bridge, you should make sure that:
 * 1. This module is instantiated in the top module;
 * 2. Top module should be of type MultiIOModule.
 * You only need to instantiate this module in the top module. No extra wire connection is required.
 */

class DebugBridge(IP_CORE_NAME : String="DebugBridge") extends BlackBox {
	override val desiredName = IP_CORE_NAME
	val io = IO(new Bundle{
		val clk = Input(Clock())
		val S_BSCAN_drck = Input(UInt(1.W))
		val S_BSCAN_shift = Input(UInt(1.W))
		val S_BSCAN_tdi = Input(UInt(1.W))
		val S_BSCAN_update = Input(UInt(1.W))
		val S_BSCAN_sel = Input(UInt(1.W))
		val S_BSCAN_tdo = Output(UInt(1.W))
		val S_BSCAN_tms = Input(UInt(1.W))
		val S_BSCAN_tck = Input(UInt(1.W))
		val S_BSCAN_runtest = Input(UInt(1.W))
		val S_BSCAN_reset = Input(UInt(1.W))
		val S_BSCAN_capture = Input(UInt(1.W))
		val S_BSCAN_bscanid_en = Input(UInt(1.W))
	})

	def getTCL() = {
		val s1 = f"\ncreate_ip -name debug_bridge -vendor xilinx.com -library ip -version 3.0 -module_name ${desiredName}\n"
		var s2 = f"set_property -dict [list CONFIG.C_DESIGN_TYPE {1}] [get_ips ${desiredName}]\n"
		println(s1 + s2)
	}
}

object DebugBridge {
    var objName = ""

    def apply(clk : Clock, IP_CORE_NAME : String="DebugBridge") = {
        this.objName = IP_CORE_NAME
        val S_BSCAN = chisel3.experimental.IO.apply(new Bundle{
            val drck 		= Input(UInt(1.W))
            val shift 		= Input(UInt(1.W))
            val tdi			= Input(UInt(1.W))
            val update		= Input(UInt(1.W))
            val sel			= Input(UInt(1.W))
            val tdo			= Output(UInt(1.W))
            val tms			= Input(UInt(1.W))
            val tck			= Input(UInt(1.W))
            val runtest		= Input(UInt(1.W))
            val reset		= Input(UInt(1.W))
            val capture		= Input(UInt(1.W))
            val bscanid_en	= Input(UInt(1.W))
        })

        val instDebugBridge = Module(new DebugBridge(IP_CORE_NAME=IP_CORE_NAME))
        instDebugBridge.io.clk	                := clk
        instDebugBridge.io.S_BSCAN_drck		    <> S_BSCAN.drck
        instDebugBridge.io.S_BSCAN_shift	    <> S_BSCAN.shift
        instDebugBridge.io.S_BSCAN_tdi		    <> S_BSCAN.tdi
        instDebugBridge.io.S_BSCAN_update	    <> S_BSCAN.update
        instDebugBridge.io.S_BSCAN_sel		    <> S_BSCAN.sel
        instDebugBridge.io.S_BSCAN_tdo		    <> S_BSCAN.tdo
        instDebugBridge.io.S_BSCAN_tms		    <> S_BSCAN.tms
        instDebugBridge.io.S_BSCAN_tck		    <> S_BSCAN.tck
        instDebugBridge.io.S_BSCAN_runtest	    <> S_BSCAN.runtest
        instDebugBridge.io.S_BSCAN_reset	    <> S_BSCAN.reset
        instDebugBridge.io.S_BSCAN_capture	    <> S_BSCAN.capture
        instDebugBridge.io.S_BSCAN_bscanid_en	<> S_BSCAN.bscanid_en

        instDebugBridge
    }
}