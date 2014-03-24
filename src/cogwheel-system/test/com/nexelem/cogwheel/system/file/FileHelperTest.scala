package com.nexelem.cogwheel.system.file

import org.specs2.mutable.SpecificationWithJUnit
import org.apache.commons.logging.LogFactory
import scala.io.Source
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * Project: cogwheel
 * User: pdolega
 * Date: 3/24/14
 * Time: 2:15 PM
 * Version: 1.0
 */
class FileHelperTest extends SpecificationWithJUnit {

  private val log = LogFactory.getLog(getClass)

  "replacing value in files" should {
    "replace existing values correctly" in {
      val srcFile = new File(getClass.getResource("replaceFileSimple.txt").getPath)
      val testFile = new File(srcFile.getParentFile, "testReplaceFile1.txt")

      try {
        FileUtils.copyFile(srcFile, testFile)
        FileHelper.replaceValuesInFile(testFile.getAbsolutePath, "{name}" -> "world")

        val readText = Source.fromFile(testFile).mkString
        readText must beEqualTo("Hello world !")
      } finally {
        FileUtils.deleteQuietly(testFile)
      }
    }

    "replace existing values correctly in files with multiple values and EOL as last character" in {
      val srcFile = new File(getClass.getResource("replaceFileMultipleNewLineEnd.txt").getPath)
      val testFile = new File(srcFile.getParentFile, "testReplaceFile2.txt")

      try {
        FileUtils.copyFile(srcFile, testFile)
        FileHelper.replaceValuesInFile(testFile.getAbsolutePath, "{name}" -> "world", "{myName}" -> "Vladimir", "{country}" -> "Russia")

        val readText = Source.fromFile(testFile).mkString
        readText must beEqualTo("Hello world !\nMy name is Vladimir.\nFrom Russia with love.")
      } finally {
        FileUtils.deleteQuietly(testFile)
      }
    }

    "not change the file at all if marker is not found" in {
      val srcFile = new File(getClass.getResource("replaceFileSimple.txt").getPath)
      val testFile = new File(srcFile.getParentFile, "testReplaceFile3.txt")

      try {
        FileUtils.copyFile(srcFile, testFile)
        FileHelper.replaceValuesInFile(testFile.getAbsolutePath, "{non-existent}" -> "A-HA !")

        val readText = Source.fromFile(testFile).mkString
        readText must beEqualTo("Hello {name} !")
      } finally {
        FileUtils.deleteQuietly(testFile)
      }
    }
  }
}
