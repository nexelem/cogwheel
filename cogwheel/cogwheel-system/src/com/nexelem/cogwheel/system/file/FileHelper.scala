package com.nexelem.cogwheel.system.file

import java.io.{FilenameFilter, BufferedWriter, File, FileWriter}

import scala.collection.mutable.MutableList
import scala.io.Source
import scala.util.Properties

import com.nexelem.cogwheel.system.io.IOHelper
import com.nexelem.cogwheel.system.zip.ZipHelper

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.WildcardFileFilter

/**
 * Project: cogwheel
 * User: pdolega
 * Date: 3/24/14
 * Time: 1:50 PM
 * Version: 1.0
 * Utility object containing methods for operating on files and directories.
 */
object FileHelper {

  /**
   * Replaces values in given file.
   * @param filePath
   * @param markerReplacements replacement values
   */
  def replaceValuesInFile(filePath: String, markerReplacements: (String, String)*) {
    replaceValuesInFile(filePath, markerReplacements, Properties.lineSeparator)
  }

  /**
   * Replaces values in given file.
   * @param filePath
   * @param markerReplacements replacement values
   * @param separator line separator (defaults to system specific)
   */
  def replaceValuesInFile(filePath: String, markerReplacements: Seq[(String, String)], separator: String = Properties.lineSeparator) {
    var buffWriter: BufferedWriter = null
    var fileWriter: FileWriter = null
    var srcFileSource: Source = null

    val newFilename = File.createTempFile("temp", filePath.substring(filePath.lastIndexOf(".")), new File(filePath).getParentFile()).getAbsolutePath()

    try {
      fileWriter = new FileWriter(newFilename)
      buffWriter = new BufferedWriter(fileWriter)

      srcFileSource = Source.fromFile(filePath)
      val lines = srcFileSource.getLines.toTraversable
      val linesExceptLast = lines.slice(0, lines.size - 1)

      linesExceptLast.foreach(line => {
        writeLine(line + separator, buffWriter, markerReplacements:_*)
      })
      writeLine(lines.last, buffWriter, markerReplacements:_*)

    } finally {
      if (buffWriter != null) buffWriter.close()
      if (srcFileSource != null) srcFileSource.close()
    }

    new File(filePath).delete()
    new File(newFilename).renameTo(new File(filePath))
  }

  /**
   * Unpacks file, performs given operation on its content and repacks it back.
   * @param filePath source file is being get from there.
   * @param targetPath repacked file is sent there.
   * @param operation closure representing operation executed on unpacked zip contents.
   */
  def repackAndProcess(filePath: String, targetPath: String)(operation: String => Unit) {
    val tempDir = File.createTempFile("temp", null).getAbsolutePath()
    val fileName = new File(filePath).getName()

    FileUtils.forceDelete(new File(tempDir))
    FileUtils.forceMkdir(new File(tempDir))
    ZipHelper.extractZip(filePath, tempDir)

    operation(tempDir)

    ZipHelper.createZip(tempDir, tempDir + File.separator + fileName)
    FileUtils.copyFile(new File(tempDir, fileName), new File(targetPath))
    FileUtils.deleteDirectory(new File(tempDir))
  }

  /**
   * Scans given file contents and tries to match regex expression.
   * @return seq of seqs where each element contains 0: matched line (as a whole) and next number of matched regex groups (if defined in regex)
   */
  def matchLinesInFile(filePath: String, regex: String): Seq[Seq[String]] = {
    val matchedLines = MutableList[Seq[String]]()
    val pattern = regex.r
    val source = Source.fromFile(filePath)
    try {
      source.getLines().foreach { line =>
        val matched = pattern.findFirstMatchIn(line)
        if(matched.isDefined) {
          val count = matched.get.groupCount
          val matchedLine = MutableList[String]()

          matchedLine += line
          for(groupIndex <- 1 to count) {
            matchedLine += matched.get.group(groupIndex)
          }

          matchedLines += matchedLine
        }
      }
    } finally {
      IOHelper.closeQuietely(source)
    }

    matchedLines
  }

  /**
   * Searches for a specified file, allowing to use a wildcard symbol (*) in the filename
   * @param filePathAndNameWithWildcard path with a pathname
   * @return true if there is a match; otherwise returns false
   */
  def fileExists(filePathAndNameWithWildcard: String): Boolean = {
    val temp = new File(filePathAndNameWithWildcard)

    val dir = temp.getParentFile
    val fileName = temp.getName

    val fileFilter = new WildcardFileFilter(fileName)
    val files = dir.listFiles(fileFilter.asInstanceOf[FilenameFilter])
    if (!files.isEmpty) true else false
  }

  private def writeLine(line: String, buffWriter: BufferedWriter, markerReplacements: (String, String)*) {
    var changedLine: String = line
    markerReplacements.foreach(replacementMarker => changedLine = changedLine.replaceAllLiterally(replacementMarker._1, replacementMarker._2))
    buffWriter.write(changedLine, 0, changedLine.length)
  }
}
