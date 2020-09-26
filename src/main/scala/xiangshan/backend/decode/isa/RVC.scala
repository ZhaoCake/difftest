// This package is used to deal with RVC decode
package xiangshan.backend.decode.isa

import chisel3._
import chisel3.util._
import xiangshan.backend.decode.HasInstrType
import xiangshan.FuType
import xiangshan.backend._

trait HasRVCConst {

  val RVCRegNumTable = List(
    "b000".U -> 8.U,
    "b001".U -> 9.U,
    "b010".U -> 10.U,
    "b011".U -> 11.U,
    "b100".U -> 12.U,
    "b101".U -> 13.U,
    "b110".U -> 14.U,
    "b111".U -> 15.U
  )

  // Imm src
  def ImmNone    = "b10000".U
  def ImmLWSP    = "b00000".U
  def ImmLDSP    = "b00001".U
  def ImmSWSP    = "b00010".U
  def ImmSDSP    = "b00011".U
  def ImmSW      = "b00100".U
  def ImmSD      = "b00101".U
  def ImmLW      = "b00110".U
  def ImmLD      = "b00111".U
  def ImmJ       = "b01000".U
  def ImmB       = "b01001".U
  def ImmLI      = "b01010".U
  def ImmLUI     = "b01011".U
  def ImmADDI    = "b01100".U
  def ImmADDI16SP = "b01101".U
  def ImmADD4SPN = "b01110".U

  // REG src
  def DtCare     = "b0000".U // reg x0
  def REGrs      = "b0011".U
  def REGrt      = "b0001".U
  def REGrd      = "b0010".U
  def REGrs1     = "b0100".U
  def REGrs2     = "b0101".U
  def REGrs1p    = "b0110".U
  def REGrs2p    = "b0111".U
  def REGx1      = "b1000".U
  def REGx2      = "b1001".U
}

object RVCInstr extends HasInstrType with HasRVCConst {

  // RVC 00
//   def C_XX    = BitPat("b????????????????_???_?_10_987_65_432_10")
  def C_ILLEGAL  = BitPat("b0000000000000000_000_0_00_000_00_000_00")
  def C_ADDI4SPN = BitPat("b????????????????_000_?_??_???_??_???_00")
  def C_FLD      = BitPat("b????????????????_001_?_??_???_??_???_00")
//   def C_LQ    = BitPat("b????????????????_001_?_??_???_??_???_00")
  def C_LW       = BitPat("b????????????????_010_?_??_???_??_???_00")
//   def C_FLW   = BitPat("b????????????????_011_?_??_???_??_???_00") // RV32FC Only
  def C_LD       = BitPat("b????????????????_011_?_??_???_??_???_00")
  // def C_LI    = BitPat("b????????????????_100_?_??_???_??_???_00") //reserved
  def C_FSD      = BitPat("b????????????????_101_?_??_???_??_???_00")
//   def C_SQ    = BitPat("b????????????????_101_?_??_???_??_???_00")
  def C_SW       = BitPat("b????????????????_110_?_??_???_??_???_00")
//   def C_FSW   = BitPat("b????????????????_111_?_??_???_??_???_00") // RV32FC Only
  def C_SD       = BitPat("b????????????????_111_?_??_???_??_???_00")

  // RVC 01
  def C_NOP     = BitPat("b????????????????_000_?_00_000_??_???_01")
  def C_ADDI    = BitPat("b????????????????_000_?_??_???_??_???_01")
  // def C_JAL     = BitPat("b????????????????_001_?_??_???_??_???_01")
  def C_ADDIW   = BitPat("b????????????????_001_?_??_???_??_???_01")
  def C_LI      = BitPat("b????????????????_010_?_??_???_??_???_01")
  def C_ADDI16SP= BitPat("b????????????????_011_?_00_010_??_???_01")
  def C_LUI     = BitPat("b????????????????_011_?_??_???_??_???_01")
  def C_SRLI    = BitPat("b????????????????_100_?_00_???_??_???_01")
//   def C_SRLI64    = BitPat("b????????????????_100_0_01_???_00_000_01")
  def C_SRAI    = BitPat("b????????????????_100_?_01_???_??_???_01")
//   def C_SAI64    = BitPat("b????????????????_100_0_01_???_00_000_01")
  def C_ANDI    = BitPat("b????????????????_100_?_10_???_??_???_01")
  def C_SUB     = BitPat("b????????????????_100_0_11_???_00_???_01")
  def C_XOR     = BitPat("b????????????????_100_0_11_???_01_???_01")
  def C_OR      = BitPat("b????????????????_100_0_11_???_10_???_01")
  def C_AND     = BitPat("b????????????????_100_0_11_???_11_???_01")
  def C_SUBW    = BitPat("b????????????????_100_1_11_???_00_???_01")
  def C_ADDW    = BitPat("b????????????????_100_1_11_???_01_???_01")
//   def C_RES     = BitPat("b????????????????_100_1_11_???_??_???_01")
//   def C_RES     = BitPat("b????????????????_100_1_11_???_??_???_01")
  def C_J       = BitPat("b????????????????_101_?_??_???_??_???_01")
  def C_BEQZ    = BitPat("b????????????????_110_?_??_???_??_???_01")
  def C_BNEZ    = BitPat("b????????????????_111_?_??_???_??_???_01")

