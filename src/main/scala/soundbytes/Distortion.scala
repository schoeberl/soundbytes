package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * Exponential distortion via lookup table.
 * Gain can be controlled via an input parameter.
 */
class Distortion extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(SInt(16.W)))
    val out = new DecoupledIO(SInt(16.W))
    //val gain = Input(UInt(8.W))
  })

  // number of integer bits on the gain control
  val gainBits = 3
  val maxGain = (1 << gainBits) - 1

  // number of bits required for absolute signal values without gain
  val sourceBits = 16 - 1
  val maxSignal = (1 << sourceBits) - 1
  
  // number of bits required for absolute signal values incl. gain
  val gainSourceBits = sourceBits + gainBits
  val maxGainSignal = (1 << gainSourceBits) - 1

  val lookupBits = 9
  val fractBits = gainSourceBits - lookupBits
  val lookupSteps = 1 << fractBits
  
  // TODO: we could exclude the 0-index
  val lookupValues = Range(/*lookupSteps*/ 0, maxGainSignal, lookupSteps).map(i => maxSignal * (1.0 - scala.math.exp(-i.toDouble / maxSignal.toDouble)))
  val lookupTable = VecInit(lookupValues.map(v => scala.math.round(v).asSInt(16.W)))

  val regData = RegInit(0.S(16.W))
  val idle :: hasValue :: Nil = Enum(2)
  val regState = RegInit(idle)
  
  val gain = Wire(UInt(8.W))
  gain := 32.U
  
  io.out.bits := regData

  val inVal = io.in.bits
  val inValAbs = inVal.abs.asUInt.min(((1 << 15) - 1).U).tail(1)
  val inValGain = inValAbs * gain
  val lookupIndex = inValGain >> (sourceBits + 8 - lookupBits)

  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
      	when(inVal(15)) {
          regData := -lookupTable(lookupIndex)
        }.otherwise {
          regData := lookupTable(lookupIndex)
	      }
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
