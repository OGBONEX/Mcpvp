package com.ogbonex.mcpvp;

import java.util.List;

final class ArenaThemes {
    static final List<String> THEMES = List.of(
            "Classic", "Desert", "Nether", "Snow", "Sky", "Nature", "Stone",
            "Water", "Medieval", "Industrial", "Fantasy", "Town", "End", "Modern"
    );
    static boolean isValid(String theme) {
        return THEMES.stream().anyMatch(value -> value.equalsIgnoreCase(theme));
    }
    private ArenaThemes() {}
}
