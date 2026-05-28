package common

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, StringParam}

// DSP48E2 primitive BlackBox for AMD UltraScale+/Versal FPGAs.
// Only the ports used for unsigned multiplication are connected;
// unused ports are tied off inside this wrapper.
class DSP48E2(
    // Pipeline register configuration
    AREG: Int = 1,      // 0, 1, or 2 pipeline stages for A
    BREG: Int = 1,      // 0, 1, or 2 pipeline stages for B
    MREG: Int = 1,      // 0 or 1 pipeline stage for M (multiplier)
    PREG: Int = 1,      // 0 or 1 pipeline stage for P (output)
    ACASCREG: Int = 1,  // Must match AREG (or AREG-1 if AREG=2)
    BCASCREG: Int = 1,  // Must match BREG (or BREG-1 if BREG=2)
    // Use the multiplier
    USE_MULT: String = "MULTIPLY",
    // A_INPUT / B_INPUT: "DIRECT" or "CASCADE"
    A_INPUT: String = "DIRECT",
    B_INPUT: String = "DIRECT"
) extends BlackBox(Map(
    "AREG"      -> IntParam(AREG),
    "BREG"      -> IntParam(BREG),
    "MREG"      -> IntParam(MREG),
    "PREG"      -> IntParam(PREG),
    "ACASCREG"  -> IntParam(ACASCREG),
    "BCASCREG"  -> IntParam(BCASCREG),
    "USE_MULT"  -> StringParam(USE_MULT),
    "A_INPUT"   -> StringParam(A_INPUT),
    "B_INPUT"   -> StringParam(B_INPUT)
)) {
    val io = IO(new Bundle {
        val CLK         = Input(Clock())
        // Data ports
        val A           = Input(UInt(30.W))
        val B           = Input(UInt(18.W))
        val C           = Input(UInt(48.W))
        val D           = Input(UInt(27.W))
        val P           = Output(UInt(48.W))
        // Cascade ports
        val ACIN        = Input(UInt(30.W))
        val BCIN        = Input(UInt(18.W))
        val PCIN        = Input(UInt(48.W))
        val ACOUT       = Output(UInt(30.W))
        val BCOUT       = Output(UInt(18.W))
        val PCOUT       = Output(UInt(48.W))
        // Control
        val OPMODE      = Input(UInt(9.W))
        val ALUMODE     = Input(UInt(4.W))
        val INMODE      = Input(UInt(5.W))
        val CARRYINSEL  = Input(UInt(3.W))
        val CARRYIN     = Input(Bool())
        // Clock enables
        val CEA1        = Input(Bool())
        val CEA2        = Input(Bool())
        val CEB1        = Input(Bool())
        val CEB2        = Input(Bool())
        val CEC         = Input(Bool())
        val CED         = Input(Bool())
        val CEM         = Input(Bool())
        val CEP         = Input(Bool())
        val CEAD        = Input(Bool())
        val CEALUMODE   = Input(Bool())
        val CECTRL      = Input(Bool())
        val CECARRYIN   = Input(Bool())
        val CEINMODE    = Input(Bool())
        // Resets
        val RSTA        = Input(Bool())
        val RSTB        = Input(Bool())
        val RSTC        = Input(Bool())
        val RSTD        = Input(Bool())
        val RSTM        = Input(Bool())
        val RSTP        = Input(Bool())
        val RSTALLCARRYIN  = Input(Bool())
        val RSTALUMODE     = Input(Bool())
        val RSTCTRL        = Input(Bool())
        val RSTINMODE      = Input(Bool())
        // Misc outputs (directly unused but must exist)
        val CARRYOUT    = Output(UInt(4.W))
        val XOROUT      = Output(UInt(8.W))
        val OVERFLOW    = Output(Bool())
        val UNDERFLOW   = Output(Bool())
        val PATTERNDETECT       = Output(Bool())
        val PATTERNBDETECT      = Output(Bool())
        val MULTSIGNOUT         = Output(Bool())
        val CARRYCASCOUT        = Output(Bool())
        val MULTSIGNIN          = Input(Bool())
        val CARRYCASCIN         = Input(Bool())
    })
}

