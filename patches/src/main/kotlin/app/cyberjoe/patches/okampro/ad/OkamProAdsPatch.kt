package app.cyberjoe.patches.okampro.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

// Fingerprint usando el log único de la app para evitar problemas con R8
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
        // Inyectamos el código directamente en el método que contiene ese Log
        SplashMethodCallFingerprint.method.addInstructions(
            0,
            """
                const-string v0, "Morphe-AdDebug"
                const-string v1, "-> Intercepted MethodChannel Call"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                
                # Leer el nombre del método (f893a es el campo de MethodCall en R8)
                iget-object v0, p1, Lp028b8/C1564j;->f893a:Ljava/lang/String;
                const-string v1, "jumpToActivity"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v1
                if-eqz v1, :allow
                
                # Bloquear y devolver true (mo369a es el método success en R8)
                const-string v0, "Morphe-AdDebug"
                const-string v1, "-> Blocked Splash Ad (jumpToActivity)"
                invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
                
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                invoke-interface {p2, v0}, Lp028b8/C1565k${'$'}d;->mo369a(Ljava/lang/Object;)V
                return-void
                
                :allow
            """.trimIndent()
        )
    }
}
