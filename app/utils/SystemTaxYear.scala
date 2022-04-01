package utils

import com.google.inject.Inject
import config.ApplicationConfig


class SystemTaxYear @Inject()(applicationConfig: ApplicationConfig) {
  def current() = applicationConfig.currentTaxYear()
}
