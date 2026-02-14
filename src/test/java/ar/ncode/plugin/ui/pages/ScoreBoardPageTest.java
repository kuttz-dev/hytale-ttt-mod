package ar.ncode.plugin.ui.pages;

import ar.ncode.plugin.TroubleInTrorkTownPlugin;
import ar.ncode.plugin.component.PlayerGameModeInfo;
import ar.ncode.plugin.component.death.ConfirmedDeath;
import ar.ncode.plugin.component.death.LostInCombat;
import ar.ncode.plugin.config.CustomConfig;
import ar.ncode.plugin.model.PlayerComponents;
import ar.ncode.plugin.model.enums.RoleGroup;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Test suite for ScoreBoardPage.getTableRows() method.
 * <p>
 * This suite tests the logic that categorizes players into different groups:
 * - alivePlayers: Players with a valid round role and no death components
 * - confirmedDeaths: Players with the ConfirmedDeath component
 * - lostInCombat: Players with the LostInCombat component (if showLostInCombat is true)
 * - spectators: Players without a current round role
 */
class ScoreBoardPageTest {

	// Mock ComponentTypes for testing
	private static Object mockConfirmedDeathComponentType;
	private static Object mockLostInCombatComponentType;

	@BeforeAll
	static void setUp() throws Exception {
		// Mock the static config field in TroubleInTrorkTownPlugin
		Config<CustomConfig> mockConfig = Mockito.mock(Config.class);
		CustomConfig customConfig = new CustomConfig();

		// Configure the mock to return a CustomConfig instance when get() is called
		Mockito.when(mockConfig.get()).thenReturn(customConfig);

		// Set the mock config in the TroubleInTrorkTownPlugin class
		TroubleInTrorkTownPlugin.config = mockConfig;

		// Create mock ComponentTypes for testing
		mockConfirmedDeathComponentType = new ComponentType<>();
		mockLostInCombatComponentType = new ComponentType<>();

		// Inject the mock ComponentTypes into the actual classes using reflection
		setStaticField(ConfirmedDeath.class, "componentType", mockConfirmedDeathComponentType);
		setStaticField(LostInCombat.class, "componentType", mockLostInCombatComponentType);
	}

