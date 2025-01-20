package fangshan.bundle

import chisel3._

class IFUInputBundle(width: Int) extends Bundle {
  val read:    Bool = Bool()
  val address: UInt = UInt(width.W)
}

class IFUOutputBundle(width: Int) extends Bundle {
  val inst: UInt = UInt(width.W)
}

class CtrlSigBundle extends Bundle {
  val rd: UInt = UInt(4.W)
}

class ALUInputBundle extends Bundle {
  val rs1:   UInt = UInt(32.W)
  val rs2:   UInt = UInt(32.W)
  val aluOp: UInt = UInt(4.W)
}

class IDUInputBundle(width: Int) extends Bundle {
  val inst: UInt = UInt(width.W)
}

class IDUOutputBundle extends Bundle {
  val aluBundle = new ALUInputBundle
  val ctrlSigs  = new CtrlSigBundle
}

class EXUInputBundle extends Bundle {
  val aluBundle = new ALUInputBundle
  val ctrlSigs  = new CtrlSigBundle
}

class EXUOutputBundle(regWidth: Int, regNum: Int) extends Bundle {
  val update: Bool = Bool()
  val result: UInt = UInt(regWidth.W)
  val rd:     UInt = UInt(regNum.W)
}
