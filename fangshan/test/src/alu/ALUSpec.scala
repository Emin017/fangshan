package fangshan.rtl.alu

import chisel3._
import fangshan.rtl.simulator.FSSimulator
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3.simulator.PeekPokeAPI._
import fangshan.rtl.{FangShanALU, FangShanALUParams}

class ALUSpec extends AnyFlatSpec with Matchers {
  def width     = 32
  def dutModule = new FangShanALU(FangShanALUParams(width))

  val simulate = FSSimulator.getSimulator("ALUSpec")

  behavior.of("ALU")
  it should "add rs1 and rs2" in {
    simulate(dutModule) { dut =>
      dut.io.input.rs1.poke(10.U)
      dut.io.input.rs2.poke(15.U)
      dut.io.input.func3Opcode.poke(0.U) // opcode for addition
      dut.io.output.result.expect(25.U)
    }
  }
  it should "set less than correctly" in {
    simulate(dutModule) { dut =>
      dut.io.input.rs1.poke(10.U)
      dut.io.input.rs2.poke(15.U)
      dut.io.input.func3Opcode.poke(2.U)
      dut.io.output.result.expect(1.U)

      dut.io.input.rs1.poke(20.U)
      dut.io.input.rs2.poke(15.U)
      dut.io.input.func3Opcode.poke(2.U)
      dut.io.output.result.expect(0.U)
    }
  }
  it should "handle signed comparisons correctly" in {
    simulate(dutModule) { dut =>
      dut.io.input.rs1.poke("hFFFFFFF0".U) // -16 in signed
      dut.io.input.rs2.poke(15.U)
      dut.io.input.func3Opcode.poke(2.U)
      dut.io.output.result.expect(1.U)     // -16 < 15

      dut.io.input.rs1.poke(10.U)
      dut.io.input.rs2.poke("hFFFFFFF5".U) // -11 in signed
      dut.io.input.func3Opcode.poke(2.U)
      dut.io.output.result.expect(0.U) // 10 > -11
    }
  }

  it should "handle unsigned comparisons correctly" in {
    simulate(dutModule) { dut =>
      dut.io.input.rs1.poke(10.U)
      dut.io.input.rs2.poke(15.U)
      dut.io.input.func3Opcode.poke(3.U)
      dut.io.output.result.expect(true.B) // 10 < 15 unsigned

      dut.io.input.rs1.poke("hFFFFFFF0".U) // 4294967280 in unsigned
      dut.io.input.rs2.poke(15.U)
      dut.io.input.func3Opcode.poke(3.U)
      dut.io.output.result.expect(false.B) // 4294967280 > 15 unsigned
    }
  }

  it should "perform shift operations correctly" in {
    simulate(dutModule) { dut =>
      // sll
      dut.io.input.rs1.poke(1.U)
      dut.io.input.rs2.poke(1.U)
      dut.io.input.func3Opcode.poke(1.U)
      dut.io.output.result.expect(2.U)

      // srl
      dut.io.input.rs1.poke(2.U)
      dut.io.input.rs2.poke(1.U)
      dut.io.input.func3Opcode.poke(5.U)
      dut.io.input.isArith.poke(false.B)
      dut.io.output.result.expect(1.U)

      // sra
      dut.io.input.rs1.poke("hFFFFFFF0".U) // -16
      dut.io.input.rs2.poke(1.U)
      dut.io.input.func3Opcode.poke(5.U)
      dut.io.input.isArith.poke(true.B)
      dut.io.output.result.expect("hFFFFFFF8".U) // -8
    }
  }
}
