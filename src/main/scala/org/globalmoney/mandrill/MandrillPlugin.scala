package org.globalmoney.mandrill

import java.util.Date

import com.typesafe.config.ConfigRenderOptions
import play.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.{Application}
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

class MandrillPlugin(app: Application) extends play.api.Plugin {

  private lazy val mock = app.configuration.getBoolean("mandrill.mock").getOrElse(false)

  private lazy val mandrillInstance: MandrillAPI = {
    val defaultMailRequestConf = app.configuration.getObject("mandrill.default.mail.json")
    val defaultMail = if (defaultMailRequestConf.isDefined) {
              Serializer.deserializeEmail(Json.parse(defaultMailRequestConf.get.render(ConfigRenderOptions.concise()))) match {
                case mail: JsSuccess[Email] =>
                  mail.get
                case e: JsError =>
                  Logger.info("Could not parse default mail request (mandrill.default.mail.json in application.conf). Will use default. Error:" + e.errors.toString())
                  new Email()
              }
            } else {
              Logger.info("Default mail request not found (mandrill.default.mail.json in application.conf). Will use default")
              new Email()
            }

    if (mock) {
      new MockMandrill(defaultMail)
    } else {
      val apiKeyStr = app.configuration.getString("mandrill.key").getOrElse(
          throw new RuntimeException("mandrill.key needs to be set in application.conf in order to use this plugin (or set mandrill.mock to true)")
      )
      val apiUrlStr = app.configuration.getString("mandrill.url").getOrElse(
        throw new RuntimeException("mandrill.url needs to be set in application.conf in order to use this plugin (or set mandrill.mock to true)")
      )
      val apiMailUrlStr = app.configuration.getString("mandrill.mail.url").getOrElse("/messages/send.json")


      new RestMandrill(new ApiUrls(apiUrlStr, apiMailUrlStr), apiKeyStr, defaultMail)
    }
  }

  override lazy val enabled = true

  override def onStart() {
    mandrillInstance
  }

  def instance = mandrillInstance
}

private case class ApiUrls(apiUrl: String, apiMailUrl: String) {
  def getMailUrl: String = {apiUrl.concat(apiMailUrl)}
}

object MandrillPlugin {
  def send(email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None)(implicit app: play.api.Application) = app.plugin(classOf[MandrillPlugin]).get.instance.send(email, async, ipPool, sendAt)
}


trait MandrillAPI {
    def send(email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None): Future[Response]
}

class MockMandrill(defaultMail: Email) extends MandrillAPI {
  override def send(email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None): Future[Response] = {
    Logger.debug("mock implementation, send email")
    Logger.debug(Serializer.printJson(new Request("", email.withDefaults(defaultMail)), pretty = true))
    Future(new Response())
  }
}

class RestMandrill(apiUrls: ApiUrls, key: String, defaultMail: Email) extends MandrillAPI {
  override def send(email: Email,
                    async: Option[Boolean] = Some(false),
                    ipPool: Option[String] = None,
                    sendAt: Option[Date] = None): Future[Response] = {

    if (Logger.isDebugEnabled) {
      Logger.debug("Request:")
      Logger.debug(Serializer.printJson(new Request(key, email.withDefaults(defaultMail), async, ipPool, sendAt), pretty = true))
    }

    val futureResponse: Future[WSResponse] = WS.url(apiUrls.getMailUrl).post(
                          Serializer.printJson(new Request(key, email.withDefaults(defaultMail), async, ipPool, sendAt)))

    futureResponse.map {
      resp => {
        if (Logger.isDebugEnabled) {
          Logger.debug("Response:")
          Logger.debug(resp.body)
        }

        val respContent = if (resp.status == 200)
                            Serializer.deserializeResult(resp.json)
                          else
                            Serializer.deserializeError(resp.json)
        respContent match {
          case r: JsSuccess[Response] => { r.get }
          case e: JsError => {
            throw new RuntimeException("Response format error: " + e.toString + ". Response: " + resp.body)
          }
        }
      }
    }
  }
}

