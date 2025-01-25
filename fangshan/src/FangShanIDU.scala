package fangshan.idu

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.properties.{AnyClassType, Property}
import chisel3.util.{log2Ceil, Cat, DecoupledIO, Valid}
import fangshan.FangShanParameter
import fangshan.bundle.{ALUInputBundle, CtrlSigBundle}

case class FangShanIDUParams(
  regNum: Int,
  width: Int) {
  def regNumWidth = log2Ceil(regNum)

  def regWidth = width

  def inputBundle = {
    new Bundle {
      val inst = UInt(width.W)
    }
  }

  def outputBundle = {
    new Bundle {
      val aluBundle = new ALUInputBundle
      val ctrlsigs  = new CtrlSigBundle
    }
  }
}

class FangShanIDUInterface(parameter: FangShanIDUParams) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(Bool())
  val input  = Flipped(Valid(parameter.inputBundle))
  val output = DecoupledIO(parameter.outputBundle)
}

@instantiable
class FangShanIDU(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanIDUInterface(parameter.iduParams))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  def opcode(inst: UInt): UInt = inst(6, 0)

  def funct3(inst: UInt): UInt = inst(14, 12)

  def immI(inst: UInt): UInt = Cat(inst(31, 25))

  def isAddi(inst: UInt): Bool = {
    funct3(inst) === 0.U && opcode(inst) === 0x13.U
  }

  def aluOpGen(inst: UInt): UInt = {
    Mux(isAddi(inst), 1.U, 0.U)
  }

  def srcGen(inst: UInt): Seq[Data] = {
    Seq(inst(19, 15), Mux(isAddi(inst), immI(inst), inst(24, 20)))
  }

  def rdGen(inst: UInt): UInt = inst(11, 7)

  val inst = io.input.bits.inst
  val src  = srcGen(inst)
  io.output.valid                := true.B
  io.output.bits.aluBundle.rs1   := src(0)
  io.output.bits.aluBundle.rs2   := src(1)
  io.output.bits.ctrlsigs.rd     := rdGen(inst)
  io.output.bits.aluBundle.aluOp := aluOpGen(inst)
}
