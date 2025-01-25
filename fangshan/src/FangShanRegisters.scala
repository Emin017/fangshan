package fangshan.registers

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.log2Ceil
import chisel3.{ImplicitClock, _}

case class FangShanRegistersParams(
  regNum: Int,
  width: Int) {
  def RegNumWidth = log2Ceil(regNum)
  def RegWidth    = width
}

class FangShanRegistersIO(
  regNum:     Int,
  regNumWith: Int,
  width:      Int)
    extends Bundle {
  val clock       = Input(Clock())
  val reset       = Input(Bool())
  val readAddr    = Input(UInt(regNumWith.W))
  val readData    = Output(UInt(width.W))
  val writeAddr   = Input(UInt(regNum.W))
  val writeData   = Input(UInt(width.W))
  val writeEnable = Input(Bool())
}

@instantiable
class FangShanRegistersFile(params: FangShanRegistersParams)
    extends FixedIORawModule(new FangShanRegistersIO(params.regNum, params.RegNumWidth, params.RegWidth))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val registers = RegInit(VecInit(Seq.fill(params.regNum)(0.U(params.width.W))))

  when(io.writeEnable) {
    registers(io.writeAddr) := io.writeData
  }

  io.readData := registers(io.readAddr)
}
