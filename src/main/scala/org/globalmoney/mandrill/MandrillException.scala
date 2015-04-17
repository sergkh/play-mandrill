package org.globalmoney.mandrill

/**
 *
 * @author Sergey Khruschak <sergey.khruschak@gmail.com>
 * Created on 4/16/15.
 */
class MandrillException(error: String) extends RuntimeException(error) {}

class MandrillSendingException(val error: MandrillError) extends MandrillException(error.toString) {}