// Helper: instantiate a DSP48E2 configured for unsigned A*B multiply.
// Returns P[47:0] = zero-extended(A) * zero-extended(B).
// Pipeline: AREG=1, BREG=1, MREG=1, PREG=preg → latency = 2+mreg+preg cycles
object DSP48E2Mul {
    def apply(
        clock: Clock,
        a: UInt,       // up to 27 bits (placed in A[29:0], upper bits zero)
        b: UInt,       // up to 18 bits (placed in B[17:0])
        preg: Int = 1, // set to 0 to omit output register
        pcin: UInt = 0.U(48.W),
        useC: Boolean = false,
        c: UInt = 0.U(48.W),
        opmode: UInt = "b000000101".U(9.W) // default: P = M (multiply only)
    ): (UInt, UInt) = {    // returns (P, PCOUT)
        val dsp = Module(new DSP48E2(
            AREG = 1, BREG = 1, MREG = 1, PREG = preg,
            ACASCREG = 1, BCASCREG = 1,
            USE_MULT = "MULTIPLY",
            A_INPUT = "DIRECT", B_INPUT = "DIRECT"
        ))
        dsp.io.CLK := clock

        // Data inputs: zero-extend A to 30 bits, B to 18 bits
        dsp.io.A := a.pad(30)
        dsp.io.B := b.pad(18)
        dsp.io.C := c.pad(48)
        dsp.io.D := 0.U

        // Cascade inputs (unused when DIRECT)
        dsp.io.ACIN := 0.U
        dsp.io.BCIN := 0.U
        dsp.io.PCIN := pcin

        // Control: OPMODE = 000_00_0101 → P = 0 + M (multiply, no accumulate)
        //   W=00(zero), Z=00(zero), Y=01, X=01 → XY = M
        dsp.io.OPMODE     := opmode
        dsp.io.ALUMODE    := 0.U      // Z + (X + Y + CIN) → add
        dsp.io.INMODE     := "b00000".U  // A2*B2 (use registered A and B)
        dsp.io.CARRYINSEL := 0.U
        dsp.io.CARRYIN    := false.B

        // Clock enables: all enabled
        dsp.io.CEA1      := true.B
        dsp.io.CEA2      := true.B
        dsp.io.CEB1      := true.B
        dsp.io.CEB2      := true.B
        dsp.io.CEC       := true.B
        dsp.io.CED       := true.B
        dsp.io.CEM       := true.B
        dsp.io.CEP       := true.B
        dsp.io.CEAD      := true.B
        dsp.io.CEALUMODE := true.B
        dsp.io.CECTRL    := true.B
        dsp.io.CECARRYIN := true.B
        dsp.io.CEINMODE  := true.B

        // Resets: all deasserted
        dsp.io.RSTA           := false.B
        dsp.io.RSTB           := false.B
        dsp.io.RSTC           := false.B
        dsp.io.RSTD           := false.B
        dsp.io.RSTM           := false.B
        dsp.io.RSTP           := false.B
        dsp.io.RSTALLCARRYIN  := false.B
        dsp.io.RSTALUMODE     := false.B
        dsp.io.RSTCTRL        := false.B
        dsp.io.RSTINMODE      := false.B

        // Unused cascade inputs
        dsp.io.MULTSIGNIN  := false.B
        dsp.io.CARRYCASCIN := false.B

        (dsp.io.P, dsp.io.PCOUT)
    }
}


