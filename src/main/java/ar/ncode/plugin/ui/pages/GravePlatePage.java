package ar.ncode.plugin.ui.pages;

import ar.ncode.plugin.component.GraveStoneWithNameplate;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.model.TranslationKey;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class GravePlatePage extends InteractiveCustomUIPage<GravePlatePage.InteractionEvent> {

	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private final GraveStoneWithNameplate graveStoneWithNameplate;

	public GravePlatePage(@NonNullDecl PlayerRef playerRef, @NonNullDecl CustomPageLifetime lifetime,
	                      GraveStoneWithNameplate graveStoneWithNameplate) {
		super(playerRef, lifetime, InteractionEvent.CODEC);
		this.graveStoneWithNameplate = graveStoneWithNameplate;
	}


	@Override
	public void build(@NonNullDecl Ref<EntityStore> reference, @NonNullDecl UICommandBuilder builder,
	                  @NonNullDecl UIEventBuilder eventBuilder, @NonNullDecl Store<EntityStore> store
	) {
		builder.append("Pages/Grave/grave-plate.ui");
		builder.set("#TitleText.Text", Message.translation(TranslationKey.GRAVESTONE_PLATE_TITLE.get()));
		builder.set("#ReportDeath.Text", Message.translation(TranslationKey.GRAVESTONE_PLATE_REPORT_DEATH.get()));
		builder.set("#CloseBtn.Text", Message.translation(TranslationKey.GRAVESTONE_PLATE_CLOSE.get()));

		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ReportDeath", EventData.of("Action",
				"reportDeath"));
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseBtn", EventData.of("Action", "close"));

		if (graveStoneWithNameplate == null) {
			return;
		}

		builder.set("#Player.Text", Message.translation(TranslationKey.GRAVESTONE_PLATE_PLAYER.get()));
		builder.set("#PlayerValue.Text", graveStoneWithNameplate.getDeadPlayerName());

		if (graveStoneWithNameplate.getDeadPlayerRole() != null) {
			builder.set("#Role.Text", Message.translation(TranslationKey.GRAVESTONE_PLATE_ROLE.get()));
			builder.set("#RoleValue.Text", graveStoneWithNameplate.getDeadPlayerRole().getTranslationKey());
		}

		if (graveStoneWithNameplate.getTimeOfDeath() != null) {
			builder.set("#DeathTime.Text", Message.translation(TranslationKey.GRAVESTONE_PLATE_DEATH_TIME.get()));
			builder.set("#DeathTimeValue.Text", graveStoneWithNameplate.getTimeOfDeath());
		}

		if (graveStoneWithNameplate.getCauseOfDeath() != null) {
			builder.set("#DeathCause.Text", Message.translation(TranslationKey.GRAVESTONE_PLATE_DEATH_CAUSE.get()));
			String translationKey = graveStoneWithNameplate.getCauseOfDeath().getTranslationKey();
			builder.set("#DeathCauseValue.Text", Message.translation(translationKey));
		}
	}

	@Override
	public void handleDataEvent(
			Ref<EntityStore> reference, Store<EntityStore> store, GravePlatePage.InteractionEvent event
	) {
		if (event == null) {
			close();
			return;
		}

		if ("reportDeath".equals(event.action)) {
			Ref<EntityStore> deadPlayerReference = graveStoneWithNameplate.getDeadPlayerReference();

			if (deadPlayerReference == null) {
				LOGGER.atSevere().log("No se pudo confirmar un cadaver");
				close();
				return;
			}

			store.ensureComponent(deadPlayerReference, ConfirmedDeath.componentType);
		}

		close();
	}

	public static class InteractionEvent {

		public static final BuilderCodec<InteractionEvent> CODEC =
				BuilderCodec.builder(InteractionEvent.class, InteractionEvent::new)
						.append(new KeyedCodec<>("Action", Codec.STRING),
								(d, v) -> d.action = v, d -> d.action)
						.add()
						.build();

		public String action;
	}
}