	/**
	 * Helper method to set static fields using reflection
	 */
	private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(null, value);
	}

	@NonNullDecl
	private static PlayerComponents createPlayer(RoleGroup role, String name, boolean confirmedDeath, boolean lostInCombat) {
		PlayerGameModeInfo info = PlayerGameModeInfo.builder()
				.role(role)
				.build();

		Player player = Mockito.mock(Player.class);
		Mockito.when(player.getDisplayName()).thenReturn(name);

		PlayerRef targetPlayerRef = Mockito.mock();

		Ref<EntityStore> reference = Mockito.mock();
		Mockito.when(reference.isValid()).thenReturn(true);

		Store<EntityStore> store = Mockito.mock();
		Mockito.when(reference.getStore()).thenReturn(store);

		// Use thenAnswer to dynamically return the correct component based on the ComponentType
		Mockito.when(store.getComponent(ArgumentMatchers.eq(reference), ArgumentMatchers.any()))
				.thenAnswer(invocation -> {
					Object componentTypeArg = invocation.getArgument(1);

					// Check if this is a ConfirmedDeath component request
					if (componentTypeArg == mockConfirmedDeathComponentType) {
						return confirmedDeath ? Mockito.mock(ConfirmedDeath.class) : null;
					}

					// Check if this is a LostInCombat component request
					if (componentTypeArg == mockLostInCombatComponentType) {
						return lostInCombat ? Mockito.mock(LostInCombat.class) : null;
					}

					// For any other component type
					return null;
				});

		return new PlayerComponents(player, targetPlayerRef, info, reference);
	}

	@Test
	void getTableRows() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents playerComponents = createPlayer(RoleGroup.INNOCENT, "josephkm", true, true);

		players.add(playerComponents);

		var result = ScoreBoardPage.getTableRows(players, true);
		Assertions.assertEquals(0, result.alivePlayers.size());
	}

	/**
	 * Scenario A: Active Player
	 * A player entity exists with a valid player role component but no death-related components.
	 * The logic should place them in the alive category.
	 */
	@Test
	void testScenarioA_ActivePlayer() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents activePlayer = createPlayer(RoleGroup.INNOCENT, "player_innocent", false, false);
		activePlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(activePlayer);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(1, result.alivePlayers.size(), "Should have 1 alive player");
		Assertions.assertEquals(0, result.confirmedDeaths.size(), "Should have 0 confirmed deaths");
		Assertions.assertEquals(0, result.lostInCombat.size(), "Should have 0 lost in combat");
		Assertions.assertEquals(0, result.spectators.size(), "Should have 0 spectators");
		Assertions.assertEquals("player_innocent", result.alivePlayers.get(0).component().getDisplayName());
	}

	/**
	 * Scenario A Extended: Multiple Active Players with Different Roles
	 * Tests that multiple players with different roles are correctly categorized as alive.
	 */
	@Test
	void testScenarioA_MultipleActivePlayers() {
		Collection<PlayerComponents> players = new ArrayList<>();

		PlayerComponents innocentPlayer = createPlayer(RoleGroup.INNOCENT, "player_innocent", false, false);
		innocentPlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(innocentPlayer);

		PlayerComponents traitorPlayer = createPlayer(RoleGroup.TRAITOR, "player_traitor", false, false);
		traitorPlayer.info().setCurrentRoundRole(RoleGroup.TRAITOR);
		players.add(traitorPlayer);

		PlayerComponents detectivePlayer = createPlayer(RoleGroup.DETECTIVE, "player_detective", false, false);
		detectivePlayer.info().setCurrentRoundRole(RoleGroup.DETECTIVE);
		players.add(detectivePlayer);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(3, result.alivePlayers.size(), "Should have 3 alive players");
		Assertions.assertEquals(0, result.confirmedDeaths.size());
		Assertions.assertEquals(0, result.lostInCombat.size());
		Assertions.assertEquals(0, result.spectators.size());
	}

	/**
	 * Scenario B: Confirmed Death
	 * A player entity has the confirmed death component.
	 * The logic should ignore other death flags and place them in the confirmed death list.
	 */
	@Test
	void testScenarioB_ConfirmedDeath() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents deadPlayer = createPlayer(RoleGroup.INNOCENT, "player_dead", true, false);
		deadPlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(deadPlayer);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(0, result.alivePlayers.size(), "Should have 0 alive players");
		Assertions.assertEquals(1, result.confirmedDeaths.size(), "Should have 1 confirmed death");
		Assertions.assertEquals(0, result.lostInCombat.size(), "Should have 0 lost in combat");
		Assertions.assertEquals(0, result.spectators.size(), "Should have 0 spectators");
		Assertions.assertEquals("player_dead", result.confirmedDeaths.get(0).component().getDisplayName());
	}

	/**
	 * Scenario B Extended: Confirmed Death Takes Priority
	 * A player has both ConfirmedDeath and LostInCombat components.
	 * The confirmed death should take priority (it's checked first).
	 */
	@Test
	void testScenarioB_ConfirmedDeathPriority() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents deadPlayer = createPlayer(RoleGroup.INNOCENT, "player_dead_both", true, true);
		deadPlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(deadPlayer);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(0, result.alivePlayers.size());
		Assertions.assertEquals(1, result.confirmedDeaths.size(), "Should prioritize confirmed death");
		Assertions.assertEquals(0, result.lostInCombat.size(), "Lost in combat should not be included");
		Assertions.assertEquals(0, result.spectators.size());
	}

	/**
	 * Scenario C: Lost in Combat (Show Enabled)
	 * A player entity has the lost in combat component.
	 * If the scoreboard setting to show these players is active (showLostInCombat=true),
	 * they should appear in the lost in combat category.
	 */
	@Test
	void testScenarioC_LostInCombat_ShowEnabled() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents combatPlayer = createPlayer(RoleGroup.INNOCENT, "player_lost_combat", false, true);
		combatPlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(combatPlayer);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(0, result.alivePlayers.size(), "Should have 0 alive players");
		Assertions.assertEquals(0, result.confirmedDeaths.size(), "Should have 0 confirmed deaths");
		Assertions.assertEquals(1, result.lostInCombat.size(), "Should have 1 lost in combat");
		Assertions.assertEquals(0, result.spectators.size(), "Should have 0 spectators");
		Assertions.assertEquals("player_lost_combat", result.lostInCombat.get(0).component().getDisplayName());
	}

	/**
	 * Scenario C Extended: Lost in Combat (Show Disabled)
	 * A player entity has the lost in combat component.
	 * If the scoreboard setting to show these players is disabled (showLostInCombat=false),
	 * they should be treated as alive players (continue through the logic).
	 */
	@Test
	void testScenarioC_LostInCombat_ShowDisabled() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents combatPlayer = createPlayer(RoleGroup.INNOCENT, "player_lost_combat_hidden", false, true);
		combatPlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(combatPlayer);

		var result = ScoreBoardPage.getTableRows(players, false);

		Assertions.assertEquals(1, result.alivePlayers.size(), "Should treat lost in combat as alive when showLostInCombat=false");
		Assertions.assertEquals(0, result.confirmedDeaths.size());
		Assertions.assertEquals(0, result.lostInCombat.size(), "Should have 0 lost in combat when disabled");
		Assertions.assertEquals(0, result.spectators.size());
	}

	/**
	 * Scenario D: Late Joiner (Spectator)
	 * A player joins the server while a round is active.
	 * The logic should detect the lack of a starting role (currentRoundRole == null)
	 * and place them in the spectator category.
	 */
	@Test
	void testScenarioD_LateJoiner_Spectator() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents spectator = createPlayer(RoleGroup.SPECTATOR, "late_joiner", false, false);
		// currentRoundRole is null by default in PlayerGameModeInfo
		players.add(spectator);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(0, result.alivePlayers.size(), "Should have 0 alive players");
		Assertions.assertEquals(0, result.confirmedDeaths.size(), "Should have 0 confirmed deaths");
		Assertions.assertEquals(0, result.lostInCombat.size(), "Should have 0 lost in combat");
		Assertions.assertEquals(1, result.spectators.size(), "Should have 1 spectator");
		Assertions.assertEquals("late_joiner", result.spectators.get(0).component().getDisplayName());
	}

	/**
	 * Scenario D Extended: Spectator with Role
	 * A player has SPECTATOR as their main role but no currentRoundRole.
	 * They should be placed in the spectator category.
	 */
	@Test
	void testScenarioD_SpectatorWithoutCurrentRole() {
		Collection<PlayerComponents> players = new ArrayList<>();
		PlayerComponents spectator = createPlayer(RoleGroup.SPECTATOR, "spectator_player", false, false);
		spectator.info().setCurrentRoundRole(null); // Explicitly set to null
		players.add(spectator);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(0, result.alivePlayers.size());
		Assertions.assertEquals(0, result.confirmedDeaths.size());
		Assertions.assertEquals(0, result.lostInCombat.size());
		Assertions.assertEquals(1, result.spectators.size(), "Player without current round role should be spectator");
	}

	/**
	 * Complex State Scenario: All Four Types Present
	 * This scenario tests the scoreboard when all four types of players are present:
	 * - Active players with valid roles
	 * - Players with confirmed death
	 * - Players lost in combat
	 * - Spectators
	 */
	@Test
	void testComplexScenario_AllPlayerTypes() {
		Collection<PlayerComponents> players = new ArrayList<>();

		// Active player - innocent
		PlayerComponents activeInnocent = createPlayer(RoleGroup.INNOCENT, "active_innocent", false, false);
		activeInnocent.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(activeInnocent);

		// Active player - traitor
		PlayerComponents activeTraitor = createPlayer(RoleGroup.TRAITOR, "active_traitor", false, false);
		activeTraitor.info().setCurrentRoundRole(RoleGroup.TRAITOR);
		players.add(activeTraitor);

		// Confirmed death
		PlayerComponents confirmedDead = createPlayer(RoleGroup.DETECTIVE, "confirmed_dead", true, false);
		confirmedDead.info().setCurrentRoundRole(RoleGroup.DETECTIVE);
		players.add(confirmedDead);

		// Lost in combat
		PlayerComponents lostInCombat = createPlayer(RoleGroup.INNOCENT, "lost_in_combat", false, true);
		lostInCombat.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(lostInCombat);

		// Spectator (late joiner)
		PlayerComponents spectator = createPlayer(RoleGroup.SPECTATOR, "spectator", false, false);
		spectator.info().setCurrentRoundRole(null);
		players.add(spectator);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(2, result.alivePlayers.size(), "Should have 2 active players");
		Assertions.assertEquals(1, result.confirmedDeaths.size(), "Should have 1 confirmed death");
		Assertions.assertEquals(1, result.lostInCombat.size(), "Should have 1 lost in combat");
		Assertions.assertEquals(1, result.spectators.size(), "Should have 1 spectator");

		// Verify the specific players are in the right categories
		Assertions.assertTrue(
				result.alivePlayers.stream()
						.anyMatch(p -> "active_innocent".equals(p.component().getDisplayName())),
				"Should contain active_innocent"
		);
		Assertions.assertTrue(
				result.alivePlayers.stream()
						.anyMatch(p -> "active_traitor".equals(p.component().getDisplayName())),
				"Should contain active_traitor"
		);
		Assertions.assertEquals("confirmed_dead", result.confirmedDeaths.get(0).component().getDisplayName());
		Assertions.assertEquals("lost_in_combat", result.lostInCombat.get(0).component().getDisplayName());
		Assertions.assertEquals("spectator", result.spectators.get(0).component().getDisplayName());
	}

	/**
	 * Edge Case: Empty Player List
	 * Tests that an empty player list is handled correctly.
	 */
	@Test
	void testEdgeCase_EmptyPlayerList() {
		Collection<PlayerComponents> players = new ArrayList<>();

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(0, result.alivePlayers.size());
		Assertions.assertEquals(0, result.confirmedDeaths.size());
		Assertions.assertEquals(0, result.lostInCombat.size());
		Assertions.assertEquals(0, result.spectators.size());
	}

	/**
	 * Edge Case: Player with Invalid Reference
	 * Tests that players with invalid references are skipped.
	 */
	@Test
	void testEdgeCase_InvalidPlayerReference() {
		Collection<PlayerComponents> players = new ArrayList<>();

		// Valid active player
		PlayerComponents activePlayer = createPlayer(RoleGroup.INNOCENT, "active_player", false, false);
		activePlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(activePlayer);

		// Player with invalid reference
		PlayerComponents invalidPlayer = Mockito.mock(PlayerComponents.class);
		Ref<EntityStore> invalidRef = Mockito.mock();
		Mockito.when(invalidRef.isValid()).thenReturn(false);
		Mockito.when(invalidPlayer.reference()).thenReturn(invalidRef);
		players.add(invalidPlayer);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(1, result.alivePlayers.size(), "Should only count the valid player");
		Assertions.assertEquals("active_player", result.alivePlayers.get(0).component().getDisplayName());
	}

	/**
	 * Edge Case: Player with Null PlayerGameModeInfo
	 * Tests that players without info are skipped.
	 */
	@Test
	void testEdgeCase_NullPlayerInfo() {
		Collection<PlayerComponents> players = new ArrayList<>();

		// Valid active player
		PlayerComponents activePlayer = createPlayer(RoleGroup.INNOCENT, "active_player", false, false);
		activePlayer.info().setCurrentRoundRole(RoleGroup.INNOCENT);
		players.add(activePlayer);

		// Player with null info
		PlayerComponents nullInfoPlayer = Mockito.mock(PlayerComponents.class);
		Ref<EntityStore> ref = Mockito.mock();
		Mockito.when(ref.isValid()).thenReturn(true);
		Mockito.when(nullInfoPlayer.reference()).thenReturn(ref);
		Mockito.when(nullInfoPlayer.info()).thenReturn(null);
		players.add(nullInfoPlayer);

		var result = ScoreBoardPage.getTableRows(players, true);

		Assertions.assertEquals(1, result.alivePlayers.size(), "Should only count the valid player with info");
		Assertions.assertEquals("active_player", result.alivePlayers.get(0).component().getDisplayName());
	}
}
