// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.properties.{AnyClassType, Class, ClassType, Property}
import chisel3.util.circt.dpi.RawUnclockedNonVoidFunctionCall
import chisel3.util.{Counter, HasExtModuleInline}

object FangShanTestBenchParameter {
  implicit def rwP: upickle.default.ReadWriter[FangShanTestBenchParameter] =
    upickle.default.macroRW
}

/** Parameter of [[FangShan]]. */
case class FangShanTestBenchParameter(
  testVerbatimParameter: TestVerbatimParameter,
  fangshanParameter:     FangShanParameter,
  timeout:               Int,
  testSize:              Int)
    extends SerializableModuleParameter {
  require(
    (testVerbatimParameter.useAsyncReset) ||
      (!testVerbatimParameter.useAsyncReset),
    "Reset Type check failed."
  )
}

@instantiable
class FangShanTestBenchOM(parameter: FangShanTestBenchParameter) extends Class {
  val fangshan:   Property[ClassType] = IO(Output(Property[AnyClassType]()))
  @public
  val fangshanIn: Property[ClassType] = IO(Input(Property[AnyClassType]()))

  fangshan := fangshanIn
}

class FangShanTestBenchInterface(parameter: FangShanTestBenchParameter) extends Bundle {
//  val om = Output(Property[AnyClassType]())
}

@instantiable
class FangShanTestBench(val parameter: FangShanTestBenchParameter)
    extends FixedIORawModule(new FangShanTestBenchInterface(parameter))
    with SerializableModule[FangShanTestBenchParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock                  = verbatim.io.clock
  override protected def implicitReset: Reset                  = verbatim.io.reset
  // Instantiate Drivers
  val verbatim:                         Instance[TestVerbatim] = Instantiate(
    new TestVerbatim(parameter.testVerbatimParameter)
  )
  // Instantiate DUT.
  val dut:                              Instance[FangShan]     = Instantiate(new FangShan(parameter.fangshanParameter))
  // Instantiate OM
  //  val omInstance = Instantiate(new FangShanTestBenchOM(parameter))
  //  io.om := DontCare
  //  omInstance.fangshanIn := dut.io.om

  dut.io.clock := implicitClock
  dut.io.reset := implicitReset

  // Simulation Logic
  val simulationTime: UInt = RegInit(0.U(64.W))
  simulationTime := simulationTime + 1.U

  val (_, callWatchdog) = Counter(true.B, parameter.timeout / 2)
  val watchdogCode: UInt = RawUnclockedNonVoidFunctionCall("fangshan_watchdog", UInt(8.W))(callWatchdog)

  // Stop simulation when watchdogCode is not 0 or the DUT is not busy after reset cycle.
  val stopCondition: Bool = (watchdogCode =/= 0.U) ||
    (!dut.io.reset.asBool && !dut.io.output && (simulationTime > 0.U))
  when(stopCondition) {
    stop(cf"""{"event":"SimulationStop","reason": ${watchdogCode},"cycle":${simulationTime}}\n""")
  }

  dut.io.input.valid := false.B;
  dut.io.input.bits  := DontCare;
}

object TestVerbatimParameter {
  implicit def rwP: upickle.default.ReadWriter[TestVerbatimParameter] =
    upickle.default.macroRW
}

case class TestVerbatimParameter(
  useAsyncReset:    Boolean,
  initFunctionName: String,
  dumpFunctionName: String,
  clockFlipTick:    Int,
  resetFlipTick:    Int)
    extends SerializableModuleParameter

@instantiable
class TestVerbatimOM(parameter: TestVerbatimParameter) extends Class {
  val useAsyncReset:    Property[Boolean]   = IO(Output(Property[Boolean]()))
  val initFunctionName: Property[String]    = IO(Output(Property[String]()))
  val dumpFunctionName: Property[String]    = IO(Output(Property[String]()))
  val clockFlipTick:    Property[Int]       = IO(Output(Property[Int]()))
  val resetFlipTick:    Property[Int]       = IO(Output(Property[Int]()))
  val fangshan:         Property[ClassType] = IO(Output(Property[AnyClassType]()))
  @public
  val fangshanIn:       Property[ClassType] = IO(Input(Property[AnyClassType]()))
  fangshan         := fangshanIn
  useAsyncReset    := Property(parameter.useAsyncReset)
  initFunctionName := Property(parameter.initFunctionName)
  dumpFunctionName := Property(parameter.dumpFunctionName)
  clockFlipTick    := Property(parameter.clockFlipTick)
  resetFlipTick    := Property(parameter.resetFlipTick)
}

/** Test blackbox for clockgen, wave dump and extra testbench-only codes. */
class TestVerbatimInterface(parameter: TestVerbatimParameter) extends Bundle {
  val clock: Clock = Output(Clock())
  val reset: Reset = Output(Bool())
}

@instantiable
class TestVerbatim(parameter: TestVerbatimParameter)
    extends FixedIOExtModule(new TestVerbatimInterface(parameter))
    with HasExtModuleInline {
  setInline(
    s"$desiredName.sv",
    s"""module $desiredName(output reg clock, output reg reset);
       |  export "DPI-C" function ${parameter.dumpFunctionName};
       |  function ${parameter.dumpFunctionName}(input string file);
       |`ifdef VCS
       |    $$fsdbDumpfile(file);
       |    $$fsdbDumpvars("+all");
       |    $$fsdbDumpSVA;
       |    $$fsdbDumpon;
       |`endif
       |`ifdef VERILATOR
       |    $$dumpfile(file);
       |    $$dumpvars(0);
       |`endif
       |  endfunction;
       |
       |  import "DPI-C" context function void ${parameter.initFunctionName}();
       |  initial begin
       |    ${parameter.initFunctionName}();
       |    clock = 1'b0;
       |    reset = 1'b1;
       |  end
       |  initial #(${parameter.resetFlipTick}) reset = 1'b0;
       |  always #${parameter.clockFlipTick} clock = ~clock;
       |endmodule
       |""".stripMargin
  )
}
