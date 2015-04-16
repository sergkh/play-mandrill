package org.globalmoney.mandrill

import java.util.Date

import com.typesafe.config.{ConfigRenderOptions, ConfigFactory}
import org.globalmoney.mandrill.MergeLanguageType.MergeLanguageType
import org.globalmoney.mandrill.RecipientType.RecipientType
import org.globalmoney.mandrill.RejectReasonType.RejectReasonType
import org.globalmoney.mandrill.SendStatusType.SendStatusType
import play.api.libs.json._
import play.api.libs.functional.syntax._

import play.api.libs.json.Writes.dateWrites
import play.api.libs.json.Reads.dateReads

/**
 * Created by Viktor Zakalyuzhnyy on 07.04.15.
 */
case class Request(key: String, message: Email, async: Option[Boolean] = Some(false),
                    ipPool: Option[String] = None, sendAt: Option[Date] = None) {

  private[mandrill] def withDefaults(default: Request): Request = {
    new Request(key,
                message.withDefaults(default.message),
                async.fold(default.async){Some(_)},
                ipPool.fold(default.ipPool){Some(_)},
                sendAt.fold(default.sendAt){Some(_)})
  }
}

case class AdditionalSettings(triggers: Triggers = new Triggers(),
                              headers: Option[Map[String, String]] = None,
                              bccAddress: Option[String] = None,
                              trackingDomain: Option[String] = None,
                              signingDomain: Option[String] = None,
                              returnPathDomain: Option[String] = None,
                              mergeLanguage: MergeLanguageType = MergeLanguageType.MAILCHIMP,
                              globalMergeVars: Option[Seq[MergeVar]] = None,
                              mergeVars: Option[Seq[PerRecipMergeVar]] = None,
                              tags: Option[Seq[String]] = None,
                              subAccount: Option[String] = None,
                              googleAnalyticsDomains: Option[Seq[String]] = None,
                              googleAnalyticsCampaign: Option[Seq[String]] = None,
                              metadata: Option[Map[String, String]] = None,
                              recipientMetadata: Option[Seq[PerRecipMetadata]] = None) {

  private[mandrill] def withDefaults(default: AdditionalSettings): AdditionalSettings = {
    new AdditionalSettings(triggers.withDefaults(default.triggers),
                          headers.fold(default.headers){Some(_)},
                          bccAddress.fold(default.bccAddress){Some(_)},
                          trackingDomain.fold(default.trackingDomain){Some(_)},
                          signingDomain.fold(default.signingDomain){Some(_)},
                          returnPathDomain.fold(default.returnPathDomain){Some(_)},
                          if (Option(mergeLanguage).isDefined) mergeLanguage else default.mergeLanguage,
                          globalMergeVars.fold(default.globalMergeVars){Some(_)},
                          mergeVars.fold(default.mergeVars){Some(_)},
                          tags.fold(default.tags){Some(_)},
                          subAccount.fold(default.subAccount){Some(_)},
                          googleAnalyticsDomains.fold(default.googleAnalyticsCampaign){Some(_)},
                          googleAnalyticsCampaign.fold(default.googleAnalyticsCampaign){Some(_)},
                          metadata.fold(default.metadata){Some(_)},
                          recipientMetadata.fold(default.recipientMetadata){Some(_)})
  }
}

case class Triggers(important: Option[Boolean] = Some(false),
                    trackOpens: Option[Boolean] = None,
                    trackClicks: Option[Boolean] = None,
                    autoText: Option[Boolean] = None,
                    autoHtml: Option[Boolean] = None,
                    inlineCss: Option[Boolean] = None,
                    urlStripQs: Option[Boolean] = None,
                    preserveRecipients: Option[Boolean] = None,
                    viewContentLink: Option[Boolean] = None,
                    merge: Option[Boolean] = None) {
  private[mandrill] def withDefaults(default: Triggers): Triggers = {
    new Triggers()
  }
}

