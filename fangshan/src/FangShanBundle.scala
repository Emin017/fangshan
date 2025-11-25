// SPDX-License-Identifier: MulanPSL-2.0
// SPDX-FileCopyrightText: 2025 Emin (Qiming Chu) <cchuqiming@gmail.com>

package fangshan.rtl

import chisel3._
import chisel3.util.experimental.decode.DecodeBundle
import fangshan.rtl.axi.AXIBundle

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
class CtrlSigBundle(lsOpBits: Int) extends Bundle {
  val ebreak:      Bool = Bool()
  val lsuOpcode:   UInt = UInt(lsOpBits.W)
  val aluOpcode:   UInt = UInt(2.W)
  val func3Opcode: UInt = UInt(3.W)
}

/** ALUInputBundle, which is used to define the input bundle of the ALU, include [[rs1]], [[rs2]], [[func3Opcode]]
  */
class ALUInputBundle(width: Int) extends Bundle {
  val rs1:         UInt = UInt(width.W)
  val rs2:         UInt = UInt(width.W)
  val pc:          UInt = UInt(width.W)
  val func3Opcode: UInt = UInt(3.W) // Use func3 to define ALU operation
  val isAdd:       Bool = Bool()
  val isArith:     Bool = Bool()
}

class ALUOutputBundle(width: Int) extends Bundle {
  val result: UInt = UInt(width.W)
}

class SrcBundle(width: Int) extends Bundle {
  val rs1: UInt = UInt(width.W)
  val rs2: UInt = UInt(width.W)
  val rd:  UInt = UInt(width.W)
}

/** IDUInputBundle, which is used to define the input bundle of the IDU, include [[inst]]
  */
class IDUInputBundle(width: Int) extends Bundle {
  val inst: UInt = UInt(width.W)
}

/** IDUOutputBundle, which is used to define the output bundle of the IDU, include [[aluBundle]] and [[ctrlSigs]]
  */
class IDUOutputBundle(width: Int, lsuOpBits: Int) extends Bundle {
  val srcBundle = new SrcBundle(width)
  val ctrlSigs  = new CtrlSigBundle(lsuOpBits)
}

class LSUInputBundle(opcodeBits: Int) extends Bundle {
  import fangshan.rtl.decoder.FangShanDecodeParameter.LSUOpcode.LSUExtractBundle
  val ctrlInput = new LSUExtractBundle
  val address   = UInt(32.W)
  val dataInput = UInt(32.W)
}

class LSUOutputBundle extends Bundle {
  val dataOut: UInt = UInt(32.W)
}

/** EXUInputBundle, which is used to define the input bundle of the EXU, include [[aluBundle]] and [[ctrlSigs]]
  */
class EXUInputBundle(width: Int, lsuOpBits: Int) extends Bundle {
  val srcBundle = new SrcBundle(width)
  val ctrlSigs  = new CtrlSigBundle(lsuOpBits)
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
