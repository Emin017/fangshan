package fangshan.memory

import chisel3._
import chisel3.reflect.DataMirror
import chisel3.util.Valid
import chisel3.util.circt.dpi.RawClockedNonVoidFunctionCall
import fangshan.FangShanParameter
import fangshan.bundle.{MemReadIO, MemWriteIO}

case class FangShanMemoryParams(
  xlen: Int,
  mask: Int) {
  def width: Int = xlen
  def wmask: Int = mask
}

class FangShanMemoryInterface(parameter: FangShanMemoryParams) extends Bundle {
  val clock: Clock = Input(Clock())
  val reset: Reset = Input(Reset())

  /** Memory read interface, includes address */
  val read: Valid[MemReadIO] = Flipped(Valid(new MemReadIO(parameter.width)))

  /** Memory read data */
  val dataOut: UInt = Output(UInt(parameter.width.W))

  /** Memory write interface, include address, write data, write mask */
  val write: Valid[MemWriteIO] = Flipped(Valid(new MemWriteIO(parameter.width, parameter.wmask)))
}

class FangShanMemory(parameter: FangShanParameter) extends RawModule {
  val io: FangShanMemoryInterface = IO(new FangShanMemoryInterface(parameter.memParams))

  io.dataOut := RawClockedNonVoidFunctionCall(
    "mem_read",
    UInt(parameter.width.W),
    Some(Seq("addr", "rvalid")),
    Some("rdata")
  )(io.clock, true.B, io.read.bits.address, io.read.valid)
}
