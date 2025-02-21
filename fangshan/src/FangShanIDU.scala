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

  import fangshan.rtl.decoder.{FangShandecodeParameter => decoderParams}

  def signEXT(imm: UInt) = Cat(Fill(20, imm(11)), imm)

  def immI(inst: UInt): UInt = signEXT(inst(31, 20))

  def isAddi(opcode: UInt): Bool = opcode === decoderParams.addiOpcode

  def isEbreak(opcode: UInt): Bool = opcode === decoderParams.ebreakOpcode

  val inst:            UInt         = io.input.bits.inst
  val decodeResult:    DecodeBundle = Decoder.decode(inst)
  val decodeOpcode:    UInt         = decodeResult(Opcode)
  val decodeAluOpcode: UInt         = decodeResult(AluOpcode)

  def srcGen(inst: UInt): Seq[Data] = {
    Seq(inst(19, 15), Mux(decodeAluOpcode(0), immI(inst), inst(24, 20)))
  }

  val src:       Seq[Data] = srcGen(inst)
  val instValid: Bool      = isEbreak(inst) || isAddi(decodeOpcode)

  io.output.valid                := io.input.valid && instValid
  io.output.bits.aluBundle.rs1   := src.head
  io.output.bits.aluBundle.rs2   := src.last
  io.output.bits.ctrlSigs.rd     := Mux(decodeResult(RdEn), inst(11, 7), 0.U)
  io.output.bits.aluBundle.aluOp := decodeAluOpcode
  io.output.bits.ctrlSigs.ebreak := isEbreak(inst)

  dontTouch(decodeResult)
  dontTouch(decodeOpcode)
  dontTouch(decodeAluOpcode)
  assert(!isEbreak(inst) || isAddi(decodeOpcode), "Invalid instruction")
}
