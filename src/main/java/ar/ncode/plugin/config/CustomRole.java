package ar.ncode.plugin.config;

import ar.ncode.plugin.model.TranslationKey;
import ar.ncode.plugin.model.enums.RoleGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomRole {

	public static final BuilderCodec<CustomRole> CODEC = BuilderCodec.builder(CustomRole.class, CustomRole::new)
			.append(new KeyedCodec<>("Id", Codec.STRING),
					(c, value, extraInfo) -> c.id = value,
					(c, extraInfo) -> c.id)
			.add()
			.append(new KeyedCodec<>("TranslationKey", Codec.STRING),
					(c, value, extraInfo) -> c.translationKey = value,
					(c, extraInfo) -> c.translationKey)
			.add()
			.append(new KeyedCodec<>("CustomBackgroundColor", Codec.STRING),
					(c, value, extraInfo) -> c.customGuiColor = value,
					(c, extraInfo) -> c.customGuiColor)
			.add()
			.append(new KeyedCodec<>("MinimumAssignedPlayersWithRole", Codec.INTEGER),
					(c, value, extraInfo) -> c.minimumAssignedPlayersWithRole = value,
					(c, extraInfo) -> c.minimumAssignedPlayersWithRole)
			.add()
			.append(new KeyedCodec<>("MaxAssignedPlayersWithRole", Codec.INTEGER),
					(c, value, extraInfo) -> c.maxAssignedPlayersWithRole = value,
					(c, extraInfo) -> c.maxAssignedPlayersWithRole)
			.add()
			.append(new KeyedCodec<>("RoleGroup", Codec.STRING),
					(c, v, extraInfo) -> c.roleGroup = v == null || v.isEmpty() ? null : RoleGroup.valueOf(v),
					(c, extraInfo) -> c.roleGroup == null ? "" : c.roleGroup.name())
			.add()
			.append(new KeyedCodec<>("SecretRole", Codec.BOOLEAN),
					(c, value, extraInfo) -> c.secretRole = value,
					(c, extraInfo) -> c.secretRole)
			.add()
			.append(new KeyedCodec<>("Ratio", Codec.INTEGER),
					(c, value, extraInfo) -> c.ratio = value,
					(c, extraInfo) -> c.ratio)
			.add()
			.append(new KeyedCodec<>("StartingItems", Codec.STRING_ARRAY),
					(c, value, extraInfo) -> c.startingItems = value,
					(c, extraInfo) -> c.startingItems)
			.add()
			.append(new KeyedCodec<>("StoreItems", Codec.STRING_ARRAY),
					(c, value, extraInfo) -> c.storeItems = value,
					(c, extraInfo) -> c.storeItems)
			.add()
			.append(new KeyedCodec<>("StartingCredits", Codec.INTEGER),
					(c, value, extraInfo) -> c.startingCredits = value,
					(c, extraInfo) -> c.startingCredits)
			.add()
			.build();

	private String id;
	@Getter(AccessLevel.NONE)
	private String translationKey;
	private String customGuiColor;
	@Builder.Default
	private int minimumAssignedPlayersWithRole = 0;
	private int maxAssignedPlayersWithRole;
	private RoleGroup roleGroup;
	@Builder.Default
	private boolean secretRole = true;
	private int ratio;
	private String[] startingItems;
	private String[] storeItems;
	@Builder.Default
	private int startingCredits = 0;

	public boolean hasStore() {
		return storeItems != null && storeItems.length > 0;
	}

	public String getTranslationKey() {
		return TranslationKey.getWithPrefix(translationKey);
	}

}
