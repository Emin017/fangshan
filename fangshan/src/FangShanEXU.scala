// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.rtl

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, Instance, Instantiate}
import chisel3.util.{log2Ceil, DecoupledIO, Valid}
import fangshan.rtl.decoder.FangShanDecodeParameter.{LSUOpcode => lsuDecoderParams}
import fangshan.utils.{FangShanUtils => utils}

/** EXUParams, which is used to define the parameters of the EXU
  * @param regNum
  *   number of registers
  * @param width
  *   width of registers
  */
case class FangShanEXUParams(
  regNum:    Int,
  width:     Int,
  wmask:     Int,
  lsuOpBits: Int,
  lsuAXIId: Int) {

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

  def lsuParams: FangShanLSUParams = FangShanLSUParams(xlen = width, regNum = regNum, lsuOpBits, lsuAXIId)

  def aluParams: FangShanALUParams = FangShanALUParams(width)
}

/** EXUInterface, Execution Unit Interface
  * @param parameter
  *   parameters of the EXU
  */
class FangShanEXUInterface(parameter: FangShanEXUParams) extends Bundle {
  val clock:  Clock                       = Input(Clock())
  val reset:  Bool                        = Input(Bool())
  val input:  DecoupledIO[EXUInputBundle] = Flipped(
    DecoupledIO(new EXUInputBundle(parameter.width, parameter.lsuOpBits))
  )
  val output: Valid[EXUOutputBundle]      = Valid(new EXUOutputBundle(parameter.regWidth, parameter.regNum))
}

/** EXU, Execution Unit
  * @param parameter
  *   parameters of the EXU
  */
@instantiable
class FangShanEXU(val parameter: FangShanEXUParams)
    extends FixedIORawModule(new FangShanEXUInterface(parameter))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val lsu: FangShanLSU = Module(new FangShanLSU(parameter.lsuParams))
  utils.withClockAndReset(lsu.io.elements, implicitClock, implicitReset)
  utils.dontCarePorts(lsu.axiIn.elements)
  utils.dontCarePorts(lsu.in.elements)
  lsu.io.input.ctrlInput := lsuDecoderParams.extractLsuOp(io.input.bits.ctrlSigs.lsuOpcode)

  val alu: Instance[FangShanALU] = Instantiate(new FangShanALU(parameter.aluParams))
  utils.dontCarePorts(alu.io.elements)
  dontTouch(alu.io)
  alu.io.input.rs1         := io.input.bits.srcBundle.rs1
  alu.io.input.rs2         := io.input.bits.srcBundle.rs2
  alu.io.input.isAdd       := io.input.bits.ctrlSigs.aluOpcode(0)
  alu.io.input.isArith     := io.input.bits.ctrlSigs.aluOpcode(1)
  alu.io.input.func3Opcode := io.input.bits.ctrlSigs.func3Opcode

  val res: UInt = WireInit(0.U(parameter.width.W))
  res := alu.io.output.result

  val ebreak: Bool = io.input.bits.ctrlSigs.ebreak
  io.input.ready        := true.B
  io.output.valid       := true.B
  io.output.bits.update := true.B && !RegNext(io.reset).asBool
  io.output.bits.result := res
  io.output.bits.rd     := io.input.bits.srcBundle.rd
  dontTouch(ebreak)
  dontTouch(res)
}
