package common

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

class CMACPin extends Bundle{
	val tx_p 		= Output(UInt(4.W))
	val tx_n 		= Output(UInt(4.W))
	val rx_p 		= Input(UInt(4.W))
	val rx_n 		= Input(UInt(4.W))
	val gt_clk_p   = Input(Clock())
	val gt_clk_n   = Input(Clock())
}

class DDRPin extends Bundle{
    val ddr0_sys_100M_p=Input(Clock())  
    val ddr0_sys_100M_n=Input(Clock()) 
    val act_n       =Output(UInt(1.W))                      
    val adr         =Output(UInt(17.W))               
    val ba          =Output(UInt(2.W))             
    val bg          =Output(UInt(2.W))               
    val cke         =Output(UInt(1.W))               
    val odt         =Output(UInt(1.W))            
    val cs_n        =Output(UInt(1.W))           
    val ck_t        =Output(UInt(1.W))             
    val ck_c        =Output(UInt(1.W))            
    val reset_n     =Output(UInt(1.W))                      
    val parity      =Output(UInt(1.W))                      
    val dq          =Analog(72.W)               
    val dqs_t       =Analog(18.W)              
    val dqs_c       =Analog(18.W)	
}