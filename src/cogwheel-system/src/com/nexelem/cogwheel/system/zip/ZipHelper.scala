package com.nexelem.cogwheel.system.zip

import java.io._
import org.apache.commons.compress.archivers.zip.{ZipFile, ZipArchiveEntry, ZipArchiveOutputStream}
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.io.FileUtils

/**
 * Created by mzagorski on 28.03.14.
 */
object ZipHelper {
  private final val ARCHIVE_FILE_SEPARATOR: String = "/"


  def createZip(directoryPath: String, zipPath: String) {
    val dir: File = new File(directoryPath)
    val zipFile: File = new File(zipPath)
    if (!dir.isDirectory) {
      throw new IllegalArgumentException(directoryPath + " is not a valid directory")
    }
    var tOut: ZipArchiveOutputStream = null
    try {
      tOut = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))
      for (f <- dir.listFiles) {
        if (!(zipFile == f)) {
          addFileToZip(tOut, f, "")
        }
      }
    }
    finally {
      IOUtils.closeQuietly(tOut)
    }
  }


  def extractZip(archivePath: String, destinationPath: String) {
    val archiveFile: File = new File(archivePath)
    if(!archiveFile.exists || !archiveFile.isFile) {
      throw new IllegalArgumentException(archiveFile.getAbsolutePath + " is not a valid file")
    }
    var zipFile: ZipFile = null
    try {
      zipFile = new ZipFile(archiveFile)
      val entries: java.util.Enumeration[ZipArchiveEntry] = zipFile.getEntries
      while (entries.hasMoreElements) {
        val entry: ZipArchiveEntry = entries.nextElement
        extractEntry(zipFile, entry, destinationPath)
      }
    }
    catch {
      case e: Exception => {
        e.printStackTrace
      }
    } finally {
      IOUtils.closeQuietly(zipFile)
    }
  }


  private def addFileToZip(zOut: ZipArchiveOutputStream, f: File, base: String) {
    val entryName: String = base + f.getName
    val zipEntry: ZipArchiveEntry = new ZipArchiveEntry(f, entryName)
    zOut.putArchiveEntry(zipEntry)
    if (f.isFile) {
      writeFileContent(zOut, f)
    }
    zOut.closeArchiveEntry
    val children: Array[File] = f.listFiles
    if (children != null) {
      for (child <- children) {
        addFileToZip(zOut, child, entryName + ARCHIVE_FILE_SEPARATOR)
      }
    }
  }


  private def writeFileContent(zOut: ZipArchiveOutputStream, f: File) {
    var fInputStream: FileInputStream = null
    try {
      fInputStream = new FileInputStream(f)
      IOUtils.copy(fInputStream, zOut)
    }
    finally {
      IOUtils.closeQuietly(fInputStream)
    }
  }


  private def extractEntry(zipFile: ZipFile, entry: ZipArchiveEntry, destPath: String) {
    val f: File = new File(destPath, entry.getName.replace(ARCHIVE_FILE_SEPARATOR, File.separator))
    if (entry.isDirectory) {
      FileUtils.forceMkdir(f)
      return
    }
    if (f.exists && !entry.isDirectory) {
      FileUtils.forceDelete(f)
    }
    var fos: FileOutputStream = null
    val content: InputStream = zipFile.getInputStream(entry)
    try {
      fos = new FileOutputStream(f)
      IOUtils.copy(content, fos)
    }
    finally {
      IOUtils.closeQuietly(content)
      IOUtils.closeQuietly(fos)
    }
  }

}
