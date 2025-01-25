// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan.registers

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.log2Ceil

case class FangShanRegistersParams(
  regNum: Int,
  width: Int) {
  def RegNumWidth: Int = log2Ceil(regNum)

  def RegWidth: Int = width
}

class FangShanRegistersIO(
  regNum:     Int,
  regNumWith: Int,
  width:      Int)
    extends Bundle {
  val clock:       Clock = Input(Clock())
  val reset:       Bool  = Input(Bool())
  val readAddr:    UInt  = Input(UInt(regNumWith.W))
  val readData:    UInt  = Output(UInt(width.W))
  val writeAddr:   UInt  = Input(UInt(regNum.W))
  val writeData:   UInt  = Input(UInt(width.W))
  val writeEnable: Bool  = Input(Bool())
}

@instantiable
class FangShanRegistersFile(params: FangShanRegistersParams)
    extends FixedIORawModule(new FangShanRegistersIO(params.regNum, params.RegNumWidth, params.RegWidth))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val registers: Vec[UInt] = RegInit(VecInit(Seq.fill(params.regNum)(0.U(params.width.W))))

  when(io.writeEnable) {
    registers(io.writeAddr) := io.writeData
  }

  io.readData := registers(io.readAddr)

  dontTouch(registers)
}