// DSPFP32 primitive BlackBox for AMD Versal AI Core Series FPGAs.
// Contains a floating-point multiplier (FPM) and a floating-point adder (FPA)
// with separate outputs.
class DSPFP32(
    // Feature Control Attributes
    A_FPTYPE:    String = "B32",
    A_INPUT:     String = "DIRECT",
    BCASCSEL:    String = "B",
    B_D_FPTYPE:  String = "B32",
    B_INPUT:     String = "DIRECT",
    PCOUTSEL:    String = "FPA",
    USE_MULT:    String = "MULTIPLY",
    // Register Control Attributes
    ACASCREG:    Int = 1,
    AREG:        Int = 1,
    FPA_PREG:    Int = 1,
    FPBREG:      Int = 1,
    FPCREG:      Int = 3,
    FPDREG:      Int = 1,
    FPMPIPEREG:  Int = 1,
    FPM_PREG:    Int = 1,
    FPOPMREG:    Int = 3,
    INMODEREG:   Int = 1,
    RESET_MODE:  String = "SYNC"
) extends BlackBox(Map(
    "A_FPTYPE"    -> StringParam(A_FPTYPE),
    "A_INPUT"     -> StringParam(A_INPUT),
    "BCASCSEL"    -> StringParam(BCASCSEL),
    "B_D_FPTYPE"  -> StringParam(B_D_FPTYPE),
    "B_INPUT"     -> StringParam(B_INPUT),
    "PCOUTSEL"    -> StringParam(PCOUTSEL),
    "USE_MULT"    -> StringParam(USE_MULT),
    "ACASCREG"    -> IntParam(ACASCREG),
    "AREG"        -> IntParam(AREG),
    "FPA_PREG"    -> IntParam(FPA_PREG),
    "FPBREG"      -> IntParam(FPBREG),
    "FPCREG"      -> IntParam(FPCREG),
    "FPDREG"      -> IntParam(FPDREG),
    "FPMPIPEREG"  -> IntParam(FPMPIPEREG),
    "FPM_PREG"    -> IntParam(FPM_PREG),
    "FPOPMREG"    -> IntParam(FPOPMREG),
    "INMODEREG"   -> IntParam(INMODEREG),
    "RESET_MODE"  -> StringParam(RESET_MODE)
)) {
    val io = IO(new Bundle {
        val CLK             = Input(Clock())
        // Data inputs (A decomposed into sign/exp/man)
        val A_SIGN          = Input(Bool())
        val A_EXP           = Input(UInt(8.W))
        val A_MAN           = Input(UInt(23.W))
        // Data inputs (B decomposed into sign/exp/man)
        val B_SIGN          = Input(Bool())
        val B_EXP           = Input(UInt(8.W))
        val B_MAN           = Input(UInt(23.W))
        // Data inputs (C is 32-bit Binary32)
        val C               = Input(UInt(32.W))
        // Data inputs (D decomposed into sign/exp/man)
        val D_SIGN          = Input(Bool())
        val D_EXP           = Input(UInt(8.W))
        val D_MAN           = Input(UInt(23.W))
        // Control inputs
        val FPINMODE        = Input(Bool())
        val FPOPMODE        = Input(UInt(7.W))
        // Cascade inputs
        val ACIN_SIGN       = Input(Bool())
        val ACIN_EXP        = Input(UInt(8.W))
        val ACIN_MAN        = Input(UInt(23.W))
        val BCIN_SIGN       = Input(Bool())
        val BCIN_EXP        = Input(UInt(8.W))
        val BCIN_MAN        = Input(UInt(23.W))
        val PCIN            = Input(UInt(32.W))
        // Cascade outputs
        val ACOUT_SIGN      = Output(Bool())
        val ACOUT_EXP       = Output(UInt(8.W))
        val ACOUT_MAN       = Output(UInt(23.W))
        val BCOUT_SIGN      = Output(Bool())
        val BCOUT_EXP       = Output(UInt(8.W))
        val BCOUT_MAN       = Output(UInt(23.W))
        val PCOUT           = Output(UInt(32.W))
        // Data outputs
        val FPA_OUT         = Output(UInt(32.W))
        val FPA_OVERFLOW    = Output(Bool())
        val FPA_UNDERFLOW   = Output(Bool())
        val FPA_INVALID     = Output(Bool())
        val FPM_OUT         = Output(UInt(32.W))
        val FPM_OVERFLOW    = Output(Bool())
        val FPM_UNDERFLOW   = Output(Bool())
        val FPM_INVALID     = Output(Bool())
        // Clock enables
        val CEA1            = Input(Bool())
        val CEA2            = Input(Bool())
        val CEB             = Input(Bool())
        val CEC             = Input(Bool())
        val CED             = Input(Bool())
        val CEFPA           = Input(Bool())
        val CEFPINMODE      = Input(Bool())
        val CEFPM           = Input(Bool())
        val CEFPMPIPE       = Input(Bool())
        val CEFPOPMODE      = Input(Bool())
        // Resets
        val ASYNC_RST       = Input(Bool())
        val RSTA            = Input(Bool())
        val RSTB            = Input(Bool())
        val RSTC            = Input(Bool())
        val RSTD            = Input(Bool())
        val RSTFPA          = Input(Bool())
        val RSTFPINMODE     = Input(Bool())
        val RSTFPM          = Input(Bool())
        val RSTFPMPIPE      = Input(Bool())
        val RSTFPOPMODE     = Input(Bool())
    })
}

