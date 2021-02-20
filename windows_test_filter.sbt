val excludeTestsIfWindows = Set(
  // stripMargin test
  "scalikejdbc.SQLInterpolationStringSuite",
  // stripMargin test
  "scalikejdbc.interpolation.SQLSyntaxSpec",
  // ???
  "scalikejdbc.jodatime.JodaTypeBinderSpec",
  "somewhere.DatabasePublisherTckTest",
)

ThisBuild / Test / testOptions ++= {
  if (scala.util.Properties.isWin) {
    Seq(Tests.Exclude(excludeTestsIfWindows))
  } else {
    Nil
  }
}
