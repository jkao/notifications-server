name := "notifications-server"

version := "1.0"

scalaVersion := "2.12.0"

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"
testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")
