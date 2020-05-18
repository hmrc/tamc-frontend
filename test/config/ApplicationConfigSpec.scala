package config

import test_utils.TAMCSetupSpec

class ApplicationConfigSpec extends TAMCSetupSpec {

  "check rates for earliest valid year" when {

    "return valid year" in {
      ApplicationConfig.TAMC_BEGINNING_YEAR should be(2016)
    }

  }

}
