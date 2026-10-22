package app.cyberjoe.patches.okampro.ad

import app.morphe.patcher.patch.rawResourcePatch
import java.io.File

@Suppress("unused")
val okamProSignatureBypassPatch = rawResourcePatch(
    name = "Bypass O-KAM Pro Signature Check",
    description = "Patches libOKSMARTJIAMI.so to force the native checkSignature function to always return true.",
    default = true
) {
    compatibleWith("com.okampro.oksmart")

    execute {
        // Ruta de la librería nativa
        val libPath = "lib/arm64-v8a/libOKSMARTJIAMI.so"
        val libFile = context.get(libPath)
        
        if (!libFile.exists()) {
            return@execute
        }

        val bytes = libFile.readBytes()

        // 1. Patrón exacto extraído de Ghidra (sub sp, sp, #0x70; stp x29, x30...)
        val originalPattern = byteArrayOf(
            0xFF.toByte(), 0xC3.toByte(), 0x01.toByte(), 0xD1.toByte(),
            0xFD.toByte(), 0x7B.toByte(), 0x05.toByte(), 0xA9.toByte()
        )

        // 2. Instrucciones ARM64 para forzar el retorno a TRUE (1) y salir
        // mov w0, #1 (0x52800020 en little-endian es 20 00 80 52)
        // ret (0xD65F03C0 en little-endian es C0 03 5F D6)
        val bypassInstructions = byteArrayOf(
            0x20, 0x00, 0x80.toByte(), 0x52, 
            0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte()
        )

        val index = indexOf(bytes, originalPattern)
        if (index != -1) {
            for (i in bypassInstructions.indices) {
                bytes[index + i] = bypassInstructions[i]
            }
            libFile.writeBytes(bytes)
        }
    }
}

// Función auxiliar para buscar el patrón de bytes
fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
    if (needle.isEmpty() || haystack.size < needle.size) return -1
    for (i in 0..(haystack.size - needle.size)) {
        var found = true
        for (j in needle.indices) {
            if (haystack[i + j] != needle[j]) {
                found = false
                break
            }
        }
        if (found) return i
    }
    return -1
}
