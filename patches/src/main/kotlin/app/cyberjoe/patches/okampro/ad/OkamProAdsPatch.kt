package app.cyberjoe.patches.okampro.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

// Fingerprint que busca el manejador de Flutter que abre la pantalla del anuncio
private object SplashMethodCallFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Lio/flutter/plugin/common/MethodCall;",
        "Lio/flutter/plugin/common/MethodChannel\$Result;"
    ),
    strings = listOf("jumpToActivity")
)

@Suppress("unused")
val removeOkamProAdsPatch = bytecodePatch(
    name = "Remove O-KAM Pro Ads",
    description = "Blocks SplashAdActivity and prevents the Flutter engine from launching the splash ad.",
    default = true
) {
    // IMPORTANTE: Cambiamos el paquete al nuevo
    compatibleWith("com.okampro.oksmart")

    execute {
        // Interceptamos el MethodChannel de Flutter para evitar que se abra SplashAdActivity
        SplashMethodCallFingerprint.method.addInstructions(
            0,
            """
                # Obtener nombre del método de Flutter
                iget-object v0, p1, Lio/flutter/plugin/common/MethodCall;->method:Ljava/lang/String;
                
                # Comprobar si es "jumpToActivity"
                const-string v1, "jumpToActivity"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :allow
                
                # Si es jumpToActivity, lo bloqueamos
                const-string v0, "Morphe-AdDebug"
                const-string v1, "-> Blocked Splash Ad (jumpToActivity)"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                
                # Devolvemos true para simular éxito y evitar el Intent
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                return-void
                
                :allow
            """.trimIndent()
        )
    }
}
