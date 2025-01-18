package fangshan.bundle

import chisel3._
import chisel3.util._

class CtrlSigBundle extends Bundle {
  val rd = UInt(4.W)
}

class ALUInputBundle extends Bundle {
  val rs1   = UInt(32.W)
  val rs2   = UInt(32.W)
  val aluOp = UInt(4.W)
}
