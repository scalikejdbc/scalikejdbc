val excludeTests = Set(
  "scalikejdbc.specs2.mutable.AutoRollbackSpec",
  "scalikejdbc.QueryInterfaceSpec",
  "scalikejdbc.TxBoundaryMissingImplicitsSpec",
  "scalikejdbc.LoanPatternSpec",
  "scalikejdbc.SubQuerySpec",
  "scalikejdbc.SQL_AttributesSpec",
  "scalikejdbc.SQLInterpolationSpec",
  "scalikejdbc.InformationSchemaSpec",
  "scalikejdbc.ColumnSQLSyntaxProviderSpec",
  "scalikejdbc.PartialResultSQLSyntaxProviderSpec",
)

ThisBuild / Test / testOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) =>
      Seq(Tests.Exclude(excludeTests))
    case _ =>
      Nil
  }
}
