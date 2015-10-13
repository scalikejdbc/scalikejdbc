import sbt._, Keys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import com.typesafe.tools.mima.plugin.MimaKeys.{previousArtifact, reportBinaryIssues, binaryIssueFilters}

object MimaSettings {

  val mimaSettings = Seq.empty
  // NOTICE: Since 2.3.x series, we don't exclude bin incompatibility after 2.x.0 release.
  /*
  val mimaSettings = MimaPlugin.mimaDefaultSettings ++ Seq(
    previousArtifact := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor <= 11 =>
          Some(organization.value % s"${name.value}_${scalaBinaryVersion.value}" % "2.2.0")
        case _ =>
          None
      }
    },
    test in Test := {
      reportBinaryIssues.value
      (test in Test).value
    }
  )
  */

}
