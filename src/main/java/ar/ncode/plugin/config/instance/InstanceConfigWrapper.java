package ar.ncode.plugin.config.instance;

import com.hypixel.hytale.server.core.util.Config;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class InstanceConfigWrapper {

    Config<InstanceConfig> instanceConfig;
    Path path;
    String safeName;
    String displayName;
    String folderName;

}
