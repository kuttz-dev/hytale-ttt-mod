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
					(c, v, extraInfo) -> c.id = v,
					(c, extraInfo) -> c.id)
			.add()
			.append(new KeyedCodec<>("TranslationKey", Codec.STRING),
					(c, v, extraInfo) -> c.translationKey = v,
					(c, extraInfo) -> c.translationKey)
			.add()
			.append(new KeyedCodec<>("CustomBackgroundColor", Codec.STRING),
					(c, v, extraInfo) -> c.customGuiColor = v,
					(c, extraInfo) -> c.customGuiColor)
			.add()
			.append(new KeyedCodec<>("MinimumAssignedPlayersWithRole", Codec.INTEGER),
					(c, v, extraInfo) -> c.minimumAssignedPlayersWithRole = v,
					(c, extraInfo) -> c.minimumAssignedPlayersWithRole)
			.add()
			.append(new KeyedCodec<>("MaxAssignedPlayersWithRole", Codec.INTEGER),
					(c, v, extraInfo) -> c.maxAssignedPlayersWithRole = v,
					(c, extraInfo) -> c.maxAssignedPlayersWithRole)
			.add()
			.append(new KeyedCodec<>("RoleGroup", Codec.STRING),
					(c, v, extraInfo) -> c.roleGroup = v == null || v.isEmpty() ? null : RoleGroup.valueOf(v),
					(c, extraInfo) -> c.roleGroup == null ? "" : c.roleGroup.name())
			.add()
			.append(new KeyedCodec<>("SecretRole", Codec.BOOLEAN),
					(c, v, extraInfo) -> c.secretRole = v,
					(c, extraInfo) -> c.secretRole)
			.add()
			.append(new KeyedCodec<>("Ratio", Codec.INTEGER),
					(c, v, extraInfo) -> c.ratio = v,
					(c, extraInfo) -> c.ratio)
			.add()
			.append(new KeyedCodec<>("StartingItems", Codec.STRING_ARRAY),
					(c, v, extraInfo) -> c.startingItems = v,
					(c, extraInfo) -> c.startingItems)
			.add()
			.append(new KeyedCodec<>("StoreItems", Codec.STRING_ARRAY),
					(c, v, extraInfo) -> c.storeItems = v,
					(c, extraInfo) -> c.storeItems)
			.add()
			.append(new KeyedCodec<>("StartingCredits", Codec.INTEGER),
					(c, v, extraInfo) -> c.startingCredits = v,
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
