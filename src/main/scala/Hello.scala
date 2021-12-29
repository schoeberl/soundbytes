import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

object Hello extends App {
  println("Hello SoundBytes")

  val buf = new Array[Byte](2)
  val frequency = 44100
  val af = new AudioFormat(frequency.toFloat, 16, 1, true, false)
  val sdl = AudioSystem.getSourceDataLine(af)
  sdl.open()
  sdl.start()
  val durationMs = 2000
  val numberOfTimesFullSinFuncPerSec = 441 //number of times in 1sec sin function repeats
  val numberOfSamplesToRepresentFullSin = frequency.toFloat / numberOfTimesFullSinFuncPerSec
  val iterations = (durationMs * 44100.toFloat / 1000).toInt
  for (i <- 0 to iterations) {
    val angle = i / (numberOfSamplesToRepresentFullSin / 2.0) * Math.PI // /divide with 2 since sin goes 0PI to 2PI
    val a = (Math.sin(angle) * 32767).toShort //32767 - max value for sample to take (-32767 to 32767)
    buf(0) = (a & 0xFF).toByte //write 8bits ________WWWWWWWW out of 16
    buf(1) = (a >> 8).toByte //write 8bits WWWWWWWW________ out of 16

    sdl.write(buf, 0, 2)
  }
  sdl.drain()
}
