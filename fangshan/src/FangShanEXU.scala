// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.exu

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.bundle.{EXUInputBundle, EXUOutputBundle}
import fangshan.FangShanParameter

/** EXUParams, which is used to define the parameters of the EXU
  * @param regNum
  *   number of registers
  * @param width
  *   width of registers
  */
case class FangShanEXUParams(
  regNum: Int,
  width: Int) {

  /** regNumWidth, width of the number of registers
    * @return
    *   Int
    */
  def regNumWidth: Int = log2Ceil(regNum)

  /** regWidth, width of registers
    * @return
    *   Int
    */
  def regWidth: Int = width

  /** inputBundle, input bundle of the EXU
    * @return
    *   EXUInputBundle
    */
  def inputBundle: EXUInputBundle = new EXUInputBundle

  /** outputBundle, output bundle of the EXU
    * @return
    *   EXUOutputBundle
    */
  def outputBundle: EXUOutputBundle = new EXUOutputBundle(regWidth, regNum)
}

/** EXUInterface, Execution Unit Interface
  * @param parameter
  *   parameters of the EXU
  */
class FangShanEXUInterface(parameter: FangShanEXUParams) extends Bundle {
  val clock:  Clock                       = Input(Clock())
  val reset:  Bool                        = Input(Bool())
  val input:  DecoupledIO[EXUInputBundle] = Flipped(DecoupledIO(parameter.inputBundle))
  val output: Valid[EXUOutputBundle]      = Valid(parameter.outputBundle)
}

/** EXU, Execution Unit
  * @param parameter
  *   parameters of the EXU
  */
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

  val diffRes: Bool = oldRes =/= res
  val ebreak:  Bool = io.input.bits.ctrlSigs.ebreak
  when(diffRes) {
    oldRes          := res
    io.input.ready  := true.B
    io.output.valid := true.B
  }.otherwise {
    io.input.ready  := false.B
    io.output.valid := false.B
  }
  io.output.bits.update := Mux(io.input.valid && (diffRes || !ebreak), true.B, false.B)
  dontTouch(ebreak)
  dontTouch(res)
  io.output.bits.result := res
  io.output.bits.rd     := io.input.bits.ctrlSigs.rd
}
