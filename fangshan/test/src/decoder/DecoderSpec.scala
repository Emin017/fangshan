package fangshan.rtl.decoder

import chisel3._
import fangshan.rtl.simulator.FSSimulator
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3.simulator.PeekPokeAPI._

class DecoderSpec extends AnyFlatSpec with Matchers {
  // Wrapper to expose Decoder outputs for testing
  class DecoderWrapper extends Module {
    val io = IO(new Bundle {
      val inst = Input(UInt(32.W))
      val out  = Output(new Bundle {
        val instValid = Bool()
        val immType   = UInt(3.W)
        val aluOpcode = UInt(2.W)
        val lsuOpcode = UInt(FangShanDecodeParameter.LSUOpcode.lsuOpcodeBits.W)
        val rs1En     = Bool()
        val rs2En     = Bool()
        val rdEn      = Bool()
      })
    })

    val decodeResult = Decoder.decode(io.inst)

    io.out.instValid := decodeResult(InstValid)
    io.out.immType   := decodeResult(ImmType)
    io.out.aluOpcode := decodeResult(AluOpcode)
    io.out.lsuOpcode := decodeResult(LsuOpcode)
    io.out.rs1En     := decodeResult(Rs1En)
    io.out.rs2En     := decodeResult(Rs2En)
    io.out.rdEn      := decodeResult(RdEn)
  }

  val simulate = FSSimulator.getSimulator("decoder")
  behavior.of("Decoder")

  it should "report InstValid as true for valid RV32I instructions" in {
    simulate(new DecoderWrapper) { dut =>
      // Test ADDI (I-type)
      dut.io.inst.poke("h00100093".U(32.W)) // ADDI x1, x0, 1
      dut.io.out.instValid.expect(true.B)

      // Test ADD (R-type)
      dut.io.inst.poke("h003100B3".U(32.W)) // ADD x1, x2, x3
      dut.io.out.instValid.expect(true.B)

      // Test LW (Load)
      dut.io.inst.poke("h00C32283".U(32.W)) // LW x5, 12(x6)
      dut.io.out.instValid.expect(true.B)

      // Test SW (Store)
      dut.io.inst.poke("h00112023".U(32.W)) // SW x1, 0(x2)
      dut.io.out.instValid.expect(true.B)

      // Test BEQ (Branch)
      dut.io.inst.poke("h00208063".U(32.W)) // BEQ x1, x2, 0
      dut.io.out.instValid.expect(true.B)

      // Test JAL (Jump)
      dut.io.inst.poke("h000000EF".U(32.W)) // JAL x1, 0
      dut.io.out.instValid.expect(true.B)

      // Test JALR (Jump Register)
      dut.io.inst.poke("h000080E7".U(32.W)) // JALR x1, x1, 0
      dut.io.out.instValid.expect(true.B)

      // Test LUI (U-type)
      dut.io.inst.poke("h000010B7".U(32.W)) // LUI x1, 1
      dut.io.out.instValid.expect(true.B)

      // Test AUIPC (U-type)
      dut.io.inst.poke("h00001097".U(32.W)) // AUIPC x1, 1
      dut.io.out.instValid.expect(true.B)
    }
  }

  it should "report InstValid as false for invalid instructions" in {
    simulate(new DecoderWrapper) { dut =>
      // Test invalid opcode (all zeros)
      dut.io.inst.poke("h00000000".U(32.W))
      dut.io.out.instValid.expect(false.B)

      // Test invalid opcode pattern
      dut.io.inst.poke("hFFFFFFFF".U(32.W))
      dut.io.out.instValid.expect(false.B)

      // Test random invalid instruction
      dut.io.inst.poke("h12345678".U(32.W))
      dut.io.out.instValid.expect(false.B)
    }
  }

  it should "decode ADDI instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // ADDI x1, x0, 1
      // imm=1(12), rs1=0(5), funct3=0(3), rd=1(5), opcode=0010011(7)
      // 000000000001 00000 000 00001 0010011
      // 0x00100093
      val addiInst = "h00100093".U(32.W)

      dut.io.inst.poke(addiInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(false.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify ALU Opcode (ADDI -> b001)
      dut.io.out.aluOpcode.expect(1.U)
    }
  }

  it should "decode SW instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // SW x1, 0(x2)
      // imm=0(12), rs2=1(5), rs1=2(5), funct3=010(3), imm=0(5), opcode=0100011(7)
      // 0000000 00001 00010 010 00000 0100011
      // 0x00112023
      val swInst = "h00112023".U(32.W)

