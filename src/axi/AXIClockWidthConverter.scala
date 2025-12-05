package common.axi
import chisel3._
import chisel3.util._
import common.storage._

class AXIDataWWidthConversion (IN_WIDTH : Int, OUT_WIDTH: Int, ID_WIDTH:Int, USER_WIDTH:Int) extends Module {
    val GCD = {
        var a = IN_WIDTH
        var b = OUT_WIDTH
        while (a != b) {
            if (a > b) {
                a = a - b
            } else {
                b = b - a
            }
        }
        a
    }
    val LCM = IN_WIDTH * OUT_WIDTH / GCD

    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new AXI_DATA_W(64,IN_WIDTH,ID_WIDTH,USER_WIDTH)))
        val out = Decoupled(new AXI_DATA_W(64,OUT_WIDTH,ID_WIDTH,USER_WIDTH))
    })

    val in          = RegSlice(io.in)
    val core        = Module(new WidthConversionInner(IN_WIDTH, OUT_WIDTH))

    core.io.in.bits.counter := parseKeepSignal(in.bits.strb, IN_WIDTH)
    core.io.in.bits.data    := in.bits.data
    core.io.in.bits.last    := in.bits.last
    core.io.in.valid        := in.valid
    in.ready             := core.io.in.ready

    val userTmpIn   = Wire(Decoupled(UInt(USER_WIDTH.W)))
    userTmpIn.bits  := in.bits.user
    userTmpIn.valid := in.valid
    val userTmpOut  = RegSlice(8)(userTmpIn)
    userTmpOut.ready    := core.io.out.ready
    io.out.bits.user    := userTmpOut.bits
    io.out.bits.data    := core.io.out.bits.data
    io.out.bits.last    := core.io.out.bits.last
    io.out.bits.strb    := genKeepSignal(core.io.out.bits.counter, OUT_WIDTH)
    io.out.valid        := core.io.out.valid
    core.io.out.ready   := io.out.ready
}

class AXIDataRWidthConversion (IN_WIDTH : Int, OUT_WIDTH: Int, ID_WIDTH:Int, USER_WIDTH:Int) extends Module {
    val GCD = {
        var a = IN_WIDTH
        var b = OUT_WIDTH
        while (a != b) {
            if (a > b) {
                a = a - b
            } else {
                b = b - a
            }
        }
        a
    }
    val LCM = IN_WIDTH * OUT_WIDTH / GCD

    val io = IO(new Bundle {
        val in  = Flipped(Decoupled(new AXI_DATA_R(64,IN_WIDTH,ID_WIDTH,USER_WIDTH)))
        val out = Decoupled(new AXI_DATA_R(64,OUT_WIDTH,ID_WIDTH,USER_WIDTH))
    })

    val in          = RegSlice(io.in)
    val core        = Module(new WidthConversionInner(IN_WIDTH, OUT_WIDTH))

    core.io.in.bits.counter := parseKeepSignal(-1.S((IN_WIDTH/8).W).asUInt, IN_WIDTH)
    core.io.in.bits.data    := in.bits.data
    core.io.in.bits.last    := in.bits.last
    core.io.in.valid        := in.valid
    in.ready             := core.io.in.ready

    val userTmpIn       = Wire(Decoupled(UInt(USER_WIDTH.W)))
    userTmpIn.bits      := in.bits.user
    userTmpIn.valid     := in.valid
    val userTmpOut      = RegSlice(8)(userTmpIn)
    userTmpOut.ready    := core.io.out.ready

    val idTmpIn         = Wire(Decoupled(UInt(ID_WIDTH.W)))
    idTmpIn.bits        := in.bits.id
    idTmpIn.valid       := in.valid
    val idTmpOut        = RegSlice(8)(idTmpIn)
    idTmpOut.ready      := core.io.out.ready

    val respTmpIn       = Wire(Decoupled(UInt(2.W)))
    respTmpIn.bits      := in.bits.resp
    respTmpIn.valid     := in.valid
    val respTmpOut      = RegSlice(8)(respTmpIn)
    respTmpOut.ready    := core.io.out.ready

