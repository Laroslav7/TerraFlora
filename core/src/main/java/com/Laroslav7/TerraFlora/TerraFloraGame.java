package com.Laroslav7.TerraFlora;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TerraFloraGame extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen());
    }
}