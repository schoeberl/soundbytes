package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * Mixer for two audio channels.
 * 
 * Without fullControl set, channel 0 is always present with at least 2^(-mixWidth) while channel 1 can be muted entirely.
 * Similarly, channel 0 can be present with up to 1 while channel 1 can be present with up to 1-2^(-mixWidth).
 */
class Mixer(dataWidth: Int = 16, mixWidth: Int = 6, fullControl: Boolean = false) extends Module {
  val totalMixWidth = mixWidth + (if(fullControl) 1 else 0)
  
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(Vec(2, SInt(dataWidth.W))))
    val out = new DecoupledIO(SInt(dataWidth.W))
    val mix = Input(UInt(totalMixWidth.W))
  })

  val maxMix = 1 << mixWidth
  
  // State Variables.
  val idle :: hasValue :: Nil = Enum(2)
  val regState = RegInit(idle)

  // Input.
  val mix = if(fullControl) io.mix.min(maxMix.U) else io.mix
  
  // Output.
  val regMul = RegInit(0.S(dataWidth.W + 1))
  val regInVal = RegInit(0.S(dataWidth.W))
  io.out.bits := (regInVal + regMul).tail(1).asSInt()
  
  // Logic.
  val signalDiff = io.in.bits(1) -& io.in.bits(0)
 
  // FSM.
  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
        regMul := (signalDiff * mix) >> mixWidth
        regInVal := io.in.bits(0)         
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
