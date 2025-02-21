// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.rtl

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util.experimental.decode.DecodeBundle
import chisel3.util.{log2Ceil, Cat, DecoupledIO, Fill, Valid}
import fangshan.rtl.decoder._

/** IDUParams, which is used to define the parameters of the IDU
  * @param regNum
  *   number of registers
  * @param width
  *   width of registers
  */
case class FangShanIDUParams(
  regNum: Int,
  width: Int) {
  def regNumWidth: Int = log2Ceil(regNum)

  /** regWidth, width of registers
    * @return
    *   Int
    */
  def regWidth: Int = width

  /** inputBundle, input bundle of the IDU
    * @return
    *   IDUInputBundle
    */
  def inputBundle: IDUInputBundle = new IDUInputBundle(width)

  /** outputBundle, output bundle of the IDU
    * @return
    *   IDUOutputBundle
    */
  def outputBundle: IDUOutputBundle = new IDUOutputBundle
}

/** IDUInterface, Instruction Decode Unit Interface
  * @param parameter
  *   parameters of the IDU
  */
class FangShanIDUInterface(parameter: FangShanIDUParams) extends Bundle {
  val clock:  Clock                        = Input(Clock())
  val reset:  Reset                        = Input(Bool())
  val input:  Valid[IDUInputBundle]        = Flipped(Valid(parameter.inputBundle))
  val output: DecoupledIO[IDUOutputBundle] = DecoupledIO(parameter.outputBundle)
}

/** IDU, Instruction Decode Unit
  * @param parameter
  *   parameters of the IDU
  */
@instantiable
class FangShanIDU(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanIDUInterface(parameter.iduParams))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  def opcode(inst: UInt): UInt = inst(6, 0)

  def funct3(inst: UInt): UInt = inst(14, 12)

  def immI(inst: UInt): UInt = signEXT(inst(31, 20))

  def isAddi(inst: UInt): Bool = {
    funct3(inst) === 0.U && opcode(inst) === 0x13.U
  }

  def aluOpGen(inst: UInt): UInt = {
    Mux(isAddi(inst), 1.U, 0.U)
  }

  def srcGen(inst: UInt): Seq[Data] = {
    Seq(inst(19, 15), Mux(isAddi(inst), immI(inst), inst(24, 20)))
  }

  def rdGen(inst: UInt): UInt = inst(11, 7)

  def isEbreak(inst: UInt): Bool = inst === 0x00100073.U

  def signEXT(imm: UInt) = Cat(Fill(20, imm(11)), imm)

  val inst:         UInt         = io.input.bits.inst
  val src:          Seq[Data]    = srcGen(inst)
  val decodeResult: DecodeBundle = Decoder.decode(inst)
  val decodeOpcode: UInt         = decodeResult(Opcode)

  io.output.bits.ctrlSigs.opcode := opcode(inst)
  dontTouch(io.output.bits.ctrlSigs.opcode)
  dontTouch(decodeResult)
  dontTouch(decodeOpcode)
  val instValid: Bool = isEbreak(inst) || isAddi(inst)

  io.output.valid                := io.input.valid && instValid
  io.output.bits.aluBundle.rs1   := src.head
  io.output.bits.aluBundle.rs2   := src.last
  io.output.bits.ctrlSigs.rd     := rdGen(inst)
  io.output.bits.aluBundle.aluOp := aluOpGen(inst)
  io.output.bits.ctrlSigs.ebreak := isEbreak(inst)
  io.output.bits.ctrlSigs.rdEn   := decodeResult(RdEn)

  assert(!isEbreak(inst) || isAddi(inst), "Invalid instruction")
}
