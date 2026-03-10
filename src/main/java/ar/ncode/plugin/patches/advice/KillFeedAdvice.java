package ar.ncode.plugin.patches.advice;

import net.bytebuddy.asm.Advice;

/**
 * Suppresses "Store is currently processing!" exceptions thrown by
 * DeathSystems$KillFeed.onComponentAdded when death occurs outside normal game flow.
 * Uses only JDK classes to avoid classloader visibility issues when inlined into server bytecode.
 */
public class KillFeedAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Thrown(readOnly = false) Throwable thrown) {
        if (thrown instanceof IllegalStateException) {
            String msg = thrown.getMessage();
            if (msg != null && msg.contains("Store is currently processing")) {
                System.out.println("[KillFeedAdvice] Suppressed Store processing exception in KillFeed");
                thrown = null;
            }
        }
    }
}
