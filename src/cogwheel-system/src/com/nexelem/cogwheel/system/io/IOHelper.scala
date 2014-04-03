package com.nexelem.cogwheel.system.io

/**
 * Project: cogwheel
 * User: pdolega
 * Date: 4/3/14
 * Time: 7:05 PM
 * Version: 1.0
 * Utility methods containing methods for general IO handling.
 */
object IOHelper {

  /**
   * Closes passed object with use of close() method. On error does nothing for now - exactly like IOUtils.closeQuietly from apache commons-io
   * @param closable any object having method close with no params and no return type. Btw this a nice use of structural typing (aka. duck typing for static typing)
   */
  def closeQuietely(closable: { def close() }) {
    try {
      closable.close()
    } catch {
      case _: Exception => // nothing...
    }
  }
}
