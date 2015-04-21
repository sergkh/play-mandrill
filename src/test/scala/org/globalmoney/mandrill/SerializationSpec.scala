package org.globalmoney.mandrill

import org.specs2.mutable._
import org.specs2.runner._
import play.api.libs.json.Json
import Serializer._
/**
 * Created by sergeykhruschak on 4/21/15.
 */
class SerializationSpec extends Specification {

  "Model" should {

    "accept empty config" in {
      Json.parse("{}").as[Email] mustEqual Email("")
    }

  }
}
