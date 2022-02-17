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
  val lookupSteps = 1 << (gainSourceBits - lookupBits)
  val fractBits = sourceBits + 8 - lookupBits
  
  // we exclude the 0-index
  val lookupValues = Range(lookupSteps, maxGainSignal, lookupSteps).map(i => maxSignal * (1.0 - scala.math.exp(-4.0 * i.toDouble / maxSignal.toDouble)))
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
  val lookupIndex = inValGain.head(lookupBits)
  val lookupFraction = inValGain.tail(lookupBits)

  val lookupLow = WireDefault(0.S(16.W))
  val lookupHigh = WireDefault(0.S(16.W))

  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
        
        // 0-index is excluded --> mux
        when(lookupIndex === 0.U) {
          lookupLow := 0.S
        }.otherwise{
          lookupLow := lookupTable(lookupIndex - 1.U)
        }

        lookupHigh := lookupTable(lookupIndex)
        
      	when(inVal(15)) {
          regData := -(((lookupHigh - lookupLow) * lookupFraction).head(16).asSInt + lookupLow)
        }.otherwise {
          regData := ((lookupHigh - lookupLow) * lookupFraction).head(16).asSInt + lookupLow
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
