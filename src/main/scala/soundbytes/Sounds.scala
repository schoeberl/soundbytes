package soundbytes

import soundbytes.Sounds._

import javax.sound.sampled.{AudioFormat, AudioSystem}

object Sounds {

  val buf = new Array[Byte](2)
  val af = new AudioFormat(Constants.SampleFrequency.toFloat, 16, 1, true, false)
  val sdl = AudioSystem.getSourceDataLine(af)

  def getSamples = {
    val samples = new Array[Short](50000)

    val halfPeriod = Constants.SampleFrequency / 440 / 2
    var cnt = 0
    var high = true
    var attack = true

    var env = 0
    for (i <- 0 until  samples.length) {
      var s = if (high) 20000 else -20000
      if (cnt == halfPeriod) {
        cnt = 0
        high = !high
      } else {
        cnt += 1
      }
      s = s * env / 30000
      if (env > 30000) attack = false
      if (attack) {
        env += 10
      } else if (env >= 0) {
        env -= 1
      }
      samples(i) = s.toShort
    }
    samples
  }

  def startPlayer = {
    sdl.open()
    sdl.start()
  }

  def play(s: Short): Unit = {
    buf(0) = (s & 0xFF).toByte // low byte first
    buf(1) = (s >> 8).toByte

    sdl.write(buf, 0, 2)
  }
  def stopPlayer = {
    sdl.drain()
  }


}

object PlaySounds extends App {
  val samples = getSamples
  startPlayer

  for (s <- samples) {
    play(s)
  }
  stopPlayer
}
