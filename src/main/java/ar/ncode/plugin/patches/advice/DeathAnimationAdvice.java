package ar.ncode.plugin.patches.advice;

import net.bytebuddy.asm.Advice;

/**
 * Suppresses "Store is currently processing!" exceptions thrown by
 * DeathSystems$DeathAnimation.onComponentAdded when death occurs outside normal game flow.
 * Uses only JDK classes to avoid classloader visibility issues when inlined into server bytecode.
 */
public class DeathAnimationAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Thrown(readOnly = false) Throwable thrown) {
        if (thrown instanceof IllegalStateException) {
            String msg = thrown.getMessage();
            if (msg != null && msg.contains("Store is currently processing")) {
                System.out.println("[DeathAnimationAdvice] Suppressed Store processing exception in DeathAnimation");
                thrown = null;
            }
        }
    }
}
