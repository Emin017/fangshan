// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.ifu

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.bundle.{IFUInputBundle, IFUOutputBundle}
import fangshan.FangShanParameter
import fangshan.memory.FangShanMemory
import fangshan.utils.{FangShanUtils => utils}

/** IFUParams, which is used to define the parameters of the IFU
  * @param regNum:
  *   number of registers
  * @param width:
  *   width of registers
  */
case class FangShanIFUParams(
  regNum: Int,
  width: Int) {
  def RegNumWidth: Int = log2Ceil(regNum)

  def RegWidth: Int = width

  def inputBundle: IFUInputBundle = new IFUInputBundle(width)

  def outputBundle: IFUOutputBundle = new IFUOutputBundle(width)
}

/** IFUInterface, Instruction Fetch Unit Interface
  * @param parameter
  *   parameters of the IFU
  */
class FangShanIFUInterface(parameter: FangShanIFUParams) extends Bundle {
  val clock:  Clock                       = Input(Clock())
  val reset:  Reset                       = Input(Bool())
  val input:  DecoupledIO[IFUInputBundle] = Flipped(DecoupledIO(parameter.inputBundle))
  val output: Valid[IFUOutputBundle]      = Valid(parameter.outputBundle)
  //  val probe  = Output(Probe(new FangShanProbe(parameter), layers.Verification))
  //  val om     = Output(Property[AnyClassType]())
}

/** IFU, Instruction Fetch Unit
  * @param parameter
  *   parameters of the IFU
  */
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
  val M: FangShanMemory = Module(new FangShanMemory(parameter))

  utils.withClockAndReset(M.io.elements, implicitClock, implicitReset)

  utils.dontCarePorts(M.io.write.elements)
  M.io.write.valid := false.B

  M.io.read.bits.address := io.input.bits.address
  M.io.read.valid        := io.input.valid
  io.output.bits.inst    := M.io.dataOut

  dontTouch(io.output.bits.inst)
  dontTouch(io.input.bits.read)
  dontTouch(io.input.bits.address)
}
