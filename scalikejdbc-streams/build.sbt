enablePlugins(TestNGPlugin)
testNGVersion := "6.10"
testNGSuites := Seq(((resourceDirectory in Test).value / "testng.xml").absolutePath)
