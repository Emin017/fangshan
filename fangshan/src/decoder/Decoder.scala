package fangshan.rtl.decoder

import chisel3._
import chisel3.util.{BitPat, Cat}
import chisel3.util.experimental.decode.{BoolDecodeField, DecodeBundle, DecodeField, DecodePattern, DecodeTable}
import org.chipsalliance.rvdecoderdb.{extractResource, Instruction}
import fangshan.rtl.decoder.{FangShanDecodeParameter => params}

object FangShanDecodeParameter {
  def ebreakOpcode: BitPat = BitPat("b00000000000100000000000001110011")

  object ALUOpcode {
    def isArithShift:  BitPat = BitPat("b1")
    def notArithShift: BitPat = BitPat("b0")

    def isAdd: BitPat = BitPat("b1")
    def isSub: BitPat = BitPat("b0")

    def aluOpcodeBits: Int = isArithShift.width + isAdd.width

    def addOpcode:      BitPat = notArithShift ## isAdd
    def subOpcode:      BitPat = notArithShift ## isSub
    def sraOpcode:      BitPat = isArithShift ## isAdd
    def otherAluOpcode: BitPat = BitPat("b??")

    def extractBundle: Bundle = new Bundle {
      val isArithShift: Bool = Bool()
      val isAdd:        Bool = Bool()
    }
  }

  object LSUOpcode {
    private def isReadOrWrite:   BitPat = BitPat("b1")
    private def notReadAndWrite: BitPat = BitPat("b0")
    private def enableBits:      Int    = isReadOrWrite.width

    private def isLoad:          BitPat = BitPat("b0")
    private def isStore:         BitPat = BitPat("b1")
    private def isLoadStoreBits: Int    = isStore.width

    private def byteMask:       BitPat = BitPat("b00000001")
    private def halfMask:       BitPat = BitPat("b00000011")
    private def wordMask:       BitPat = BitPat("b00001111")
    private def doubleWordMask: BitPat = BitPat("b11111111")
    private def maskBits:       Int    = doubleWordMask.width

    private def oneByte:   BitPat = BitPat("b00")
    private def twoByte:   BitPat = BitPat("b01")
    private def fourByte:  BitPat = BitPat("b10")
    private def eightByte: BitPat = BitPat("b11")
    private def sizeBits:  Int    = eightByte.width

    private def isSigned:   BitPat = BitPat("b0")
    private def isUnsigned: BitPat = BitPat("b1")
    private def signBits:   Int    = isSigned.width

    // LSU Decode Patterns
    // lsu opcode format: [isSigned(1)] [size(2)] [mask(8)] [isLoadStore(1)] [enable(1)]
    // Load opcodes
    def lbOpcode:  BitPat = isSigned ## oneByte ## byteMask ## isLoad ## isReadOrWrite
    def lhOpcode:  BitPat = isSigned ## twoByte ## halfMask ## isLoad ## isReadOrWrite
    def lwOpcode:  BitPat = isSigned ## fourByte ## wordMask ## isLoad ## isReadOrWrite
    def lbuOpcode: BitPat = isUnsigned ## oneByte ## byteMask ## isLoad ## isReadOrWrite
    def lhuOpcode: BitPat = isUnsigned ## twoByte ## halfMask ## isLoad ## isReadOrWrite

    // Store opcodes
    def sbOpcode: BitPat = isSigned ## oneByte ## byteMask ## isStore ## isReadOrWrite
    def shOpcode: BitPat = isSigned ## twoByte ## halfMask ## isStore ## isReadOrWrite
    def swOpcode: BitPat = isSigned ## fourByte ## wordMask ## isStore ## isReadOrWrite

    def nonOpcode: BitPat = BitPat("b?????????????")

    def lsuOpcodeBits: Int = signBits + sizeBits + maskBits + isLoadStoreBits + enableBits // 1 + 2 + 8 + 1 + 1 = 13

    final class LSUExtractBundle extends Bundle {
      val enableReadWrite: Bool = Bool()
      val isLoadStore:     UInt = UInt(isLoadStoreBits.W)
      val writeMask:       UInt = UInt(maskBits.W)
      val size:            UInt = UInt(sizeBits.W)
      val isSigned:        UInt = UInt(signBits.W)
    }

    def extractBundle: LSUExtractBundle = new LSUExtractBundle

    def extractLsuOp(opcode: UInt): LSUExtractBundle = {
      require(opcode.getWidth == lsuOpcodeBits, s"opcode width must be $lsuOpcodeBits")
      val extractedBundle = new LSUExtractBundle
      opcode.asTypeOf(extractedBundle)
    }
  }
}

