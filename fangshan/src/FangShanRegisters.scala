// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan.registers

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.log2Ceil
import fangshan.FangShanParameter

case class FangShanRegistersParams(
  num: Int,
  width: Int) {
  def regNumbers: Int = log2Ceil(num)

  def dataWidth: Int = width
}

class FangShanRegistersIO(params: FangShanRegistersParams) extends Bundle {
  val clock:       Clock = Input(Clock())
  val reset:       Bool  = Input(Bool())
  val readAddr:    UInt  = Input(UInt(params.regNumbers.W))
  val readData:    UInt  = Output(UInt(params.dataWidth.W))
  val writeAddr:   UInt  = Input(UInt(params.regNumbers.W))
  val writeData:   UInt  = Input(UInt(params.dataWidth.W))
  val writeEnable: Bool  = Input(Bool())
}

@instantiable
class FangShanRegistersFile(params: FangShanParameter)
    extends FixedIORawModule(new FangShanRegistersIO(params.regParams))
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
