// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2024 Jiuyang Liu <liu@jiuyang.me>

import mill._
import mill.scalalib._

trait HasChisel extends ScalaModule {
  // Define these for building chisel from source
  def chiselModule: Option[ScalaModule]

  override def moduleDeps = super.moduleDeps ++ chiselModule

  def chiselPluginJar: T[Option[PathRef]]

  override def scalacOptions = T(
    super.scalacOptions() ++ chiselPluginJar().map(path => s"-Xplugin:${path.path}") ++ Seq(
      "-Ymacro-annotations",
      "-deprecation",
      "-feature",
      "-language:reflectiveCalls",
      "-language:existentials",
      "-language:implicitConversions"
    )
  )

  override def scalacPluginClasspath: T[Agg[PathRef]] = T(super.scalacPluginClasspath() ++ chiselPluginJar())

  // Define these for building chisel from ivy
  def chiselIvy: Option[Dep]

  override def ivyDeps = T(super.ivyDeps() ++ chiselIvy)

  def chiselPluginIvy: Option[Dep]

  override def scalacPluginIvyDeps: T[Agg[Dep]] = T(
    super.scalacPluginIvyDeps() ++ chiselPluginIvy.map(Agg(_)).getOrElse(Agg.empty[Dep])
  )
}

trait HasRVDecoderDB extends ScalaModule {
  def rvdecoderdbModule: ScalaModule
  def riscvOpcodesPath:  T[PathRef]
  override def moduleDeps = super.moduleDeps ++ Seq(rvdecoderdbModule)
  def riscvOpcodesTar:    T[PathRef]      = T {
    val tmpDir = os.temp.dir()
    os.makeDir(tmpDir / "unratified")
    os.walk(riscvOpcodesPath().path)
      .filter(f =>
        // We only want to rv32i opcodes
        f.baseName.contains("rv64_i") ||
          f.baseName.contains("rv32_i") ||
          f.baseName.contains("rv_i") ||
          f.ext == "csv" // This csv file should be included, and it is needed by the decoder
      )
      .groupBy(_.segments.contains("unratified"))
      .map {
        case (true, fs)  => fs.map(os.copy.into(_, tmpDir / "unratified"))
        case (false, fs) => fs.map(os.copy.into(_, tmpDir))
      }
    os.proc("tar", "cf", T.dest / "riscv-opcodes.tar", ".").call(tmpDir)
    PathRef(T.dest)
  }
  override def resources: T[Seq[PathRef]] = super.resources() ++ Some(riscvOpcodesTar())
}

trait FangShanModule extends ScalaModule with HasChisel with HasRVDecoderDB {
  def rvdecoderdbModule: ScalaModule
  override def moduleDeps = super.moduleDeps ++ Seq(rvdecoderdbModule)
}

trait ElaboratorModule extends ScalaModule with HasChisel {
  def generators:       Seq[ScalaModule]
  def circtInstallPath: T[PathRef]
  override def moduleDeps = super.moduleDeps ++ generators
  def mainargsIvy: Dep
  override def ivyDeps      = T(super.ivyDeps() ++ Seq(mainargsIvy))
  override def javacOptions = T(super.javacOptions() ++ Seq("--enable-preview", "--release", "21"))
  override def forkArgs: T[Seq[String]] = T(
    super.forkArgs() ++ Seq(
      "--enable-native-access=ALL-UNNAMED",
      "--enable-preview",
      s"-Djava.library.path=${circtInstallPath().path / "lib"}"
    )
  )
}
