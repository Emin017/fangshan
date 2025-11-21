// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>

import mill._
import mill.scalalib._
import mill.define.{Command, TaskModule}
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import mill.scalalib.TestModule.Utest
import mill.util.Jvm
import coursier.maven.MavenRepository
import $file.dependencies.chisel.build
import $file.dependencies.rvdecoderdb.common
import $file.common

object deps {
  val scalaVer = "2.13.15"
  val mainargs = ivy"com.lihaoyi::mainargs:0.5.0"
  val oslib = ivy"com.lihaoyi::os-lib:0.9.1"
  val upickle = ivy"com.lihaoyi::upickle:3.3.1"
}

object chisel extends Chisel

trait Chisel extends millbuild.dependencies.chisel.build.Chisel {
  def crossValue = deps.scalaVer
  override def millSourcePath = os.pwd / "dependencies" / "chisel"
}

object rvdecoderdb extends RVDecoderDB

trait RVDecoderDB extends millbuild.dependencies.rvdecoderdb.common.RVDecoderDBJVMModule {
  def scalaVersion = T(deps.scalaVer)
  def osLibIvy = deps.oslib
  def upickleIvy = deps.upickle
  override def millSourcePath = os.pwd / "dependencies" / "rvdecoderdb" / "rvdecoderdb"
}

object fangshan extends FangShan
trait FangShan extends millbuild.common.FangShanModule with ScalafmtModule {
  def scalaVersion = T(deps.scalaVer)

  def rvdecoderdbModule = rvdecoderdb
  def riscvOpcodesPath = T(PathRef(os.pwd / "dependencies" / "riscv-opcodes"))

  def chiselModule = Some(chisel)
  def chiselPluginJar = T(Some(chisel.pluginModule.jar()))
  def chiselIvy = None
  def chiselPluginIvy = None

  object test extends ScalaTests with TestModule.ScalaTest {
    override def ivyDeps = T(super.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest:3.2.19",
      ivy"org.scalatestplus::scalacheck-1-18:3.2.19.0",
    ))
  }
}

object elaborator extends Elaborator
trait Elaborator extends millbuild.common.ElaboratorModule with ScalafmtModule {
  def scalaVersion = T(deps.scalaVer)

  def circtInstallPath =
    T.input(PathRef(os.Path(T.ctx().env("CIRCT_INSTALL_PATH"))))

  def generators = Seq(fangshan)

  def mainargsIvy = deps.mainargs

  def chiselModule = Some(chisel)
  def chiselPluginJar = T(Some(chisel.pluginModule.jar()))
  def chiselPluginIvy = None
  def chiselIvy = None
}