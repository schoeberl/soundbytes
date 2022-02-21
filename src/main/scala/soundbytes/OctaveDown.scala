package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * Experimental octave down effect based on an indirect divider circuit.
 * Does not work very well at the moment.
 *
 * Potential improvement:
 * 1) Lowpass filtering (i.e. counter) on the regControl flipping condition to track the peaks more accurately.
 *    This would require having multiple samples on the fly at the same time until a decision for regControl can be made.
 * 2) Filtering of the output signal should help to eliminate artifacts.
 */
class OctaveDown(dataWidth: Int = 16) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(SInt(dataWidth.W)))
    val out = new DecoupledIO(SInt(dataWidth.W))
  })

  // State Variables.
  val idle :: hasValue :: Nil = Enum(2)
  val regState = RegInit(idle)

  val regInVal = RegInit(0.S(dataWidth.W))
  val regLowPeak = RegInit(0.S(dataWidth.W))

  val regRising = RegInit(false.B)
  val regControl = RegInit(false.B)

  // Input.
  val inVal = io.in.bits
  
  // Output.
  val valOffset = regInVal -& regLowPeak
  val valScaled = valOffset >> 1
  
  when (regControl) {
    io.out.bits := -valScaled
  }.otherwise {
    io.out.bits := valScaled
  }
  
  // FSM.
  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
        when(!regRising & inVal(dataWidth - 1) & inVal > regInVal) {
          regRising := true.B
          regLowPeak := regInVal
          regControl := !regControl
        }
        
        when(regRising & !inVal(dataWidth - 1) & inVal < regInVal) {
          regRising := false.B
        }
        
        regInVal := io.in.bits
        
        regState := hasValue
      }
    }
    is (hasValue) {
      when(io.out.ready) {
        regState := idle
      }
    }
  }
}
