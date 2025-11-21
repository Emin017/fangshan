package fangshan.rtl.simulator

import chisel3.simulator._
import svsim._

class FSVerilatorSimulator(val workspacePath: String) extends SingleBackendSimulator[verilator.Backend] {
  val backend                            = verilator.Backend.initializeFromProcessEnvironment()
  val tag                                = "verilator"
  val commonCompilationSettings          = CommonCompilationSettings()
  val backendSpecificCompilationSettings = verilator.Backend.CompilationSettings()
}
