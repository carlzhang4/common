package common.partialReconfig

import chisel3._
import chisel3.util._
import common.CMACPin
import common.DDRPin
import common.axi._

class QDMAStaticIO(VIVADO_VERSION:String, PCIE_WIDTH:Int, SLAVE_BRIDGE:Boolean, AXI_BRIDGE:Boolean) extends Bundle{
    val axi_aclk                    = Output(UInt(1.W))
    val axi_aresetn                 = Output(UInt(1.W))

    val m_axib_awid					= if (AXI_BRIDGE){Some(Output(UInt(4.W)))} else None
    val m_axib_awaddr				= if (AXI_BRIDGE){Some(Output(UInt(64.W)))} else None
    val m_axib_awlen				= if (AXI_BRIDGE){Some(Output(UInt(8.W)))} else None
    val m_axib_awsize				= if (AXI_BRIDGE){Some(Output(UInt(3.W)))} else None
    val m_axib_awburst				= if (AXI_BRIDGE){Some(Output(UInt(2.W)))} else None
    val m_axib_awprot				= if (AXI_BRIDGE){Some(Output(UInt(3.W)))} else None
    val m_axib_awlock				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None
    val m_axib_awcache				= if (AXI_BRIDGE){Some(Output(UInt(4.W)))} else None
    val m_axib_awvalid				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None
    val m_axib_awready				= if (AXI_BRIDGE){Some(Input(UInt(1.W)))} else None

    val m_axib_wdata				= if (AXI_BRIDGE){Some(Output(UInt(512.W)))} else None
    val m_axib_wstrb				= if (AXI_BRIDGE){Some(Output(UInt(64.W)))} else None
    val m_axib_wlast				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None
    val m_axib_wvalid				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None
    val m_axib_wready				= if (AXI_BRIDGE){Some(Input(UInt(1.W)))} else None

    val m_axib_bid					= if (AXI_BRIDGE){Some(Input(UInt(4.W)))} else None
    val m_axib_bresp				= if (AXI_BRIDGE){Some(Input(UInt(2.W)))} else None
    val m_axib_bvalid				= if (AXI_BRIDGE){Some(Input(UInt(1.W)))} else None
    val m_axib_bready				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None

    val m_axib_arid					= if (AXI_BRIDGE){Some(Output(UInt(4.W)))} else None
    val m_axib_araddr				= if (AXI_BRIDGE){Some(Output(UInt(64.W)))} else None
    val m_axib_arlen				= if (AXI_BRIDGE){Some(Output(UInt(8.W)))} else None
    val m_axib_arsize				= if (AXI_BRIDGE){Some(Output(UInt(3.W)))} else None
    val m_axib_arburst				= if (AXI_BRIDGE){Some(Output(UInt(2.W)))} else None
    val m_axib_arprot				= if (AXI_BRIDGE){Some(Output(UInt(3.W)))} else None
    val m_axib_arlock				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None
    val m_axib_arcache				= if (AXI_BRIDGE){Some(Output(UInt(4.W)))} else None
    val m_axib_arvalid				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None
    val m_axib_arready				= if (AXI_BRIDGE){Some(Input(UInt(1.W)))} else None

    val m_axib_rid					= if (AXI_BRIDGE){Some(Input(UInt(4.W)))} else None
    val m_axib_rdata				= if (AXI_BRIDGE){Some(Input(UInt(512.W)))} else None
    val m_axib_rresp				= if (AXI_BRIDGE){Some(Input(UInt(2.W)))} else None
    val m_axib_rlast				= if (AXI_BRIDGE){Some(Input(UInt(1.W)))} else None
    val m_axib_rvalid				= if (AXI_BRIDGE){Some(Input(UInt(1.W)))} else None
    val m_axib_rready				= if (AXI_BRIDGE){Some(Output(UInt(1.W)))} else None


    val m_axil_awaddr				= Output(UInt(32.W))
    val m_axil_awvalid				= Output(UInt(1.W))
    val m_axil_awready				= Input(UInt(1.W))

    val m_axil_wdata				= Output(UInt(32.W))
    val m_axil_wstrb				= Output(UInt(4.W))
    val m_axil_wvalid				= Output(UInt(1.W))
    val m_axil_wready				= Input(UInt(1.W))