    io.out.bits.user    := userTmpOut.bits
    io.out.bits.id      := idTmpOut.bits
    io.out.bits.resp    := respTmpOut.bits
    io.out.bits.data    := core.io.out.bits.data
    io.out.bits.last    := core.io.out.bits.last
    io.out.valid        := core.io.out.valid
    core.io.out.ready   := io.out.ready
}

object AXIClockWidthConverter{
    def apply[T1 <: AXI, T2 <: AXI] (m_gen: T1, m_clk: Clock, m_rstn: Bool, s_gen: T2, s_clk: Clock, s_rstn: Bool) = {
        val aw_out_tmp      = Wire(Decoupled(new AXI_ADDR(s_gen.aw.bits.addr.getWidth, s_gen.w.bits.data.getWidth, s_gen.aw.bits.id.getWidth, s_gen.aw.bits.user.getWidth, s_gen.aw.bits.len.getWidth)))
        val ar_out_tmp      = Wire(Decoupled(new AXI_ADDR(s_gen.ar.bits.addr.getWidth, s_gen.r.bits.data.getWidth, s_gen.ar.bits.id.getWidth, s_gen.ar.bits.user.getWidth, s_gen.ar.bits.len.getWidth)))
        if (m_gen.w.bits.data.getWidth > s_gen.w.bits.data.getWidth) {
            // Master width is larger than slave width, thus slave clock is higher than master clock.
            // Master -> Clock Converter -> Width Converter -> Slave
            val c_cvt_aw    = XConverter(chiselTypeOf(m_gen.aw.bits), m_clk, m_rstn, s_clk)
            val c_cvt_ar    = XConverter(chiselTypeOf(m_gen.ar.bits), m_clk, m_rstn, s_clk)
            val c_cvt_w     = XConverter(chiselTypeOf(m_gen.w.bits), m_clk, m_rstn, s_clk)
            val c_cvt_r     = XConverter(chiselTypeOf(m_gen.r.bits), s_clk, s_rstn, m_clk)
            val c_cvt_b     = XConverter(chiselTypeOf(m_gen.b.bits), s_clk, s_rstn, m_clk)
            
            val w_cvt_w     = withClockAndReset(s_clk, ~s_rstn) {
                Module(new AXIDataWWidthConversion(
                    m_gen.w.bits.data.getWidth, 
                    s_gen.w.bits.data.getWidth,
                    m_gen.aw.bits.id.getWidth,
                    m_gen.w.bits.user.getWidth
                ))
            }
            val w_cvt_r     = withClockAndReset(s_clk, ~s_rstn) {
                Module(new AXIDataRWidthConversion(
                    s_gen.r.bits.data.getWidth, 
                    m_gen.r.bits.data.getWidth,
                    s_gen.r.bits.id.getWidth,
                    s_gen.r.bits.user.getWidth
                ))
            }

            c_cvt_aw.io.in  <> m_gen.aw
            c_cvt_ar.io.in  <> m_gen.ar
            c_cvt_w.io.in   <> m_gen.w
            c_cvt_r.io.out  <> m_gen.r
            c_cvt_b.io.out  <> m_gen.b

            c_cvt_aw.io.out <> s_gen.aw
            c_cvt_ar.io.out <> s_gen.ar
            c_cvt_b.io.in   <> s_gen.b

            w_cvt_w.io.in   <> c_cvt_w.io.out
            w_cvt_w.io.out  <> s_gen.w
            w_cvt_r.io.in   <> s_gen.r
            w_cvt_r.io.out  <> c_cvt_r.io.in

            aw_out_tmp      := c_cvt_aw.io.out
            ar_out_tmp      := c_cvt_ar.io.out
        } else {
            // Master width is smaller than slave width, thus master clock is higher than slave clock.
            // Master -> Width Converter -> Clock Converter -> Slave
            val c_cvt_aw    = XConverter(chiselTypeOf(s_gen.aw.bits), m_clk, m_rstn, s_clk)
            val c_cvt_ar    = XConverter(chiselTypeOf(s_gen.ar.bits), m_clk, m_rstn, s_clk)
            val c_cvt_w     = XConverter(chiselTypeOf(s_gen.w.bits), m_clk, m_rstn, s_clk)
            val c_cvt_r     = XConverter(chiselTypeOf(s_gen.r.bits), s_clk, s_rstn, m_clk)
            val c_cvt_b     = XConverter(chiselTypeOf(s_gen.b.bits), s_clk, s_rstn, m_clk)

            val w_cvt_w     = withClockAndReset(m_clk, ~m_rstn) {
                Module(new AXIDataWWidthConversion(
                    m_gen.w.bits.data.getWidth, 
                    s_gen.w.bits.data.getWidth,
                    m_gen.aw.bits.id.getWidth,
                    m_gen.w.bits.user.getWidth
                ))
            }
            val w_cvt_r     = withClockAndReset(m_clk, ~m_rstn) {
                Module(new AXIDataRWidthConversion(
                    s_gen.r.bits.data.getWidth, 
                    m_gen.r.bits.data.getWidth,
                    s_gen.r.bits.id.getWidth,
                    s_gen.r.bits.user.getWidth
                ))
            }

            w_cvt_w.io.in   <> m_gen.w
            w_cvt_w.io.out  <> c_cvt_w.io.in
            w_cvt_r.io.in   <> c_cvt_r.io.out
            w_cvt_r.io.out  <> m_gen.r

            c_cvt_aw.io.in  <> m_gen.aw
            c_cvt_ar.io.in  <> m_gen.ar
            c_cvt_b.io.out  <> m_gen.b

            c_cvt_aw.io.out <> s_gen.aw
            c_cvt_ar.io.out <> s_gen.ar
            c_cvt_w.io.out  <> s_gen.w
            c_cvt_r.io.in   <> s_gen.r
            c_cvt_b.io.in   <> s_gen.b

            aw_out_tmp      := c_cvt_aw.io.out
            ar_out_tmp      := c_cvt_ar.io.out
        }

        // Converting AW and AR size and len.
        s_gen.aw.bits.size  := log2Ceil(s_gen.w.bits.data.getWidth / 8).U(3.W)
        s_gen.ar.bits.size  := log2Ceil(s_gen.r.bits.data.getWidth / 8).U(3.W)
        if (m_gen.w.bits.data.getWidth <= s_gen.w.bits.data.getWidth) {
            val tmpLen1 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            val tmpLen2 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            val tmpLen3 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            tmpLen1     := aw_out_tmp.bits.len + 1.U
            tmpLen2     := tmpLen1(tmpLen1.getWidth - 1, log2Ceil(s_gen.w.bits.data.getWidth / m_gen.w.bits.data.getWidth))
            tmpLen3     := tmpLen2 - 1.U
            s_gen.aw.bits.len   := tmpLen3
            val tmpLen4 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            val tmpLen5 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            val tmpLen6 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            tmpLen4     := ar_out_tmp.bits.len + 1.U
            tmpLen5     := tmpLen4(tmpLen4.getWidth - 1, log2Ceil(s_gen.r.bits.data.getWidth / m_gen.r.bits.data.getWidth))
            tmpLen6     := tmpLen5 - 1.U
            s_gen.ar.bits.len   := tmpLen6
        } else {
            // Master len is larger than or equal to slave len, thus we can use the master len.
            val tmpLen1 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            val tmpLen2 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            val tmpLen3 = Wire(UInt((m_gen.aw.bits.len.getWidth+1).W))
            tmpLen1     := aw_out_tmp.bits.len + 1.U
            tmpLen2     := Cat(tmpLen1, 0.U(log2Ceil(m_gen.w.bits.data.getWidth/s_gen.w.bits.data.getWidth).W))
            tmpLen3     := tmpLen2 - 1.U
            s_gen.aw.bits.len   := tmpLen3
            val tmpLen4 = Wire(UInt((m_gen.ar.bits.len.getWidth+1).W))
            val tmpLen5 = Wire(UInt((m_gen.ar.bits.len.getWidth+1).W))
            val tmpLen6 = Wire(UInt((m_gen.ar.bits.len.getWidth+1).W))
            tmpLen4     := ar_out_tmp.bits.len + 1.U
            tmpLen5     := Cat(tmpLen4, 0.U(log2Ceil(m_gen.r.bits.data.getWidth/s_gen.r.bits.data.getWidth).W))
            tmpLen6     := tmpLen5 - 1.U
            s_gen.ar.bits.len   := tmpLen6
        }
    }
}