  //RVC 11
  def C_SLLI    = BitPat("b????????????????_000_?_??_???_??_???_10")
//   def C_SLLI64  = BitPat("b????????????????_000_0_??_???_00_000_10")
  def C_FLDSP   = BitPat("b????????????????_001_?_??_???_??_???_10")
//   def C_LQSP    = BitPat("b????????????????_001_?_??_???_??_???_10")
  def C_LWSP    = BitPat("b????????????????_010_?_??_???_??_???_10")
//  def C_FLWSP   = BitPat("b????????????????_011_?_??_???_??_???_10") // RV32FC Only
  def C_LDSP    = BitPat("b????????????????_011_?_??_???_??_???_10")
  def C_JR      = BitPat("b????????????????_100_0_??_???_00_000_10")
  def C_MV      = BitPat("b????????????????_100_0_??_???_??_???_10")
  def C_EBREAK  = BitPat("b????????????????_100_1_00_000_00_000_10")
  def C_JALR    = BitPat("b????????????????_100_1_??_???_00_000_10")
  def C_ADD     = BitPat("b????????????????_100_1_??_???_??_???_10")
  def C_FSDSP   = BitPat("b????????????????_101_?_??_???_??_???_10")
//   def C_SQSP    = BitPat("b????????????????_101_?_??_???_??_???_10")
  def C_SWSP    = BitPat("b????????????????_110_?_??_???_??_???_10")
//  def C_FSWSP   = BitPat("b????????????????_111_?_??_???_??_???_10") // RV32FC Only
  def C_SDSP    = BitPat("b????????????????_111_?_??_???_??_???_10")

  // TODO: HINT
  // TODO: RES

//   def is_C_ADDI4SPN(op: UInt) = op(12,5) =/= 0.U

  val table = Array(
    C_ILLEGAL    -> List(InstrN, FuType.csr, CSROpType.jmp),
    C_ADDI4SPN   -> List(InstrI, FuType.alu, ALUOpType.add),
    C_FLD        -> List(InstrFI, FuType.ldu, LSUOpType.ld),
    C_LW         -> List(InstrI, FuType.ldu, LSUOpType.lw),
    C_LD         -> List(InstrI, FuType.ldu, LSUOpType.ld),
    C_FSD        -> List(InstrFS, FuType.stu, LSUOpType.sd),
    C_SW         -> List(InstrS, FuType.stu, LSUOpType.sw),
    C_SD         -> List(InstrS, FuType.stu, LSUOpType.sd),
    C_NOP        -> List(InstrI, FuType.alu, ALUOpType.add),
    C_ADDI       -> List(InstrI, FuType.alu, ALUOpType.add),
    // C_JAL        -> List(InstrI, FuType.alu, ALUOpType.add),//RV32C only
    C_ADDIW      -> List(InstrI, FuType.alu, ALUOpType.addw),
    C_LI         -> List(InstrI, FuType.alu, ALUOpType.add),
    C_ADDI16SP   -> List(InstrI, FuType.alu, ALUOpType.add),
    C_LUI        -> List(InstrI, FuType.alu, ALUOpType.add),
    C_SRLI       -> List(InstrI, FuType.alu, ALUOpType.srl),
    C_SRAI       -> List(InstrI, FuType.alu, ALUOpType.sra),
    C_ANDI       -> List(InstrI, FuType.alu, ALUOpType.and),
    C_SUB        -> List(InstrR, FuType.alu, ALUOpType.sub),
    C_XOR        -> List(InstrR, FuType.alu, ALUOpType.xor),
    C_OR         -> List(InstrR, FuType.alu, ALUOpType.or),
    C_AND        -> List(InstrR, FuType.alu, ALUOpType.and),
    C_SUBW       -> List(InstrR, FuType.alu, ALUOpType.subw),
    C_ADDW       -> List(InstrR, FuType.alu, ALUOpType.addw),
    C_J          -> List(InstrJ, FuType.jmp, JumpOpType.jal),
    C_BEQZ       -> List(InstrB, FuType.alu, ALUOpType.beq),
    C_BNEZ       -> List(InstrB, FuType.alu, ALUOpType.bne),
    C_SLLI       -> List(InstrI, FuType.alu, ALUOpType.sll),
    // C_FLDSP      -> List(InstrI, FuType.alu, ALUOpType.add),
    C_LWSP       -> List(InstrI, FuType.ldu, LSUOpType.lw),
    // C_FLWSP      -> List(InstrI, FuType.alu, ALUOpType.add),
    C_LDSP       -> List(InstrI, FuType.ldu, LSUOpType.ld),
    C_JR         -> List(InstrI, FuType.jmp, JumpOpType.jalr),
    C_MV         -> List(InstrR, FuType.alu, ALUOpType.add),
    C_EBREAK     -> List(InstrI, FuType.alu, ALUOpType.add),
    C_JALR       -> List(InstrI, FuType.jmp, JumpOpType.jalr),
    C_ADD        -> List(InstrR, FuType.alu, ALUOpType.add),
    // C_FSDSP      -> List(InstrI, FuType.alu, ALUOpType.add),
    C_SWSP       -> List(InstrS, FuType.stu, LSUOpType.sw),
    // C_FSWSP      -> List(InstrI, FuType.alu, ALUOpType.add),
    C_SDSP       -> List(InstrS, FuType.stu, LSUOpType.sd)
  )

