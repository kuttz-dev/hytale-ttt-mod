package ar.ncode.plugin.patches;

import ar.ncode.plugin.patches.advice.PlayDeathAnimationAdvice;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class CancelPlayDeathAnimationInteraction {

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
                            LOGGER.atInfo().log("Play-death-animation patch transformed: %s (loaded=%s)", typeDescription.getName(), loaded);
                        }

                        @Override
                        public void onError(String typeName, ClassLoader classLoader,
                                            net.bytebuddy.utility.JavaModule module, boolean loaded,
                                            Throwable throwable) {
                            LOGGER.atWarning().log("Play-death-animation patch FAILED for: %s - %s", typeName, throwable.getMessage());
                        }
                    })
                    .type(is(DeathSystems.class))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                            builder.visit(Advice.to(PlayDeathAnimationAdvice.class)
                                    .on(named("playDeathAnimation")
                                            .and(isStatic())
                                            .and(takesArguments(5)))))
                    .installOn(instrumentation);

            LOGGER.atInfo().log("Play-death-animation safety patch applied.");

        } catch (Throwable throwable) {
            LOGGER.atWarning().log("Could not apply play-death-animation patch: %s", throwable.getMessage());
        }
    }
}
