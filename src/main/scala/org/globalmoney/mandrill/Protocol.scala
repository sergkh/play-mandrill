package org.globalmoney.mandrill

import java.util.Date

import com.typesafe.config.{ConfigRenderOptions, ConfigFactory}
import org.globalmoney.mandrill.MergeLanguageType.MergeLanguageType
import org.globalmoney.mandrill.RecipientType.RecipientType
import org.globalmoney.mandrill.RejectReasonType.RejectReasonType
import org.globalmoney.mandrill.SendStatusType.SendStatusType
import play.api.data.validation.ValidationError
import play.api.libs.json.Writes.StringWrites
import play.api.libs.json._
import play.api.libs.functional.syntax._

import play.api.libs.json.Writes.dateWrites
import play.api.libs.json.Reads.dateReads

/**
 * Created by Viktor Zakalyuzhnyy on 07.04.15.
 */
case class Request(key: String, message: Email, template: Option[Template] = None, async: Option[Boolean] = Some(false),
                   ipPool: Option[String] = None, sendAt: Option[Date] = None) {


  private[mandrill] def withDefaults(default: Request) = Request(
    key,
    message.withDefaults(default.message),
    template orElse default.template,
    async orElse default.async,
    ipPool orElse default.ipPool,
    sendAt orElse default.sendAt
  )

}
case class Template(name: String, content: Seq[MergeVar] = Seq.empty)

case class AdditionalSettings(triggers: Triggers = new Triggers(),
                              headers: Option[Map[String, String]] = None,
                              bccAddress: Option[String] = None,
                              trackingDomain: Option[String] = None,
                              signingDomain: Option[String] = None,
                              returnPathDomain: Option[String] = None,
                              mergeLanguage: Option[MergeLanguageType] = None,
                              globalMergeVars: Seq[MergeVar] = Seq.empty,
                              mergeVars: Seq[PerRecipMergeVar] = Seq.empty,
                              tags: Seq[String] = Seq.empty,
                              subAccount: Option[String] = None,
                              googleAnalyticsDomains: Seq[String] = Seq.empty,
                              googleAnalyticsCampaign: Seq[String] = Seq.empty,
                              metadata: Option[Map[String, String]] = None,
                              recipientMetadata: Seq[PerRecipMetadata] = Seq.empty) {

  private[mandrill] def withDefaults(default: AdditionalSettings): AdditionalSettings = AdditionalSettings(
    triggers.withDefaults(default.triggers),
    headers orElse default.headers,
    bccAddress orElse default.bccAddress,
    trackingDomain orElse default.trackingDomain,
    signingDomain orElse default.signingDomain,
    returnPathDomain orElse default.returnPathDomain,
    mergeLanguage orElse default.mergeLanguage orElse Some(MergeLanguageType.MAILCHIMP),
    if(globalMergeVars.nonEmpty) globalMergeVars else default.globalMergeVars,
    if(mergeVars.nonEmpty) mergeVars else default.mergeVars,
    if(tags.nonEmpty) tags else default.tags,
    if(subAccount.nonEmpty) subAccount else default.subAccount,
    if(googleAnalyticsDomains.nonEmpty) googleAnalyticsDomains else default.googleAnalyticsDomains,
    if(googleAnalyticsCampaign.nonEmpty) googleAnalyticsCampaign else default.googleAnalyticsCampaign,
    metadata orElse default.metadata,
    if(recipientMetadata.nonEmpty) recipientMetadata else default.recipientMetadata
  )
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
  private[mandrill] def withDefaults(default: Triggers): Triggers = Triggers(
    important orElse default.important,
    trackOpens orElse default.trackOpens,
    trackClicks orElse default.trackClicks,
    autoText orElse default.autoText,
    autoHtml orElse default.autoHtml,
    inlineCss orElse default.inlineCss,
    urlStripQs orElse default.urlStripQs,
    preserveRecipients orElse default.preserveRecipients,
    viewContentLink orElse default.viewContentLink,
    merge orElse default.merge
  )
}

case class Email(subject: String,
                from: Option[Sender] = None,
                to: Seq[Recipient] = Seq.empty,
                bodyText: Option[String] = None,
                bodyHtml: Option[String] = None,
                attachments: Seq[FileAttachment] = Seq.empty,
                images: Seq[ContentImage] = Seq.empty,
                settings: AdditionalSettings = AdditionalSettings()) {

  private[mandrill] def withDefaults(default: Email) = Email(
    subject,
    from orElse default.from,
    if(to.nonEmpty) to else default.to,
    bodyText orElse default.bodyText,
    bodyHtml orElse default.bodyHtml,
    attachments,
    images,
    settings.withDefaults(default.settings)
  )
}