   val cExtraTable = Array(
    C_ADDI4SPN   -> List(ImmADD4SPN, REGx2, DtCare, REGrs2p),
    C_FLD        -> List(ImmLD, REGrs1p, DtCare, REGrs2p),
    C_LW         -> List(ImmLW, REGrs1p, DtCare, REGrs2p),
    C_LD         -> List(ImmLD, REGrs1p, DtCare, REGrs2p),
    C_FSD        -> List(ImmSD, REGrs1p, REGrs2p, DtCare),
    C_SW         -> List(ImmSW, REGrs1p, REGrs2p, DtCare),
    C_SD         -> List(ImmSD, REGrs1p, REGrs2p, DtCare),
    C_NOP        -> List(ImmNone, DtCare, DtCare, DtCare),
    C_ADDI       -> List(ImmADDI, REGrd, DtCare, REGrd),
    // C_JAL        -> List(),
    C_ADDIW      -> List(ImmADDI, REGrd, DtCare, REGrd),
    C_LI         -> List(ImmLI, DtCare, DtCare, REGrd),
    C_ADDI16SP   -> List(ImmADDI16SP, REGx2, DtCare, REGx2),
    C_LUI        -> List(ImmLUI, DtCare, DtCare, REGrd),
    C_SRLI       -> List(ImmLI, REGrs1p, DtCare, REGrs1p),
    C_SRAI       -> List(ImmLI, REGrs1p, DtCare, REGrs1p),
    C_ANDI       -> List(ImmLI, REGrs1p, DtCare, REGrs1p),
    C_SUB        -> List(ImmNone, REGrs1p, REGrs2p, REGrs1p),
    C_XOR        -> List(ImmNone, REGrs1p, REGrs2p, REGrs1p),
    C_OR         -> List(ImmNone, REGrs1p, REGrs2p, REGrs1p),
    C_AND        -> List(ImmNone, REGrs1p, REGrs2p, REGrs1p),
    C_SUBW       -> List(ImmNone, REGrs1p, REGrs2p, REGrs1p),
    C_ADDW       -> List(ImmNone, REGrs1p, REGrs2p, REGrs1p),
    C_J          -> List(ImmJ, DtCare, DtCare, DtCare),
    C_BEQZ       -> List(ImmB, REGrs1p, DtCare, DtCare), // rd: x0
    C_BNEZ       -> List(ImmB, REGrs1p, DtCare, DtCare), // rd: x0
    C_SLLI       -> List(ImmLI, REGrd, DtCare, REGrd),
    C_FLDSP      -> List(ImmLDSP, REGx2, DtCare, REGrd),
    // C_LQSP       -> List(),
    C_LWSP       -> List(ImmLWSP, REGx2, DtCare, REGrd),
    C_LDSP       -> List(ImmLDSP, REGx2, DtCare, REGrd),
    C_JR         -> List(ImmNone, REGrs1, DtCare, DtCare),
    C_MV         -> List(ImmNone, REGrs2, DtCare, REGrd),
    C_EBREAK     -> List(ImmNone, DtCare, DtCare, DtCare), //not implemented
    C_JALR       -> List(ImmNone, REGrs1, DtCare, REGx1),
    C_ADD        -> List(ImmNone, REGrd, REGrs2, REGrd),
    C_FSDSP      -> List(ImmSDSP, REGx2, REGrs2, DtCare),
    // C_SQSP       -> List(),
    C_SWSP       -> List(ImmSWSP, REGx2, REGrs2, DtCare),
    C_SDSP       -> List(ImmSDSP, REGx2, REGrs2, DtCare)
   )

   //TODO: support pc = 2 aligned address
   //TODO: branch predictor support pc = 2 align 
}
