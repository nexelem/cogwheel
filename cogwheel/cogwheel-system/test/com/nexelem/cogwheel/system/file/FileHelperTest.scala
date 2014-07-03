package com.nexelem.cogwheel.system.file

import java.io.{File, PrintWriter}

import com.nexelem.cogwheel.system.zip.ZipHelper
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.LogFactory
import org.specs2.mutable.SpecificationWithJUnit

import scala.io.Source

import scala.util.Properties

/**
 * Project: cogwheel
 * User: pdolega
 * Date: 3/24/14
 * Time: 2:15 PM
 * Version: 1.0
 */
class FileHelperTest extends SpecificationWithJUnit {

  private val log = LogFactory.getLog(getClass)
  private val separator = Properties.lineSeparator
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

        readText must beEqualTo(s"Hello world !${separator}My name is Vladimir.${separator}From Russia with love.")
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
      val operation = createTextFilesModifyingOperation(testFilesContent)
      val extractedDir = new File(resultZip.getParent + File.separator + "result")
      try {
        FileHelper.repackAndProcess(testZip.getAbsolutePath, resultZip.getAbsolutePath)(operation)
        resultZip.isFile must beEqualTo(true)
        FileUtils.forceMkdir(extractedDir)
        extractedDir.isDirectory must beEqualTo(true)
        ZipHelper.extractZip(resultZip.getAbsolutePath, extractedDir.getAbsolutePath)
        val resultFiles = extractedDir.listFiles()
        resultFiles.foreach { resultFile => {
          val source = Source.fromFile(resultFile)
          try {
            source.mkString must beEqualTo(testFilesContent(resultFile.getName))
          }
          finally {
            source.close()
          }
        }
        }
        success
      } finally {
        FileUtils.deleteQuietly(resultZip)
        FileUtils.deleteQuietly(extractedDir)
      }
    }
  }

  "searching for regex in files" should {
    "return proper entry if there exist simple entry that matches regex" in {
      val srcPath = getClass.getResource("regex_sample.txt").getPath
      val matchedSeq = FileHelper.matchLinesInFile(srcPath, ".*stars and the.*")

      matchedSeq must haveSize(1)
      matchedSeq(0) must haveSize(1)
      matchedSeq(0)(0) must beEqualTo("But the stars and the stillness")
    }

    "return proper entries if there are 3 lines matched and groups in each" in {
      val srcPath = getClass.getResource("regex_sample.txt").getPath
      val matchedSeq = FileHelper.matchLinesInFile(srcPath, "^(\\w+) (\\w+) (\\w+) (\\w+)$")

      matchedSeq must haveSize(3)
      matchedSeq(0) must haveSize(5)

      matchedSeq(0)(0) must beEqualTo("The lightning and thunder")
      matchedSeq(0)(1) must beEqualTo("The")
      matchedSeq(0)(2) must beEqualTo("lightning")
      matchedSeq(0)(3) must beEqualTo("and")
      matchedSeq(0)(4) must beEqualTo("thunder")

      matchedSeq(1)(0) must beEqualTo("They go and come")
      matchedSeq(1)(1) must beEqualTo("They")
      matchedSeq(1)(2) must beEqualTo("go")
      matchedSeq(1)(3) must beEqualTo("and")
      matchedSeq(1)(4) must beEqualTo("come")

      matchedSeq(2)(0) must beEqualTo("Are always at home")
      matchedSeq(2)(1) must beEqualTo("Are")
      matchedSeq(2)(2) must beEqualTo("always")
      matchedSeq(2)(3) must beEqualTo("at")
      matchedSeq(2)(4) must beEqualTo("home")
    }

    "return nothing if there is not match at all" in {
      val srcPath = getClass.getResource("regex_sample.txt").getPath
      val matchedSeq = FileHelper.matchLinesInFile(srcPath, "^this regex is not found$")

      matchedSeq must beEmpty
    }
  }

  "searching for a file by name with a wildcard" should {
    val srcFile = new File(getClass.getResource("searchFileTest.txt").getPath)
    val location = srcFile.getParent

    "return true if the exact file name is provided" in {
      val testVal = location + "//searchFileTest.txt"
      val fileExists = FileHelper.fileExists(testVal)

      fileExists must beEqualTo(true)
    }

    "return true if there is a match" in {
      val testVal = location + "//searchFileTe*.txt"
      val fileExists = FileHelper.fileExists(testVal)

      fileExists must beEqualTo(true)
    }

    "return false if there is no match" in {
      val testVal = location + "//searchFileTe*a.txt"
      val fileExists = FileHelper.fileExists(testVal)

      fileExists must beEqualTo(false)
    }
  }

  private def createTextFilesModifyingOperation(fileMap : scala.collection.mutable.Map[String, String]): String => Unit = {
    val operation = (tmpPath: String) => {
      val tmpDir = new File(tmpPath)
      tmpDir.isDirectory must beEqualTo(true)
      val files = tmpDir.listFiles
      for (i <- 0 to files.length - 1) {
        val source = Source.fromFile(files(i))
        val out = new PrintWriter(files(i).getAbsolutePath, "UTF-8")
        try {
          val readText = source.mkString + " test " + i
          fileMap(files(i).getName) = readText
          out.print(readText)
        }
        finally {
          source.close()
          out.close()
        }
      }
    }
    operation
  }
}
