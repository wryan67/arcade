package arcade;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javafx.scene.image.Image;

import java.io.InputStream;

/**
 *
 * @author wryan
 */
class FxImage extends Image {
    private final GameInfo info;

    public FxImage(InputStream imageStream,  GameInfo info) {
        super(imageStream);
        this.info = info;
    }

    public GameInfo getInfo() {
        return info;
    }


}
