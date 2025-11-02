package info.gabrielszabo.ublog.config;

import java.util.Optional;

import info.gabrielszabo.jdi.config.ConfigService;

public enum ConfigServiceImpl implements ConfigService {

    INSTANCE;

    @Override
    public Optional<String> get(String arg0) {
        for (Config config : Config.values()) {
            if (config.key().equals(arg0)) {
                return Optional.of(config.value());
            }
        }
        return Optional.empty();
    }
    
}
