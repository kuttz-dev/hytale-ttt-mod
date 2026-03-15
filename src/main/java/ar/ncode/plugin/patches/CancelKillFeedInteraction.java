package ar.ncode.plugin.patches;

import ar.ncode.plugin.patches.advice.KillFeedAdvice;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class CancelKillFeedInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public void apply() {
        try {
            Instrumentation instrumentation = ByteBuddyAgent.getInstrumentation();

            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(new AgentBuilder.Listener.Adapter() {
                        @Override
                        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
                                                     net.bytebuddy.utility.JavaModule module, boolean loaded,
                                                     net.bytebuddy.dynamic.DynamicType dynamicType) {
                            LOGGER.atInfo().log("Kill-feed patch transformed: %s (loaded=%s)", typeDescription.getName(), loaded);
                        }

                        @Override
                        public void onError(String typeName, ClassLoader classLoader,
                                            net.bytebuddy.utility.JavaModule module, boolean loaded,
                                            Throwable throwable) {
                            LOGGER.atWarning().log("Kill-feed patch FAILED for: %s - %s", typeName, throwable.getMessage());
                        }
                    })
                    .type(is(DeathSystems.KillFeed.class))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                            builder.visit(Advice.to(KillFeedAdvice.class).on(named("onComponentAdded"))))
                    .installOn(instrumentation);

            LOGGER.atInfo().log("Kill-feed safety patch applied.");

        } catch (Throwable throwable) {
            LOGGER.atWarning().log("Could not apply kill-feed patch: %s", throwable.getMessage());
        }
    }
}
