package com.appswithchatgpt.zombiezhootergpt;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

public class MenuScreen implements Screen {

    private Game game;
    private SpriteBatch batch;
    private BitmapFont font;
    private int lastKillCount;

    public MenuScreen(Game game, int lastKillCount) {
        this.game = game;
        this.lastKillCount = lastKillCount;
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void show() {
        // This method is called when this screen becomes the current screen for a Game.
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        GlyphLayout layout1 = new GlyphLayout(font, "Last Kill Count: " + lastKillCount);
        float x1 = (Gdx.graphics.getWidth() - layout1.width) / 2;
        font.draw(batch, "Last Kill Count: " + lastKillCount, x1, Gdx.graphics.getHeight() / 2 + layout1.height / 2);

        GlyphLayout layout2 = new GlyphLayout(font, "Tap anywhere to start!");
        float x2 = (Gdx.graphics.getWidth() - layout2.width) / 2;
        font.draw(batch, "Tap anywhere to start!", x2, Gdx.graphics.getHeight() / 2 - 100 - layout2.height / 2);

        font.getData().setScale(4, 4); // Scales the font by a factor of 2 in both dimensions
        batch.end();

        if (Gdx.input.isTouched()) {
            game.setScreen(new GameView(game));
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }

    // Implement the other methods from the Screen interface...
}

