// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.bundle

import chisel3._

/** Memory read interface, includes [[address]] */
class MemReadIO(width: Int) extends Bundle {
  val address: UInt = UInt(width.W)
}

/** Memory write interface, include [[address]], write [[data]], write [[mask]] */
class MemWriteIO(width: Int, maskWidth: Int) extends Bundle {
  val address: UInt = UInt(width.W)
  val data:    UInt = UInt(width.W)
  val mask:    UInt = UInt(maskWidth.W)
}

/** IFUInputBundle, which is used to define the input bundle of the IFU, include [[read]] and [[address]]
  */
class IFUInputBundle(width: Int) extends Bundle {
  val read:    Bool = Bool()
  val address: UInt = UInt(width.W)
}

/** IFUOutputBundle, which is used to define the output bundle of the IFU, include [[inst]]
  */
class IFUOutputBundle(width: Int) extends Bundle {
  val inst: UInt = UInt(width.W)
}

/** CtrlSigBundle, which is used to define the control signals, include [[rd]]
  */
class CtrlSigBundle extends Bundle {
  val rd: UInt = UInt(4.W)
}

/** ALUInputBundle, which is used to define the input bundle of the ALU, include [[rs1]], [[rs2]], [[aluOp]]
  */
class ALUInputBundle extends Bundle {
  val rs1:   UInt = UInt(32.W)
  val rs2:   UInt = UInt(32.W)
  val aluOp: UInt = UInt(4.W)
}

/** IDUInputBundle, which is used to define the input bundle of the IDU, include [[inst]]
  */
class IDUInputBundle(width: Int) extends Bundle {
  val inst: UInt = UInt(width.W)
}

/** IDUOutputBundle, which is used to define the output bundle of the IDU, include [[aluBundle]] and [[ctrlSigs]]
  */
class IDUOutputBundle extends Bundle {
  val aluBundle = new ALUInputBundle
  val ctrlSigs  = new CtrlSigBundle
}

/** EXUInputBundle, which is used to define the input bundle of the EXU, include [[aluBundle]] and [[ctrlSigs]]
  */
class EXUInputBundle extends Bundle {
  val aluBundle = new ALUInputBundle
  val ctrlSigs  = new CtrlSigBundle
}

/** EXUOutputBundle, which is used to define the output bundle of the EXU, include [[update]], [[result]], [[rd]]
  * @param regWidth
  *   width of registers
  * @param regNum
  *   number of registers
  */
class EXUOutputBundle(regWidth: Int, regNum: Int) extends Bundle {
  val update: Bool = Bool()
  val result: UInt = UInt(regWidth.W)
  val rd:     UInt = UInt(regNum.W)
}