case class Email(bodyHtml: Option[String] = None,
                 bodyText: Option[String] = None,
                 subject: Option[String] = None,
                 from: Option[String] = None,
                 fromName: Option[String] = None,
                 recipients: Option[Seq[Recipient]] = None,
                 attachments: Option[Seq[FileAttachment]] = None,
                 images: Option[Seq[ContentImage]] = None,
                 settings: AdditionalSettings = new AdditionalSettings()) {
  private[mandrill] def withDefaults(default: Email): Email = {
    new Email(bodyHtml.fold(default.bodyHtml){Some(_)},
              bodyText.fold(default.bodyText){Some(_)},
              subject.fold(default.subject){Some(_)},
              from.fold(default.from){Some(_)},
              fromName.fold(default.fromName){Some(_)},
              recipients.fold(default.recipients){Some(_)},
              attachments.fold(default.attachments){Some(_)},
              images.fold(default.images){Some(_)},
              settings.withDefaults(default.settings)
    )
  }
}

case class Recipient(email: String,
                     name: Option[String] = None,
                     recipType: RecipientType = RecipientType.TO)

case class FileAttachment(mimeType: String,
                          name: String, content: String)
case class ContentImage(mimeType: String,
                        name: String, content: String)

case class MergeVar(name: String,
                    content: String)
case class PerRecipMergeVar(email: String,
                            vars: Option[Seq[MergeVar]] = None)
case class PerRecipMetadata(email: String,
                            values: Option[Map[String, Int]] = None)


case class Error(status: SendStatusType,
                 code: Int,
                 name: String,
                 message: String)

case class Response(results: Option[List[Result]] = None, error: Option[Error] = None) {
  def isError: Boolean = {
    error.isDefined
  }
}
case class Result(email: String,
                  status: SendStatusType,
                  rejectReason: RejectReasonType,
                  id: String)

object RecipientType extends Enumeration {
  type RecipientType = Value
  val TO = Value("to")
  val CC = Value("cc")
  val BCC = Value("bcc")
}

object MergeLanguageType extends Enumeration {
  type MergeLanguageType = Value
  val MAILCHIMP = Value("mailchimp")
  val HANDLEBARS = Value("handlebars")
}

object SendStatusType extends Enumeration {
  type SendStatusType = Value
  val SENT = Value("sent")
  val QUEUED = Value("queued")
  val SCHEDULED = Value("scheduled")
  val REJECTED = Value("rejected")
  val INVALID = Value("invalid")
  val ERROR = Value("error")
}

object RejectReasonType extends Enumeration {
  type RejectReasonType = Value
  val HARD_BOUNCE = Value("hard-bounce")
  val SOFT_BOUNCE = Value("soft-bounce")
  val SPAM = Value("spam")
  val UNSUB = Value("unsub")
  val CUSTOM = Value("custom")
  val INVALID_SENDER = Value("invalid-sender")
  val INVALID = Value("invalid")
  val TEST_MODE_LIMIT = Value("test-mode-limit")
  val RULE = Value("rule")
}

object Serializer {
  implicit val customDateWrites: Writes[java.util.Date] = dateWrites("yyyy-MM-dd HH:mm:ss")
  implicit val customDateReads: Reads[java.util.Date] = dateReads("yyyy-MM-dd HH:mm:ss")

  implicit val mergeLanguageFormat = EnumJson.enumFormat(MergeLanguageType)
  implicit val recipientTypeFormat = EnumJson.enumFormat(RecipientType)
  implicit val sendStatusTypeFormat = EnumJson.enumFormat(SendStatusType)
  implicit val rejectReasonTypeFormat = EnumJson.enumFormat(RejectReasonType)

  implicit val perRecipMatadataFormat: Format[PerRecipMetadata] = (
    (__ \ "rcpt").format[String] and
    (__ \ "values").formatNullable[Map[String, Int]]
  )(PerRecipMetadata.apply, unlift(PerRecipMetadata.unapply))

  implicit val mergeVarFormat: Format[MergeVar] = (
    (__ \ "name").format[String] and
    (__ \ "content").format[String]
  )(MergeVar.apply, unlift(MergeVar.unapply))

  implicit val perRecipMergeVarFormat: Format[PerRecipMergeVar] = (
    (__ \ "rcpt").format[String] and
    (__ \ "vars").formatNullable[Seq[MergeVar]]
  )(PerRecipMergeVar.apply, unlift(PerRecipMergeVar.unapply))

