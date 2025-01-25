// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin <cchuqiming@gmail.com>

package fangshan.elaborator

import mainargs._
import fangshan.{FangShan, FangShanParameter}
import chisel3.experimental.util.SerializableModuleElaborator

object FangShanMain extends SerializableModuleElaborator {
  val topName = "FangShan"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class FangShanParameterMain(
    @arg(name = "width") width: Int,
    @arg(name = "regNum") regNum: Int) {
    require(width > 0, "width must be a non-negative integer")
    require(chisel3.util.isPow2(width), "width must be a power of 2")
    def convert: FangShanParameter = FangShanParameter(width, regNum)
  }

  implicit def FangShanParameterMainParser: ParserForClass[FangShanParameterMain] =
    ParserForClass[FangShanParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  FangShanParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[FangShan, FangShanParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
