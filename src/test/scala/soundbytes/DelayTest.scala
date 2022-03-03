package soundbytes

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
//import chiseltest.experimental.TestOptionBuilder._
//import chiseltest.internal.WriteVcdAnnotation
import org.scalatest.FlatSpec
import soundbytes.Sounds._

class DelayTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Delay"

  it should "play" in {
    test(new Delay()).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      val samples = getFileSamples("sample.wav")
      val outSamples = new Array[Short](samples.length)
      val delSamples = Array.fill[Short]((1 << 12) * 8)(0)

      var finished = false
      
      // no timeout, as a bunch of 0 samples would lead to a timeout.
      dut.clock.setTimeout(0)
      dut.io.signalOut.ready.poke(true.B)
      dut.io.signalOut.ready.poke(true.B)

      val delayLengthSecond = 44100 / 8
      val delayLength100m = delayLengthSecond / 10
      val delayLenghts = Array(delayLength100m * 2, delayLength100m * 4, delayLength100m * 6, delayLength100m * 2)

      dut.io.mix.poke((1 << 5).asUInt)
      dut.io.feedback.poke((1 << 4).asUInt)
      dut.io.delayMaxLength.poke(((1 << 12) - 1).asUInt)
      
      // Write the samples
      val th = fork {
        dut.io.signalIn.valid.poke(true.B)
        for (s <- 0 until samples.length / 8) {
          var delayLenghtsIndex = s * delayLenghts.length / (samples.length / 8)
          dut.io.delayLength.poke(delayLenghts(delayLenghtsIndex).asUInt)
          for (i <- 0 until 8) {
            dut.io.signalIn.bits(i).poke(samples(s * 8 + i).asSInt)
          }
          dut.clock.step()
          while (!dut.io.signalIn.ready.peek.litToBoolean) {
            dut.clock.step()
          }
        }
        finished = true
      }

      // Write the delay
      val th2 = fork {
        dut.io.delayInRsp.valid.poke(true.B)
        while(!finished) {
          if (dut.io.delayInReq.valid.peek.litToBoolean) {
            for (i <- 0 until 8) {
              dut.io.delayInRsp.bits(i).poke(delSamples(dut.io.delayInReq.bits.peek.litValue.toInt * 8 + i).asSInt)
            }
          }
          dut.clock.step()
        }
      }
      
      // Read the delay
      val th3 = fork {
        dut.io.delayOut.ready.poke(true.B)
        while(!finished) {
          if (dut.io.delayOut.valid.peek.litToBoolean) {
            for (i <- 0 until 8) {
              delSamples(dut.io.delayOutPtr.peek.litValue.toInt * 8 + i) = dut.io.delayOut.bits(i).peek.litValue.toShort
            }
          }
          dut.clock.step()
        }
      }
      
      // Playing in real-time does not work, so record the result
      var idx = 0
      while (!finished) {
        if (dut.io.signalOut.valid.peek.litToBoolean) {
          for (i <- 0 until 8) {
            outSamples(idx) = dut.io.signalOut.bits(i).peek.litValue.toShort
            idx += 1
          }
        }
        dut.clock.step()
      }
      th.join()
      th2.join()
      th3.join()

      saveArray(outSamples, "sample_delay_out.wav")

    }
  }
}
