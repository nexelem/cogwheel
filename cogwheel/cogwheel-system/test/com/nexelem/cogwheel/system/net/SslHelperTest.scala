package com.nexelem.cogwheel.system.net

import org.specs2.mutable.SpecificationWithJUnit

/**
 * Project: cogwheel
 * User: pdolega
 * Date: 4/3/14
 * Time: 7:12 PM
 * Version: 1.0
 */
class SslHelperTest extends SpecificationWithJUnit {

  "closing objects" should {
    "close successfully objects that doesn't throw exception" in {
      var closed = false
      class CloseTest {
        def close() {
          closed = true
        }
      }

      closed must beFalse
      IOHelper.closeQuietely(new CloseTest)
      closed must beTrue
    }

    "close successfully objects throws exception" in {
      var closed = false
      class CloseExceptionTest {
        def close() {
          closed = true
          throw new Exception
        }
      }

      closed must beFalse
      IOHelper.closeQuietely(new CloseExceptionTest)
      closed must beTrue
    }
  }
}