  implicit val recipientFormat: Format[Recipient] = (
    (__ \ "email").format[String] and
    (__ \ "name").formatNullable[String] and
    (__ \ "type").format[RecipientType]
  )(Recipient.apply, unlift(Recipient.unapply))

  implicit val fileAttachmentFormat: Format[FileAttachment] = (
    (__ \ "type").format[String] and
    (__ \ "name").format[String] and
    (__ \ "content").format[String]
  )(FileAttachment.apply, unlift(FileAttachment.unapply))

  implicit val contentImageFormat: Format[ContentImage] = (
    (__ \ "type").format[String] and
    (__ \ "name").format[String] and
    (__ \ "content").format[String]
  )(ContentImage.apply, unlift(ContentImage.unapply))

  implicit val triggersFormat: Format[Triggers] = (
    (__ \ "important").formatNullable[Boolean] and
    (__ \ "track_opens").formatNullable[Boolean] and
    (__ \ "track_clicks").formatNullable[Boolean] and
    (__ \ "auto_text").formatNullable[Boolean] and
    (__ \ "auto_html").formatNullable[Boolean] and
    (__ \ "inline_css").formatNullable[Boolean] and
    (__ \ "url_strip_qs").formatNullable[Boolean] and
    (__ \ "preserve_recipients").formatNullable[Boolean] and
    (__ \ "view_content_link").formatNullable[Boolean] and
    (__ \ "merge").formatNullable[Boolean]
  )(Triggers.apply, unlift(Triggers.unapply))

  implicit val settingsFormat: Format[AdditionalSettings] = (
    (__).format[Triggers] and
    (__ \ "headers").formatNullable[Map[String, String]] and
    (__ \ "bcc_address").formatNullable[String] and
    (__ \ "tracking_domain").formatNullable[String] and
    (__ \ "signing_domain").formatNullable[String] and
    (__ \ "return_path_domain").formatNullable[String] and
    (__ \ "merge_language").format[MergeLanguageType] and
    (__ \ "global_merge_vars").formatNullable[Seq[MergeVar]] and
    (__ \ "merge_vars").formatNullable[Seq[PerRecipMergeVar]] and
    (__ \ "tags").formatNullable[Seq[String]] and
    (__ \ "subaccount").formatNullable[String] and
    (__ \ "google_analytics_domains").formatNullable[Seq[String]] and
    (__ \ "google_analytics_campaign").formatNullable[Seq[String]] and
    (__ \ "metadata").formatNullable[Map[String, String]] and
    (__ \ "recipient_metadata").formatNullable[Seq[PerRecipMetadata]]
  )(AdditionalSettings.apply, unlift(AdditionalSettings.unapply))

  implicit val emailFormat: Format[Email] = (
    (__ \ "html").formatNullable[String] and
    (__ \ "text").formatNullable[String] and
    (__ \ "subject").formatNullable[String] and
    (__ \ "from_email").formatNullable[String] and
    (__ \ "from_name").formatNullable[String] and
    (__ \ "to").formatNullable[Seq[Recipient]] and
    (__ \ "attachments").formatNullable[Seq[FileAttachment]] and
    (__ \ "images").formatNullable[Seq[ContentImage]] and
    (__).format[AdditionalSettings]
  )(Email.apply, unlift(Email.unapply))

  implicit val requestFormat: Format[Request] = (
    (__ \ "key").format[String] and
    (__ \ "message").format[Email] and
    (__ \ "async").formatNullable[Boolean] and
    (__ \ "ip_pool").formatNullable[String] and
    (__ \ "send_at").formatNullable[Date]
  )(Request.apply, unlift(Request.unapply))

  implicit val errorFormat: Format[Error] = (
    (__ \ "status").format[SendStatusType] and
    (__ \ "code").format[Int] and
    (__ \ "name").format[String] and
    (__ \ "message").format[String]
    )(Error.apply, unlift(Error.unapply))

  implicit val resultFormat: Format[Result] = (
    (__ \ "email").format[String] and
    (__ \ "status").format[SendStatusType] and
    (__ \ "reject_reason").format[RejectReasonType] and
    (__ \ "_id").format[String]
  )(Result.apply, unlift(Result.unapply))

