import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  def apply(): Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageMinimumBranchTotal := 85,
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    ScoverageKeys.coverageExcludedPackages:= ".*Reverse.*;.*Routes.*;view.*",
  )
}
