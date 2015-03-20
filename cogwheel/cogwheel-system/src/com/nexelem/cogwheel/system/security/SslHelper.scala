package com.nexelem.cogwheel.system.security

import java.io.FileInputStream
import java.net.URL
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl._

import resource._

/**
 * Helper object for dealing with SSL certificates.
 */
object SslHelper {
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

    val connection = new URL(address).openConnection().asInstanceOf[HttpsURLConnection]
    connection.setSSLSocketFactory(factory)

    val certs = connection.getServerCertificates
    certs.foreach { cert =>
      println(cert.getType)
      println(cert.getPublicKey)
      println(cert.getType)
    }
  }
}

class SavingTrustManager(trustManager: X509TrustManager) extends X509TrustManager {

  var chain: Array[X509Certificate] = null

  def getAcceptedIssuers: Array[X509Certificate] = throw new UnsupportedOperationException()

  def checkClientTrusted(chain: Array[X509Certificate], authType: String) = throw new UnsupportedOperationException()

  def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
    this.chain = chain
    trustManager.checkServerTrusted(chain, authType)
  }
}