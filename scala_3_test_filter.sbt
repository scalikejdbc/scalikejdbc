val excludeTests = Set(
  "scalikejdbc.specs2.mutable.AutoRollbackSpec",
)

ThisBuild / Test / testOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) =>
      Seq(Tests.Exclude(excludeTests))
    case _ =>
      Nil
  }
}
