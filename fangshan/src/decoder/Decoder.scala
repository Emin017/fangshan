package fangshan.rtl.decoder

import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode.{BoolDecodeField, DecodeBundle, DecodeField, DecodePattern, DecodeTable}
import org.chipsalliance.rvdecoderdb.Instruction

case class FangShanDecodePattern(inst: Instruction) extends DecodePattern {
  override def bitPat: BitPat = BitPat("b" + inst.encoding.toString)
}

object Opcode extends DecodeField[FangShanDecodePattern, UInt] {
  def name: String = "opcode"

  override def chiselType: UInt = UInt(8.W)

  def genTable(i: FangShanDecodePattern): BitPat = i.inst.name match {
    case "addi" => BitPat("b00000001")
    case "slli" => BitPat("b00000010")
    case "srli" => BitPat("b00000011")
    case "srai" => BitPat("b00000100")
    case "andi" => BitPat("b00000101")
    case "ori"  => BitPat("b00000110")
    case "xori" => BitPat("b00000111")
    case _      => {
      println(s"Unknown instruction: ${i.inst}")
      BitPat("b00000000")
    }
  }
}

object ImmType extends DecodeField[FangShanDecodePattern, UInt] {
  def name: String = "imm_type"

  override def chiselType: UInt = UInt(7.W)

  def genTable(i: FangShanDecodePattern): BitPat = {
    val immType = i.inst.args
      .map(_.name match {
        case "imm12"                 => BitPat("b0000001")
        case "imm12hi" | "imm12lo"   => BitPat("b0000010")
        case "bimm12hi" | "bimm12lo" => BitPat("b0000011")
        case "imm20"                 => BitPat("b0000100")
        case "jimm20"                => BitPat("b0000101")
        case "shamtd"                => BitPat("b0000110")
        case "shamtw"                => BitPat("b0000111")
        case _                       => BitPat("b???????")
      })
      .filterNot(_ == BitPat("b???????"))
      .headOption
      .getOrElse(BitPat("b???????"))
    immType
  }
}

object Rs1En extends BoolDecodeField[FangShanDecodePattern] {
  def name: String = "rs1En"

  override def genTable(i: FangShanDecodePattern): BitPat = {
    val rs1En =
      if (
        i.inst.args
          .map(_.name)
          .contains("rs1")
      ) {
        y
      } else {
        n
      }
    rs1En
  }
}

object Rs2En extends BoolDecodeField[FangShanDecodePattern] {
  def name: String = "rs2En"

  override def genTable(i: FangShanDecodePattern): BitPat = {
    val rs2En =
      if (
        i.inst.args
          .map(_.name)
          .contains("rs2")
      ) {
        y
      } else {
        n
      }
    rs2En
  }
}

object RdEn extends BoolDecodeField[FangShanDecodePattern] {
  def name: String = "rdEn"

  override def genTable(i: FangShanDecodePattern): BitPat = {
    val rdEn =
      if (
        i.inst.args
          .map(_.name)
          .contains("rd")
      ) {
        y
      } else {
        n
      }
    rdEn
  }
}

object Decoder {
  private def allDecodeField:   Seq[DecodeField[FangShanDecodePattern, _ >: Bool <: UInt]] =
    Seq(Opcode, ImmType, Rs1En, Rs2En, RdEn)
  private def allDecodePattern: Seq[FangShanDecodePattern]                                 =
    allInstructions.map(FangShanDecodePattern(_)).toSeq.sortBy(_.inst.name)

  private def decodeTable: DecodeTable[FangShanDecodePattern] = {
    val table = new DecodeTable(allDecodePattern, allDecodeField)
    println(table.table)
    table
  }

  final def decode: UInt => DecodeBundle = decodeTable.decode
  final def bundle: DecodeBundle         = decodeTable.bundle

  private val allInstructions: Seq[Instruction] = {
    org.chipsalliance.rvdecoderdb
      .instructions(org.chipsalliance.rvdecoderdb.extractResource(getClass.getClassLoader))
      .filter { instruction =>
        instruction.instructionSet.name match {
          case "rv_i"   => true
          case "rv32_i" => true // only support rv32i
          case _        => false
        }
      }
      .filter(_.pseudoFrom.isEmpty)
  }.toSeq
}
