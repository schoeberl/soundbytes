package soundbytes

import javax.sound.sampled.{AudioFormat, AudioSystem}

object Sounds extends App {

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
      s = s * env / 15000
      if (env > 15000) attack = false
      if (attack) {
        env += 10
      } else if (env >= 0) {
        env -= 1
      }
      samples(i) = s.toShort
    }
    samples
  }

  val buf = new Array[Byte](2)
  val af = new AudioFormat(Constants.SampleFrequency.toFloat, 16, 1, true, false)
  val sdl = AudioSystem.getSourceDataLine(af)
  sdl.open()
  sdl.start()

  val samples = getSamples

  for (s <- samples) {
    buf(0) = (s & 0xFF).toByte // low byte first
    buf(1) = (s >> 8).toByte

    sdl.write(buf, 0, 2)
  }
  sdl.drain()
}
