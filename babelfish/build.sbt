name := "babelfish"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.typelevel"     %% "scodec-core"    % "1.3.1",
  "org.bouncycastle"  %  "bcprov-jdk15on" % "1.51",
  "org.bouncycastle"  %  "bcpkix-jdk15on" % "1.51",
  "org.bouncycastle"  %  "bcmail-jdk15on" % "1.51",
  "coop.plausible.nx" %% "no-exceptions"  % "1.0.1",
  "io.argonaut"       %% "argonaut"       % "6.1-M4",
  "org.specs2"        %% "specs2"         % "2.4.6" % "test"
)

resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"

addCompilerPlugin("coop.plausible.nx" %% "no-exceptions" % "1.0.1")
