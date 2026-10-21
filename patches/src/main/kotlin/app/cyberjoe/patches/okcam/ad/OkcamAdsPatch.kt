package app.cyberjoe.patches.okcam.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.methodFingerprint

@Suppress("unused")
val removeOkcamAdsPatch = bytecodePatch(
    name = "Remove O-KAM Ads",
    description = "Test: Blocks AdScope SDK initialization and intercepts Flutter MethodChannel to prevent Interstitial, Native, and Splash ads.",
    default = true
) {
    compatibleWith("com.okcam.ookcam")

    // 1. Fingerprint para interceptar la inicialización del SDK (AdScope)
    val ampsInitFingerprint = methodFingerprint(
        returnType = "V",
        parameters = listOf(
            "Landroid/content/Context;",
            "Lxyz/adscope/amps/init/AMPSInitConfig;",
            "Lxyz/adscope/amps/init/inter/IAMPSInitCallback;"
        )
    )

    // 2. Fingerprint para interceptar las llamadas de Flutter (MainActivity$e)
    val onMethodCallFingerprint = methodFingerprint(
        returnType = "V",
        parameters = listOf(
            "Lio/flutter/plugin/common/MethodCall;",
            "Lio/flutter/plugin/common/MethodChannel\$Result;"
        ),
        strings = listOf("interstitial", "ToAndroidView", "jumpToActivity")
    )

    execute {
        // FASE 1: Neutralizar la inicialización del SDK (Fake success)
        val initMethod = ampsInitFingerprint.result?.method
        if (initMethod != null) {
            initMethod.addInstructions(
                0,
                """
                    const-string v0, "Morphe-AdDebug"
                    const-string v1, "Blocked AdScope SDK initialization"
                    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    
                    invoke-interface {p2}, Lxyz/adscope/amps/init/inter/IAMPSInitCallback;->successCallback()V
                    return-void
                """.trimIndent()
            )
        }

        // FASE 2: Interceptar los anuncios en el puente de Flutter
        val onMethodCallMethod = onMethodCallFingerprint.result?.method
        if (onMethodCallMethod != null) {
            onMethodCallMethod.addInstructions(
                0,
                """
                    # Obtener nombre del método de Flutter
                    iget-object v0, p1, Lio/flutter/plugin/common/MethodCall;->method:Ljava/lang/String;
                    
                    # LOG 1: Imprimir en Logcat qué está pidiendo Flutter
                    const-string v1, "Morphe-AdDebug"
                    new-instance v2, Ljava/lang/StringBuilder;
                    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
                    const-string v3, "Flutter invoked: "
                    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                    move-result-object v2
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                    
                    # Comparaciones
                    const-string v1, "interstitial"
                    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v1
                    if-eqz v1, :fake_success
                    
                    const-string v1, "jumpToActivity"
                    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v1
                    if-eqz v1, :fake_success
                    
                    const-string v1, "ToAndroidView"
                    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v1
                    if-eqz v1, :fake_native
                    
                    const-string v1, "androidViewHeight"
                    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v1
                    if-eqz v1, :fake_native
                    
                    goto :allow
                    
                    :fake_native
                    const-string v1, "Morphe-AdDebug"
                    const-string v2, "-> Blocked Native Ad (Height 0)"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

                    const-wide/16 v0, 0x0
                    invoke-static {v0, v1}, Ljava/lang/Double;->valueOf(D)Ljava/lang/Double;
                    move-result-object v0
                    invoke-interface {p2, v0}, Lio/flutter/plugin/common/MethodChannel${'$'}Result;->success(Ljava/lang/Object;)V
                    return-void
                    
                    :fake_success
                    const-string v1, "Morphe-AdDebug"
                    const-string v2, "-> Blocked Interstitial/Splash Ad"
                    invoke-static {v1, v2}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

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
}
