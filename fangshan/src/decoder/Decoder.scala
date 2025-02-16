package fangshan.rtl.decoder

import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode.{DecodeBundle, DecodeField, DecodePattern, DecodeTable}
import org.chipsalliance.rvdecoderdb.Instruction

case class FangShanDecodePattern(inst: Instruction) extends DecodePattern {
  override def bitPat: BitPat = BitPat("b" + inst.encoding.toString)
}

object Opcode extends DecodeField[FangShanDecodePattern, UInt] {
  def name:                               String = "opcode"
  override def chiselType:                UInt   = UInt(8.W)
  def genTable(i: FangShanDecodePattern): BitPat = i.inst.name match {
    case "add" => BitPat("b00000001")
    case "sub" => BitPat("b00000010")
    case "and" => BitPat("b00000011")
    case "or"  => BitPat("b00000100")
    case _     => BitPat("b00000000")
  }
}

object ImmType extends DecodeField[FangShanDecodePattern, UInt] {
  def name:                               String = "imm_type"
  override def chiselType:                UInt   = UInt(3.W)
  def genTable(i: FangShanDecodePattern): BitPat = {
    val immType = i.inst.args
      .map(_.name match {
        case "imm12"                 => BitPat("b000")
        case "imm12hi" | "imm12lo"   => BitPat("b001")
        case "bimm12hi" | "bimm12lo" => BitPat("b010")
        case "imm20"                 => BitPat("b011")
        case "jimm20"                => BitPat("b100")
        case "shamtd"                => BitPat("b101")
        case "shamtw"                => BitPat("b110")
        case _                       => BitPat("b???")
      })
      .filterNot(_ == BitPat("b???"))
      .headOption
      .getOrElse(BitPat("b???"))
    immType
  }
}

object Decoder {
  private def allDecodeField:   Seq[DecodeField[FangShanDecodePattern, UInt]] = Seq(Opcode, ImmType)
  private def allDecodePattern: Seq[FangShanDecodePattern]                    =
    allInstructions.map(FangShanDecodePattern(_)).toSeq.sortBy(_.inst.name)

  private def decodeTable: DecodeTable[FangShanDecodePattern] = new DecodeTable(allDecodePattern, allDecodeField)

  final def decode: UInt => DecodeBundle = decodeTable.decode
  final def bundle: DecodeBundle         = decodeTable.bundle

  private val allInstructions: Seq[Instruction] = {
    org.chipsalliance.rvdecoderdb
      .instructions(org.chipsalliance.rvdecoderdb.extractResource(getClass.getClassLoader))
      .filter { instruction =>
        instruction.instructionSet.name match {
          case "rv32_i" => true // only support rv32i
          case _        => false
        }
      }
  }.toSeq
}
