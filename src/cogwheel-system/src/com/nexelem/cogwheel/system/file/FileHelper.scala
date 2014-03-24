package com.nexelem.cogwheel.system.file

import java.io.{FileWriter, BufferedWriter, File}
import scala.io.Source
import scala.util.Properties

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
   * @param filePath path to the file.
   * @param markerReplacements replacement values
   */
  def replaceValuesInFile(filePath: String, markerReplacements: (String, String)*) {

    var buffWriter: BufferedWriter = null
    var fileWriter: FileWriter = null

    val newFilename = File.createTempFile("temp", filePath.substring(filePath.lastIndexOf(".")), new File(filePath).getParentFile()).getAbsolutePath()

    try {
      fileWriter = new FileWriter(newFilename)
      buffWriter = new BufferedWriter(fileWriter)

      val lines = Source.fromFile(filePath).getLines.toTraversable
      val linesExceptLast = lines.slice(0, lines.size - 1)

      linesExceptLast.foreach(line => {
        writeLine(line, buffWriter, markerReplacements:_*)
        buffWriter.newLine()
      })
      writeLine(lines.last, buffWriter, markerReplacements:_*)

    } finally {
      if (buffWriter != null) buffWriter.close()
    }

    new File(filePath).delete()
    new File(newFilename).renameTo(new File(filePath))
  }

  private def writeLine(line: String, buffWriter: BufferedWriter, markerReplacements: (String, String)*) {
    var changedLine: String = line
    markerReplacements.foreach(replacementMarker => changedLine = changedLine.replaceAllLiterally(replacementMarker._1, replacementMarker._2))
    buffWriter.write(changedLine, 0, changedLine.length)
  }
}
