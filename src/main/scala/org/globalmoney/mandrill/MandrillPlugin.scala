package org.globalmoney.mandrill


/**
 * plugin interface
 */
trait MaindrillPlugin extends play.api.Plugin {
  def instance: MandrillAPI
}


class MandrillPlugin(app: play.api.Application) extends MaindrillPlugin {

  private lazy val mock = app.configuration.getBoolean("mandrill.mock").getOrElse(false)

  private lazy val mandrillInstance: MandrillAPI = {
    if (mock) {
      MockMandrill
    } else {
      val apiKey = app.configuration.getString("mandrill.key").getOrElse(
        throw new RuntimeException("mandrill.key needs to be set in application.conf in order to use this plugin (or set mandrill.mock to true)")
      )
      new RestMandrill(apiKey)
    }
  }

  override lazy val enabled = true

  override def onStart() {
    mandrillInstance
  }

  def instance = mandrillInstance
}


trait MandrillAPI {

}

object MockMandrill extends MandrillAPI {}

class RestMandrill(key: String) extends MandrillAPI {}