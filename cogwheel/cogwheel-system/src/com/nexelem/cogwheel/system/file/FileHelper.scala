package com.nexelem.cogwheel.system.file

import java.io.{FileWriter, BufferedWriter, File}
import scala.io.Source
import scala.util.Properties
import com.nexelem.cogwheel.system.process.ProcessHelper._
import scala.collection.mutable
import com.nexelem.cogwheel.system.io.IOHelper

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

    val newFilename = File.createTempFile("temp", filePath.substring(filePath.lastIndexOf(".")), new File(filePath).getParentFile()).getAbsolutePath()

    try {
      fileWriter = new FileWriter(newFilename)
      buffWriter = new BufferedWriter(fileWriter)

      val lines = Source.fromFile(filePath).getLines.toTraversable
      val linesExceptLast = lines.slice(0, lines.size - 1)

      linesExceptLast.foreach(line => {
        writeLine(line + separator, buffWriter, markerReplacements:_*)
      })
      writeLine(lines.last, buffWriter, markerReplacements:_*)

    } finally {
      if (buffWriter != null) buffWriter.close()
    }

    new File(filePath).delete()
    new File(newFilename).renameTo(new File(filePath))
  }

  /**
   * FIXME 1. this method should be rewritten to use Java/Scala notions instead of system specific (bash, unzip etc.) 2. There should be appropriate unit test(s).
   *
   * Unpacks file, performs given operation on its content and repacks it back.
   * @param filePath source file is being get from there.
   * @param targetPath repacked file is sent there.
   * @param operation closure representing operation executed on unpacked zip contents.
   */
  def repackAndProcess(filePath: String, targetPath: String)(operation: String => Unit) {
    val tempDir = File.createTempFile("temp", null).getAbsolutePath()
    val fileName = new File(filePath).getName()

    bash(s"rm -rf $tempDir")
    bash(s"mkdir $tempDir")

    bash(s"unzip -q -o $filePath -d $tempDir")
    operation(tempDir)
    bash(s"cd $tempDir; zip -r $fileName ./*")
    bash(s"cp $tempDir/$fileName $targetPath")

    bash(s"rm -rf $tempDir")
  }

  /**
   * Scans given file contents and tries to match regex expression.
   * @return seq of seqs where each element contains 0: matched line (as a whole) and next number of matched regex groups (if defined in regex)
   */
  def matchLinesInFile(filePath: String, regex: String): Seq[Seq[String]] = {
    val matchedLines = mutable.MutableList[Seq[String]]()
    val pattern = regex.r
    val source = Source.fromFile(filePath)
    try {
      source.getLines().foreach { line =>
        val matched = pattern.findFirstMatchIn(line)
        if(matched.isDefined) {
          val count = matched.get.groupCount
          val matchedLine = mutable.MutableList[String]()

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

  private def writeLine(line: String, buffWriter: BufferedWriter, markerReplacements: (String, String)*) {
    var changedLine: String = line
    markerReplacements.foreach(replacementMarker => changedLine = changedLine.replaceAllLiterally(replacementMarker._1, replacementMarker._2))
    buffWriter.write(changedLine, 0, changedLine.length)
  }
}
