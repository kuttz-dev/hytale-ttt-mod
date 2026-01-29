package ar.ncode.plugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CustomConfig {

	public static final BuilderCodec<CustomConfig> CODEC =
			BuilderCodec.builder(CustomConfig.class, CustomConfig::new)
					.append(new KeyedCodec<>("InnocentColor", Codec.STRING),
							(config, value, extraInfo) -> config.innocentColor = value,  // Setter
							(config, extraInfo) -> config.innocentColor)                     // Getter
					.add()
					.append(new KeyedCodec<>("TraitorColor", Codec.STRING),
							(config, value, extraInfo) -> config.traitorColor = value,
							(config, extraInfo) -> config.traitorColor)
					.add()
					.append(new KeyedCodec<>("RequiredPlayersToStartRound", Codec.INTEGER),
							(config, value, extraInfo) -> config.requiredPlayersToStartRound = value,
							(config, extraInfo) -> config.requiredPlayersToStartRound)
					.add()
					.append(new KeyedCodec<>("MinAmountOfTraitors", Codec.INTEGER),
							(config, value, extraInfo) -> config.minAmountOfTraitors = value,
							(config, extraInfo) -> config.minAmountOfTraitors)
					.add()
					.documentation("There will be 1 traitor for every N players, here you define N")
					.append(new KeyedCodec<>("TraitorsRatio", Codec.INTEGER),
							(config, value, extraInfo) -> config.traitorsRatio = value,
							(config, extraInfo) -> config.traitorsRatio)
					.add()
					.append(new KeyedCodec<>("MinAmountOfDetectives", Codec.INTEGER),
							(config, value, extraInfo) -> config.minAmountOfDetectives = value,
							(config, extraInfo) -> config.minAmountOfDetectives)
					.add()
					.documentation("There will be 1 detective for every N players, here you define N")
					.append(new KeyedCodec<>("DetectivesRatio", Codec.INTEGER),
							(config, value, extraInfo) -> config.detectivesRatio = value,
							(config, extraInfo) -> config.detectivesRatio)
					.add()
					.documentation("Lets you disable the detectives ratio after some point")
					.append(new KeyedCodec<>("MaxDetectives", Codec.INTEGER),
							(config, value, extraInfo) -> config.maxDetectives = value,
							(config, extraInfo) -> config.maxDetectives)
					.add()
					.append(new KeyedCodec<>("TimeBeforeRoundInSeconds", Codec.INTEGER),
							(config, value, extraInfo) -> config.timeBeforeRoundInSeconds = value,
							(config, extraInfo) -> config.timeBeforeRoundInSeconds)
					.add()
					.append(new KeyedCodec<>("RoundDurationInSeconds", Codec.INTEGER),
							(config, value, extraInfo) -> config.roundDurationInSeconds = value,
							(config, extraInfo) -> config.roundDurationInSeconds)
					.add()
					.append(new KeyedCodec<>("TimeAfterRoundInSeconds", Codec.INTEGER),
							(config, value, extraInfo) -> config.timeAfterRoundInSeconds = value,
							(config, extraInfo) -> config.timeAfterRoundInSeconds)
					.add()
					.append(new KeyedCodec<>("TimeToVoteMapInSeconds", Codec.INTEGER),
							(config, value, extraInfo) -> config.timeToVoteMapInSeconds = value,
							(config, extraInfo) -> config.timeToVoteMapInSeconds)
					.add()
					.append(new KeyedCodec<>("TimeBeforeChangingMapInSeconds", Codec.INTEGER),
							(config, value, extraInfo) -> config.timeBeforeChangingMapInSeconds = value,
							(config, extraInfo) -> config.timeBeforeChangingMapInSeconds)
					.add()
					.append(new KeyedCodec<>("KarmaStartingValue", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaStartingValue = value,
							(config, extraInfo) -> config.karmaStartingValue)
					.add()
					.append(new KeyedCodec<>("KarmaForDisconnectingMiddleRound", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaForDisconnectingMiddleRound = value,
							(config, extraInfo) -> config.karmaForDisconnectingMiddleRound)
					.add()
					.append(new KeyedCodec<>("KaramPointsForTraitorKillingInnocent", Codec.INTEGER),
							(config, value, extraInfo) -> config.karamPointsForTraitorKillingInnocent = value,
							(config, extraInfo) -> config.karamPointsForTraitorKillingInnocent)
					.add()
					.append(new KeyedCodec<>("KaramPointsForTraitorKillingDetective", Codec.INTEGER),
							(config, value, extraInfo) -> config.karamPointsForTraitorKillingDetective = value,
							(config, extraInfo) -> config.karamPointsForTraitorKillingDetective)
					.add()
					.append(new KeyedCodec<>("KaramPointsForTraitorKillingTraitor", Codec.INTEGER),
							(config, value, extraInfo) -> config.karamPointsForTraitorKillingTraitor = value,
							(config, extraInfo) -> config.karamPointsForTraitorKillingTraitor)
					.add()
					.append(new KeyedCodec<>("KarmaPointsForInnocentKillingTraitor", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaPointsForInnocentKillingTraitor = value,
							(config, extraInfo) -> config.karmaPointsForInnocentKillingTraitor)
					.add()
					.append(new KeyedCodec<>("KarmaPointsForInnocentKillingDetective", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaPointsForInnocentKillingDetective = value,
							(config, extraInfo) -> config.karmaPointsForInnocentKillingDetective)
					.add()
					.append(new KeyedCodec<>("KarmaPointsForInnocentKillingInnocent", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaPointsForInnocentKillingInnocent = value,
							(config, extraInfo) -> config.karmaPointsForInnocentKillingInnocent)
					.add()
					.append(new KeyedCodec<>("KarmaPointsForDetectiveKillingTraitor", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaPointsForDetectiveKillingTraitor = value,
							(config, extraInfo) -> config.karmaPointsForDetectiveKillingTraitor)
					.add()
					.append(new KeyedCodec<>("KarmaPointsForDetectiveKillingInnocent", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaPointsForDetectiveKillingInnocent = value,
							(config, extraInfo) -> config.karmaPointsForDetectiveKillingInnocent)
					.add()
					.append(new KeyedCodec<>("KarmaPointsForDetectiveKillingDetective", Codec.INTEGER),
							(config, value, extraInfo) -> config.karmaPointsForDetectiveKillingDetective = value,
							(config, extraInfo) -> config.karmaPointsForDetectiveKillingDetective)
					.add()
					.append(new KeyedCodec<>("StartingItemsInHotbar", Codec.STRING_ARRAY),
							(config, value, extraInfo) -> config.startingItemsInHotbar = value,
							(config, extraInfo) -> config.startingItemsInHotbar)
					.add()
					.append(new KeyedCodec<>("StartingItemsInInventory", Codec.STRING_ARRAY),
							(config, value, extraInfo) -> config.startingItemsInInventory = value,
							(config, extraInfo) -> config.startingItemsInInventory)
					.add()
					.append(new KeyedCodec<>("TraitorStoreItems", Codec.STRING_ARRAY),
							(config, value, extraInfo) -> config.traitorStoreItems = value,
							(config, extraInfo) -> config.traitorStoreItems)
					.add()
					.append(new KeyedCodec<>("DetectiveStoreItems", Codec.STRING_ARRAY),
							(config, value, extraInfo) -> config.detectiveStoreItems = value,
							(config, extraInfo) -> config.detectiveStoreItems)
					.add()
					.append(new KeyedCodec<>("PlayerGraveId", Codec.STRING),
							(config, value, extraInfo) -> config.playerGraveId = value,
							(config, extraInfo) -> config.playerGraveId)
					.add()
					.append(new KeyedCodec<>("RoundPerMap", Codec.INTEGER),
							(config, value, extraInfo) -> config.roundPerMap = value,
							(config, extraInfo) -> config.roundPerMap)
					.add()
					.append(new KeyedCodec<>("MapsInARowForVoting", Codec.INTEGER),
							(config, value, extraInfo) -> config.mapsInARowForVoting = value,
							(config, extraInfo) -> config.mapsInARowForVoting)
					.add()
					.append(new KeyedCodec<>("WorldTemplatesFolder", Codec.STRING),
							(config, value, extraInfo) -> config.worldTemplatesFolder = value,
							(config, extraInfo) -> config.worldTemplatesFolder)
					.add()
					.append(new KeyedCodec<>("DebugMode", Codec.BOOLEAN),
							(config, value, extraInfo) -> config.debugMode = value,
							(config, extraInfo) -> config.debugMode)
					.add()
					.build();

	// Sets Player's hud background color for current role
	private String innocentColor = "#33CC76";
	// Sets Player's hud background color for current role and scoreboard row background color
	private String traitorColor = "#B01515";

	// Sets required amount of players to start a round
	private int requiredPlayersToStartRound = 1;
	private int minAmountOfTraitors = 1;
	private int traitorsRatio = 4;
	private int minAmountOfDetectives = 0;
	private int detectivesRatio = 11;
	private int maxDetectives = 10;
	// Time in seconds before the round starts
	private int timeBeforeRoundInSeconds = 10;
	private int roundDurationInSeconds = 30;
	private int timeAfterRoundInSeconds = 5;
	private int timeToVoteMapInSeconds = 30;
	private int timeBeforeChangingMapInSeconds = 5;

	// Sets the starting value for each player's karma
	private int karmaStartingValue = 1000;

	private int karmaForDisconnectingMiddleRound = -100;

	private int karamPointsForTraitorKillingInnocent = 10;
	private int karamPointsForTraitorKillingDetective = karamPointsForTraitorKillingInnocent + 10;
	private int karamPointsForTraitorKillingTraitor = -100;

	private int karmaPointsForInnocentKillingTraitor = 10;
	private int karmaPointsForInnocentKillingDetective = -100;
	private int karmaPointsForInnocentKillingInnocent = -50;

	private int karmaPointsForDetectiveKillingTraitor = 10;
	private int karmaPointsForDetectiveKillingInnocent = -50;
	private int karmaPointsForDetectiveKillingDetective = -100;


	private int itemsInARowForTheShop = 5;

	private String[] startingItemsInHotbar = new String[]{
			"Weapon_Shortbow_Combat:1",
	};

	private String[] startingItemsInInventory = new String[]{
			"Weapon_Arrow_Crude:200"
	};

	private String[] traitorStoreItems = new String[]{
			"Weapon_Daggers_Doomed:1"
	};

	private String[] detectiveStoreItems = new String[]{
			"Weapon_Staff_Frost:1"
	};

	private String playerGraveId = "Player_Grave";
	private int roundPerMap = 1;
	private int mapsInARowForVoting = 3;
	private String worldTemplatesFolder = "universe/templates";
	@Setter
	private boolean debugMode = true;

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
		for (int i = 0; i < configuredValues.length; i++) {
			stacks.add(parseItemEntry(configuredValues[i]));
		}

		return stacks;
	}


	public List<ItemStack> getItems(String[] configuredValues) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < configuredValues.length; i++) {
			stacks.addAll(parseItemEntry(configuredValues[i]));
		}

		return stacks;
	}

}
