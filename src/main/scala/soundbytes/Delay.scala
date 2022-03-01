package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * Delay unit which is based on a read and write pointer for the dleay buffer.
 *
 * Future improvements:
 * Make it capable of adjusting the difference between the two pointers (delay duration) by means of linear interpolation.
 */
class Delay(dataWidth: Int = 16, ptrWidth: Int = 12, packetSize: Int = 8, mixWidth: Int = 6, feedbackWidth: Int = 6) extends Module {
  val io = IO(new Bundle {
    val signalIn = Flipped(new DecoupledIO(Vec(packetSize, SInt(dataWidth.W))))
    val signalOut = new DecoupledIO(Vec(packetSize, SInt(dataWidth.W)))
    
    val delayInReq = new ValidIO(UInt(ptrWidth.W))
    val delayInRsp = Flipped(new ValidIO(Vec(packetSize, SInt(dataWidth.W))))
    val delayOut = new DecoupledIO(Vec(packetSize, SInt(dataWidth.W)))
    val delayOutPtr = Output(UInt(ptrWidth.W))
    
    val delayLength = Input(UInt(ptrWidth.W))
    val delayMaxLength = Input(UInt(ptrWidth.W))
    val mix = Input(UInt(mixWidth.W))
    val feedback = Input(UInt(feedbackWidth.W))
  })
  
  // Actual Delay Engine.
  val engine = Module(new DelayEngine(dataWidth, mixWidth, feedbackWidth))
  engine.io.in.valid := false.B
  engine.io.out.ready := false.B
  engine.io.mix := io.mix
  engine.io.feedback := io.feedback
  engine.io.in.bits(0) := 0.S
  engine.io.in.bits(1) := 0.S
  
  // Delay Sample Pointers.
  val rdPtr = RegInit(0.U(ptrWidth.W))
  val wrPtr = WireDefault(0.U(ptrWidth.W))
  when (rdPtr + io.delayLength > io.delayMaxLength) {
    wrPtr := rdPtr + io.delayLength - io.delayMaxLength
  }.otherwise {
    wrPtr := rdPtr + io.delayLength
  }
  
  // State Variables.
  val idle :: readDelay :: compute :: writeDelay :: hasValue :: Nil = Enum(5)
  val regState = RegInit(idle)
  val computeCounter = Counter(packetSize)
  
  val regSigInVals = RegInit(VecInit(Seq.fill(packetSize)(0.S(dataWidth.W))))
  val regDelInVals = RegInit(VecInit(Seq.fill(packetSize)(0.S(dataWidth.W))))
  val regSigOutVals = RegInit(VecInit(Seq.fill(packetSize)(0.S(dataWidth.W))))
  val regDelOutVals = RegInit(VecInit(Seq.fill(packetSize)(0.S(dataWidth.W))))
  
  // FSM.
  io.signalOut.bits := regSigOutVals
  io.delayInReq.valid := false.B
  io.delayInReq.bits := rdPtr
  io.delayOut.valid := false.B
  io.delayOutPtr := wrPtr
  io.delayOut.bits := regDelOutVals

  io.signalIn.ready := regState === idle
  io.signalOut.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when (io.signalIn.valid) {
        regSigInVals := io.signalIn.bits
        regState := readDelay
      }
    }
    is (readDelay) {
      io.delayInReq.valid := true.B

      when (io.delayInRsp.valid) {
        regDelInVals := io.delayInRsp.bits
        regState := compute
        
        computeCounter.reset()
      }
    }
    is (compute) {
      when (engine.io.in.ready) {
        engine.io.in.valid := true.B
        engine.io.in.bits(0) := regSigInVals(computeCounter.value)
        engine.io.in.bits(1) := regDelInVals(computeCounter.value)
      }

      when (engine.io.out.valid) {
        engine.io.out.ready := true.B
        regSigOutVals(computeCounter.value) := engine.io.out.bits(0)
        regDelOutVals(computeCounter.value) := engine.io.out.bits(1)
        
        when (computeCounter.inc()) {
          regState := writeDelay
        }
      }    
    }
    is (writeDelay) {
      io.delayOut.valid := true.B

      when (io.delayOut.ready) {       
        when (rdPtr >= io.delayMaxLength) {
          rdPtr := 0.U
        }.otherwise {
          rdPtr := rdPtr + 1.U
        }
        
        regState := hasValue
      }
    }
    is (hasValue) {
      when (io.signalOut.ready) {
        regState := idle
      }
    }
  }
  
}
