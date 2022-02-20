# soundbytes

Sound effects and music related hardware (in Chisel)

# Status

 * Hello world done:
   * With generated tone
   * Tremolo and Distortion effects in Chisel
   * Playback in simulation on the PC
   * Reading and writing of .wav files

# Next Steps

 * More effects
   * Delay
   * Overdrive
   * Octaver
   * Highpass/Lowpass/Bandpass
   * Mixer
 * Maybe refactor Distortion -> LookupTableBase which can be used for distortion, overdrive etc.
 * Look into related work, what is available in open source
 * Add recording a PC for the simulation
 * Get audio IO running in an FPGA
   * AC97 AD/DA converter
   * Simple sigma-delta IO
 * Make it a project for students at DTU (Fagprojekt)
   * sigma-delta simple IO
   * Connect I2S stuff (ADDAC for Basys 3)
   * Ac97
   * Effects
   * Synthesizer

# Notes

* A full ready/valid handshake is a bit of an overkill
  * The source may just use a periodic single cycle valid

# Resources

* wavefile tools, but Scala 2.11 https://github.com/mziccard/scala-audio-file/blob/master/src/main/scala/me/mziccard/audio/WavFile.scala
* Java libraries https://docs.oracle.com/javase/tutorial/sound/sampled-overview.html
