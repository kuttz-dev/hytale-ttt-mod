package ar.ncode.plugin.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@Getter
@RequiredArgsConstructor
public class CustomMap {

	private final String mapName;
	private final Path mapPreviewPath;

}
