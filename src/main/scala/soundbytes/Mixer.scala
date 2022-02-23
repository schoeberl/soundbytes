package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * Mixer for two audio channels.
 * Currently, channel 0 is always present with at least 2^(-mixWidth) while channel 1 can be muted entirely.
 * Similarly, channel 0 can be present with up to 1 while channel 1 can be present with up to 2^mixWidth-1.
 */
class Mixer(dataWidth: Int = 16, mixWidth: Int = 6) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(Vec(2, SInt(dataWidth.W))))
    val out = new DecoupledIO(SInt(dataWidth.W))
    val mix = Input(UInt(mixWidth.W))
  })

  val maxMix = (1 << mixWidth) - 1
  
  // State Variables.
  val idle :: hasValue :: Nil = Enum(2)
  val regState = RegInit(idle)
  
  // Output.
  val regMul = RegInit(0.S(dataWidth.W + 1))
  io.out.bits := (io.in.bits(0) + regMul) >> 1
  
  // Logic.
  val signalDiff = io.in.bits(1) -& io.in.bits(0)
 
  // FSM.
  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
        regMul := (signalDiff * io.mix) >> mixWidth
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
