package com.nexelem.cogwheel.system.file

import org.specs2.mutable.SpecificationWithJUnit
import org.apache.commons.logging.LogFactory
import scala.io.Source
import org.apache.commons.io.FileUtils
import java.io.{PrintWriter, File}
import com.nexelem.cogwheel.system.zip.ZipHelper

/**
 * Project: cogwheel
 * User: pdolega
 * Date: 3/24/14
 * Time: 2:15 PM
 * Version: 1.0
 */
class FileHelperTest extends SpecificationWithJUnit {

  private val log = LogFactory.getLog(getClass)

  sequential // sekwencyjne wykonanie testow

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

  "repacking and processing" should {
    "repack existing archive correctly" in {
      val testZip = new File(getClass.getResource("repack_test.zip").getPath)
      val resultZip = new File(testZip.getParent, "result.zip")
      val testFilesContent = scala.collection.mutable.Map[String, String]()
      val operation = (tmpPath : String) => {
        val tmpDir = new File(tmpPath)
        tmpDir.isDirectory must beEqualTo(true)
        val files = tmpDir.listFiles
        for (i <- 0 to files.length-1) {
          val source = Source.fromFile(files(i))
          val out = new PrintWriter(files(i).getAbsolutePath, "UTF-8")
          try{
            val readText = source.mkString + " test " + i
            testFilesContent(files(i).getName) = readText
            out.print(readText)
          }
          finally{
            source.close
            out.close
          }
        }
      }
      val extractedDir = new File(resultZip.getParent + File.separator + "result")
      try {
        FileHelper.repackAndProcess(testZip.getAbsolutePath, resultZip.getAbsolutePath)(operation)
        resultZip.isFile must beEqualTo(true)
        FileUtils.forceMkdir(extractedDir)
        extractedDir.isDirectory must beEqualTo(true)
        ZipHelper.extractZip(resultZip.getAbsolutePath, extractedDir.getAbsolutePath)
        val resultFiles = extractedDir.listFiles()
        for (i <- 0 to resultFiles.length-1) {
          val source = Source.fromFile(resultFiles(i))
          try{
            source.mkString must beEqualTo(testFilesContent(resultFiles(i).getName))
          }
          finally{
            source.close
          }
        }
        success
      } finally {
        FileUtils.deleteQuietly(resultZip)
        FileUtils.deleteQuietly(resultZip)
        FileUtils.deleteQuietly(extractedDir)
      }
    }
  }
}
