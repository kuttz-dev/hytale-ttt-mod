package ar.ncode.plugin.patches;

import ar.ncode.plugin.patches.advice.ExplosionUtilsAdvice;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.ExplosionUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;

import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class CancelExplosionInteraction {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public void apply() {
        try {
            new ByteBuddy()
                    .redefine(ExplosionUtils.class)
                    .visit(Advice.to(ExplosionUtilsAdvice.class)
                            .on(named("performExplosion")
                            .and(isStatic()))
                    ).make()
                    .load(
                            ExplosionUtils.class.getClassLoader(),
                            ClassReloadingStrategy.fromInstalledAgent()
                    );

            LOGGER.atInfo().log("Explosion block-damage patch applied.");

        } catch (Throwable throwable) {
            LOGGER.atWarning().log("Could not apply explosion patch: %s", throwable.getMessage());
        }

}
}
