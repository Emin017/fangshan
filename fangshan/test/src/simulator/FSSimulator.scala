package fangshan.rtl.simulator

import chisel3.simulator._
import svsim._
import java.nio.file.Files
import java.io.File
import scala.reflect.io.Directory

object FSSimulator extends PeekPokeAPI {
  trait SimulatorWithWorkspace {
    def apply[T <: chisel3.RawModule](
      module: => T
    )(body:   T => Unit
    ): Unit
  }

  def getSimulator(workspace: String): SimulatorWithWorkspace = {
    new SimulatorWithWorkspace {
      def apply[T <: chisel3.RawModule](
        module: => T
      )(body:   T => Unit
      ): Unit = {
        simulate(workspace, module)(body)
      }
    }
  }

  def simulate[T <: chisel3.RawModule](
    workspace: String,
    module:    => T
  )(body:      (T) => Unit
  ): Unit = {
    makeSimulator(workspace).simulate(module)({ module => body(module.wrapped) }).result
  }

  private class DefaultVerilatorSimulator(val workspacePath: String) extends SingleBackendSimulator[verilator.Backend] {
    val backend                            = verilator.Backend.initializeFromProcessEnvironment()
    val tag                                = "verilator"
    val commonCompilationSettings          = CommonCompilationSettings()
    val backendSpecificCompilationSettings = verilator.Backend.CompilationSettings()

    sys.addShutdownHook {
      (new Directory(new File(workspacePath))).deleteRecursively()
    }
  }

  private def makeSimulator(workspace: String): DefaultVerilatorSimulator = {
    val id = ProcessHandle.current().pid().toString()
    new DefaultVerilatorSimulator(
      workspacePath = Files.createTempDirectory(s"${workspace}_${id}_").toString
    )
  }
}