  def printJson (request: Request, pretty: Boolean = false): String = {
    val json = Json.toJson(request)
    if (pretty) Json.prettyPrint(json) else Json.stringify(json)
  }

  private[mandrill] def deserializeResult (json: JsValue): JsResult[Response] = {
    json.validate[List[Result]] match {
      case r: JsSuccess[List[Result]] => {
        JsSuccess(new Response(Some(r.get)))
      }
      case e: JsError => { e }
    }
  }

  private[mandrill] def deserializeError (json: JsValue): JsResult[Response] = {
    json.validate[Error] match {
      case r: JsSuccess[Error] => {
        JsSuccess(new Response(None, Some(r.get)))
      }
      case e: JsError => { e }
    }
  }

  private[mandrill] def deserializeEmail (json: JsValue): JsResult[Email] = {
    json.validate[Email]
  }

  def main2(args: Array[String]) {
    val conf = ConfigFactory.load()
    val str = conf.getList("response").render(ConfigRenderOptions.concise())

    val resp = Json.parse(str).validate[List[Result]]

    print(resp)
  }

  def main(args: Array[String]) {

    val request = new Request("key", new Email(Some("<p>Example HTML content</p>"),
                                               Some("Example text content"),
                                               Some("example subject"),
                                               Some("message.from_email@example.com"),
                                               None,
                                               None,
                                               Some(Seq(new FileAttachment("text/plain", "myfile.txt", "ZXhhbXBsZSBmaWxl"))),
                                               Some(Seq(new ContentImage("image/png", "IMAGECID", "ZXhhbXBsZSBmaWxl"))),
                                                      new AdditionalSettings(headers = Some(Map("Reply-To3" -> "message.reply@example.com", "2" -> "2")),
                                                                                  bccAddress = Some("message.bcc_address@example.com"),
                                                                                  globalMergeVars = Some(Seq(new MergeVar("merge1", "merge1 content"))),
                                                                                  mergeVars = Some(Seq(new PerRecipMergeVar("recipient.email@example.com",
                                                                                                                       Some(Seq(new MergeVar("merge2", "merge2 content")))))),
                                                                                  tags = Some(Seq("password-resets")),
                                                                                  subAccount = Some("customer-123"),
                                                                                  googleAnalyticsDomains = Some(Seq("example.com")),
                                                                                  googleAnalyticsCampaign = Some(Seq("message.from_email@example.com")),
                                                                                  metadata = Some(Map("website" -> "www.example.com", "website2" -> "www.example2.com")),
                                                                                  recipientMetadata = Some(Seq(new PerRecipMetadata("recipient.email@example.com", Some(Map("user_id" -> 123456)))))
                                                      )
                                            ),
                                sendAt = Some(new Date))

//    val conf = ConfigFactory.load()
//    val defaultJsonStr = conf.getObject("default_message_json").render(ConfigRenderOptions.concise())
//    Json.parse(defaultJsonStr).validate[Email]match {
//            case m: JsSuccess[Email] => {
//              val mail: Email = m.get
//
//              print(mail)
//            }
//            case e: JsError => {
//              print(e)
//            }
//          }


    val conf = ConfigFactory.load()
    val defaultJsonStr = conf.getObject("mandrill.default.mail.json").render(ConfigRenderOptions.concise())
    Json.parse(defaultJsonStr).validate[Email] match {
            case r: JsSuccess[Email] => {
              val requestDef: Email = r.get


            }
            case e: JsError => {
              print(e)
            }
          }





//    val defaultJson = Json.fromJson(defaultJsonStr)
////    val obj = ConfigFactory.parseString(jsonStr).getObject("message").unwrapped()
////    val defaultMessage = defaultJson.validate[Email]
//
//    val str1 = Json.prettyPrint(json)
//
//    val jsonStr = Json.stringify(json)
//
//    Json.parse(jsonStr).validate[Request] match {
//      case r: JsSuccess[Request] => {
//        val request: Request = r.get
//        val json = Json.toJson(request)
//        print(str1 == Json.prettyPrint(json))
//      }
//      case e: JsError => {
//        print(e)
//      }
//    }
  }
}


