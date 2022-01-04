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
  val regTick = RegInit(false.B)
  regTick := false.B

  val multiply = WireDefault(0x0ffff.U(16.W))

  val inVal = io.in.bits
  io.out.bits := regData

  io.in.ready := regState === idle
  io.out.valid := regState === hasValue
  switch (regState) {
    is (idle) {
      when(io.in.valid) {
        regData := (inVal * multiply) >> 16
        regState := hasValue
        regTick := true.B
      }
    }
    is (hasValue) {
      when(io.out.ready) {
        regState := idle
      }
    }
  }

  // Compute the tremolo
  val regCnt = RegInit(0x8000.U(16.W))
  val regUp = RegInit(true.B)
  when (regTick) {
    when (regUp) {
      regCnt := regCnt + 8.U
      when (regCnt > 0xff00.U) {
        regUp := false.B
      }
    } .otherwise {
      regCnt := regCnt - 8.U
      when (regCnt < 0x8000.U) {
        regUp := true.B
      }
    }
  }
  multiply := regCnt

}