// Helper: instantiate a DSPFP32 configured for FP32 multiply.
// FPM_OUT = A * B. Latency = AREG + FPMPIPEREG + FPM_PREG = 3 cycles.
object DSPFP32Mul {
    val LATENCY = 3  // AREG(1) + FPMPIPEREG(1) + FPM_PREG(1)
    def apply(clock: Clock, a: UInt, b: UInt, ce: Bool = true.B): (UInt, UInt) = {
        val dsp = Module(new DSPFP32(
            A_FPTYPE    = "B32",
            B_D_FPTYPE  = "B32",
            A_INPUT     = "DIRECT",
            B_INPUT     = "DIRECT",
            USE_MULT    = "MULTIPLY",
            PCOUTSEL    = "FPM",
            AREG        = 1,
            ACASCREG    = 1,
            FPBREG      = 1,
            FPCREG      = 0,
            FPDREG      = 0,
            FPMPIPEREG  = 1,
            FPM_PREG    = 1,
            FPA_PREG    = 0,
            FPOPMREG    = 0,
            INMODEREG   = 0,
            RESET_MODE  = "SYNC"
        ))
        dsp.io.CLK := clock

        // Decompose FP32 input A into sign/exp/man
        dsp.io.A_SIGN := a(31)
        dsp.io.A_EXP  := a(30, 23)
        dsp.io.A_MAN  := a(22, 0)
        // Decompose FP32 input B into sign/exp/man
        dsp.io.B_SIGN := b(31)
        dsp.io.B_EXP  := b(30, 23)
        dsp.io.B_MAN  := b(22, 0)
        // Unused inputs
        dsp.io.C      := 0.U
        dsp.io.D_SIGN := false.B
        dsp.io.D_EXP  := 0.U
        dsp.io.D_MAN  := 0.U
        // Control: FPINMODE=1 (select B), FPOPMODE=0 (adder not used)
        dsp.io.FPINMODE := true.B
        dsp.io.FPOPMODE := 0.U
        // Cascade inputs (unused)
        dsp.io.ACIN_SIGN := false.B
        dsp.io.ACIN_EXP  := 0.U
        dsp.io.ACIN_MAN  := 0.U
        dsp.io.BCIN_SIGN := false.B
        dsp.io.BCIN_EXP  := 0.U
        dsp.io.BCIN_MAN  := 0.U
        dsp.io.PCIN      := 0.U
        // Clock enables
        dsp.io.CEA1       := ce
        dsp.io.CEA2       := ce
        dsp.io.CEB        := ce
        dsp.io.CEC        := ce
        dsp.io.CED        := ce
        dsp.io.CEFPA      := ce
        dsp.io.CEFPINMODE := ce
        dsp.io.CEFPM      := ce
        dsp.io.CEFPMPIPE  := ce
        dsp.io.CEFPOPMODE := ce
        // Resets: all deasserted
        dsp.io.ASYNC_RST   := false.B
        dsp.io.RSTA        := false.B
        dsp.io.RSTB        := false.B
        dsp.io.RSTC        := false.B
        dsp.io.RSTD        := false.B
        dsp.io.RSTFPA      := false.B
        dsp.io.RSTFPINMODE := false.B
        dsp.io.RSTFPM      := false.B
        dsp.io.RSTFPMPIPE  := false.B
        dsp.io.RSTFPOPMODE := false.B

        (dsp.io.FPM_OUT, dsp.io.PCOUT)
    }
}

