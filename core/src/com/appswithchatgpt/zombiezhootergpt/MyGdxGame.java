package com.appswithchatgpt.zombiezhootergpt;

import com.badlogic.gdx.Game;

public class MyGdxGame extends Game {

    @Override
    public void create () {
        setScreen(new MenuScreen(this, 0));
    }
}

