package com.nexelem.cogwheel.system.process

import scala.sys.process._
import scala.language.postfixOps
import scala.collection.mutable.MutableList
import java.util.Properties

/**
 * Project: cogwheel
 * User: pdolega
 * Date: 3/24/14
 * Time: 1:54 PM
 * Version: 1.0
 * Utility object for operating on/with processes.
 */
object ProcessHelper {

  /**
   * Executes simple bash command.
   */
  def bash(cmd: String) {
    val returnCode = Seq("bash", "-c", cmd) !;
    if (returnCode != 0)
      throw new ProcessException(returnCode)
  }

  /**
   * Executes bash command and returns output.
   */
  def bashOutput(cmd: String): String = {
    val outputList = new MutableList[String]
    val outputLogger = ProcessLogger(outputLine => outputList += outputLine)

    val returnCode = Seq("bash", "-c", cmd) ! outputLogger;
    if (returnCode != 0)
      throw new ProcessException(returnCode)

    return outputList.mkString("\n")
  }

  /**
   * Executes bash command asynchronously with given environmental variables.
   */
  def bashAsync(cmd: String, extraEnv: (String, String)*) {
    val workingDir = None

    val process = Process(Seq("bash", "-c", cmd), workingDir, extraEnv: _*)
    val io = new ProcessIO(stdin => (),
      stdout => scala.io.Source.fromInputStream(stdout).getLines.foreach(println),
      stderr => ())
    process.run(io)
  }

  /**
   * Gets value from properties or retrieves default if value does not exist
   */
  def getOrDefault(props: Properties, key: String, defaultVal: String) = {
    val propVal = props.getProperty(key)
    if (propVal != null) propVal else defaultVal
  }
}

class ProcessException(val returnCode: Int) extends RuntimeException