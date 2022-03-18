package soundbytes

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
//import chiseltest.experimental.TestOptionBuilder._
//import chiseltest.internal.WriteVcdAnnotation
import org.scalatest.FlatSpec
import soundbytes.Sounds._

class DecoupledTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "DecoupledMux"

  it should "mux" in {
    test(new DecoupledMux(UInt(8.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.sel.poke(true.B)

      dut.io.in1.valid.poke(false.B)
      dut.io.in1.bits.poke(1.asUInt)
      
      dut.io.in2.valid.poke(false.B)
      dut.io.in2.bits.poke(2.asUInt)

      dut.io.out.ready.poke(false.B)

      dut.clock.step()
      dut.clock.step()
      
      dut.io.in1.valid.poke(true.B)      
      dut.clock.step()

      dut.io.out.ready.poke(true.B)
      dut.clock.step()

      dut.io.sel.poke(false.B)
      dut.clock.step()

      dut.io.in2.valid.poke(true.B)      
      dut.clock.step()
      dut.clock.step()
    }   
  }

  behavior of "DecoupledDemux"

  it should "demux" in {
    test(new DecoupledDemux(UInt(8.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(0)

      dut.io.sel.poke(true.B)

      dut.io.in.valid.poke(false.B)
      dut.io.in.bits.poke(1.asUInt)
      
      dut.io.out1.ready.poke(false.B)
      dut.io.out2.ready.poke(false.B)

      dut.clock.step()
      dut.clock.step()
      
      dut.io.in.valid.poke(true.B)      
      dut.clock.step()

      dut.io.out2.ready.poke(true.B)
      dut.clock.step()

      dut.io.out1.ready.poke(true.B)
      dut.clock.step()

      dut.io.sel.poke(false.B)      
      dut.clock.step()
      dut.clock.step()
    }
  }
}
