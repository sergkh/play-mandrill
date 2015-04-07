package org.globalmoney.mandrill

import java.io.{File, FilterOutputStream, PrintStream}
import javax.mail.internet.InternetAddress

import org.apache.commons.mail._
import play.api._
import scala.collection.JavaConverters._

class MailerPlugin(app: play.api.Application) extends play.api.Plugin {

  private lazy val mock = app.configuration.getBoolean("smtp.mock").getOrElse(false)

  private lazy val mailerInstance: MailerAPI = {
    if (mock) {
      MockMailer
    } else {
      val smtpHost = app.configuration.getString("smtp.host").getOrElse(throw new RuntimeException("smtp.host needs to be set in application.conf in order to use this plugin (or set smtp.mock to true)"))
      val smtpPort = app.configuration.getInt("smtp.port").getOrElse(25)
      val smtpSsl = app.configuration.getBoolean("smtp.ssl").getOrElse(false)
      val smtpTls = app.configuration.getBoolean("smtp.tls").getOrElse(false)
      val smtpUser = app.configuration.getString("smtp.user")
      val smtpPassword = app.configuration.getString("smtp.password")
      val debugMode = app.configuration.getBoolean("smtp.debug").getOrElse(false)
      val smtpTimeout = app.configuration.getInt("smtp.timeout")
      val smtpConnectionTimeout = app.configuration.getInt("smtp.connectiontimeout")
      new CommonsMailer(smtpHost, smtpPort, smtpSsl, smtpTls, smtpUser, smtpPassword, debugMode, smtpTimeout, smtpConnectionTimeout) {
        override def send(email: MultiPartEmail): String = email.send()
        override def createMultiPartEmail(): MultiPartEmail = new MultiPartEmail()
        override def createHtmlEmail(): HtmlEmail = new HtmlEmail()
      }
    }
  }

  override lazy val enabled = !app.configuration.getString("apachecommonsmailerplugin").filter(_ == "disabled").isDefined

  override def onStart() {
    mailerInstance
  }

  def instance = mailerInstance
}