// Helper: instantiate a DSPFP32 configured for FP32 add.
// FPA_OUT = C + A * 1.0 = C + A.
// B is hardwired to 1.0 (0x3F800000). Input `a` goes to A port, input `b` goes to C port.
// FPOPMODE = 0000101: Z=C(01), X=FPM(01) → FPA = C + FPM = C + A*1.0.
// Latency = AREG(1) + FPMPIPEREG(1) + FPA_PREG(1) = 3 cycles.
// FPCREG is set to 2 to match the multiply pipeline delay (AREG+FPMPIPEREG=2 stages
// before FPM reaches the adder, FPCREG=2 aligns C with FPM).
object DSPFP32Add {
    val LATENCY = 3  // AREG(1) + FPMPIPEREG(1) + FPA_PREG(1)
    def apply(clock: Clock, a: UInt, b: UInt, ce: Bool = true.B): (UInt, UInt) = {
        val dsp = Module(new DSPFP32(
            A_FPTYPE    = "B32",
            B_D_FPTYPE  = "B32",
            A_INPUT     = "DIRECT",
            B_INPUT     = "DIRECT",
            USE_MULT    = "MULTIPLY",
            PCOUTSEL    = "FPA",
            AREG        = 1,
            ACASCREG    = 1,
            FPBREG      = 1,
            FPCREG      = 2,       // Align C with multiply pipeline (AREG + FPMPIPEREG = 2)
            FPDREG      = 0,
            FPMPIPEREG  = 1,
            FPM_PREG    = 0,       // No extra register on FPM; it feeds directly into FPA
            FPA_PREG    = 1,       // Output register on FPA
            FPOPMREG    = 0,       // FPOPMODE is static, no pipeline needed
            INMODEREG   = 0,
            RESET_MODE  = "SYNC"
        ))
        dsp.io.CLK := clock

        // Input `a` goes to A port (will be multiplied by 1.0)
        dsp.io.A_SIGN := a(31)
        dsp.io.A_EXP  := a(30, 23)
        dsp.io.A_MAN  := a(22, 0)
        // B is hardwired to 1.0f = 0x3F800000 (sign=0, exp=0x7F, man=0)
        dsp.io.B_SIGN := false.B
        dsp.io.B_EXP  := "h7F".U(8.W)
        dsp.io.B_MAN  := 0.U(23.W)
        // Input `b` goes to C port (32-bit Binary32)
        dsp.io.C      := b
        // Unused D input
        dsp.io.D_SIGN := false.B
        dsp.io.D_EXP  := 0.U
        dsp.io.D_MAN  := 0.U
        // Control: FPINMODE=1 (select B), FPOPMODE=0011001 → FPA = C + FPM
        dsp.io.FPINMODE := true.B
        dsp.io.FPOPMODE := "b0011001".U(7.W)
        // Cascade inputs (unused)
        dsp.io.ACIN_SIGN := false.B
        dsp.io.ACIN_EXP  := 0.U
        dsp.io.ACIN_MAN  := 0.U
        dsp.io.BCIN_SIGN := false.B
        dsp.io.BCIN_EXP  := 0.U
        dsp.io.BCIN_MAN  := 0.U
        dsp.io.PCIN      := 0.U
        // Clock enables
        dsp.io.CEA1       := ce
        dsp.io.CEA2       := ce
        dsp.io.CEB        := ce
        dsp.io.CEC        := ce
        dsp.io.CED        := ce
        dsp.io.CEFPA      := ce
        dsp.io.CEFPINMODE := ce
        dsp.io.CEFPM      := ce
        dsp.io.CEFPMPIPE  := ce
        dsp.io.CEFPOPMODE := ce
        // Resets: all deasserted
        dsp.io.ASYNC_RST   := false.B
        dsp.io.RSTA        := false.B
        dsp.io.RSTB        := false.B
        dsp.io.RSTC        := false.B
        dsp.io.RSTD        := false.B
        dsp.io.RSTFPA      := false.B
        dsp.io.RSTFPINMODE := false.B
        dsp.io.RSTFPM      := false.B
        dsp.io.RSTFPMPIPE  := false.B
        dsp.io.RSTFPOPMODE := false.B

        (dsp.io.FPA_OUT, dsp.io.PCOUT)
    }
}

