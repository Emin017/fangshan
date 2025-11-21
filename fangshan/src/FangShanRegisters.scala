// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.rtl

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util.log2Ceil

/** FangShanRegistersParams, which is used to define the parameters of the registers
  * @param num
  *   number of registers
  * @param width
  *   width of registers
  */
case class FangShanRegistersParams(
  num: Int,
  width: Int) {
  def regNumbers: Int = log2Ceil(num)

  def dataWidth: Int = width
}

/** FangShanRegProbe, which is used to define the probe of the registers
  * @param parameter
  *   parameters of the registers
  */
class FangShanRegProbe(params: FangShanRegistersParams) extends Bundle {
  val haltValue: UInt = UInt(params.dataWidth.W)
}

/** FangShanRegistersIO, which is used to define the IO of the registers
  * @param params
  *   parameters of the registers
  */
class FangShanRegistersIO(params: FangShanRegistersParams) extends Bundle {
  val clock:       Clock            = Input(Clock())
  val reset:       Bool             = Input(Bool())
  val readAddr:    UInt             = Input(UInt(params.regNumbers.W))
  val readData:    UInt             = Output(UInt(params.dataWidth.W))
  val writeAddr:   UInt             = Input(UInt(params.regNumbers.W))
  val writeData:   UInt             = Input(UInt(params.dataWidth.W))
  val writeEnable: Bool             = Input(Bool())
  val probe:       FangShanRegProbe = Output(Probe(new FangShanRegProbe(params), layers.Verification))
}

/** FangShanRegistersFile, which is used to define the registers
  * @param parameter
  *   parameters of the registers
  */
@instantiable
class FangShanRegistersFile(parameter: FangShanRegistersParams)
    extends FixedIORawModule(new FangShanRegistersIO(parameter))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val registers: Vec[UInt] = RegInit(VecInit(Seq.fill(parameter.num)(0.U(parameter.width.W))))

  when(io.writeEnable) {
    registers(io.writeAddr) := io.writeData
  }

  io.readData := registers(io.readAddr)

  val probeWire: FangShanRegProbe = Wire(new FangShanRegProbe(parameter))
  define(io.probe, ProbeValue(probeWire))
  probeWire.haltValue := registers(10)

  dontTouch(probeWire)
  dontTouch(registers)
}
