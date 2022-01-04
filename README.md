# soundbytes
Sound effects and music related hardware (in Chisel)

# Next Steps

* Create a Hello World example
  * Create a simple sound sample (a kind of pop)
  * Apply a simple effect e.g., delay
  * Write a .wav file
  * All in Chisel simulation
* Look into related work, what is available in open source
* Add recording and playback on a PC for the simulation
* Get the Hello Wrld running in an FPGA
* Get audio IO running in an FPGA
  * AC97 AD/DA converter
  * Simple sigma-delta IO
* Make it a project for students at DTU

# Notes

* A full ready/valid handshake is a bit of an overkill
  * The source may just use a periodic single cycle valid

# Resources

* wavefile tools, but Scala 2.11 https://github.com/mziccard/scala-audio-file/blob/master/src/main/scala/me/mziccard/audio/WavFile.scala
* Java libraries https://docs.oracle.com/javase/tutorial/sound/sampled-overview.html