// Helper: instantiate a DSPFP32 configured for FP32 accumulate.
// FPA_OUT = P + A * 1.0 (accumulate: adds input `a` to the running sum in P register).
// B is hardwired to 1.0 (0x3F800000). Input `a` goes to A port.
// FPOPMODE = 0100001: Z=P(10), X=FPM(01) → FPA = P + FPM = P + A*1.0.
// Latency = AREG(1) + FPMPIPEREG(1) + FPA_PREG(1) = 3 cycles (initial).
// After the pipeline is primed, one accumulation per cycle.
// FPA_PREG=1 is required for P feedback.
// `last` zeroes the P register (via RSTFPA) to start a new accumulation.
object DSPFP32Acc {
    val LATENCY = 3  // AREG(1) + FPMPIPEREG(1) + FPA_PREG(1)
    def apply(clock: Clock, a: UInt, en: Bool, last: Bool = false.B, ce: Bool = true.B): (UInt, UInt) = {

        val p0sel   = Mux(en, "b01".U(2.W), "b00".U(2.W))
        val p1sel   = Mux(RegNext(last), "b000".U(3.W), "b100".U(3.W))

        val dsp = Module(new DSPFP32(
            A_FPTYPE    = "B32",
            B_D_FPTYPE  = "B32",
            A_INPUT     = "DIRECT",
            B_INPUT     = "DIRECT",
            USE_MULT    = "MULTIPLY",
            PCOUTSEL    = "FPA",
            AREG        = 1,
            ACASCREG    = 1,
            FPBREG      = 1,
            FPCREG      = 0,       // C port unused in accumulate mode
            FPDREG      = 0,
            FPMPIPEREG  = 1,
            FPM_PREG    = 0,       // No extra register on FPM; it feeds directly into FPA
            FPA_PREG    = 1,       // Output register on FPA (required for P feedback)
            FPOPMREG    = 2,       // pipeline is needed
            INMODEREG   = 0,
            RESET_MODE  = "SYNC"
        ))
        dsp.io.CLK := clock

        // Input `a` goes to A port (will be multiplied by 1.0)
        dsp.io.A_SIGN := a(31)
        dsp.io.A_EXP  := a(30, 23)
        dsp.io.A_MAN  := a(22, 0)
        // B is hardwired to 1.0f = 0x3F800000 (sign=0, exp=0x7F, man=0)
        dsp.io.B_SIGN := false.B
        dsp.io.B_EXP  := "h7F".U(8.W)
        dsp.io.B_MAN  := 0.U(23.W)
        // C port unused (Z mux selects P feedback, not C)
        dsp.io.C      := 0.U
        // Unused D input
        dsp.io.D_SIGN := false.B
        dsp.io.D_EXP  := 0.U
        dsp.io.D_MAN  := 0.U
        // Control: FPINMODE=1 (select B), FPOPMODE=0100001 → FPA = P + FPM
        //   Z mux = 10 (P feedback), X mux = 01 (FPM)
        dsp.io.FPINMODE := true.B
        dsp.io.FPOPMODE := Cat("b00".U(2.W), p1sel, p0sel)
        // Cascade inputs (unused)
        dsp.io.ACIN_SIGN := false.B
        dsp.io.ACIN_EXP  := 0.U
        dsp.io.ACIN_MAN  := 0.U
        dsp.io.BCIN_SIGN := false.B
        dsp.io.BCIN_EXP  := 0.U
        dsp.io.BCIN_MAN  := 0.U
        dsp.io.PCIN      := 0.U
        // Clock enables
        dsp.io.CEA1       := ce
        dsp.io.CEA2       := ce
        dsp.io.CEB        := ce
        dsp.io.CEC        := ce
        dsp.io.CED        := ce
        dsp.io.CEFPA      := ce
        dsp.io.CEFPINMODE := ce
        dsp.io.CEFPM      := ce
        dsp.io.CEFPMPIPE  := ce
        dsp.io.CEFPOPMODE := ce
        // Resets: RSTFPA driven by last to clear accumulator
        dsp.io.ASYNC_RST   := false.B
        dsp.io.RSTA        := false.B
        dsp.io.RSTB        := false.B
        dsp.io.RSTC        := false.B
        dsp.io.RSTD        := false.B
        dsp.io.RSTFPA      := false.B
        dsp.io.RSTFPINMODE := false.B
        dsp.io.RSTFPM      := false.B
        dsp.io.RSTFPMPIPE  := false.B
        dsp.io.RSTFPOPMODE := false.B

        (dsp.io.FPA_OUT, dsp.io.PCOUT)
    }
}

