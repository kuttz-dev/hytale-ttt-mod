package ar.ncode.plugin.config;

import ar.ncode.plugin.model.TranslationKey;
import ar.ncode.plugin.model.enums.RoleGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public class CustomConfig {

	public static final BuilderCodec<CustomConfig> CODEC =
			BuilderCodec.builder(CustomConfig.class, CustomConfig::new)
					.append(new KeyedCodec<>("RequiredPlayersToStartRound", Codec.INTEGER),
							(config, v, _) -> config.requiredPlayersToStartRound = v,
							(config, _) -> config.requiredPlayersToStartRound)
					.add()
					.append(new KeyedCodec<>("TimeBeforeRoundInSeconds", Codec.INTEGER),
							(config, v, _) -> config.timeBeforeRoundInSeconds = v,
							(config, _) -> config.timeBeforeRoundInSeconds)
					.add()
					.append(new KeyedCodec<>("RoundDurationInSeconds", Codec.INTEGER),
							(config, v, _) -> config.roundDurationInSeconds = v,
							(config, _) -> config.roundDurationInSeconds)
					.add()
					.append(new KeyedCodec<>("TimeAfterRoundInSeconds", Codec.INTEGER),
							(config, v, _) -> config.timeAfterRoundInSeconds = v,
							(config, _) -> config.timeAfterRoundInSeconds)
					.add()
					.append(new KeyedCodec<>("TimeToVoteMapInSeconds", Codec.INTEGER),
							(config, v, _) -> config.timeToVoteMapInSeconds = v,
							(config, _) -> config.timeToVoteMapInSeconds)
					.add()
					.append(new KeyedCodec<>("TimeBeforeChangingMapInSeconds", Codec.INTEGER),
							(config, v, _) -> config.timeBeforeChangingMapInSeconds = v,
							(config, _) -> config.timeBeforeChangingMapInSeconds)
					.add()
					.append(new KeyedCodec<>("KarmaStartingValue", Codec.INTEGER),
							(config, v, _) -> config.karmaStartingValue = v,
							(config, _) -> config.karmaStartingValue)
					.add()
					.append(new KeyedCodec<>("KarmaForDisconnectingMiddleRound", Codec.INTEGER),
							(config, v, _) -> config.karmaForDisconnectingMiddleRound = v,
							(config, _) -> config.karmaForDisconnectingMiddleRound)
					.add()
					.append(new KeyedCodec<>("KaramPointsForKillingSameRoleGroup", Codec.INTEGER),
							(config, v, _) -> config.karamPointsForKillingSameRoleGroup = v,
							(config, _) -> config.karamPointsForKillingSameRoleGroup)
					.add()
					.append(new KeyedCodec<>("KaramPointsForKillingOppositeRoleGroup", Codec.INTEGER),
							(config, v, _) -> config.karamPointsForKillingOppositeRoleGroup = v,
							(config, _) -> config.karamPointsForKillingOppositeRoleGroup)
					.add()
					.append(new KeyedCodec<>("PlayerGraveId", Codec.STRING),
							(config, v, _) -> config.playerGraveId = v,
							(config, _) -> config.playerGraveId)
					.add()
					.append(new KeyedCodec<>("LootBoxBlockId", Codec.STRING),
							(config, v, _) -> config.lootBoxBlockId = v,
							(config, _) -> config.lootBoxBlockId)
					.add()
					.append(new KeyedCodec<>("RoundsPerMap", Codec.INTEGER),
							(config, v, _) -> config.roundsPerMap = v,
							(config, _) -> config.roundsPerMap)
					.add()
					.append(new KeyedCodec<>("MapsInARowForVoting", Codec.INTEGER),
							(config, v, _) -> config.mapsInARowForVoting = v,
							(config, _) -> config.mapsInARowForVoting)
					.add()
					.append(new KeyedCodec<>("WorldTemplatesFolder", Codec.STRING),
							(config, v, _) -> config.worldTemplatesFolder = v,
							(config, _) -> config.worldTemplatesFolder)
					.add()
					.append(new KeyedCodec<>("Roles", ArrayCodec.ofBuilderCodec(CustomRole.CODEC, CustomRole[]::new)),
							(config, v, _) -> config.roles = v,
							(config, _) -> config.roles)
					.add()
					.build();
	public static final CustomRole DETECTIVE_ROLE = CustomRole.builder()
			.id("detective")
			.translationKey(TranslationKey.getWithPrefix("hud_current_role_detective"))
			.customGuiColor("#1F5CC4")
			.minimumAssignedPlayersWithRole(0)
			.ratio(11)
			.startingCredits(1)
			.secretRole(false)
			.roleGroup(RoleGroup.INNOCENT)
			.storeItems(new String[]{
					"TTT_Potion_Veritaserum:30",
					"Weapon_Deployable_Healing_Totem:1"
			})
			.build();
	public static final CustomRole INNOCENT_ROLE = CustomRole.builder()
			.id("innocent")
			.translationKey(TranslationKey.getWithPrefix("hud_current_role_innocent"))
			.minimumAssignedPlayersWithRole(1)
			.ratio(1)
			.secretRole(true)
			.roleGroup(RoleGroup.INNOCENT)
			.build();
	public static final CustomRole TRAITOR_ROLE = CustomRole.builder()
			.id("traitor")
			.translationKey(TranslationKey.getWithPrefix("hud_current_role_traitor"))
			.minimumAssignedPlayersWithRole(1)
			.ratio(4)
			.startingCredits(1)
			.secretRole(true)
			.roleGroup(RoleGroup.TRAITOR)
			.storeItems(new String[]{
					"Weapon_Daggers_Doomed:1"
			})
			.build();

	private final int itemsInARowForTheShop = 5;
	// Sets required amount of players to start a round
	private int requiredPlayersToStartRound = 3;
	// Time in seconds before the round starts
	private int timeBeforeRoundInSeconds = 10;
	private int roundDurationInSeconds = 10 * 60;
	private int timeAfterRoundInSeconds = 5;
	private int timeToVoteMapInSeconds = 30;
	private int timeBeforeChangingMapInSeconds = 5;
	// Sets the starting value for each component's karma
	private int karmaStartingValue = 1000;
	private int karmaForDisconnectingMiddleRound = -100;
	private int karamPointsForKillingOppositeRoleGroup = 10;
	private int karamPointsForKillingSameRoleGroup = -10;

	private CustomRole[] roles = new CustomRole[]{
			DETECTIVE_ROLE,
			INNOCENT_ROLE,
			TRAITOR_ROLE
	};

	private String playerGraveId = "Player_Grave";
	private String lootBoxBlockId = "Furniture_Human_Ruins_Chest_Small";
	private int roundsPerMap = 8;
	private int mapsInARowForVoting = 3;
	private String worldTemplatesFolder = "universe/templates";

	public static List<ItemStack> parseItemEntry(String line) {
		if (line == null || line.isBlank()) {
			throw new IllegalArgumentException("Store entry cannot be null or blank");
		}

		List<ItemStack> items = new ArrayList<>();

		String[] itemTokens = line.split("\\|");
		for (String token : itemTokens) {
			if (token.isBlank()) continue;

			String[] parts = token.split(":", 2);
			String itemId = parts[0].trim();

			int amount = 1;
			if (parts.length == 2) {
				try {
					amount = Integer.parseInt(parts[1].trim());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(
							"Invalid amount in store entry: '" + token + "'"
					);
				}
			}

			items.add(new ItemStack(itemId, amount));
		}

		if (items.isEmpty()) {
			throw new IllegalArgumentException("Store entry produced no items: " + line);
		}

		return items;
	}

	public List<List<ItemStack>> getItemsGroups(String[] configuredValues) {
		List<List<ItemStack>> stacks = new ArrayList<>();

		if (configuredValues == null) {
			return stacks;
		}

		for (String configuredValue : configuredValues) {
			stacks.add(parseItemEntry(configuredValue));
		}

		return stacks;
	}


	public List<ItemStack> getItems(String[] configuredValues) {
		List<ItemStack> stacks = new ArrayList<>();

		if (configuredValues == null) {
			return stacks;
		}

		for (String configuredValue : configuredValues) {
			stacks.addAll(parseItemEntry(configuredValue));
		}

		return stacks;
	}

	public Optional<CustomRole> getRoleByName(String name) {
		if (name == null || "".equalsIgnoreCase(name.trim())) {
			return Optional.empty();
		}

		return Arrays.stream(this.roles).filter(r -> name.equalsIgnoreCase(r.getId())).findFirst();
	}

}
