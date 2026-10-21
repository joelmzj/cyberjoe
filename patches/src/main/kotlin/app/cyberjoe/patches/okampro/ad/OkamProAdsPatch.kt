package app.cyberjoe.patches.okampro.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

// Fingerprint que localiza el método onMethodCall de MainActivity$b
private object SplashMethodCallFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("kakaopay============url==>")
)

@Suppress("unused")
val removeOkamProAdsPatch = bytecodePatch(
    name = "Remove O-KAM Pro Ads",
    description = "Blocks SplashAdActivity and prevents the Flutter engine from launching the splash ad.",
    default = true
) {
    compatibleWith("com.okampro.oksmart")

    execute {
        SplashMethodCallFingerprint.method.addInstructions(
            0,
            """
                # Retorno inmediato para evitar cualquier referencia a MethodChannel.Result
                return-void
            """.trimIndent()
        )
    }
}
