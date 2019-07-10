val excludeTestsIfWindows = Set(
  // stripMargin test
  "scalikejdbc.SQLInterpolationStringSuite",
  // stripMargin test
  "scalikejdbc.interpolation.SQLSyntaxSpec",
  // ???
  "scalikejdbc.jodatime.JodaTypeBinderSpec",
)

testOptions in Test in ThisBuild ++= {
  if (scala.util.Properties.isWin) {
    Seq(Tests.Exclude(excludeTestsIfWindows))
  } else {
    Nil
  }
}
