package app.cyberjoe.patches.okampro.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

// Fingerprint que busca el método onMethodCall usando el log único de la app
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
                # Simulamos que la llamada se completó con éxito para que Flutter no se quede esperando
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                
                # Nos salimos del método sin ejecutar el código original (evitando la referencia a C1564j)
                return-void
            """.trimIndent()
        )
    }
}
