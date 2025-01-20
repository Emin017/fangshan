package fangshan.idu

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.{log2Ceil, Cat, DecoupledIO, Valid}
import fangshan.FangShanParameter
import fangshan.bundle.{IDUInputBundle, IDUOutputBundle}

case class FangShanIDUParams(
  regNum: Int,
  width: Int) {
  def regNumWidth: Int = log2Ceil(regNum)

  def regWidth: Int = width

  def inputBundle: IDUInputBundle = new IDUInputBundle(width)

  def outputBundle: IDUOutputBundle = new IDUOutputBundle
}

class FangShanIDUInterface(parameter: FangShanIDUParams) extends Bundle {
  val clock:  Clock                        = Input(Clock())
  val reset:  Reset                        = Input(Bool())
  val input:  Valid[IDUInputBundle]        = Flipped(Valid(parameter.inputBundle))
  val output: DecoupledIO[IDUOutputBundle] = DecoupledIO(parameter.outputBundle)
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

  val inst: UInt      = io.input.bits.inst
  val src:  Seq[Data] = srcGen(inst)
  io.output.valid                := true.B
  io.output.bits.aluBundle.rs1   := src.head
  io.output.bits.aluBundle.rs2   := src.last
  io.output.bits.ctrlSigs.rd     := rdGen(inst)
  io.output.bits.aluBundle.aluOp := aluOpGen(inst)
}
