package ar.ncode.plugin.ui.pages;

import ar.ncode.plugin.config.instance.InstanceConfigWrapper;
import ar.ncode.plugin.ecs.component.PlayerGameModeInfo;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ar.ncode.plugin.TroubleInTrorkTownPlugin.config;
import static ar.ncode.plugin.TroubleInTrorkTownPlugin.gameModeStateForWorld;
import static ar.ncode.plugin.model.enums.TranslationKey.*;

public class MapVotePage extends InteractiveCustomUIPage<MapVotePage.MapVoteInteractionEvent> {

    private final List<InstanceConfigWrapper> maps;
    private final PlayerGameModeInfo playerInfo;

    public MapVotePage(@NonNullDecl PlayerRef playerRef, @NonNullDecl CustomPageLifetime lifetime, Collection<InstanceConfigWrapper> maps, PlayerGameModeInfo playerInfo) {
        super(playerRef, lifetime, MapVotePage.MapVoteInteractionEvent.CODEC);
        this.maps = new ArrayList<>(maps);
        this.playerInfo = playerInfo;
    }

    @Override
    public void build(
            @NonNullDecl Ref<EntityStore> reference, @NonNullDecl UICommandBuilder builder,
            @NonNullDecl UIEventBuilder eventBuilder, @NonNullDecl Store<EntityStore> store
    ) {
        builder.append("Pages/MapVote/map-vote.ui");
        builder.set("#TitleLabel.Text", Message.translation(MAP_VOTE_TITLE.get()));

        int rowSize = config.get().getMapsInARowForVoting();
        int rows = maps.size() / rowSize;
        if (maps.size() % rowSize > 0) {
            rows++;
        }

        builder.appendInline("#Rows", "Group #Row {}");

        for (int row = 0; row < rows; row++) {
            builder.appendInline("#Rows", "Group #Row { LayoutMode: Left; }");
            String rowSelector = "#Rows[" + (row + 1) + "]";
            builder.appendInline(rowSelector, "Group #Entry {}");

            for (int i = 0; i < rowSize && (i + row * rowSize) < maps.size(); i++) {
                int entryIndex = i + row * rowSize;
                InstanceConfigWrapper entry = maps.get(entryIndex);

                builder.append(rowSelector, "Pages/MapVote/entry.ui");
                String itemPrefix = rowSelector + "[" + (i + 1) + "] ";

                String mapPreview = "UI/Custom/Images/Worlds/" + entry.getFolderName() + ".png";
                builder.set(itemPrefix + "#EntryIcon.AssetPath", mapPreview);
                builder.set(itemPrefix + "#EntryName.Text", entry.getDisplayName());

                builder.set(itemPrefix + "#EntryButton.Text", Message.translation(MAP_VOTE_VOTE_FOR_MAP.get()));
                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        itemPrefix + "#EntryButton",
                        EventData.of("MapName", entry.getSafeName())
                );
            }

        }

    }

    @Override
    public void handleDataEvent(
            Ref<EntityStore> reference, Store<EntityStore> store, MapVoteInteractionEvent event
    ) {
        if (event == null || event.mapName == null) {
            close();
            return;
        }

        if (playerInfo.hasAlreadyVotedMap()) {
            NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.translation(MAP_VOTE_YOU_HAVE_ALREADY_VOTED.get()),
                    NotificationStyle.Danger
            );
            sendUpdate();
        }

        var gameModeState = gameModeStateForWorld.get(super.playerRef.getWorldUuid());
        if (gameModeState != null) {
            gameModeState.addVoteForMap(event.mapName);
        }
        close();
    }

    public static class MapVoteInteractionEvent {

        public static final BuilderCodec<MapVoteInteractionEvent> CODEC =
                BuilderCodec.builder(MapVoteInteractionEvent.class, MapVoteInteractionEvent::new)
                        .append(new KeyedCodec<>("MapName", Codec.STRING),
                                (d, v) -> d.mapName = v, d -> d.mapName)
                        .add()
                        .build();

        public String mapName;
    }

}