    val m_axil_bresp				= Input(UInt(2.W))
    val m_axil_bvalid				= Input(UInt(1.W))
    val m_axil_bready				= Output(UInt(1.W))

    val m_axil_araddr				= Output(UInt(32.W))
    val m_axil_arvalid				= Output(UInt(1.W))
    val m_axil_arready				= Input(UInt(1.W))

    val m_axil_rdata				= Input(UInt(32.W))
    val m_axil_rresp				= Input(UInt(2.W))
    val m_axil_rvalid				= Input(UInt(1.W))
    val m_axil_rready				= Output(UInt(1.W))

    val soft_reset_n				= Input(Bool())

    val h2c_byp_in_st_addr			= Input(UInt(64.W))
    val h2c_byp_in_st_len			= Input(UInt(32.W))
    val h2c_byp_in_st_eop			= Input(UInt(1.W))
    val h2c_byp_in_st_sop			= Input(UInt(1.W))
    val h2c_byp_in_st_mrkr_req		= Input(UInt(1.W))
    val h2c_byp_in_st_sdi			= Input(UInt(1.W))
    val h2c_byp_in_st_qid			= Input(UInt(11.W))
    val h2c_byp_in_st_error			= Input(UInt(1.W))
    val h2c_byp_in_st_func			= Input(UInt(8.W))
    val h2c_byp_in_st_cidx			= Input(UInt(16.W))
    val h2c_byp_in_st_port_id		= Input(UInt(3.W))
    val h2c_byp_in_st_no_dma		= Input(UInt(1.W))
    val h2c_byp_in_st_vld			= Input(UInt(1.W))
    val h2c_byp_in_st_rdy			= Output(UInt(1.W))

    val c2h_byp_in_st_csh_addr		= Input(UInt(64.W))
    val c2h_byp_in_st_csh_qid		= Input(UInt(11.W))
    val c2h_byp_in_st_csh_error		= Input(UInt(1.W))
    val c2h_byp_in_st_csh_func		= Input(UInt(8.W))
    val c2h_byp_in_st_csh_port_id	= Input(UInt(3.W))
    val c2h_byp_in_st_csh_pfch_tag	= Input(UInt(7.W))
    val c2h_byp_in_st_csh_vld		= Input(UInt(1.W))
    val c2h_byp_in_st_csh_rdy		= Output(UInt(1.W))

    val s_axis_c2h_tdata			= Input(UInt(512.W))
    val s_axis_c2h_tcrc				= Input(UInt(32.W))
    val s_axis_c2h_ctrl_marker		= Input(UInt(1.W))
    val s_axis_c2h_ctrl_ecc			= Input(UInt(7.W))
    val s_axis_c2h_ctrl_len			= Input(UInt(32.W))
    val s_axis_c2h_ctrl_port_id		= Input(UInt(3.W))
    val s_axis_c2h_ctrl_qid			= Input(UInt(11.W))
    val s_axis_c2h_ctrl_has_cmpt	= Input(UInt(1.W))
    val s_axis_c2h_mty				= Input(UInt(6.W))
    val s_axis_c2h_tlast			= Input(UInt(1.W))
    val s_axis_c2h_tvalid			= Input(UInt(1.W))
    val s_axis_c2h_tready			= Output(UInt(1.W))

    
    val m_axis_h2c_tdata			= Output(UInt(512.W))
    val m_axis_h2c_tcrc				= Output(UInt(32.W))
    val m_axis_h2c_tuser_qid		= Output(UInt(11.W))
    val m_axis_h2c_tuser_port_id	= Output(UInt(3.W))
    val m_axis_h2c_tuser_err		= Output(UInt(1.W))
    val m_axis_h2c_tuser_mdata		= Output(UInt(32.W))
    val m_axis_h2c_tuser_mty		= Output(UInt(6.W))
    val m_axis_h2c_tuser_zero_byte	= Output(UInt(1.W))
    val m_axis_h2c_tlast			= Output(UInt(1.W))
    val m_axis_h2c_tvalid			= Output(UInt(1.W))
    val m_axis_h2c_tready			= Input(UInt(1.W))

