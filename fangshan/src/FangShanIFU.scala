package fangshan.ifu

import chisel3.{ImplicitClock, _}
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.probe.Probe
import chisel3.properties.{AnyClassType, Property}
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.{FangShanParameter, FangShanProbe}

case class FangShanIFUParams(
  regNum: Int,
  width: Int) {
  def RegNumWidth = log2Ceil(regNum)

  def RegWidth = width

  def inputBundle = {
    new Bundle {
      val read    = Bool()
      val address = UInt(width.W)
    }
  }

  def outputBundle = {
    new Bundle {
      val inst = UInt(width.W)
    }
  }
}

class FangShanIFUInterface(parameter: FangShanIFUParams) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(Bool())
  val input  = Flipped(DecoupledIO(parameter.inputBundle))
  val output = Valid(parameter.outputBundle)
  //  val probe  = Output(Probe(new FangShanProbe(parameter), layers.Verification))
  //  val om     = Output(Property[AnyClassType]())
}
@instantiable
class FangShanIFU(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanIFUInterface(parameter.ifuParams))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  io.input.ready      := true.B
  io.output.valid     := io.input.valid
  io.output.bits.inst := 0.U(parameter.width.W)
}
