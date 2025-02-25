import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  def apply(): Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageMinimumStmtTotal := 91,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:= ".*Reverse.*;.*Routes.*;view.*",
  )
}