// Helper: instantiate a DSPFP32 configured for FP16 add.
// Uses B16 mode for A and B inputs. Input `a` (FP16) goes to A port,
// B is hardwired to FP16 1.0 (0x3C00), so FPM = A * 1.0.
// Input `b` (FP16) is converted to FP32 in pure RTL and fed to C port.
// FPA_OUT (FP32) = C + FPM. The caller must convert FPA_OUT back to FP16.
// FPOPMODE = 0000101: Z=C(01), X=FPM(01) → FPA = C + FPM.
// Latency = AREG(1) + FPMPIPEREG(1) + FPA_PREG(1) = 3 cycles.
object DSPFP32Add16 {
    val LATENCY = 3

    /** Convert FP16 to FP32 (pure combinational RTL).
      * FP16: sign(1) + exp(5) + man(10)
      * FP32: sign(1) + exp(8) + man(23)
      */
    def fp16to32(in: UInt): UInt = {
        val sign = in(15)
        val exp  = in(14, 10)
        val man  = in(9, 0)

        val out = Wire(UInt(32.W))
        when (exp === 0.U && man === 0.U) {
            // Zero: ±0
            out := Cat(sign, 0.U(31.W))
        }.elsewhen (exp === 0.U) {
            // Subnormal: normalize by finding leading one
            // FP16 subnormal = (-1)^s * 2^(-14) * 0.man
            // Need to shift mantissa left until leading 1 is found
            val shift = PriorityEncoder(Reverse(man))  // count leading zeros in man(10 bits)
            val normMan = (man << (shift + 1.U))(9, 0) // remove the leading 1
            val normExp = (112.U(8.W) - shift)(7, 0)   // 127 - 15 - shift = 112 - shift
            out := Cat(sign, normExp, normMan, 0.U(13.W))
        }.elsewhen (exp === 31.U) {
            // Inf or NaN: exp → 255
            out := Cat(sign, "hFF".U(8.W), man, 0.U(13.W))
        }.otherwise {
            // Normal: exp32 = exp16 + 112 (= exp16 - 15 + 127)
            out := Cat(sign, (exp +& 112.U)(7, 0), man, 0.U(13.W))
        }
        out
    }

    /** Convert FP32 to FP16 (pure combinational RTL) with round-to-nearest-even. */
    def fp32to16(in: UInt): UInt = {
        val sign   = in(31)
        val exp32  = in(30, 23)
        val man32  = in(22, 0)

        val out = Wire(UInt(16.W))
        when (exp32 === 0.U) {
            // FP32 zero or subnormal → FP16 zero
            out := Cat(sign, 0.U(15.W))
        }.elsewhen (exp32 === "hFF".U(8.W)) {
            // Inf or NaN
            when (man32 === 0.U) {
                out := Cat(sign, "b11111".U(5.W), 0.U(10.W)) // ±Inf
            }.otherwise {
                out := Cat(sign, "b11111".U(5.W), 1.U(10.W)) // NaN (quiet)
            }
        }.elsewhen (exp32 < 103.U) {
            // Too small: underflow to ±0 (exp32 < 127-24 = 103)
            out := Cat(sign, 0.U(15.W))
        }.elsewhen (exp32 < 113.U) {
            // FP32 normal → FP16 subnormal (exp32 in [103, 112])
            // FP16 subnormal = (-1)^s * 2^(-14) * 0.man16
            // Shift amount = 113 - exp32 (shifting right, adding implicit 1)
            val shiftAmt = 113.U - exp32  // 1 to 10
            val fullMan  = Cat(1.U(1.W), man32(22, 13)) // 11 bits: implicit 1 + top 10 man bits
            val shifted  = (fullMan >> shiftAmt)(9, 0)
            // Round: check bits shifted out
            val roundBit = (fullMan >> (shiftAmt - 1.U))(0)
            val stickyBits = ((fullMan << (11.U - shiftAmt + 1.U)) =/= 0.U) || (man32(12, 0) =/= 0.U)
            val rounded = Mux(roundBit && (stickyBits || shifted(0)), shifted + 1.U, shifted)
            out := Cat(sign, 0.U(5.W), rounded(9, 0))
        }.elsewhen (exp32 > 142.U) {
            // Too large: overflow to ±Inf (exp32 > 127+15 = 142)
            out := Cat(sign, "b11111".U(5.W), 0.U(10.W))
        }.otherwise {
            // Normal range: exp16 = exp32 - 112
            val exp16 = (exp32 - 112.U)(4, 0)
            val man16 = man32(22, 13)  // top 10 bits of mantissa
            // Round-to-nearest-even using bit 12 (round) and bits [11:0] (sticky)
            val roundBit  = man32(12)
            val stickyBit = man32(11, 0) =/= 0.U
            val rounded   = Mux(roundBit && (stickyBit || man16(0)), man16 +& 1.U, man16 +& 0.U)
            // Handle mantissa overflow (man16 rounds up from 0x3FF to 0x400)
            when (rounded(10)) {
                out := Cat(sign, (exp16 + 1.U)(4, 0), 0.U(10.W))
            }.otherwise {
                out := Cat(sign, exp16, rounded(9, 0))
            }
        }
        out
    }

