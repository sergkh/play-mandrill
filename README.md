# play-mandrill
Play2 interface for Mandrill mail sending service. The goal is to make plugin very similar to Play-Mailer but
with support of Mandrill service capabilities and implemented using play Web Services.

# Installation

1. Add dependency:

```scala
    "org.globalmoney" % "play-mandrill" % "0.1.1-SNAPSHOT" from "https://bintray.com/artifact/download/sergkh/generic/play-mandrill_2.11-0.1.1-SNAPSHOT.jar"
```

2. Add following line into `conf/play.plugins`:

    1500:org.globalmoney.mandrill.MandrillPlugin
    
3. Configure mandrill settings:

    mandrill.key = "your secret key"

# Usage

For now plugin is for scala only. 

```scala
  import org.globalmoney.mandrill._
  import play.api.Play.current
    
  val email = Email(
    subject = "Mail subject",
    from = Sender(email = "admin@yoursite.com", name = Some("Admin")),
    to = Seq(Recipient(email = "john.smith@domain.org", name = Some("John Smith"), recipType = RecipientType.TO)),
    bodyText = Some("Text message"),
    bodyHtml = Some("<html><body>Message</body></html>"),
    attachment = Seq(FileAttachment("text/html", "Attachment name", "Content")),
    images = Seq(ContentImage("image/gif", "Image name", "Content")),
    settings = AdditionalSettings()
  )
  
  MandrillPlugin.send(email)     
```

 Please note that `MandrillPlugin.send(mail)` implicitly takes play application as argument. 
 
# Configuration
 
All, except mail subject can be setup in `application.conf` and be overridden in each mail. Configuration format is 
made in accordance to [request format in mandrill](https://mandrillapp.com/api/docs/messages.JSON.html):

```
mandrill {
  key = "key"
  from_email = "admin@yoursite.com"
  from_name = "Admin"
  
  html = "<p>Example HTML content</p>",
  text = Example text content"
  to = [ 
    {
      email = "recipient.email@example.com",
      name = "Recipient Name",
      type = "to"
    }
  ],
  
  headers = {
   "Reply-To" = "message2.reply@example.com"
   "Reply-To2" = "message2.reply@example.com"
  },
  
  important = false
  track_opens = null
  track_clicks = null
  auto_text = null
  auto_html = null
  inline_css = null
  url_strip_qs = null
  preserve_recipients = null
  view_content_link = null
  bcc_address = "message.bcc_address@example.com"
  tracking_domain = null
  signing_domain = null
  return_path_domain = null
  merge = true
  merge_language = "mailchimp"

  global_merge_vars = [ 
    {
      name = "merge1"
      content = "merge1 content"
    }
  ]

  merge_vars = [ {
    rcpt = "recipient.email@example.com"
    vars = [ {
       name = "merge2"
       content = "merge2 content"
    } ]
  } ]
  
  tags = [ "password-resets" ]
  subaccount = "customer-123"
  google_analytics_domains = [ "example.com", "example2.com" ]
  google_analytics_campaign = [ "message.from_email@example.com" ]
  
  metadata = {
    website = "www.example.com"
    website2 = "www.example2.com"
  }
  
  recipient_metadata = [ {
    rcpt = "recipient.email@example.com",
    values = {
      "user_id" : 123456
    }
  } ]
}
```
