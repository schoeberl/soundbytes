package soundbytes

import javax.sound.sampled.{AudioFormat, AudioSystem}

object Hello extends App {
  println("Hello SoundBytes")

  val buf = new Array[Byte](2)
  val sampleFrequency = Constants.SampleFrequency
  val af = new AudioFormat(sampleFrequency.toFloat, 16, 1, true, false)
  val sdl = AudioSystem.getSourceDataLine(af)
  sdl.open()
  sdl.start()
  val durationMs = 2000
  val frequency = 440
  val nrSamples = sampleFrequency.toFloat / frequency
  val iterations = (durationMs * 44100.toFloat / 1000).toInt
  for (i <- 0 to iterations) {
    val angle = i / (nrSamples / 2.0) * Math.PI // divide with 2 since sin goes 0PI to 2PI
    val a = (Math.sin(angle) * 32767).toShort
    buf(0) = (a & 0xFF).toByte // low byte first
    buf(1) = (a >> 8).toByte

    sdl.write(buf, 0, 2)
  }
  sdl.drain()
}
