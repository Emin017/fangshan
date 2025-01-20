package fangshan.ifu

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.circt.dpi.RawClockedNonVoidFunctionCall
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.bundle.{IFUInputBundle, IFUOutputBundle}
import fangshan.FangShanParameter

class FangShanMemoryInterface(parameter: FangShanParameter) extends Bundle {
  val clock:    Clock = Input(Clock())
  val reset:    Reset = Input(Reset())
  val raddr:    UInt  = Input(UInt(parameter.width.W))
  val waddr:    UInt  = Input(UInt(parameter.width.W))
  val wmask:    UInt  = Input(UInt(parameter.wmask.W))
  val wdata:    UInt  = Input(UInt(parameter.width.W))
  val memWrite: Bool  = Input(Bool())
  val memRead:  Bool  = Input(Bool())
  val rdata:    UInt  = Output(UInt(parameter.width.W))
}

class FangShanMemory(parameter: FangShanParameter) extends RawModule {
  val io: FangShanMemoryInterface = IO(new FangShanMemoryInterface(parameter))

  io.rdata := RawClockedNonVoidFunctionCall(
    "mem_read",
    UInt(parameter.width.W),
    Some(Seq("addr", "MemRead")),
    Some("rdata")
  )(io.clock, true.B, io.raddr, io.memRead)
}

case class FangShanIFUParams(
  regNum: Int,
  width: Int) {
  def RegNumWidth: Int = log2Ceil(regNum)

  def RegWidth: Int = width

  def inputBundle: IFUInputBundle = new IFUInputBundle(width)

  def outputBundle: IFUOutputBundle = new IFUOutputBundle(width)
}

class FangShanIFUInterface(parameter: FangShanIFUParams) extends Bundle {
  val clock:  Clock                       = Input(Clock())
  val reset:  Reset                       = Input(Bool())
  val input:  DecoupledIO[IFUInputBundle] = Flipped(DecoupledIO(parameter.inputBundle))
  val output: Valid[IFUOutputBundle]      = Valid(parameter.outputBundle)
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
  M.io.memWrite := false.B
  M.io.memRead  := io.input.bits.read

  io.output.bits.inst := M.io.rdata
  dontTouch(io.output.bits.inst)
  dontTouch(io.input.bits.read)
  dontTouch(io.input.bits.address)
  dontTouch(M.io.rdata)
}
