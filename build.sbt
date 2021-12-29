scalaVersion := "2.12.12"

scalacOptions := Seq("-Xsource:2.11", "-deprecation")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)


libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.3.1"