case class Sender(email: String, name: Option[String] = None)
case class Recipient(email: String, name: Option[String] = None, recipType: RecipientType = RecipientType.TO)
case class FileAttachment(mimeType: String, name: String, content: String)
case class ContentImage(mimeType: String, name: String, content: String)
case class MergeVar(name: String, content: String)
case class PerRecipMergeVar(email: String, vars: Seq[MergeVar] = Seq.empty)
case class PerRecipMetadata(email: String, values: Option[Map[String, Int]] = None)

case class MandrillError(status: SendStatusType, code: Int, name: String, message: String)

case class Result(email: String, status: SendStatusType, rejectReason: RejectReasonType, id: String)

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

  implicit val mergeVarFormat: Format[MergeVar] = Json.format[MergeVar]

  implicit val perRecipMergeVarFormat: Format[PerRecipMergeVar] = (
    (__ \ "rcpt").format[String] and
    (__ \ "vars").formatNullableIterable[Seq[MergeVar]]
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
    (__ \ "merge_language").formatNullable[MergeLanguageType] and
    (__ \ "global_merge_vars").formatNullableIterable[Seq[MergeVar]] and
    (__ \ "merge_vars").formatNullableIterable[Seq[PerRecipMergeVar]] and
    (__ \ "tags").formatNullableIterable[Seq[String]] and
    (__ \ "subaccount").formatNullable[String] and
    (__ \ "google_analytics_domains").formatNullableIterable[Seq[String]] and
    (__ \ "google_analytics_campaign").formatNullableIterable[Seq[String]] and
    (__ \ "metadata").formatNullable[Map[String, String]] and
    (__ \ "recipient_metadata").formatNullableIterable[Seq[PerRecipMetadata]]
  )(AdditionalSettings.apply, unlift(AdditionalSettings.unapply))

  implicit val templateFormat: Format[Template] = (
    (__ \ "template_name").format[String] and
    (__ \ "template_content").formatNullableIterable[Seq[MergeVar]]
    )(Template.apply, unlift(Template.unapply))

  implicit val senderFormat: Format[Sender] = (
    (__ \ "from_email").format[String](StringSkipReads) and
      (__ \ "from_name").formatNullable[String]
    )(Sender.apply, unlift(Sender.unapply))

  implicit val emailFormat: Format[Email] = (
    (__ \ "subject").format[String](StringSkipReads) and
    (__).formatNullable[Sender] and
    (__ \ "to").formatNullableIterable[Seq[Recipient]] and
    (__ \ "text").formatNullable[String] and
    (__ \ "html").formatNullable[String] and
    (__ \ "attachments").formatNullableIterable[Seq[FileAttachment]] and
    (__ \ "images").formatNullableIterable[Seq[ContentImage]] and
    (__).format[AdditionalSettings]
    )(Email.apply, unlift(Email.unapply))

  implicit val requestFormat: Format[Request] = (
    (__ \ "key").format[String] and
    (__ \ "message").format[Email] and
    (__).formatNullable[Template] and
    (__ \ "async").formatNullable[Boolean] and
    (__ \ "ip_pool").formatNullable[String] and
    (__ \ "send_at").formatNullable[Date]
    )(Request.apply, unlift(Request.unapply))

  implicit val errorFormat: Format[MandrillError] = (
    (__ \ "status").format[SendStatusType] and
    (__ \ "code").format[Int] and
    (__ \ "name").format[String] and
    (__ \ "message").format[String]
    )(MandrillError.apply, unlift(MandrillError.unapply))

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


  // http://stackoverflow.com/questions/21297987/play-scala-how-to-prevent-json-serialization-of-empty-arrays
  implicit class PathAdditions(path: JsPath) {

    def readNullableIterable[A <: Iterable[_]](implicit reads: Reads[A]): Reads[A] =
      Reads((json: JsValue) => path.applyTillLast(json).fold(
        error => error,
        result => result.fold(
          invalid = (_) => reads.reads(JsArray()),
          valid = {
            case JsNull => reads.reads(JsArray())
            case js => reads.reads(js).repath(path)
          })
      ))

    def writeNullableIterable[A <: Iterable[_]](implicit writes: Writes[A]): OWrites[A] =
      OWrites[A]{ (a: A) =>
        if (a.isEmpty) Json.obj()
        else JsPath.createObj(path -> writes.writes(a))
      }

    /** When writing it ignores the property when the collection is empty,
      * when reading undefined and empty jsarray becomes an empty collection */
    def formatNullableIterable[A <: Iterable[_]](implicit format: Format[A]): OFormat[A] =
      OFormat[A](readNullableIterable(format), writeNullableIterable(format))
  }


  object StringSkipReads extends Reads[String] { def reads(json: JsValue) = JsSuccess("") }
}


