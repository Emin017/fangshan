// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan.exu

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.bundle.{EXUInputBundle, EXUOutputBundle}
import fangshan.FangShanParameter

case class FangShanEXUParams(
  regNum: Int,
  width: Int) {
  def regNumWidth: Int = log2Ceil(regNum)

  def regWidth: Int = width

  def inputBundle: EXUInputBundle = new EXUInputBundle

  def outputBundle: EXUOutputBundle = new EXUOutputBundle(regWidth, regNum)
}

class FangShanEXUInterface(parameter: FangShanEXUParams) extends Bundle {
  val clock:  Clock                       = Input(Clock())
  val reset:  Bool                        = Input(Bool())
  val input:  DecoupledIO[EXUInputBundle] = Flipped(DecoupledIO(parameter.inputBundle))
  val output: Valid[EXUOutputBundle]      = Valid(parameter.outputBundle)
}
@instantiable
class FangShanEXU(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanEXUInterface(parameter.exuParams))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val oldRes: UInt = RegInit(0.U(parameter.width.W))
  val res:    UInt = WireInit(0.U(parameter.width.W))
  res := io.input.bits.aluBundle.rs1 + io.input.bits.aluBundle.rs2

  when(oldRes =/= res) {
    oldRes                := res
    io.output.bits.update := true.B
    io.input.ready        := true.B
    io.output.valid       := true.B
  }.otherwise {
    io.output.bits.update := false.B
    io.input.ready        := false.B
    io.output.valid       := false.B
  }
  io.output.bits.result := res
  io.output.bits.rd     := io.input.bits.ctrlSigs.rd
}
