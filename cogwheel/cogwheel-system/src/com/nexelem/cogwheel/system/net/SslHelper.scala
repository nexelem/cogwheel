package com.nexelem.cogwheel.system.net

import java.io.{File, FileInputStream, FileOutputStream}
import java.net.URL
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl._

import org.apache.commons.io.FileUtils
import org.apache.commons.logging.LogFactory
import resource._

import scala.runtime.StopException

/**
 * Helper object for dealing with SSL certificates.
 */
object SslHelper {
  private val log = LogFactory.getLog(getClass)

  def pullCertificateToTruststore(address: String, trustStorePath: String, trustStorePass: Option[String] = None, certName: String) {
    val pass = trustStorePass.getOrElse("changeit")

    val store = managed(new FileInputStream(trustStorePath)).acquireAndGet { stream =>
      val store = KeyStore.getInstance(KeyStore.getDefaultType)
      store.load(stream, pass.toCharArray)
      store
    }

    val context = SSLContext.getInstance("TLS")
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(store)
    val defaultTrustManager = trustManagerFactory.getTrustManagers()(0).asInstanceOf[X509TrustManager]
    val savingTrustManager = new SavingTrustManager(defaultTrustManager)
    context.init(null, Array[TrustManager](savingTrustManager), null)
    val factory = context.getSocketFactory()

    HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier {
      override def verify(s: String, sslSession: SSLSession): Boolean = true
    })

    log.info(s"Connecting to: ${address} in order to grab certificates...")

    val connection = new URL(address).openConnection().asInstanceOf[HttpsURLConnection]
    try {
      connection.setSSLSocketFactory(factory)
      connection.getResponseCode
    } catch {
      case e: SSLException => // nop, everything's fine
        log.debug(s"Expected exception - certificate is not yet in truststore...")
    }

    storeCertificates(store, trustStorePath, pass, certName, savingTrustManager.chain)
  }

  def storeCertificates(store: KeyStore, trustStorePath: String, pass: String, certName: String, certs: Array[X509Certificate]) {
    0.until(certs.length).foreach { num =>
      val cert = certs(num)
      log.info(s"Certificate subject DN: ${cert.getSubjectDN}")
      store.setCertificateEntry(certName + "_" + num, cert)
    }

    managed(new FileOutputStream(trustStorePath)).acquireAndGet { stream =>
      store.store(stream, pass.toCharArray)
    }
  }

  class SavingTrustManager(trustManager: X509TrustManager) extends X509TrustManager {
    var chain: Array[X509Certificate] = null

    def getAcceptedIssuers: Array[X509Certificate] = throw new UnsupportedOperationException()

    def checkClientTrusted(chain: Array[X509Certificate], authType: String) = throw new UnsupportedOperationException()

    def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
      this.chain = chain
      throw new StopException
    }
  }
}