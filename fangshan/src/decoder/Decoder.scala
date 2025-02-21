package fangshan.rtl.decoder

import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode.{BoolDecodeField, DecodeBundle, DecodeField, DecodePattern, DecodeTable}
import org.chipsalliance.rvdecoderdb.Instruction
import fangshan.rtl.decoder.{FangShandecodeParameter => params}

object FangShandecodeParameter {
  def luiOpcode:      BitPat = BitPat("b00000001")
  def auipcOpcode:    BitPat = BitPat("b00000010")
  def jalOpcode:      BitPat = BitPat("b00000011")
  def jalrOpcode:     BitPat = BitPat("b00000100")
  def beqOpcode:      BitPat = BitPat("b00000101")
  def bneOpcode:      BitPat = BitPat("b00000110")
  def bltOpcode:      BitPat = BitPat("b00000111")
  def bgeOpcode:      BitPat = BitPat("b00001000")
  def bltuOpcode:     BitPat = BitPat("b00001001")
  def bgeuOpcode:     BitPat = BitPat("b00001010")
  def lbOpcode:       BitPat = BitPat("b00001011")
  def lhOpcode:       BitPat = BitPat("b00001100")
  def lwOpcode:       BitPat = BitPat("b00001101")
  def lbuOpcode:      BitPat = BitPat("b00001110")
  def lhuOpcode:      BitPat = BitPat("b00001111")
  def sbOpcode:       BitPat = BitPat("b00010000")
  def shOpcode:       BitPat = BitPat("b00010001")
  def swOpcode:       BitPat = BitPat("b00010010")
  def addiOpcode:     BitPat = BitPat("b00010011")
  def sltiOpcode:     BitPat = BitPat("b00010100")
  def srliOpcode:     BitPat = BitPat("b00010101")
  def sraiOpcode:     BitPat = BitPat("b00010110")
  def addOpcode:      BitPat = BitPat("b00010111")
  def subOpcode:      BitPat = BitPat("b00011000")
  def sllOpcode:      BitPat = BitPat("b00011001")
  def sltOpcode:      BitPat = BitPat("b00011010")
  def sltuOpcode:     BitPat = BitPat("b00011011")
  def xorOpcode:      BitPat = BitPat("b00011100")
  def srlOpcode:      BitPat = BitPat("b00011101")
  def sraOpcode:      BitPat = BitPat("b00011110")
  def orOpcode:       BitPat = BitPat("b00011111")
  def andOpcode:      BitPat = BitPat("b00100000")
  def fenceOpcode:    BitPat = BitPat("b00100001")
  def fenceTsoOpcode: BitPat = BitPat("b00100010")
  def pauseOpcode:    BitPat = BitPat("b00100011")
  def ecallOpcode:    BitPat = BitPat("b00100100")
  def ebreakOpcode:   BitPat = BitPat("b00100101")

  private def opcodeSet: Set[BitPat] = Set(
    luiOpcode,
    auipcOpcode,
    jalOpcode,
    jalrOpcode,
    beqOpcode,
    bneOpcode,
    bltOpcode,
    bgeOpcode,
    bltuOpcode,
    bgeuOpcode,
    lbOpcode,
    lhOpcode,
    lwOpcode,
    lbuOpcode,
    lhuOpcode,
    sbOpcode,
    shOpcode,
    swOpcode,
    addiOpcode,
    sltiOpcode,
    srliOpcode,
    sraiOpcode,
    addOpcode,
    subOpcode,
    sllOpcode,
    sltOpcode,
    sltuOpcode,
    xorOpcode,
    srlOpcode,
    sraOpcode,
    orOpcode,
    andOpcode,
    fenceOpcode,
    fenceTsoOpcode,
    pauseOpcode,
    ecallOpcode,
    ebreakOpcode
  )

  def isInOpcodeSet(opcode: UInt): Bool = {
    opcodeSet.map(op => op === opcode).reduce(_ || _)
  }
}

case class FangShanDecodePattern(inst: Instruction) extends DecodePattern {
  override def bitPat: BitPat = BitPat("b" + inst.encoding.toString)
}

object Opcode extends DecodeField[FangShanDecodePattern, UInt] {
  def name: String = "opcode"

  override def chiselType: UInt = UInt(8.W)

  def genTable(i: FangShanDecodePattern): BitPat = i.inst.name match {
    case "lui"    => params.luiOpcode
    case "auipc"  => params.auipcOpcode
    case "jal"    => params.jalOpcode
    case "jalr"   => params.jalrOpcode
    case "beq"    => params.beqOpcode
    case "bne"    => params.bneOpcode
    case "blt"    => params.bltOpcode
    case "bge"    => params.bgeOpcode
    case "bltu"   => params.bltuOpcode
    case "bgeu"   => params.bgeuOpcode
    case "lb"     => params.lbOpcode
    case "lh"     => params.lhOpcode
    case "lw"     => params.lwOpcode
    case "lbu"    => params.lbuOpcode
    case "lhu"    => params.lhuOpcode
    case "sb"     => params.sbOpcode
    case "sh"     => params.shOpcode
    case "sw"     => params.swOpcode
    case "addi"   => params.addiOpcode
    case "ebreak" => params.ebreakOpcode
    case _        =>
      println(s"Unknown instruction: ${i.inst}")
      BitPat("b00000000")
  }
}

object ImmType extends DecodeField[FangShanDecodePattern, UInt] {
  def name: String = "immType"

  override def chiselType: UInt = UInt(3.W)

  def genTable(i: FangShanDecodePattern): BitPat = {
    val immType = i.inst.args
      .map(_.name match {
        case "imm12"                 => BitPat("b001")
        case "imm12hi" | "imm12lo"   => BitPat("b010")
        case "bimm12hi" | "bimm12lo" => BitPat("b011")
        case "imm20"                 => BitPat("b100")
        case "jimm20"                => BitPat("b101")
        case "shamtd"                => BitPat("b110")
        case "shamtw"                => BitPat("b111")
        case _                       => BitPat("b???")
      })
      .filterNot(_ == BitPat("b???"))
      .headOption
      .getOrElse(BitPat("b???"))
    immType
  }
}

object AluOpcode extends DecodeField[FangShanDecodePattern, UInt] {
  def name: String = "aluOpcode"

  override def chiselType: UInt = UInt(3.W)

  def genTable(i: FangShanDecodePattern): BitPat = i.inst.name match {
    case "addi" => BitPat("b001")
    case _      => BitPat("b???")
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
    Seq(Opcode, ImmType, AluOpcode, Rs1En, Rs2En, RdEn)
  private def allDecodePattern: Seq[FangShanDecodePattern]                                 =
    allInstructions.map(FangShanDecodePattern(_)).sortBy(_.inst.name)

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
