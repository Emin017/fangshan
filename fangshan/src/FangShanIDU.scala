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
  val output: DecoupledIO[IDUOutputBundle] = DecoupledIO(new IDUOutputBundle(parameter.width, parameter.lsuOpBits))
}

/** IDU, Instruction Decode Unit
  * @param parameter
  *   parameters of the IDU
  */
@instantiable
class FangShanIDU(val parameter: FangShanIDUParams)
    extends FixedIORawModule(new FangShanIDUInterface(parameter))
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
  val decodeAluOpcode: UInt         = decodeResult(AluOpcode)
  val decodeLsuOpcode: UInt         = decodeResult(LsuOpcode)

  val instValid: Bool = true.B //TODO: Placeholder for future instruction validity check

  io.output.valid                   := io.input.valid && instValid
  io.output.bits.srcBundle.rs1      := Mux(decodeRs1En, inst(19, 15), 0.U)
  io.output.bits.srcBundle.rs2      := Mux(decodeRs2En, inst(24, 20), immI(inst))
  io.output.bits.srcBundle.rd        := Mux(decodeRdEn, inst(11, 7), 0.U)
  io.output.bits.ctrlSigs.aluOpcode := decodeAluOpcode
  io.output.bits.ctrlSigs.lsuOpcode := decodeLsuOpcode
  io.output.bits.ctrlSigs.ebreak    := inst === decoderParams.ebreakOpcode

  dontTouch(decodeResult)
  dontTouch(decodeAluOpcode)
  assert(instValid, "Invalid instruction")
}
