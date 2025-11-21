package fangshan.rtl.axi

import chisel3._
import chisel3.experimental.dataview.DataView
import chisel3.util._

class AXIBundleAR(addrWidth: Int) extends Bundle {
  val addr:  UInt = UInt(addrWidth.W) // Read request address
  val id:    UInt = UInt(4.W)         // Read request ID
  val len:   UInt = UInt(8.W)         // Read request length
  val size:  UInt = UInt(3.W)         // Read request size
  val burst: UInt = UInt(2.W)         // Read request burst type
}

class AXIBundleR(dataWidth: Int) extends Bundle {
  val resp: UInt = UInt(2.W)         // Indicates whether the read operation was successful
  val data: UInt = UInt(dataWidth.W) // Read request data
  val last: Bool = Bool()            // Indicates whether this is the last data
  val id:   UInt = UInt(4.W)         // Read request ID
}

class AXIBundleAW(addrWidth: Int) extends Bundle {
  val addr:  UInt = UInt(addrWidth.W) // Write request address
  val id:    UInt = UInt(4.W)         // Write request ID
  val len:   UInt = UInt(8.W)         // Write request length
  val size:  UInt = UInt(3.W)         // Write request size
  val burst: UInt = UInt(2.W)         // Write request burst type
}

class AXIBundleW(dataWidth: Int) extends Bundle {
  val data: UInt = UInt(dataWidth.W)       // Data to be written
  val strb: UInt = UInt((dataWidth / 8).W) // Write data mask
  val last: Bool = Bool()                  // Indicates whether this is the last data
}

class AXIBundleB extends Bundle {
  val resp: UInt = UInt(2.W) // Indicates whether the write operation was successful
  val id:   UInt = UInt(4.W) // Write request ID
}

class AXIBundle(addrWidth: Int, dataWidth: Int) extends Bundle {
  val ar: IrrevocableIO[AXIBundleAR] = Irrevocable(new AXIBundleAR(addrWidth))
  val r:  IrrevocableIO[AXIBundleR]  = Flipped(Irrevocable(new AXIBundleR(dataWidth)))
  val aw: IrrevocableIO[AXIBundleAW] = Irrevocable(new AXIBundleAW(addrWidth))
  val w:  IrrevocableIO[AXIBundleW]  = Irrevocable(new AXIBundleW(dataWidth))
  val b:  IrrevocableIO[AXIBundleB]  = Flipped(Irrevocable(new AXIBundleB))
}

class VerilogAXIBundle(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val awready: Bool = Input(Bool())
  val awvalid: Bool = Output(Bool())
  val awaddr:  UInt = Output(UInt(addrWidth.W))
  val awid:    UInt = Output(UInt(4.W))
  val awlen:   UInt = Output(UInt(8.W))
  val awsize:  UInt = Output(UInt(3.W))
  val awburst: UInt = Output(UInt(2.W))
  val wready:  Bool = Input(Bool())
  val wvalid:  Bool = Output(Bool())
  val wdata:   UInt = Output(UInt(dataWidth.W))
  val wstrb:   UInt = Output(UInt((dataWidth / 8).W))
  val wlast:   Bool = Output(Bool())
  val bready:  Bool = Output(Bool())
  val bvalid:  Bool = Input(Bool())
  val bresp:   UInt = Input(UInt(2.W))
  val bid:     UInt = Input(UInt(4.W))
  val arready: Bool = Input(Bool())
  val arvalid: Bool = Output(Bool())
  val araddr:  UInt = Output(UInt(addrWidth.W))
  val arid:    UInt = Output(UInt(4.W))
  val arlen:   UInt = Output(UInt(8.W))
  val arsize:  UInt = Output(UInt(3.W))
  val arburst: UInt = Output(UInt(2.W))
  val rready:  Bool = Output(Bool())
  val rvalid:  Bool = Input(Bool())
  val rresp:   UInt = Input(UInt(2.W))
  val rdata:   UInt = Input(UInt(dataWidth.W))
  val rlast:   Bool = Input(Bool())
  val rid:     UInt = Input(UInt(4.W))
}

object AXIBundle {
  implicit val master: DataView[VerilogAXIBundle, AXIBundle] = DataView[VerilogAXIBundle, AXIBundle](
    vab => new AXIBundle(vab.addrWidth, vab.dataWidth),
    _.awvalid -> _.aw.valid,
    _.awready -> _.aw.ready,
    _.awid    -> _.aw.bits.id,
    _.awaddr  -> _.aw.bits.addr,
    _.awlen   -> _.aw.bits.len,
    _.awsize  -> _.aw.bits.size,
    _.awburst -> _.aw.bits.burst,
    _.wready  -> _.w.ready,
    _.wvalid  -> _.w.valid,
    _.wdata   -> _.w.bits.data,
    _.wstrb   -> _.w.bits.strb,
    _.wlast   -> _.w.bits.last,
    _.bready  -> _.b.ready,
    _.bvalid  -> _.b.valid,
    _.bresp   -> _.b.bits.resp,
    _.bid     -> _.b.bits.id,
    _.arready -> _.ar.ready,
    _.arvalid -> _.ar.valid,
    _.araddr  -> _.ar.bits.addr,
    _.arid    -> _.ar.bits.id,
    _.arlen   -> _.ar.bits.len,
    _.arsize  -> _.ar.bits.size,
    _.arburst -> _.ar.bits.burst,
    _.rready  -> _.r.ready,
    _.rvalid  -> _.r.valid,
    _.rresp   -> _.r.bits.resp,
    _.rdata   -> _.r.bits.data,
    _.rlast   -> _.r.bits.last,
    _.rid     -> _.r.bits.id
  )
}
