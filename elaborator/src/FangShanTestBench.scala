// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan.elaborator

import mainargs._
import fangshan.{FangShanTestBench, FangShanTestBenchParameter, TestVerbatimParameter}
import fangshan.elaborator.FangShanMain.FangShanParameterMain
import chisel3.experimental.util.SerializableModuleElaborator

object FangShanTestBenchMain extends SerializableModuleElaborator {
  val topName = "FangShanTestBench"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class FangShanTestBenchParameterMain(
    @arg(name = "testVerbatimParameter") testVerbatimParameter: TestVerbatimParameterMain,
    @arg(name = "fangshanParameter") fangshanParameter:         FangShanParameterMain,
    @arg(name = "timeout") timeout:                             Int,
    @arg(name = "testSize") testSize: Int) {
    def convert: FangShanTestBenchParameter = FangShanTestBenchParameter(
      testVerbatimParameter.convert,
      fangshanParameter.convert,
      timeout,
      testSize
    )
  }

  case class TestVerbatimParameterMain(
    @arg(name = "useAsyncReset") useAsyncReset:       Boolean,
    @arg(name = "initFunctionName") initFunctionName: String,
    @arg(name = "dumpFunctionName") dumpFunctionName: String,
    @arg(name = "clockFlipTick") clockFlipTick:       Int,
    @arg(name = "resetFlipTick") resetFlipTick: Int) {
    def convert: TestVerbatimParameter = TestVerbatimParameter(
      useAsyncReset:    Boolean,
      initFunctionName: String,
      dumpFunctionName: String,
      clockFlipTick:    Int,
      resetFlipTick:    Int
    )
  }

  implicit def TestVerbatimParameterMainParser: ParserForClass[TestVerbatimParameterMain] =
    ParserForClass[TestVerbatimParameterMain]

  implicit def FangShanParameterMainParser: ParserForClass[FangShanParameterMain] =
    ParserForClass[FangShanParameterMain]

  implicit def FangShanTestBenchParameterMainParser: ParserForClass[FangShanTestBenchParameterMain] =
    ParserForClass[FangShanTestBenchParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  FangShanTestBenchParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[FangShanTestBench, FangShanTestBenchParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