    def apply(clock: Clock, a: UInt, b: UInt, ce: Bool = true.B): (UInt, UInt) = {
        val dsp = Module(new DSPFP32(
            A_FPTYPE    = "B16",
            B_D_FPTYPE  = "B16",
            A_INPUT     = "DIRECT",
            B_INPUT     = "DIRECT",
            USE_MULT    = "MULTIPLY",
            PCOUTSEL    = "FPA",
            AREG        = 1,
            ACASCREG    = 1,
            FPBREG      = 1,
            FPCREG      = 2,       // Align C with multiply pipeline
            FPDREG      = 0,
            FPMPIPEREG  = 1,
            FPM_PREG    = 0,
            FPA_PREG    = 1,
            FPOPMREG    = 0,
            INMODEREG   = 0,
            RESET_MODE  = "SYNC"
        ))
        dsp.io.CLK := clock

        // Input `a` (FP16) → A port: sign=a[15], exp=a[14:10], man=a[9:0]
        dsp.io.A_SIGN := a(15)
        dsp.io.A_EXP  := Cat(0.U(3.W), a(14, 10))
        dsp.io.A_MAN  := Cat(a(9, 0), 0.U(13.W))
        // B is hardwired to FP16 1.0 = 0x3C00 (sign=0, exp=15, man=0)
        dsp.io.B_SIGN := false.B
        dsp.io.B_EXP  := 15.U(8.W)
        dsp.io.B_MAN  := 0.U(23.W)
        // Input `b` (FP16) → converted to FP32, then to C port
        dsp.io.C      := fp16to32(b)
        // Unused D input
        dsp.io.D_SIGN := false.B
        dsp.io.D_EXP  := 0.U
        dsp.io.D_MAN  := 0.U
        // Control: FPINMODE=1 (select B), FPOPMODE=0011001 → FPA = C + FPM
        dsp.io.FPINMODE := true.B
        dsp.io.FPOPMODE := "b0011001".U(7.W)
        // Cascade inputs (unused)
        dsp.io.ACIN_SIGN := false.B
        dsp.io.ACIN_EXP  := 0.U
        dsp.io.ACIN_MAN  := 0.U
        dsp.io.BCIN_SIGN := false.B
        dsp.io.BCIN_EXP  := 0.U
        dsp.io.BCIN_MAN  := 0.U
        dsp.io.PCIN      := 0.U
        // Clock enables
        dsp.io.CEA1       := ce
        dsp.io.CEA2       := ce
        dsp.io.CEB        := ce
        dsp.io.CEC        := ce
        dsp.io.CED        := ce
        dsp.io.CEFPA      := ce
        dsp.io.CEFPINMODE := ce
        dsp.io.CEFPM      := ce
        dsp.io.CEFPMPIPE  := ce
        dsp.io.CEFPOPMODE := ce
        // Resets: all deasserted
        dsp.io.ASYNC_RST   := false.B
        dsp.io.RSTA        := false.B
        dsp.io.RSTB        := false.B
        dsp.io.RSTC        := false.B
        dsp.io.RSTD        := false.B
        dsp.io.RSTFPA      := false.B
        dsp.io.RSTFPINMODE := false.B
        dsp.io.RSTFPM      := false.B
        dsp.io.RSTFPMPIPE  := false.B
        dsp.io.RSTFPOPMODE := false.B

        (dsp.io.FPA_OUT, dsp.io.PCOUT)
    }
}