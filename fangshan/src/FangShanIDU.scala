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
  width:  Int,
  lsuOpBits: Int) {
  def regNumWidth: Int = log2Ceil(regNum)

  /** regWidth, width of registers
    * @return
    *   Int
    */
  def regWidth: Int = width
}

/** IDUInterface, Instruction Decode Unit Interface
  * @param parameter
  *   parameters of the IDU
  */
class FangShanIDUInterface(parameter: FangShanIDUParams) extends Bundle {
  val clock:  Clock                        = Input(Clock())
  val reset:  Reset                        = Input(Bool())
  val input:  Valid[IDUInputBundle]        = Flipped(Valid(new IDUInputBundle(parameter.width)))
  val output: DecoupledIO[IDUOutputBundle] = DecoupledIO(new IDUOutputBundle(parameter.lsuOpBits))
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

  import fangshan.rtl.decoder.{FangShanDecodeParameter => decoderParams}

  def signEXT(imm: UInt) = Cat(Fill(20, imm(11)), imm)

  def immI(inst: UInt): UInt = signEXT(inst(31, 20))

  val inst:            UInt         = io.input.bits.inst
  val decodeResult:    DecodeBundle = Decoder.decode(inst)
  val decodeRs1En:     Bool         = decodeResult(Rs1En)
  val decodeRs2En:     Bool         = decodeResult(Rs2En)
  val decodeRdEn:      Bool         = decodeResult(RdEn)
  val decodeOpcode:    UInt         = decodeResult(Opcode)
  val decodeAluOpcode: UInt         = decodeResult(AluOpcode)
  val decodeLsuOpcode: UInt         = decodeResult(LsuOpcode)

  val instValid: Bool = decoderParams.isInOpcodeSet(decodeOpcode)

  io.output.valid                   := io.input.valid && instValid
  io.output.bits.aluBundle.rs1      := Mux(decodeRs1En, inst(19, 15), 0.U)
  io.output.bits.aluBundle.rs2      := Mux(decodeRs2En, inst(24, 20), immI(inst))
  io.output.bits.ctrlSigs.rd        := Mux(decodeRdEn, inst(11, 7), 0.U)
  io.output.bits.aluBundle.opcode   := decodeAluOpcode
  io.output.bits.ctrlSigs.lsuOpcode := decodeLsuOpcode
  io.output.bits.ctrlSigs.ebreak    := decodeOpcode === decoderParams.ebreakOpcode

  dontTouch(decodeResult)
  dontTouch(decodeOpcode)
  dontTouch(decodeAluOpcode)
  assert(instValid, "Invalid instruction")
}