    val axis_c2h_status_drop		= Output(UInt(1.W))
    val axis_c2h_status_last		= Output(UInt(1.W))
    val axis_c2h_status_cmp			= Output(UInt(1.W))
    val axis_c2h_status_valid		= Output(UInt(1.W))
    val axis_c2h_status_qid			= Output(UInt(11.W))

    val s_axis_c2h_cmpt_tdata					= Input(UInt(512.W))
    val s_axis_c2h_cmpt_size					= Input(UInt(2.W))
    val s_axis_c2h_cmpt_dpar					= Input(UInt(16.W))
    val s_axis_c2h_cmpt_tvalid					= Input(UInt(1.W))
    val s_axis_c2h_cmpt_tready					= Output(UInt(1.W))
    val s_axis_c2h_cmpt_ctrl_qid				= Input(UInt(11.W))
    val s_axis_c2h_cmpt_ctrl_cmpt_type			= Input(UInt(2.W))
    val s_axis_c2h_cmpt_ctrl_wait_pld_pkt_id	= Input(UInt(16.W))
    val s_axis_c2h_cmpt_ctrl_no_wrb_marker		= if(VIVADO_VERSION=="202101" || VIVADO_VERSION=="202002") Some(Input(UInt(1.W))) else None
    val s_axis_c2h_cmpt_ctrl_port_id			= Input(UInt(3.W))
    val s_axis_c2h_cmpt_ctrl_marker				= Input(UInt(1.W))
    val s_axis_c2h_cmpt_ctrl_user_trig			= Input(UInt(1.W))
    val s_axis_c2h_cmpt_ctrl_col_idx			= Input(UInt(3.W))
    val s_axis_c2h_cmpt_ctrl_err_idx			= Input(UInt(3.W))

    //ignore other
    val h2c_byp_out_rdy							= Input(UInt(1.W))

    //ignore other
    val c2h_byp_out_rdy							= Input(UInt(1.W))

    //ignore other
    val tm_dsc_sts_rdy							= Input(UInt(1.W))

    val dsc_crdt_in_vld							= Input(UInt(1.W))
    val dsc_crdt_in_rdy							= Output(UInt(1.W))
    val dsc_crdt_in_dir							= Input(UInt(1.W))
    val dsc_crdt_in_fence						= Input(UInt(1.W))
    val dsc_crdt_in_qid							= Input(UInt(11.W))
    val dsc_crdt_in_crdt						= Input(UInt(16.W))

    //ignore other
    val qsts_out_rdy							= Input(UInt(1.W))

    val usr_irq_in_vld							= Input(UInt(1.W))
    val usr_irq_in_vec							= Input(UInt(11.W))
    val usr_irq_in_fnc							= Input(UInt(8.W))
    val usr_irq_out_ack							= Output(UInt(1.W))
    val usr_irq_out_fail						= Output(UInt(1.W))

    val s_axib_awid					= if (SLAVE_BRIDGE) {Some(Input(UInt(4.W)))} else None
    val s_axib_awaddr				= if (SLAVE_BRIDGE) {Some(Input(UInt(64.W)))} else None
    val s_axib_awlen				= if (SLAVE_BRIDGE) {Some(Input(UInt(8.W)))} else None
    val s_axib_awsize				= if (SLAVE_BRIDGE) {Some(Input(UInt(3.W)))} else None
    val s_axib_awuser				= if (SLAVE_BRIDGE) {Some(Input(UInt(12.W)))} else None
    val s_axib_awburst				= if (SLAVE_BRIDGE) {Some(Input(UInt(2.W)))} else None
    val s_axib_awregion				= if (SLAVE_BRIDGE) {Some(Input(UInt(4.W)))} else None
    val s_axib_awvalid				= if (SLAVE_BRIDGE) {Some(Input(UInt(1.W)))} else None
    val s_axib_awready				= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None

    val s_axib_wdata				= if (SLAVE_BRIDGE) {Some(Input(UInt(512.W)))} else None
    val s_axib_wstrb				= if (SLAVE_BRIDGE) {Some(Input(UInt(64.W)))} else None
    val s_axib_wuser				= if (SLAVE_BRIDGE) {Some(Input(UInt(64.W)))} else None
    val s_axib_wlast				= if (SLAVE_BRIDGE) {Some(Input(UInt(1.W)))} else None
    val s_axib_wvalid				= if (SLAVE_BRIDGE) {Some(Input(UInt(1.W)))} else None
    val s_axib_wready				= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None

