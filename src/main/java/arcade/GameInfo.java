package arcade;

import java.io.Serializable;

/**
 * Created by wryan on 8/20/2015.
 */
public class GameInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private GameType type;
    private String gameTypeText;
    private String alias;
    private String name;
    private String imageFileName;
    private String ahkFileName;

//    public GameInfo() {}
//    public GameInfo(GameType type, String name, String alias) {
//        this.type=type;
//        this.name=name;
//        this.alias=alias;
//    }

    public GameType getType() {
        return type;
    }

    public void setType(GameType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getGameTypeText() {
        return gameTypeText;
    }

    public void setGameTypeText(String gameTypeText) {
        this.gameTypeText = gameTypeText;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getAhkFileName() {
        return ahkFileName;
    }

    public void setAhkFileName(String ahkFileName) {
        this.ahkFileName = ahkFileName;
    }
}
