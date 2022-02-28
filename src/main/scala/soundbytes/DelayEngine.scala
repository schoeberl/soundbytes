package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * The basic delay engine required for delay and feedback computation based on two parallel mixers.
 * May not be the most ideal setup but should be sufficient for now.
 */
class DelayEngine(dataWidth: Int = 16, mixWidth: Int = 6, feedbackWidth: Int = 6) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(Vec(2, SInt(dataWidth.W))))
    val out = new DecoupledIO(Vec(2, SInt(dataWidth.W)))
    val mix = Input(UInt(mixWidth.W))
    val feedback = Input(UInt(feedbackWidth.W))
  })
  
  val delayMix = new Mixer(dataWidth, mixWidth)
  val feedbackMix = new Mixer(dataWidth, feedbackWidth)
  
  delayMix.io.in.valid := io.in.valid
  feedbackMix.io.in.valid := io.in.valid
  io.in.ready := delayMix.io.in.ready & feedbackMix.io.in.valid
  
  io.out.valid := delayMix.io.out.valid & feedbackMix.io.out.valid
  delayMix.io.out.ready := io.out.ready 
  feedbackMix.io.out.ready := io.out.ready 
  
  // Input is always present in the output signal (delay < 1)
  delayMix.io.in.bits(0) := io.in.bits(0)
  delayMix.io.in.bits(1) := io.in.bits(1)
  io.out.bits(0) := delayMix.io.out.bits
  
  // Input is always present in the delay output signal (feedback < 1)
  feedbackMix.io.in.bits(0) := io.in.bits(0)
  feedbackMix.io.in.bits(1) := io.in.bits(1)
}
