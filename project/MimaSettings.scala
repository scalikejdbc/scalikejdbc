import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import com.typesafe.tools.mima.plugin.MimaKeys.{previousArtifacts, reportBinaryIssues, binaryIssueFilters}

/*
 * MiMa settings of ScalikeJDBC libs.
 *
 * Since 2.3.x series, we don't exclude bin incompatibility after 2.x.0 release.
 *
 * see also: https://github.com/scalikejdbc/scalikejdbc/blob/master/CONTRIBUTING.md
 */
object MimaSettings {

  // The `previousVersion` must be exactly the previous version (e.g. "2.3.1" for "2.3.2-SNAPSHOT").
  //
  // The following bad scenario is the reason we must obey the rule:
  //
  //  - your build is toward 2.3.2 release and the `previousVersion` is "2.3.0"
  //  - you've added new methods since 2.3.1
  //  - you're going to remove some of the methods in 2.3.2
  //  - MiMa cannot detect such bin-incompatibility
  //
  val previousVersion = "2.3.0"

  val mimaSettings = MimaPlugin.mimaDefaultSettings ++ Seq(
    previousArtifacts := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor <= 11 =>
          Set(organization.value % s"${name.value}_${scalaBinaryVersion.value}" % previousVersion)
        case _ =>
          Set.empty
      }
    },
    test in Test := {
      reportBinaryIssues.value
      (test in Test).value
    }
  )
}
