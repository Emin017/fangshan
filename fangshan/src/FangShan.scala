// SPDX-License-Identifier: Unlicense
// SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.properties.{Class, Property}
import chisel3.util.DecoupledIO

import fangshan.idu.{FangShanIDU, FangShanIDUParams}
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

  def wmask: Int = 8

  def registerParams: FangShanRegistersParams = FangShanRegistersParams(regNum, width)

  def ifuParams: FangShanIFUParams = FangShanIFUParams(regNum, width)

  def iduParams: FangShanIDUParams = FangShanIDUParams(regNum, width)

  def exuParams: FangShanEXUParams = FangShanEXUParams(regNum, width)

  def connectClockAndReset(element: SeqMap[String, Data], clock: Clock, reset: Reset): Iterable[Unit] = {
    element.map { case (name, element) =>
      name match {
        case "clock" => element.asInstanceOf[Clock] := clock
        case "reset" => element.asInstanceOf[Reset] := reset
        case _       => ()
      }
    }
  }
}

/** Verification IO of [[FangShan]] */
class FangShanProbe(parameter: FangShanParameter) extends Bundle {
  val busy: Bool = Bool()
}

/** Metadata of [[FangShan]]. */
@instantiable
class FangShanOM(parameter: FangShanParameter) extends Class {
  val width: Property[Int] = IO(Output(Property[Int]()))
  width := Property(parameter.width)
}

/** Interface of [[FangShan]]. */
class FangShanInterface(parameter: FangShanParameter) extends Bundle {
  val clock:  Clock               = Input(Clock())
  val reset:  Reset               = Input(Bool())
  @public
  val input:  DecoupledIO[Bundle] = Flipped(DecoupledIO(new Bundle {}))
  @public
  val output: Bool                = Bool()
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
  val pc:   UInt                            = RegInit("h80000000".U(parameter.width.W))
  val snpc: UInt                            = WireInit("h80000000".U(parameter.width.W))
  val dnpc: UInt                            = WireInit("h80000000".U(parameter.width.W))

  parameter.connectClockAndReset(ifu.io.elements, implicitClock, implicitReset)
  parameter.connectClockAndReset(idu.io.elements, implicitClock, implicitReset)
  parameter.connectClockAndReset(exu.io.elements, implicitClock, implicitReset)
  parameter.connectClockAndReset(reg.io.elements, implicitClock, implicitReset)

  io.input.ready := true.B

  ifu.io.input.bits.read    := pc(31, 2) =/= 0.U
  ifu.io.input.bits.address := pc
  ifu.io.input.valid        := true.B
  idu.io.input <> ifu.io.output
  idu.io.input.bits.inst    := ifu.io.output.bits.inst
  dontTouch(ifu.io.output.bits.inst)
  idu.io.output <> exu.io.input

  reg.io.writeEnable := exu.io.output.bits.update
  reg.io.writeData   := exu.io.output.bits.result
  reg.io.writeAddr   := exu.io.output.bits.rd
  reg.io.readAddr    := DontCare

  snpc := pc
  dnpc := Mux(exu.io.output.bits.update, snpc, pc + 4.U)
  pc   := dnpc

  io.output := exu.io.output.bits.update

  dontTouch(io.output)
  dontTouch(ifu.io.output)
  dontTouch(idu.io.input)
  dontTouch(idu.io.output)
  dontTouch(exu.io.input)
  dontTouch(exu.io.output)

  // Assign Probe
//  val probeWire: FangShanProbe = Wire(new FangShanProbe(parameter))
//  define(io.probe, ProbeValue(probeWire))
//  probeWire.busy := exu.io.output.bits.update

  // Assign Metadata
//  val omInstance: Instance[FangShanOM] = Instantiate(new FangShanOM(parameter))
//  io.om := omInstance.getPropertyReference.asAnyClassType

}
