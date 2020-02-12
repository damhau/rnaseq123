logLevel := Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.5.2")

libraryDependencies += "com.spotify" % "docker-client" % "8.7.3"
