package org.globalmoney.mandrill

import java.util.Date

import com.typesafe.config.ConfigRenderOptions
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.Application
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import Serializer._
class MandrillPlugin(app: Application) extends play.api.Plugin {

  private lazy val mock = app.configuration.getBoolean("mandrill.mock").getOrElse(false)

  private lazy val mandrillInstance: MandrillAPI = {
    val defaultMailRequestConf = app.configuration.getObject("mandrill.mail")
    val defaultMail = if (defaultMailRequestConf.isDefined) {
      Json.parse(defaultMailRequestConf.get.render(ConfigRenderOptions.concise())).validate[Email] match {
        case mail: JsSuccess[Email] =>
          mail.get
        case e: JsError =>
          throw new RuntimeException("Could not parse default mail request (mandrill.default.mail.json in application.conf). Error:" + e.errors.toString())
      }
    } else {
      throw new RuntimeException("Default mail request not found (mandrill.mail in application.conf)")
    }

    if (mock) {
      new MockMandrill(defaultMail)
    } else {
      val apiKeyStr = app.configuration.getString("mandrill.key").getOrElse(
          throw new RuntimeException("mandrill.key needs to be set in application.conf in order to use this plugin (or set mandrill.mock to true)")
      )

      val apiUrlStr = app.configuration.getString("mandrill.url").getOrElse("https://mandrillapp.com/api/1.0")
      val apiMailUrlStr = app.configuration.getString("mandrill.mail.url").getOrElse("/messages/send.json")

      new RestMandrill(apiUrlStr, apiMailUrlStr, apiKeyStr, defaultMail, app)
    }
  }

  override lazy val enabled = true

  override def onStart() {
    mandrillInstance
  }

  def instance = mandrillInstance
}


object MandrillPlugin {

  def send(email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None)(implicit app: play.api.Application) =
    app.plugin(classOf[MandrillPlugin]).get.instance.send(email, async, ipPool, sendAt)

  def sendTemplate(template: Template, email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None)(implicit app: play.api.Application) =
    app.plugin(classOf[MandrillPlugin]).get.instance.sendTemplate(template, email, async, ipPool, sendAt)
}


trait MandrillAPI {
  def send(email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None): Future[List[Result]]
  def sendTemplate(template: Template, email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None): Future[List[Result]]
}

class MockMandrill(defaultMail: Email) extends MandrillAPI {
  override def send(email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None): Future[List[Result]] = {
    sendRequest(None, email, async, ipPool, sendAt)
  }

  override def sendTemplate(template: Template, email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None): Future[List[Result]] = {
    sendRequest(Some(template), email, async, ipPool, sendAt)
  }

  def sendRequest(template: Option[Template], email: Email, async: Option[Boolean] = Some(false), ipPool: Option[String] = None, sendAt: Option[Date] = None): Future[List[Result]] = {
    Logger.debug(s"Mock mandrill email send: ${Serializer.printJson(Request("", email.withDefaults(defaultMail), template), pretty = true)}")
    Future(List[Result]())
  }
}

class RestMandrill(apiUrl: String, apiMailUrl: String, key: String, defaultMail: Email, app: Application) extends MandrillAPI {
  val mailUrl = apiUrl.concat(apiMailUrl)
  implicit val application = app

  override def send(email: Email,
                    async: Option[Boolean] = Some(false),
                    ipPool: Option[String] = None,
                    sendAt: Option[Date] = None): Future[List[Result]] = {
    sendRequest(None, email, async, ipPool, sendAt)
  }

  override def sendTemplate(template: Template, email: Email, async: Option[Boolean], ipPool: Option[String], sendAt: Option[Date]): Future[List[Result]] = {
    sendRequest(Some(template), email, async, ipPool, sendAt)
  }

  def sendRequest(template: Option[Template],
           email: Email,
           async: Option[Boolean] = Some(false),
           ipPool: Option[String] = None,
           sendAt: Option[Date] = None): Future[List[Result]] = {

    val request = Request(key, email.withDefaults(defaultMail), template, async, ipPool, sendAt)
    Logger.debug(s"Server request: ${Serializer.printJson(request, pretty = true)}")

    val responseFuture = WS.url(mailUrl).post(Serializer.printJson(request))

    responseFuture map { resp =>
      Logger.debug(s"Server response ${resp.status}, body: ${resp.body}")

      resp.status match {
        case 200 =>
          resp.json.as[List[Result]]
        case _ =>
          throw new MandrillSendingException(resp.json.as[MandrillError])
      }
    }
  }
}