    val s_axib_bid					= if (SLAVE_BRIDGE) {Some(Output(UInt(4.W)))} else None
    val s_axib_bresp				= if (SLAVE_BRIDGE) {Some(Output(UInt(2.W)))} else None
    val s_axib_bvalid				= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None
    val s_axib_bready				= if (SLAVE_BRIDGE) {Some(Input(UInt(1.W)))} else None

    val s_axib_arid					= if (SLAVE_BRIDGE) {Some(Input(UInt(4.W)))} else None
    val s_axib_araddr				= if (SLAVE_BRIDGE) {Some(Input(UInt(64.W)))} else None
    val s_axib_arlen				= if (SLAVE_BRIDGE) {Some(Input(UInt(8.W)))} else None
    val s_axib_arsize				= if (SLAVE_BRIDGE) {Some(Input(UInt(3.W)))} else None
    val s_axib_aruser				= if (SLAVE_BRIDGE) {Some(Input(UInt(12.W)))} else None
    val s_axib_arburst				= if (SLAVE_BRIDGE) {Some(Input(UInt(2.W)))} else None
    val s_axib_arregion				= if (SLAVE_BRIDGE) {Some(Input(UInt(4.W)))} else None
    val s_axib_arvalid				= if (SLAVE_BRIDGE) {Some(Input(UInt(1.W)))} else None
    val s_axib_arready				= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None

    val s_axib_rid					= if (SLAVE_BRIDGE) {Some(Output(UInt(4.W)))} else None
    val s_axib_rdata				= if (SLAVE_BRIDGE) {Some(Output(UInt(512.W)))} else None
    val s_axib_rresp				= if (SLAVE_BRIDGE) {Some(Output(UInt(2.W)))} else None
    val s_axib_ruser				= if (SLAVE_BRIDGE) {Some(Output(UInt(64.W)))} else None
    val s_axib_rlast				= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None
    val s_axib_rvalid				= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None
    val s_axib_rready				= if (SLAVE_BRIDGE) {Some(Input(UInt(1.W)))} else None

    val st_rx_msg_data	= if (SLAVE_BRIDGE) {Some(Output(UInt(32.W)))} else None
    val st_rx_msg_last	= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None
    val st_rx_msg_rdy	= if (SLAVE_BRIDGE) {Some(Input(UInt(1.W)))} else None
    val st_rx_msg_valid	= if (SLAVE_BRIDGE) {Some(Output(UInt(1.W)))} else None
}

class DDRPort extends Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())
    val axi = new AXI(34,512,4,0,8)
}

class AlveoStaticIO(
    VIVADO_VERSION:String, 
    QDMA_PCIE_WIDTH:Int, 
    QDMA_SLAVE_BRIDGE:Boolean=false, 
    QDMA_AXI_BRIDGE:Boolean=true,
    ENABLE_CMAC_CLOCK:Boolean=true,     // For compatibility to old projects.
    ENABLE_CMAC_1:Boolean=false,
    ENABLE_CMAC_2:Boolean=false,
    ENABLE_DDR_1:Boolean=false,
    ENABLE_DDR_2:Boolean=false
) extends Bundle {
    val sysClk      = Output(Clock())
    val cmacClk     = if (ENABLE_CMAC_CLOCK) {Some(Output(Clock()))} else None
    val cmacPin     = if (ENABLE_CMAC_1) {Some(Flipped(new CMACPin()))} else None
    val cmacPin2    = if (ENABLE_CMAC_2) {Some(Flipped(new CMACPin()))} else None
    val ddrPort     = if (ENABLE_DDR_1) {Some(Flipped(new DDRPort()))} else None
    val ddrPort2    = if (ENABLE_DDR_2) {Some(Flipped(new DDRPort()))} else None
    
    val qdma = new QDMAStaticIO(
        VIVADO_VERSION  = VIVADO_VERSION,
        PCIE_WIDTH      = QDMA_PCIE_WIDTH,
        SLAVE_BRIDGE    = QDMA_SLAVE_BRIDGE,
        AXI_BRIDGE      = true
        // AXI_BRIDGE      = QDMA_AXI_BRIDGE
    )
}