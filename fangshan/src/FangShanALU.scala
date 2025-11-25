package fangshan.rtl

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util._

case class FangShanALUParams(width: Int)

/** ALUInterface, Arithmetic Logic Unit Interface
  * @param parameter
  *   parameters of the ALU
  */
class FangShanALUInterface(parameter: FangShanALUParams) extends Bundle {
  val clock:  Clock           = Input(Clock())
  val reset:  Reset           = Input(Bool())
  val input:  ALUInputBundle  = Flipped(new ALUInputBundle(parameter.width))
  val output: ALUOutputBundle = new ALUOutputBundle(parameter.width)
}

/** ALU, Arithmetic Logic Unit
  * @param parameter
  *   parameters of the ALU
  */
@instantiable
class FangShanALU(val parameter: FangShanALUParams) extends FixedIORawModule(new FangShanALUInterface(parameter)) {
  val aluOp = io.input.func3Opcode

  val src1 = io.input.rs1
  val src2 = io.input.rs2

  val sum       = src1 + src2
  val subResExt = src1 +& (~src2).asUInt +& 1.U

  val sumRes = Mux(io.input.isAdd, sum, subResExt(parameter.width - 1, 0))

  val xor = src1 ^ src2
  val and = src1 & src2
  val or  = src1 | src2

  val slt  = xor(parameter.width - 1) ^ ~subResExt(parameter.width)
  val sltu = ~subResExt(parameter.width)

  val leftShift         = src1 << src2(4, 0)
  val rightShiftLogical = src1 >> src2(4, 0)
  val rightShiftArith   = (src1.asSInt >> src2(4, 0)).asUInt
  val rightShiftRes     = Mux(io.input.isArith, rightShiftArith, rightShiftLogical)
  val shiftedRes        = Mux(aluOp === 1.U, leftShift, rightShiftRes)

  io.output.result := MuxLookup(aluOp, sum)(
    Seq(
      0.U -> sum,
      1.U -> shiftedRes,
      2.U -> slt,
      3.U -> sltu,
      4.U -> xor,
      5.U -> shiftedRes,
      6.U -> or,
      7.U -> and
    )
  )
}
