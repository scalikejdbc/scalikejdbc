import testgenerator.SbtKeys._

seq(scalariformSettings: _*)

seq(testgeneratorSettings: _*)

testgeneratorEncoding in Compile := "UTF-8"

testgeneratorTestTemplate in Compile := "scalatest.FlatSpec"

testgeneratorScalaTestMatchers in Compile := "ShouldMatchers"

testgeneratorLineBreak in Compile := "LF"

