// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.ltl.Property.{eventually, not}
import chisel3.ltl.{AssertProperty, CoverProperty, Delay, Sequence}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util.circt.dpi.{RawClockedNonVoidFunctionCall, RawUnclockedNonVoidFunctionCall}
import chisel3.util.{Counter, HasExtModuleInline, RegEnable, Valid}
import chisel3.layers.Verification.Assume
import chisel3.ltl.AssumeProperty
import fangshan._

/*
object FangShanFormalParameter {
  implicit def rwP: upickle.default.ReadWriter[FangShanFormalParameter] =
    upickle.default.macroRW
}

/** Parameter of [[FangShan]]. */
case class FangShanFormalParameter(fangshanParameter: FangShanParameter) extends SerializableModuleParameter {}

@instantiable
class FangShanFormalOM(parameter: FangShanFormalParameter) extends Class {
  val fangshan   = IO(Output(Property[AnyClassType]()))
  @public
  val fangshanIn = IO(Input(Property[AnyClassType]()))
  fangshan := fangshanIn
}

class FangShanFormalInterface(parameter: FangShanFormalParameter) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val input = Flipped(Valid(new Bundle {
    val x = UInt(parameter.fangshanParameter.width.W)
    val y = UInt(parameter.fangshanParameter.width.W)
  }))
  val om    = Output(Property[AnyClassType]())
}

@instantiable
class FangShanFormal(val parameter: FangShanFormalParameter)
    extends FixedIORawModule(new FangShanFormalInterface(parameter))
    with SerializableModule[FangShanFormalParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock              = io.clock
  override protected def implicitReset: Reset              = io.reset
  // Instantiate DUT.
  val dut:                              Instance[FangShan] = Instantiate(new FangShan(parameter.fangshanParameter))
  // Instantiate OM
  val omInstance = Instantiate(new FangShanFormalOM(parameter))
  io.om                 := omInstance.getPropertyReference.asAnyClassType
  omInstance.fangshanIn := dut.io.om

  dut.io.clock := implicitClock
  dut.io.reset := implicitReset

  // LTL Checker
  import Sequence._
  val inputFire:     Sequence = dut.io.input.fire
  val inputNotFire:  Sequence = !dut.io.input.fire
  val outputFire:    Sequence = dut.io.output.valid
  val outputNotFire: Sequence = !dut.io.output.valid
  val inputNotValid: Sequence = dut.io.input.ready && !dut.io.input.valid

  dut.io.input.bits  := io.input.bits
  dut.io.input.valid := io.input.valid

  AssumeProperty(
    inputNotValid |=> not(inputFire),
    label = Some("FangShan_ASSUMPTION_INPUT_NOT_VALID")
  )
  AssumeProperty(
    dut.io.input.bits.x === 4.U && dut.io.input.bits.y === 6.U,
    label = Some("FangShan_ASSUMPTION_INPUT_4_6")
  )

  AssertProperty(
    inputFire |=> inputNotFire.repeatAtLeast(1) ### outputFire,
    label = Some("FangShan_ALWAYS_RESPONSE")
  )
  AssertProperty(
    inputFire |-> not(inputNotFire.repeatAtLeast(1) ### (outputNotFire.and(inputFire))),
    label = Some("FangShan_NO_DOUBLE_FIRE")
  )
  AssertProperty(
    outputFire |-> dut.io.output.bits === 2.U,
    label = Some("FangShan_RESULT_IS_CORRECT")
  )

  CoverProperty(
    inputNotValid,
    label = Some("FangShan_COVER_BACK_PRESSURE")
  )
}
 */
