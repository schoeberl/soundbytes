package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * Experimental octave down effect based on an indirect divider circuit.
 * Somewhat working version.
 *
 * Potential improvement:
 * 1) Tweaking of regControl flipping logic and good parameter set (based on real-world samples) is necessary
 * 2) Filtering of the output signal should help to eliminate artifacts.
 */
class OctaveDown(dataWidth: Int = 16, bufferLength: Int = 16) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(SInt(dataWidth.W)))
    val out = new DecoupledIO(SInt(dataWidth.W))
  })

  val maxSignal = (1 << (dataWidth - 1)) - 1
  val maxIndex = bufferLength - 1
  val indexBits = log2Up(bufferLength)
  
  val margin = 20

  // State Variables.
  val sampleCount = RegInit(0.U(indexBits.W))

  val regLowPeak = RegInit(0.S(dataWidth.W))
  val regLowPeakIndex = RegInit(0.U(indexBits.W))

  val regOffset = RegInit(0.S(dataWidth.W))

  val regRising = RegInit(0.U(indexBits.W))
  val regControl = RegInit(false.B)
  val regToggled = RegInit(false.B)

  // Input.
  val inVal = io.in.bits
  
  // Buffer.
  val counter = Counter(bufferLength)
  val buffer = SyncReadMem(bufferLength, SInt(dataWidth.W), SyncReadMem.ReadFirst)
  val bufferSample = buffer(counter.value)
  
  // Output.
  val valOffset = bufferSample -& regOffset
  val valScaled = valOffset >> 1  

  when (regControl) {
    io.out.bits := -valScaled
  }.otherwise {
    io.out.bits := valScaled
  }
  
  // FSM.
  io.in.ready := false.B
  io.out.valid := false.B
  
  when(io.in.valid & sampleCount < maxIndex.U) {    // write to module when not full
    when(inVal > margin.S & regToggled) {                // clearing for next peak detection
      when(regRising < maxIndex.U) {
        regRising := regRising + 1.U
      }.otherwise {
        regToggled := false.B
        regRising := 0.U
        regLowPeak := inVal
      }
    }
    
    when(regRising < maxIndex.U & !regToggled) {    // peak detection
      when(inVal < margin.S & inVal >= regLowPeak) {     // advance peak detection
        regRising := regRising + 1.U
      }.otherwise {                                 // reset peak detection
        regRising := 0.U
        regLowPeak := inVal
        regLowPeakIndex := counter.value
      }
    }
    
    bufferSample := inVal
    sampleCount := sampleCount + 1.U
    counter.inc()
    io.in.ready := true.B
  }

  when(io.out.ready & sampleCount === maxIndex.U) {
    when(counter.value === regLowPeakIndex & regRising === maxIndex.U & !regToggled) {
      regOffset := regLowPeak
      regControl := !regControl
      regRising := 0.U
      regToggled := true.B
    }
    
    sampleCount := sampleCount - 1.U
    io.out.valid := true.B
  }

}
