// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.elaborator

import mainargs._
/*
import fangshan.{FangShanFormal, FangShanFormalParameter}
import fangshan.elaborator.FangShanMain.FangShanParameterMain
import chisel3.experimental.util.SerializableModuleElaborator

object FangShanFormalMain extends SerializableModuleElaborator {
  val topName = "FangShanFormal"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class FangShanFormalParameterMain(
    @arg(name = "fangshanParameter") fangshanParameter: FangShanParameterMain) {
    def convert: FangShanFormalParameter = FangShanFormalParameter(fangshanParameter.convert)
  }

  implicit def FangShanParameterMainParser: ParserForClass[FangShanParameterMain] =
    ParserForClass[FangShanParameterMain]

  implicit def FangShanFormalParameterMainParser: ParserForClass[FangShanFormalParameterMain] =
    ParserForClass[FangShanFormalParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  FangShanFormalParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[FangShanFormal, FangShanFormalParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
 */
