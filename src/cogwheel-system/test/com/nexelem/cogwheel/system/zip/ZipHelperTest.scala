package com.nexelem.cogwheel.system.zip

import org.specs2.mutable.SpecificationWithJUnit
import java.io.{FileInputStream, File}
import org.apache.commons.io.FileUtils

/**
 * Project: cogwheel
 * User: mzagorski
 * Date: 3/4/14
 * Time: 2:15 PM
 * Version: 1.0
 */
class ZipHelperTest extends SpecificationWithJUnit {

  sequential // sekwencyjne wykonanie testow


  private def fileToBytes(f : File) : Array[Byte] = {
    val in = new FileInputStream(f)
    val bytes = new Array[Byte](f.length.toInt)
    in.read(bytes)
    in.close()
    return bytes
  }


  private def compareFileContent(sourceFile : File, resultFile : File) {
    val sourceByteArray = fileToBytes(sourceFile)
    val resultByteArray = fileToBytes(resultFile)
    sourceByteArray.length must beEqualTo(resultByteArray.length)
    for(i <- 0 to sourceByteArray.length-1) {
      sourceByteArray(i) must beEqualTo(resultByteArray(i))
    }
  }

  private def dirsAssertEquals(source : File, result : File) {
    val sourceFiles = source.listFiles()
    val resultFiles = result.listFiles()
    sourceFiles.length must beEqualTo(resultFiles.length)
    for(i <- 0 to sourceFiles.length-1) {
      var found = false;
      for(j <- 0 to resultFiles.length-1) {
        if(sourceFiles(i).getName == resultFiles(j).getName) {
          if(sourceFiles(i).isDirectory) {
            resultFiles(j).isDirectory must beEqualTo(true)
            dirsAssertEquals(sourceFiles(i), resultFiles(j))
          } else {
            compareFileContent(sourceFiles(i), resultFiles(j))
          }
          found = true
        }
      }
      if(!found) {
        failure
      }
    }
  }

  "packing and extracting archives" should {

    "pack and extrect archives correctly" in {
      val contentDirName = "test_zip_content"
      val testDir = new File(getClass.getResource(contentDirName).getPath).getParentFile
      val zipFilePath =  testDir.getAbsolutePath + File.separator + "result.zip"
      val contentDirPath = testDir.getAbsolutePath + File.separator + contentDirName
      val resultDirPath = testDir.getAbsolutePath + File.separator + "test_zip_result_dir"
      try {
        ZipHelper.createZip(contentDirPath, zipFilePath)
        ZipHelper.extractZip(zipFilePath, resultDirPath)
        val sourceDir = new File(contentDirPath)
        val resultDir = new File(resultDirPath)
        dirsAssertEquals(sourceDir, resultDir)
        success
      } finally {
        FileUtils.deleteQuietly(new File(zipFilePath))
        FileUtils.deleteQuietly(new File(resultDirPath))
      }
    }

    "pack and extract archives correctly if zip file is created inside zipped dir" in {
      val contentDirName = "test_zip_content"
      val testDir = new File(getClass.getResource(contentDirName).getPath).getParentFile
      val contentDirPath = testDir.getAbsolutePath + File.separator + contentDirName
      val zipFilePath = contentDirPath + File.separator + "result.zip"
      val resultDirPath = testDir.getAbsolutePath + File.separator + "test_zip_result_dir"
      try {
        ZipHelper.createZip(contentDirPath, zipFilePath)
        ZipHelper.extractZip(zipFilePath, resultDirPath)
        FileUtils.deleteQuietly(new File(zipFilePath))
        val sourceDir = new File(contentDirPath)
        val resultDir = new File(resultDirPath)
        dirsAssertEquals(sourceDir, resultDir)
        success
      } finally {
        FileUtils.deleteQuietly(new File(zipFilePath))
        FileUtils.deleteQuietly(new File(resultDirPath))
      }
    }

  }

}
