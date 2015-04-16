package org.globalmoney.mandrill

/**
 *
 * @author Sergey Khruschak <sergey.khruschak@gmail.com>
 * Created on 4/16/15.
 */
class MandrillException extends RuntimeException {}

class MandrillSendingException(val error: MandrillError) extends MandrillException {}