package arcade;


public enum GameType {
    UNKNOWN, MAME, Z26, ATARI800, DOSBOX, LAUNCH;

    public static GameType typeOf(String type) {
        switch (type.toUpperCase()) {
            case "MAME":      return MAME;
            case "Z26":       return Z26;
            case "ATARI2600": return Z26;
            case "ATARI800":  return ATARI800;
            case "DOSBOX":    return DOSBOX;
            case "LAUNCH":    return LAUNCH;
        }
        return UNKNOWN;
    }
}