      dut.io.inst.poke(swInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(true.B)
      dut.io.out.rdEn.expect(false.B)

      // Verify LSU Opcode
      // SW -> isSigned(0) ## fourByte(10) ## wordMask(1111) ## isStore(1) ## isReadOrWrite(1)
      // 0 ## 10 ## 1111 ## 1 ## 1 = 010111111
      dut.io.out.lsuOpcode.expect(FangShanDecodeParameter.LSUOpcode.swOpcode.value)
    }
  }

  it should "decode LB instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // LB x1, 4(x2)
      // imm=4(12), rs1=2(5), funct3=000(3), rd=1(5), opcode=0000011(7)
      // 000000000100 00010 000 00001 0000011
      // 0x00410083
      val lbInst = "h00410083".U(32.W)

      dut.io.inst.poke(lbInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(false.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify LSU Opcode
      // LB -> isSigned(0) ## oneByte(00) ## byteMask(00000001) ## isLoad(0) ## isReadOrWrite(1)
      // 0 ## 00 ## 00000001 ## 0 ## 1 = 0000000000101
      dut.io.out.lsuOpcode.expect(FangShanDecodeParameter.LSUOpcode.lbOpcode.value)
    }
  }

  it should "decode LBU instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // LBU x3, 8(x4)
      // imm=8(12), rs1=4(5), funct3=100(3), rd=3(5), opcode=0000011(7)
      // 000000001000 00100 100 00011 0000011
      // 0x00824183
      val lbuInst = "h00824183".U(32.W)

      dut.io.inst.poke(lbuInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(false.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify LSU Opcode
      // LBU -> isUnsigned(1) ## oneByte(00) ## byteMask(00000001) ## isLoad(0) ## isReadOrWrite(1)
      // 1 ## 00 ## 00000001 ## 0 ## 1 = 1000000000101
      dut.io.out.lsuOpcode.expect(FangShanDecodeParameter.LSUOpcode.lbuOpcode.value)
    }
  }

  it should "decode LW instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // LW x5, 12(x6)
      // imm=12(12), rs1=6(5), funct3=010(3), rd=5(5), opcode=0000011(7)
      // 000000001100 00110 010 00101 0000011
      // 0x00C32283
      val lwInst = "h00C32283".U(32.W)

      dut.io.inst.poke(lwInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(false.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify LSU Opcode
      // LW -> isSigned(0) ## fourByte(10) ## wordMask(00001111) ## isLoad(0) ## isReadOrWrite(1)
      // 0 ## 10 ## 00001111 ## 0 ## 1 = 0100000111101
      dut.io.out.lsuOpcode.expect(FangShanDecodeParameter.LSUOpcode.lwOpcode.value)
    }
  }

  it should "decode ADD instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // ADD x1, x2, x3
      // funct7=0000000(7), rs2=3(5), rs1=2(5), funct3=000(3), rd=1(5), opcode=0110011(7)
      // 0000000 00011 00010 000 00001 0110011
      // 0x003100B3
      val addInst = "h003100B3".U(32.W)

      dut.io.inst.poke(addInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(true.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify ALU Opcode (ADD -> addOpcode = 0b01)
      dut.io.out.aluOpcode.expect(1.U)
    }
  }

  it should "decode SUB instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // SUB x4, x5, x6
      // funct7=0100000(7), rs2=6(5), rs1=5(5), funct3=000(3), rd=4(5), opcode=0110011(7)
      // 0100000 00110 00101 000 00100 0110011
      // 0x40628233
      val subInst = "h40628233".U(32.W)

      dut.io.inst.poke(subInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(true.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify ALU Opcode (SUB -> subOpcode = 0b00)
      dut.io.out.aluOpcode.expect(0.U)
    }
  }

  it should "decode SRA instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // SRA x7, x8, x9
      // funct7=0100000(7), rs2=9(5), rs1=8(5), funct3=101(3), rd=7(5), opcode=0110011(7)
      // 0100000 01001 01000 101 00111 0110011
      // 0x409453B3
      val sraInst = "h409453B3".U(32.W)

      dut.io.inst.poke(sraInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(true.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify ALU Opcode (SRA -> sraOpcode = 0b11)
      dut.io.out.aluOpcode.expect(3.U)
    }
  }

  it should "decode SRLI instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // SRLI x10, x11, 5
      // imm=000000000101(12), rs1=11(5), funct3=101(3), rd=10(5), opcode=0010011(7)
      // 000000000101 01011 101 01010 0010011
      // 0x0055D513
      val srliInst = "h0055D513".U(32.W)

      dut.io.inst.poke(srliInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(false.B)
      dut.io.out.rdEn.expect(true.B)

      // ALU Opcode for SRLI is otherAluOpcode (don't care pattern)
      // We don't check the exact value since it's a don't care pattern
    }
  }

  it should "decode XOR instruction correctly" in {
    simulate(new DecoderWrapper) { dut =>
      // XOR x12, x13, x14
      // funct7=0000000(7), rs2=14(5), rs1=13(5), funct3=100(3), rd=12(5), opcode=0110011(7)
      // 0000000 01110 01101 100 01100 0110011
      // 0x00E6C633
      val xorInst = "h00E6C633".U(32.W)

      dut.io.inst.poke(xorInst)
      dut.clock.step()

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(true.B)
      dut.io.out.rdEn.expect(true.B)

      // ALU Opcode for XOR is otherAluOpcode (don't care pattern)
      // We don't check the exact value since it's a don't care pattern
    }
  }
}
