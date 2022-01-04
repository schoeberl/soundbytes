package soundbytes

import chisel3._
import chisel3.util._

class Tremolo extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new DecoupledIO(SInt(16.W)))
    val out = new DecoupledIO(SInt(16.W))
  })

  val regData = RegInit(0.S(16.W))
  val idle :: hasValue :: Nil = Enum(2)
  val regState = RegInit(idle)

  val inVal = io.in.bits
  io.out.bits := regData

  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
        regData := inVal
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
