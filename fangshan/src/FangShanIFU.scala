package fangshan.ifu

import chisel3.{ImplicitClock, ImplicitReset, _}
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.probe.Probe
import chisel3.properties.{AnyClassType, Property}
import chisel3.util.circt.dpi.RawClockedNonVoidFunctionCall
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.{FangShanParameter, FangShanProbe}

class FangShanMemoryInterface(parameter: FangShanParameter) extends Bundle {
  val clock:    Clock = Input(Clock())
  val reset:    Reset = Input(Reset())
  val raddr:    UInt  = Input(UInt(parameter.width.W))
  val waddr:    UInt  = Input(UInt(parameter.width.W))
  val wmask:    UInt  = Input(UInt(parameter.wmask.W))
  val wdata:    UInt  = Input(UInt(parameter.width.W))
  val MemWrite: Bool  = Input(Bool())
  val MemRead:  Bool  = Input(Bool())
  val rdata:    UInt  = Output(UInt(parameter.width.W))
}

class FangShanMemory(parameter: FangShanParameter) extends RawModule {
  val io = IO(new FangShanMemoryInterface(parameter))

  io.rdata := RawClockedNonVoidFunctionCall(
    "mem_read",
    UInt(parameter.width.W),
    Some(Seq("addr", "MemRead")),
    Some("rdata")
  )(io.clock, true.B, io.raddr, io.MemRead)
}

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
  io.output.valid     := true.B
  io.output.bits.inst := 0.U(parameter.width.W)
  val M = Module(new FangShanMemory(parameter))

  parameter.connectClockAndReset(M.io.elements, implicitClock, implicitReset)

  M.io.raddr    := io.input.bits.address
  M.io.waddr    := 0.U
  M.io.wmask    := 0.U
  M.io.wdata    := 0.U
  M.io.MemWrite := false.B
  M.io.MemRead  := io.input.bits.read

  io.output.bits.inst := M.io.rdata
  dontTouch(io.output.bits.inst)
  dontTouch(io.input.bits.read)
  dontTouch(io.input.bits.address)
  dontTouch(M.io.rdata)
}
