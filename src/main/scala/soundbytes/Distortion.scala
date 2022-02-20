package soundbytes

import scala.math._
import chisel3._
import chisel3.util._

/**
 * Exponential distortion via lookup table.
 * Gain can be controlled via an input parameter.
 *
 * The gainWidth and lookupWidth should be smaller than the dataWidth.
 * The multipliers are of size (absDataWidth) * (gainWidth) and (fractBits) * (fractBits)
 * For gainWidth <= lookupBits a single multiplier reduces to (absDataWidth) * (absDataWidth).
 */
class Distortion(dataWidth: Int = 16, gainWidth: Int = 4, lookupBits: Int = 8, maxGain: Double = 100.0, singleMultiplier : Boolean = false) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(SInt(dataWidth.W)))
    val out = new DecoupledIO(SInt(dataWidth.W))
    val gain = Input(UInt(gainWidth.W))
  })

  // Number of bits required for absolute signal values without gain.
  val absDataWidth = dataWidth - 1
  val maxSignal = (1 << absDataWidth) - 1
  
  // Number of bits required for absolute signal values incl. gain.
  val gainDataWidth = absDataWidth + gainWidth
  val maxGainSignal = (1 << gainDataWidth) - 1

  // Number of bits required for interpolation.
  val fractBits = gainDataWidth - lookupBits
  
  // Steps and gain constant for the lookup table.
  val lookupSteps = 1 << fractBits
  val gainConst = maxGain / (1 << gainWidth).toDouble
  
  // Lookup table. We exclude the 0-index (will be muxed).
  val lookupValues = Range(lookupSteps - 1, maxGainSignal + 1, lookupSteps).map(i => maxSignal * (1.0 - scala.math.exp(-gainConst * i.toDouble / maxSignal.toDouble)))
  val lookupTable = VecInit(lookupValues.map(v => scala.math.round(v).asUInt(absDataWidth.W)))

  println(gainDataWidth)
  println(maxGainSignal)
  println(fractBits)
  println(lookupSteps)
  println(gainConst)
  println(lookupValues)
  println(lookupValues.length)
  
  // State Variables.
  val idle :: distort :: hasValue :: Nil = Enum(3)
  val regState = RegInit(idle)

  // Shared Multiplier (if needed).
  val sharedMulIn1 = WireDefault(0.U(max(absDataWidth, fractBits).W))
  val sharedMulIn2 = WireDefault(0.U(max(gainWidth, fractBits).W))
  val sharedMulOut = sharedMulIn1 * sharedMulIn2
  
  // Gain stage.
  val inVal = io.in.bits
  val inValAbs = inVal.abs.asUInt.min(maxSignal.U).tail(1)  
  val regInValSign = ShiftRegister(inVal(dataWidth - 1), 2) // delay by two stages

  val gainMul = if (singleMultiplier) {sharedMulOut} else { inValAbs * io.gain }
  val regInValGain = RegNext(gainMul)
  
  // Distortion stage.
  val lookupIndex = regInValGain >> fractBits
  val lookupFraction = regInValGain(fractBits - 1, 0)

  val lookupLow = WireDefault(0.U(absDataWidth.W))
  when(lookupIndex === 0.U) { // 0-index is excluded so we mux.
    lookupLow := 0.U
  }.otherwise {
    lookupLow := lookupTable(lookupIndex - 1.U)
  }
  val lookupHigh = lookupTable(lookupIndex)
  val lookupDiff = lookupHigh - lookupLow
  val regLookupLow = RegInit(0.U(absDataWidth.W))
  
  val interpMul = if (singleMultiplier) {sharedMulOut} else { lookupDiff * lookupFraction }  
  val regInterp = RegInit(0.U(absDataWidth.W))

  // Output Code.
  val regOut = RegInit(0.U(absDataWidth.W))
  when(regInValSign === true.B) {
    io.out.bits := -(regInterp +& regLookupLow).asSInt()
  } .otherwise {
    io.out.bits := (regInterp +& regLookupLow).asSInt()
  }

  // FSM.
  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
        if(singleMultiplier) {
          sharedMulIn1 := inValAbs
          sharedMulIn2 := io.gain
        }
        regState := distort
      }
    }
    is (distort) {
      if(singleMultiplier) {
        sharedMulIn1 := lookupDiff
        sharedMulIn2 := lookupFraction
      }
      
      regLookupLow := lookupLow
      regInterp := interpMul >> fractBits
      
      regState := hasValue
    }
    is (hasValue) {
      when(io.out.ready) {
        regState := idle
      }
    }
  }
}
