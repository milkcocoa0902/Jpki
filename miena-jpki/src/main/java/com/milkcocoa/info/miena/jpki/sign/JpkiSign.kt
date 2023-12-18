package com.milkcocoa.info.miena.jpki.sign

import android.nfc.Tag
import android.util.Log
import com.milkcocoa.info.miena.adpu.Adpu
import com.milkcocoa.info.miena.adpu.CommandAdpu
import com.milkcocoa.info.miena.exception.NoVerifyCountRemainsException
import com.milkcocoa.info.miena.jpki.Jpki
import com.milkcocoa.info.miena.pin.StrPin
import com.milkcocoa.info.miena.util.Extension.toByteArray
import com.milkcocoa.info.miena.util.IsoDepUtil.critical
import com.milkcocoa.info.miena.util.IsoDepUtil.isoDep
import com.milkcocoa.info.miena.util.Sha1
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.experimental.and

/**
 * JpkiSign
 * @author keita
 * @since 2023/12/17 15:26
 */

/**
 *
 */
class JpkiSign: Jpki<StrPin>() {
    override fun selectCertificatePublicKey(tag: Tag) {
        tag.isoDep().critical {isoDep->
            val selectFileAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x01.toByte())
            )
            Adpu(isoDep).transceive(selectFileAdpu)
        }
    }

    override fun readCertificatePublicKey(tag: Tag): X509Certificate {
        return tag.isoDep().critical {isoDep->
            val adpu = Adpu(isoDep)

            val preloadAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = byteArrayOf(0x04)
            )
            val preloadResponse = adpu.transceive(preloadAdpu)
            val preloadAsn1 = preloadResponse.asn1FrameIterator().next().frameSize

            val readCertificateAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xB0.toByte(),
                P1 = 0x00,
                P2 = 0x00,
                Le = preloadAsn1.toByteArray()
            )
            val certificateByteArray = adpu.transceive(readCertificateAdpu)

            // X.509形式に変換
            CertificateFactory.getInstance("X.509").generateCertificate(
                ByteArrayInputStream(certificateByteArray.data)
            ) as X509Certificate
        }
    }

    override fun computeSignature(tag: Tag, data: ByteArray): ByteArray {
        return tag.isoDep(timeout = 5000).critical {isoDep->

            val digestInfo = Sha1(data = data).digestInfo()

            val computeDigitalSignature = CommandAdpu(
                CLA = 0x80.toByte(),
                INS = 0x2A,
                P1 = 0x00,
                P2 = 0x80.toByte(),
                Lc = byteArrayOf(digestInfo.size.toByte()),
                DF = digestInfo,
                Le = byteArrayOf(0x00)
            )
            val response = Adpu(isoDep).transceive(computeDigitalSignature)

            return@critical response.data
        }
    }

    override fun selectCertificatePin(tag: Tag) {
        tag.isoDep().critical { isoDep ->
            val adpu = Adpu(isoDep)
            val selectFile = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x1B)
            )
            adpu.transceive(selectFile)
        }
    }

    override fun selectCertificatePrivateKey(tag: Tag) {
        tag.isoDep().critical {isoDep->
            val selectFileAdpu = CommandAdpu(
                CLA = 0x00,
                INS = 0xA4.toByte(),
                P1 = 0x02,
                P2 = 0x0C,
                Lc = byteArrayOf(0x02),
                DF = byteArrayOf(0x00, 0x1A)
            )
            Adpu(isoDep).transceive(selectFileAdpu)
        }
    }
}