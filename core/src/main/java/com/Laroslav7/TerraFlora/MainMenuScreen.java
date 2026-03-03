package com.Laroslav7.TerraFlora;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Main menu for the test version 0.0.1. */
public class MainMenuScreen implements Screen {
    private final TerraFloraGame game;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private Stage stage;
    private Skin skin;

    private Texture logoTexture;
    private Texture backgroundTexture;

    public MainMenuScreen(TerraFloraGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        stage = new Stage(viewport, game.getBatch());
        skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"));

        logoTexture = new Texture(Gdx.files.internal("TerraFlora.png"));
        backgroundTexture = new Texture(Gdx.files.internal("FieldBackground.png"));

        TextButton playButton = new TextButton("Играть", skin);
        playButton.setSize(280, 80);
        playButton.setPosition((viewport.getWorldWidth() - playButton.getWidth()) / 2f, 180f);
        playButton.addListener(event -> {
            if (!playButton.isPressed()) {
                return false;
            }
            game.setScreen(new WorldScreen(game));
            return true;
        });

        stage.addActor(playButton);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(new Color(0.09f, 0.11f, 0.16f, 1f));

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        game.getBatch().draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

        float logoWidth = 620;
        float logoHeight = 250;
        game.getBatch().draw(logoTexture,
                (viewport.getWorldWidth() - logoWidth) / 2f,
                viewport.getWorldHeight() - logoHeight - 80,
                logoWidth,
                logoHeight);
        game.getBatch().end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (logoTexture != null) logoTexture.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
}
