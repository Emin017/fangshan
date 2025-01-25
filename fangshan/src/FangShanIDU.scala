package fangshan.idu

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.properties.{AnyClassType, Property}
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
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
  //  val probe  = Output(Probe(new FangShanProbe(parameter), layers.Verification))
  //  val om     = Output(Property[AnyClassType]())
}

@instantiable
class FangShanIDU(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanIDUInterface(parameter.iduParams))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  def isAddi(data: UInt): Bool = {
    data(6, 0) === 0x13.U
  }
  val inst = io.input.bits.inst
  io.output.valid                := true.B
  io.output.bits.aluBundle.aluOp := Mux(isAddi(inst), 1.U, 0.U)
  io.output.bits.aluBundle.rs1   := inst(3, 1)
  io.output.bits.aluBundle.rs2   := inst(2, 1)
  io.output.bits.ctrlsigs.id     := 1.U
  io.output.bits.ctrlsigs.isAddi := isAddi(io.input.bits.inst)
}
