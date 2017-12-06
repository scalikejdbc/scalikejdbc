testNGSettings
testNGVersion := "6.10"
testNGSuites := Seq(((resourceDirectory in Test).value / "testng.xml").absolutePath)

lazy val testngSources: Array[Byte] = {
  val src = url("https://dl.bintray.com/jmhofer/sbt-plugins/de.johoop/sbt-testng-interface_2.12/3.0.3/srcs/sbt-testng-interface_2.12-sources.jar")
  IO.withTemporaryDirectory { dir =>
    val f = dir / "temp.jar"
    IO.download(src, f)
    IO.readBytes(f)
  }
}

// https://github.com/sbt/sbt-testng/issues/15
libraryDependencies += "org.scala-sbt" % "test-interface" % "1.0" % "test"
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v >= 13 =>
      libraryDependencies.value.filterNot{ x =>
        x.organization == "de.johoop" && x.name == "sbt-testng-interface"
      }
    case _ =>
      libraryDependencies.value
  }
}

sourceGenerators in Test += {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v >= 13 =>
      task{
        val dir = (sourceManaged in Test).value
        IO.unzipStream(new java.io.ByteArrayInputStream(testngSources), dir).toSeq.filter(_.getName endsWith "scala")
      }
    case _ =>
      task(Nil)
  }
}
