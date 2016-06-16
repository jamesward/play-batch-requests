name := "play-batch-requests"

scalaVersion := "2.11.8"

libraryDependencies += ws

lazy val root = (project in file(".")).enablePlugins(PlayScala)
