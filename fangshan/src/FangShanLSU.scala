package fangshan.rtl

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util._
import fangshan.rtl.decoder.FangShanDecodeParameter.{LSUOpcode => lsuDecoderParams}
import fangshan.rtl.axi.AXIBundle
import fangshan.utils.{FangShanUtils => utils}

case class FangShanLSUParams(
  regNum:     Int,
  width:      Int,
  opcodeBits: Int,
  axiID: Int) {

  /** regNumWidth, width of the number of registers
    * @return
    *   Int
    */
  def regNumWidth: Int = log2Ceil(regNum)

  /** regWidth, width of registers
    * @return
    *   Int
    */
  def regWidth: Int = width

  def maskLen: Int = width / 8
}

class FangShanLSUInterface(parameter: FangShanLSUParams) extends Bundle {
  val clock:  Clock                  = Input(Clock())
  val reset:  Bool                   = Input(Bool())
  val axi:    AXIBundle              = new AXIBundle(32, 32)
  val input:  LSUInputBundle         = Flipped(new LSUInputBundle(parameter.opcodeBits))
  val output: Valid[LSUOutputBundle] = Valid(new LSUOutputBundle)
}

@instantiable
class FangShanLSU(val parameter: FangShanParameter)
    extends FixedIORawModule(new FangShanLSUInterface(parameter.lsuParams))
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  utils.dontCarePorts(io.axi.elements)

  val axiIn    = io.axi
  val in       = io.input
  val lsuCtrls = io.input.ctrlInput

  val readEnable:  Bool = lsuCtrls.enableReadWrite
  val writeEnable: Bool = lsuCtrls.enableReadWrite
  val writeMask:   UInt = lsuCtrls.writeMask

  val out = io.output

  val waddr:    UInt = RegInit(0.U(parameter.xlen.W))
  val wdata:    UInt = WireInit(0.U(parameter.xlen.W))
  val wstrb:    UInt = WireInit(0.U(parameter.lsuParams.maskLen.W))
  val wDataSel: UInt = WireInit(0.U(parameter.lsuParams.maskLen.W))
  val wvalid:   Bool = RegNext(writeEnable)
  val memOut:   UInt = WireInit(0.U(parameter.xlen.W))
  val rvalid:   Bool = RegNext(readEnable)
  val raddr:    UInt = RegEnable(in.address, 0.U(parameter.xlen.W), readEnable)
  val mask:     UInt = writeMask
  val wdataIn:  UInt = Mux(writeEnable, in.dataInput, 0.U(parameter.xlen.W))
  waddr    := Mux(writeEnable, in.address, waddr)
  wDataSel := mask

  val writeShiftSign: UInt = in.address(1, 0) & "b11".U(2.W)
  wstrb    := MuxLookup(writeShiftSign, 0.U(parameter.lsuParams.maskLen.W))(
    Seq(
      0.U -> wDataSel,
      1.U -> (wDataSel << 1),
      2.U -> (wDataSel << 2),
      3.U -> (wDataSel << 3)
    )
  )
  wdata    := MuxLookup(writeShiftSign, 0.U(parameter.xlen.W))(
    Seq(
      0.U -> wdataIn,
      1.U -> (wdataIn << 8),
      2.U -> (wdataIn << 16),
      3.U -> (wdataIn << 24)
    )
  )
  val wdataReg: UInt = RegInit(0.U(parameter.xlen.W))
  wdataReg := Mux(writeEnable, wdata, Mux(axiIn.w.fire, 0.U(parameter.xlen.W), wdataReg))

  val arValid:    Bool      = RegInit(false.B)
  val rReady:     Bool      = RegInit(false.B)
  val awValid:    UInt      = RegNext(writeEnable, 0.U)
  val wValid:     Bool      = RegInit(false.B)
  val bReady:     Bool      = WireInit(true.B)
  val wrOutValid: Bool      = WireInit(false.B)
  val aIn:        AXIBundle = axiIn

  val memReadReg: Bool = RegNext(readEnable, false.B)
  val isload:     Bool = RegInit(false.B)
  val isstore:    Bool = RegInit(false.B)
  isload  := Mux(memReadReg, true.B, Mux(aIn.r.fire, false.B, isload))
  isstore := Mux(writeEnable, true.B, Mux(aIn.b.fire, false.B, isstore))

  val arIdle :: arWaitReady :: arDone :: Nil = Enum(3)
  val awIdle :: awWaitReady :: awDone :: Nil = Enum(3)
  val wIdle :: wDone :: Nil                  = Enum(2)
  val bIdle :: bDone :: Nil                  = Enum(2)
  val rIdle :: rDone :: Nil                  = Enum(2)

  aIn.ar.valid     := arValid
  aIn.ar.bits.addr := raddr
  val arState: UInt = RegInit(arIdle)
  val rState:  UInt = RegInit(rIdle)
  val awState: UInt = RegInit(awIdle)
  val wState:  UInt = RegInit(wIdle)
  val bState:  UInt = RegInit(bIdle)
  switch(arState) {
    is(arIdle) {
      arState := Mux(readEnable, arWaitReady, arIdle)
      arValid := readEnable
    }
    is(arWaitReady) {
      arState          := Mux(aIn.ar.fire, arDone, arWaitReady)
      arValid          := Mux(aIn.ar.fire, false.B, true.B)
      aIn.ar.bits.addr := raddr
    }
    is(arDone) {
      arState          := arIdle
      arValid          := false.B
      aIn.ar.bits.addr := raddr
    }
  }
  aIn.r.ready := rReady
  switch(rState) {
    is(rIdle) {
      rState := Mux(aIn.r.fire && !isload, rDone, rIdle)
      memOut := Mux(aIn.r.fire, aIn.r.bits.data, 0.U)
      rReady := true.B
    }
    is(rDone) {
      rState := rIdle
      rReady := false.B
      memOut := aIn.r.bits.data
    }
  }

  val arSize: UInt = WireInit(0.U(3.W))
  arSize := lsuCtrls.size
  val dataSizeReg: UInt = RegInit(0.U(3.W))
  dataSizeReg      := Mux(readEnable, arSize, Mux(aIn.ar.fire, 0.U(3.W), dataSizeReg))
  aIn.ar.bits.size := dataSizeReg

  aIn.aw.valid     := awValid
  aIn.aw.bits.addr := waddr
  switch(awState) {
    is(awIdle) {
      awState := Mux(writeEnable, awWaitReady, awIdle)
      awValid := Mux(writeEnable, true.B, false.B)
    }
    is(awWaitReady) {
      awState := Mux(aIn.aw.fire, awDone, awWaitReady)
      awValid := Mux(aIn.aw.fire, false.B, true.B)
    }
    is(awDone) {
      awState := Mux(aIn.b.fire, awIdle, awDone)
      awState := awIdle
      awValid := false.B
    }
  }

  val awsize:    UInt = lsuCtrls.size
  val awsizeReg: UInt = RegInit(0.U(3.W))
  awsizeReg        := Mux(writeEnable, awsize, Mux(aIn.aw.fire, 0.U(3.W), awsizeReg))
  aIn.aw.bits.size := awsizeReg

  val wstrbReg: UInt = RegInit(0.U(parameter.lsuParams.maskLen.W))
  wstrbReg := Mux(writeEnable, wstrb, Mux(aIn.w.fire, 0.U(parameter.lsuParams.maskLen.W), wstrbReg))

  aIn.w.bits.data := 0.U
  aIn.w.bits.strb := 0.U
  switch(wState) {
    is(wIdle) {
      wState          := Mux(aIn.w.fire, wDone, wIdle)
      wValid          := Mux(writeEnable, true.B, Mux(aIn.w.fire, false.B, wValid))
      aIn.w.bits.data := Mux(aIn.w.fire, wdataReg, 0.U)
      aIn.w.bits.strb := wstrbReg
    }
    is(wDone) {
      wState          := Mux(aIn.b.fire, wIdle, wDone)
      wState          := wIdle
      wValid          := false.B
      aIn.w.bits.data := wdataReg
      aIn.w.bits.strb := wstrbReg
    }
  }
  aIn.w.bits.last := Mux(aIn.w.fire, true.B, false.B)
  aIn.w.valid     := wValid
  aIn.b.ready     := true.B

  val bOutValid: Bool =
    Mux(aIn.b.fire && aIn.b.bits.resp === 0.U && aIn.b.bits.id === parameter.lsuParams.axiID.U, true.B, false.B)
  val rFinished: Bool = (aIn.r.bits.resp === 0.U) && (aIn.r.bits.id === parameter.lsuParams.axiID.U) && aIn.r.fire
  out.valid := rFinished || bOutValid

  val readShiftSign: UInt = raddr(1, 0) & "b11".U(2.W)
  out.bits.dataOut := MuxLookup(readShiftSign, 0.U(parameter.xlen.W))(
    Seq(
      0.U -> memOut,
      1.U -> memOut(31, 8),
      2.U -> memOut(31, 16),
      3.U -> memOut(31, 24)
    )
  )
  dontTouch(readShiftSign)

  aIn.ar.bits.id    := parameter.lsuParams.axiID.U
  aIn.ar.bits.len   := 0.U
  aIn.ar.bits.burst := 0.U
  aIn.aw.bits.id    := parameter.lsuParams.axiID.U
  aIn.aw.bits.len   := 0.U
  aIn.aw.bits.burst := 0.U

  dontTouch(in)
  dontTouch(axiIn)
  dontTouch(out.valid)
  dontTouch(bReady)
  dontTouch(isstore)
  dontTouch(wrOutValid)
}
