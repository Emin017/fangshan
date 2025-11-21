package fangshan.rtl.decoder

import chisel3._
import fangshan.rtl.simulator._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3.simulator.PeekPokeAPI._

class DecoderSpec extends AnyFlatSpec with Matchers {
  // Wrapper to expose Decoder outputs for testing
  class DecoderWrapper extends Module {
    val io = IO(new Bundle {
      val inst = Input(UInt(32.W))
      val out  = Output(new Bundle {
        val opcode    = UInt(8.W)
        val immType   = UInt(3.W)
        val aluOpcode = UInt(3.W)
        val lsuOpcode = UInt(FangShanDecodeParameter.LSUOpcode.lsuOpcodeBits.W)
        val rs1En     = Bool()
        val rs2En     = Bool()
        val rdEn      = Bool()
      })
    })

    val decodeResult = Decoder.decode(io.inst)

    io.out.opcode    := decodeResult(Opcode)
    io.out.immType   := decodeResult(ImmType)
    io.out.aluOpcode := decodeResult(AluOpcode)
    io.out.lsuOpcode := decodeResult(LsuOpcode)
    io.out.rs1En     := decodeResult(Rs1En)
    io.out.rs2En     := decodeResult(Rs2En)
    io.out.rdEn      := decodeResult(RdEn)
  }

  val simulator = new FSVerilatorSimulator("test-dir/decoderSpec")

  behavior.of("Decoder")

  it should "decode ADDI instruction correctly" in {
    simulator.simulate(new DecoderWrapper) { controller =>
      val dut      = controller.wrapped
      // ADDI x1, x0, 1
      // imm=1(12), rs1=0(5), funct3=0(3), rd=1(5), opcode=0010011(7)
      // 000000000001 00000 000 00001 0010011
      // 0x00100093
      val addiInst = "h00100093".U(32.W)

      dut.io.inst.poke(addiInst)
      dut.clock.step()

      // Verify Opcode
      dut.io.out.opcode.expect(FangShanDecodeParameter.addiOpcode.value)

      // Verify Control Signals
      dut.io.out.rs1En.expect(true.B)
      dut.io.out.rs2En.expect(false.B)
      dut.io.out.rdEn.expect(true.B)

      // Verify ALU Opcode (ADDI -> b001)
      dut.io.out.aluOpcode.expect(1.U)
    }
  }

  it should "decode SW instruction correctly" in {
    simulator.simulate(new DecoderWrapper) { controller =>
      val dut    = controller.wrapped
      // SW x1, 0(x2)
      // imm=0(12), rs2=1(5), rs1=2(5), funct3=010(3), imm=0(5), opcode=0100011(7)
      // 0000000 00001 00010 010 00000 0100011
      // 0x00112023
      val swInst = "h00112023".U(32.W)

      dut.io.inst.poke(swInst)
      dut.clock.step()

      // Verify Opcode
      dut.io.out.opcode.expect(FangShanDecodeParameter.swOpcode.value)

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
    simulator.simulate(new DecoderWrapper) { controller =>
      val dut    = controller.wrapped
      // LB x1, 4(x2)
      // imm=4(12), rs1=2(5), funct3=000(3), rd=1(5), opcode=0000011(7)
      // 000000000100 00010 000 00001 0000011
      // 0x00410083
      val lbInst = "h00410083".U(32.W)

      dut.io.inst.poke(lbInst)
      dut.clock.step()

      // Verify Opcode
      dut.io.out.opcode.expect(FangShanDecodeParameter.lbOpcode.value)

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
    simulator.simulate(new DecoderWrapper) { controller =>
      val dut     = controller.wrapped
      // LBU x3, 8(x4)
      // imm=8(12), rs1=4(5), funct3=100(3), rd=3(5), opcode=0000011(7)
      // 000000001000 00100 100 00011 0000011
      // 0x00824183
      val lbuInst = "h00824183".U(32.W)

      dut.io.inst.poke(lbuInst)
      dut.clock.step()

      // Verify Opcode
      dut.io.out.opcode.expect(FangShanDecodeParameter.lbuOpcode.value)

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
    simulator.simulate(new DecoderWrapper) { controller =>
      val dut    = controller.wrapped
      // LW x5, 12(x6)
      // imm=12(12), rs1=6(5), funct3=010(3), rd=5(5), opcode=0000011(7)
      // 000000001100 00110 010 00101 0000011
      // 0x00C32283
      val lwInst = "h00C32283".U(32.W)

      dut.io.inst.poke(lwInst)
      dut.clock.step()

      // Verify Opcode
      dut.io.out.opcode.expect(FangShanDecodeParameter.lwOpcode.value)

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
}
