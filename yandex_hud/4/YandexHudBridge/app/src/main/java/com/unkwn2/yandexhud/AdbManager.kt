package com.unkwn2.yandexhud

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class AdbManager private constructor() : AbsAdbConnectionManager() {
    private var mPrivateKey: PrivateKey? = null
    private var mCertificate: Certificate? = null

    companion object {
        @Volatile
        private var INSTANCE: AdbManager? = null

        fun getInstance(): AdbManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdbManager().also { INSTANCE = it }
            }
        }
    }

    init {
        setApi(Build.VERSION.SDK_INT)
        val loaded = tryLoadPersistedKeys()
        if (!loaded) {
            try {
                val kpg = KeyPairGenerator.getInstance("RSA")
                kpg.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
                val keyPair = kpg.generateKeyPair()
                mPrivateKey = keyPair.private
                mCertificate = generateCert(keyPair)
                persistKeys(keyPair.private, mCertificate!!)
                FileLogger.write("AdbMgr", "key pair generated + persisted")
            } catch (t: Throwable) {
                FileLogger.write("AdbMgr", "key gen ERR: ${t.message}")
            }
        } else {
            FileLogger.write("AdbMgr", "key pair loaded from persisted storage")
        }
    }

    private fun tryLoadPersistedKeys(): Boolean {
        return try {
            val dir = File("/data/data/com.unkwn2.yandexhud/files")
            val privFile = File(dir, "adb_key.priv")
            val certFile = File(dir, "adb_key.cert")
            if (!privFile.exists() || !certFile.exists()) return false
            val privBytes = Base64.getDecoder().decode(privFile.readText().trim())
            val certBytes = Base64.getDecoder().decode(certFile.readText().trim())
            val kf = KeyFactory.getInstance("RSA")
            mPrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
            mCertificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(certBytes.inputStream()) as X509Certificate
            true
        } catch (t: Throwable) {
            FileLogger.write("AdbMgr", "load persisted keys ERR: ${t.message}")
            false
        }
    }

    private fun persistKeys(priv: PrivateKey, cert: Certificate) {
        try {
            val dir = File("/data/data/com.unkwn2.yandexhud/files")
            if (!dir.exists()) dir.mkdirs()
            File(dir, "adb_key.priv").writeText(Base64.getEncoder().encodeToString(priv.encoded))
            File(dir, "adb_key.cert").writeText(Base64.getEncoder().encodeToString(cert.encoded))
            FileLogger.write("AdbMgr", "keys persisted OK")
        } catch (t: Throwable) {
            FileLogger.write("AdbMgr", "persist keys ERR: ${t.message}")
        }
    }

    override fun getPrivateKey(): PrivateKey = mPrivateKey!!
    override fun getCertificate(): Certificate = mCertificate!!
    override fun getDeviceName(): String = "YandexHudBridge"

    @SuppressLint("PrivateApi")
    private fun generateCert(keyPair: KeyPair): X509Certificate {
        val x500Cls = Class.forName("sun.security.x509.X500Name")
        val x500 = x500Cls.getConstructor(String::class.java).newInstance("CN=YandexHudBridge")
        val validCls = Class.forName("sun.security.x509.CertificateValidity")
        val now = java.util.Date()
        val later = java.util.Date(System.currentTimeMillis() + 365L * 86400000)
        val valid = validCls.getConstructor(java.util.Date::class.java, java.util.Date::class.java).newInstance(now, later)
        val serialCls = Class.forName("sun.security.x509.CertificateSerialNumber")
        val serial = serialCls.getConstructor(Int::class.javaPrimitiveType).newInstance(1)
        val algoIdCls = Class.forName("sun.security.x509.AlgorithmId")
        val algoId = algoIdCls.getDeclaredMethod("get", String::class.java).invoke(null, "SHA256withRSA")
        val certAlgoCls = Class.forName("sun.security.x509.CertificateAlgorithmId")
        val certAlgo = certAlgoCls.getConstructor(algoIdCls).newInstance(algoId)
        val certKeyCls = Class.forName("sun.security.x509.CertificateX509Key")
        val certKey = certKeyCls.getConstructor(java.security.PublicKey::class.java).newInstance(keyPair.public)
        val subjCls = Class.forName("sun.security.x509.CertificateSubjectName")
        val subj = subjCls.getConstructor(x500Cls).newInstance(x500)
        val issuerCls = Class.forName("sun.security.x509.CertificateIssuerName")
        val issuer = issuerCls.getConstructor(x500Cls).newInstance(x500)
        val verCls = Class.forName("sun.security.x509.CertificateVersion")
        val ver = verCls.getConstructor(Int::class.javaPrimitiveType).newInstance(2)
        val infoCls = Class.forName("sun.security.x509.X509CertInfo")
        val info = infoCls.getConstructor().newInstance()
        val attrCls = Class.forName("sun.security.x509.CertificateAttribute")
        val set = infoCls.getDeclaredMethod("set", String::class.java, attrCls)
        set.isAccessible = true
        set.invoke(info, "version", ver)
        set.invoke(info, "serialNumber", serial)
        set.invoke(info, "algorithmID", certAlgo)
        set.invoke(info, "subject", subj)
        set.invoke(info, "key", certKey)
        set.invoke(info, "validity", valid)
        set.invoke(info, "issuer", issuer)
        val certImplCls = Class.forName("sun.security.x509.X509CertImpl")
        val certImpl = certImplCls.getConstructor(infoCls).newInstance(info)
        val sign = certImplCls.getDeclaredMethod("sign", java.security.PrivateKey::class.java, String::class.java)
        sign.isAccessible = true
        sign.invoke(certImpl, keyPair.private, "SHA256withRSA")
        return certImpl as X509Certificate
    }
}
