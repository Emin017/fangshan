package fangshan.exu

import chisel3.{ImplicitClock, _}
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.probe.Probe
import chisel3.properties.{AnyClassType, Property}
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.bundle.{ALUInputBundle, CtrlSigBundle}
import fangshan.{FangShanParameter, FangShanProbe}

case class FangShanEXUParams(
  regNum: Int,
  width: Int) {
  def regNumWidth = log2Ceil(regNum)

  def regWidth = width

  def inputBundle = {
    new Bundle {
      val aluBundle = new ALUInputBundle
      val ctrlsigs  = new CtrlSigBundle
    }
  }

  def outputBundle = {
    new Bundle {
      val update = Bool()
      val result = UInt(regWidth.W)
      val id     = UInt(regNum.W)
    }
  }
}

class FangShanEXUInterface(parameter: FangShanEXUParams) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(Bool())
  val input  = Flipped(DecoupledIO(parameter.inputBundle))
  val output = Valid(parameter.outputBundle)
  //  val probe  = Output(Probe(new FangShanProbe(parameter), layers.Verification))
  //  val om     = Output(Property[AnyClassType]())
}
@instantiable
class FangShanEXU(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanEXUInterface(parameter.exuParams))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  io.input.ready        := true.B
  io.output.bits.update := io.input.bits.ctrlsigs.isAddi
  io.output.bits.result := io.input.bits.aluBundle.rs1 + io.input.bits.aluBundle.rs2
  io.output.bits.id     := io.input.bits.ctrlsigs.id
  io.output.valid       := io.input.bits.ctrlsigs.isAddi
}
