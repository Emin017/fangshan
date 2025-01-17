// SPDX-License-Identifier: Unlicense
// SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan

import chisel3.{experimental, _}
import chisel3.experimental.hierarchy.{core, instantiable, public, Instance, Instantiate}
import chisel3.experimental.{BaseModule, SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.reflect.DataMirror
import chisel3.util.{DecoupledIO, Valid}
import fangshan.idu.{FangShanIDU, FangShanIDUInterface, FangShanIDUParams}
import fangshan.ifu.{FangShanIFU, FangShanIFUParams}
import fangshan.exu.{FangShanEXU, FangShanEXUParams}
import fangshan.registers.{FangShanRegistersFile, FangShanRegistersParams}

import scala.collection.immutable.SeqMap

object FangShanParameter {
  implicit def rwP: upickle.default.ReadWriter[FangShanParameter] =
    upickle.default.macroRW
}

/** Parameter of [[FangShan]] */
case class FangShanParameter(
  xlen:   Int = 32,
  regNum: Int = 16)
    extends SerializableModuleParameter {

  def width: Int = xlen

  def registerParams = FangShanRegistersParams(regNum, width)

  def ifuParams = FangShanIFUParams(regNum, width)

  def iduParams = FangShanIDUParams(regNum, width)

  def exuParams = FangShanEXUParams(regNum, width)

  def connectClockAndReset(element: SeqMap[String, Data], clock: Clock, reset: Reset) = {
    element.map { case (name, element) =>
      if (name == "clock") {
        element := clock
      } else if (name == "reset") {
        element := reset
      }
    }
  }
}

/** Verification IO of [[FangShan]] */
class FangShanProbe(parameter: FangShanParameter) extends Bundle {
  val busy = Bool()
}

/** Metadata of [[FangShan]]. */
@instantiable
class FangShanOM(parameter: FangShanParameter) extends Class {
  val width: Property[Int] = IO(Output(Property[Int]()))
  width := Property(parameter.width)
}

/** Interface of [[FangShan]]. */
class FangShanInterface(parameter: FangShanParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(Bool())
  val input  = Flipped(DecoupledIO(new Bundle {}))
  val output = UInt(parameter.width.W)
//  val probe  = Output(Probe(new FangShanProbe(parameter), layers.Verification))
//  val om     = Output(Property[AnyClassType]())
}

/** Hardware Implementation of FangShan */
@instantiable
class FangShan(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanInterface(parameter))
    with SerializableModule[FangShanParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val ifu:  Instance[FangShanIFU]           = Instantiate(new FangShanIFU(parameter))
  val idu:  Instance[FangShanIDU]           = Instantiate(new FangShanIDU(parameter))
  val exu:  Instance[FangShanEXU]           = Instantiate(new FangShanEXU(parameter))
  val reg:  Instance[FangShanRegistersFile] = Instantiate(new FangShanRegistersFile(parameter.registerParams))
  val pc:   UInt                            = RegInit(0.U(parameter.width.W))
  val dnpc: UInt                            = WireInit(0.U(parameter.width.W))

  parameter.connectClockAndReset(ifu.io.elements, implicitClock, implicitReset)
  parameter.connectClockAndReset(idu.io.elements, implicitClock, implicitReset)
  parameter.connectClockAndReset(exu.io.elements, implicitClock, implicitReset)
  parameter.connectClockAndReset(reg.io.elements, implicitClock, implicitReset)

  io.output      := DontCare
  io.input.ready := true.B

  ifu.io.input.bits.read := true.B
  ifu.io.input.bits.address :<= pc
  ifu.io.input.valid :<= true.B

  idu.io.input <> ifu.io.output
  idu.io.output <> exu.io.input

  reg.io.clock :<= io.clock
  reg.io.writeEnable :<= exu.io.output.bits.update
  reg.io.writeData :<= exu.io.output.bits.result
  reg.io.writeAddr :<= exu.io.output.bits.id
  reg.io.readAddr := DontCare

  // Assign Probe
//  val probeWire: FangShanProbe = Wire(new FangShanProbe(parameter))
//  define(io.probe, ProbeValue(probeWire))
//  probeWire.busy := exu.io.output.bits.update

  // Assign Metadata
//  val omInstance: Instance[FangShanOM] = Instantiate(new FangShanOM(parameter))
//  io.om := omInstance.getPropertyReference.asAnyClassType

}