/** FangShanDecodePattern, which is used to define the decode pattern of the FangShan
  * @param inst
  *   instruction
  */
case class FangShanDecodePattern(inst: Instruction) extends DecodePattern {
  override def bitPat: BitPat = BitPat("b" + inst.encoding.toString)
}

/** ImmType, which is used to define the immediate type decode field
  */
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

/** AluOpcode, which is used to define the ALU opcode decode field
  */
object AluOpcode extends DecodeField[FangShanDecodePattern, UInt] {
  def name: String = "aluOpcode"

  override def chiselType: UInt = UInt(2.W)

  def genTable(i: FangShanDecodePattern): BitPat = i.inst.name match {
    case "addi" => params.ALUOpcode.addOpcode
    case "add"  => params.ALUOpcode.addOpcode
    case "sub"  => params.ALUOpcode.subOpcode
    case "srai" => params.ALUOpcode.sraOpcode
    case "sra"  => params.ALUOpcode.sraOpcode
    case _      => params.ALUOpcode.otherAluOpcode
  }
}

object LsuOpcode extends DecodeField[FangShanDecodePattern, UInt] {
  def name: String = "loadStoreOpcode"

  override def chiselType: UInt = UInt(params.LSUOpcode.lsuOpcodeBits.W)

  def genTable(i: FangShanDecodePattern): BitPat = i.inst.name match {
    case "lb"  => params.LSUOpcode.lbOpcode
    case "lh"  => params.LSUOpcode.lhOpcode
    case "lw"  => params.LSUOpcode.lwOpcode
    case "lbu" => params.LSUOpcode.lbuOpcode
    case "lhu" => params.LSUOpcode.lhuOpcode
    case "sb"  => params.LSUOpcode.sbOpcode
    case "sh"  => params.LSUOpcode.shOpcode
    case "sw"  => params.LSUOpcode.swOpcode
    case _     => params.LSUOpcode.nonOpcode
  }
}

/** Rs1En, which is used to define the rs1 enable decode field
  */
object Rs1En extends BoolDecodeField[FangShanDecodePattern] {
  def name: String = "rs1En"

  override def genTable(i: FangShanDecodePattern): BitPat = {
    if (i.inst.args.exists(_.name == "rs1")) {
      y
    } else {
      n
    }
  }
}

/** Rs2En, which is used to define the rs2 enable decode field
  */
object Rs2En extends BoolDecodeField[FangShanDecodePattern] {
  def name: String = "rs2En"

  override def genTable(i: FangShanDecodePattern): BitPat = {
    if (i.inst.args.exists(_.name == "rs2")) {
      y
    } else {
      n
    }
  }
}

/** RdEn, which is used to define the rd enable decode field
  */
object RdEn extends BoolDecodeField[FangShanDecodePattern] {
  def name: String = "rdEn"

  override def genTable(i: FangShanDecodePattern): BitPat = {
    if (i.inst.args.exists(_.name == "rd")) {
      y
    } else {
      n
    }
  }
}

object Decoder {
  private def allDecodeField:   Seq[DecodeField[FangShanDecodePattern, _ >: Bool <: UInt]] =
    Seq(ImmType, AluOpcode, LsuOpcode, Rs1En, Rs2En, RdEn)
  private def allDecodePattern: Seq[FangShanDecodePattern]                                 =
    allInstructions.map(FangShanDecodePattern(_)).sortBy(_.inst.name)

  private def decodeTable: DecodeTable[FangShanDecodePattern] = new DecodeTable(allDecodePattern, allDecodeField)

  final def decode: UInt => DecodeBundle = decodeTable.decode
  final def bundle: DecodeBundle         = decodeTable.bundle

  /** allInstructions, all instructions
    * @return
    *   Seq[Instruction]
    */
  private val allInstructions: Seq[Instruction] = {
    // get all instructions from rvdecoderdb
    org.chipsalliance.rvdecoderdb
      .instructions(org.chipsalliance.rvdecoderdb.extractResource(getClass.getClassLoader))
      // filter out instructions that are not rv32i
      .filter { instruction =>
        instruction.instructionSet.name match {
          case "rv_i"   => true
          case "rv32_i" => true // only support rv32i
          case _        => false
        }
      }
      // filter out instructions that are pseudo instructions
      .filter(_.pseudoFrom.isEmpty)
  }.toSeq
}
