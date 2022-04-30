package common

import chisel3._
import chisel3.util._

class SV_QDMA extends BlackBox(){
	val io = IO(new Bundle{
		val pci_exp_txp               = Output(UInt(16.W))
		val pci_exp_txn               = Output(UInt(16.W))
		val pci_exp_rxp               = Input(UInt(16.W))
		val pci_exp_rxn               = Input(UInt(16.W))
		
		val sys_clk_p                 = Input(UInt(1.W))
		val sys_clk_n                 = Input(UInt(1.W))
		val sys_rst_n                 = Input(UInt(1.W))

		val pcie_clk                  = Output(Clock())
		val pcie_aresetn              = Output(UInt(1.W))
		
		val user_clk                  = Input(Clock())
		val user_aresetn              = Input(UInt(1.W))
		val soft_reset_n              = Input(UInt(1.W))

		val lite_control              = Output(UInt(16384.W))
		val lite_status               = Input(UInt(16384.W))
		val bridge_control            = Output(UInt(16384.W))
		val bridge_status             = Input(UInt(16384.W))


		val h2c_cmd_addr              = Input(UInt(64.W))
		val h2c_cmd_len               = Input(UInt(32.W))
		val h2c_cmd_eop               = Input(UInt(1.W))
		val h2c_cmd_sop               = Input(UInt(1.W))
		val h2c_cmd_mrkr_req          = Input(UInt(1.W))
		val h2c_cmd_sdi               = Input(UInt(1.W))
		val h2c_cmd_qid               = Input(UInt(11.W))
		val h2c_cmd_error             = Input(UInt(1.W))
		val h2c_cmd_func              = Input(UInt(8.W))
		val h2c_cmd_cidx              = Input(UInt(16.W))
		val h2c_cmd_port_id           = Input(UInt(3.W))
		val h2c_cmd_no_dma            = Input(UInt(1.W))
		val h2c_cmd_valid             = Input(UInt(1.W))
		val h2c_cmd_ready             = Output(UInt(1.W))
		val c2h_cmd_addr              = Input(UInt(64.W))
		val c2h_cmd_qid               = Input(UInt(11.W))
		val c2h_cmd_error             = Input(UInt(1.W))
		val c2h_cmd_func              = Input(UInt(8.W))
		val c2h_cmd_port_id           = Input(UInt(3.W))
		val c2h_cmd_pfch_tag          = Input(UInt(7.W))
		val c2h_cmd_len               = Input(UInt(32.W)) //
		val c2h_cmd_valid             = Input(UInt(1.W))
		val c2h_cmd_ready             = Output(UInt(1.W))

		val h2c_data_data             = Output(UInt(512.W))
		val h2c_data_tcrc             = Output(UInt(32.W))
		val h2c_data_tuser_qid        = Output(UInt(11.W))
		val h2c_data_tuser_port_id    = Output(UInt(3.W))
		val h2c_data_tuser_err        = Output(UInt(1.W))
		val h2c_data_tuser_mdata      = Output(UInt(32.W))
		val h2c_data_tuser_mty        = Output(UInt(6.W))
		val h2c_data_tuser_zero_byte  = Output(UInt(1.W))
		val h2c_data_last             = Output(UInt(1.W))
		val h2c_data_valid            = Output(UInt(1.W))
		val h2c_data_ready            = Input(UInt(1.W))
		val c2h_data_data             = Input(UInt(512.W))
		val c2h_data_tcrc             = Input(UInt(32.W))
		val c2h_data_ctrl_marker      = Input(UInt(1.W))
		val c2h_data_ctrl_ecc         = Input(UInt(7.W))
		val c2h_data_ctrl_len         = Input(UInt(32.W))
		val c2h_data_ctrl_port_id     = Input(UInt(3.W))
		val c2h_data_ctrl_qid         = Input(UInt(11.W))
		val c2h_data_ctrl_has_cmpt    = Input(UInt(1.W))
		val c2h_data_mty              = Input(UInt(6.W))
		val c2h_data_last             = Input(UInt(1.W))
		val c2h_data_valid            = Input(UInt(1.W))
		val c2h_data_ready            = Output(UInt(1.W))
	})
}

class QDMA extends RawModule{
	val io = IO(new Bundle{
		val pci_exp_txp               	= Output(UInt(16.W))
		val pci_exp_txn               	= Output(UInt(16.W))
		val pci_exp_rxp               	= Input(UInt(16.W))
		val pci_exp_rxn               	= Input(UInt(16.W))
		
		val sys_clk_p                 	= Input(UInt(1.W))
		val sys_clk_n                 	= Input(UInt(1.W))
		val sys_rst_n                 	= Input(UInt(1.W))

		val pcie_clk                  	= Output(Clock())
		val pcie_aresetn              	= Output(UInt(1.W))
		
		val user_clk                  	= Input(Clock())
		val user_aresetn              	= Input(UInt(1.W))
		val soft_reset_n              	= Input(UInt(1.W))


		val lite_control			  	= Output(Vec(512,UInt(32.W)))
		val lite_status			  		= Input(Vec(512,UInt(32.W)))
		val bridge_control			  	= Output(Vec(32,UInt(512.W)))
		val bridge_status			  	= Input(Vec(32,UInt(512.W)))
	})
	val sv = Module(new SV_QDMA)
	sv.io.pci_exp_txp		<> io.pci_exp_txp
	sv.io.pci_exp_txn		<> io.pci_exp_txn
	sv.io.pci_exp_rxp		<> io.pci_exp_rxp
	sv.io.pci_exp_rxn		<> io.pci_exp_rxn
	
	sv.io.sys_clk_p			<> io.sys_clk_p
	sv.io.sys_clk_n			<> io.sys_clk_n
	sv.io.sys_rst_n			<> io.sys_rst_n

	sv.io.pcie_clk			<> io.pcie_clk
	sv.io.pcie_aresetn		<> io.pcie_aresetn
	sv.io.user_clk			<> io.user_clk
	sv.io.user_aresetn		<> io.user_aresetn
	sv.io.soft_reset_n		<> io.soft_reset_n
	for (i<-0 until 512){
		io.lite_control(i)			<> sv.io.lite_control(i*32+31,i*32)
	}
	sv.io.lite_status				:= io.lite_status.asUInt()
	for (i<-0 until 32){
		io.bridge_control(i)		<> sv.io.bridge_control(i*512+511,i*512)
	}
	sv.io.bridge_status				:= io.bridge_status.asUInt()
	
}