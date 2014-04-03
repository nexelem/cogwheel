package com.nexelem.cogwheel.system.file

import java.io.{FileWriter, BufferedWriter, File}
import scala.io.Source
import scala.util.Properties
import com.nexelem.cogwheel.system.process.ProcessHelper._
import com.nexelem.cogwheel.system.zip.ZipHelper
import org.apache.commons.io.FileUtils

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
    val tempDir  = File.createTempFile("temp", null).getAbsolutePath()
    val fileName = new File(filePath).getName()
    FileUtils.forceDelete(new File(tempDir))
    FileUtils.forceMkdir(new File(tempDir))
    ZipHelper.extractZip(filePath, tempDir)
    operation(tempDir)
    ZipHelper.createZip(tempDir, tempDir + File.separator + fileName)
    FileUtils.copyFile(new File(tempDir, fileName), new File(targetPath))
    FileUtils.deleteDirectory(new File(tempDir))
  }

  private def writeLine(line: String, buffWriter: BufferedWriter, markerReplacements: (String, String)*) {
    var changedLine: String = line
    markerReplacements.foreach(replacementMarker => changedLine = changedLine.replaceAllLiterally(replacementMarker._1, replacementMarker._2))
    buffWriter.write(changedLine, 0, changedLine.length)
  }
}
