package com.Laroslav7.TerraFlora;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.EnumMap;
import java.util.Map;

/** Simple playable world for TerraFlora 0.0.1. */
public class WorldScreen implements Screen {
    private static final int WORLD_WIDTH = 140;
    private static final int WORLD_HEIGHT = 36;
    private static final int TILE_SIZE = 32;

    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 48f;

    private static final float GRAVITY = -1700f;
    private static final float MOVE_SPEED = 240f;
    private static final float JUMP_SPEED = 650f;

    private final TerraFloraGame game;
    private final TileType[][] world = new TileType[WORLD_WIDTH][WORLD_HEIGHT];
    private final Rectangle playerBounds = new Rectangle(200, 500, PLAYER_WIDTH, PLAYER_HEIGHT);
    private final Vector2 velocity = new Vector2();
    private final Map<ItemType, Integer> inventory = new EnumMap<>(ItemType.class);

    private boolean onGround;
    private float actionCooldown;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private Texture playerTexture;
    private Texture backgroundTexture;
    private Texture grassTexture;
    private Texture dirtTexture;
    private Texture treeTexture;
    private Texture pickaxeTexture;
    private Texture swordTexture;
    private Texture whitePixel;
    private BitmapFont font;

    private ItemType selectedTool = ItemType.PICKAXE;

    public WorldScreen(TerraFloraGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);

        playerTexture = new Texture(Gdx.files.internal("character0.png"));
        backgroundTexture = new Texture(Gdx.files.internal("FieldBackground.png"));
        grassTexture = new Texture(Gdx.files.internal("GrassFloar.png"));
        dirtTexture = new Texture(Gdx.files.internal("pieceofgrass.png"));
        treeTexture = new Texture(Gdx.files.internal("TreeOak.png"));
        pickaxeTexture = new Texture(Gdx.files.internal("TerraPickax.png"));
        swordTexture = new Texture(Gdx.files.internal("TerraSword.png"));

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        font = new BitmapFont();
        font.getData().setScale(1.15f);

        inventory.put(ItemType.WOOD, 0);
        inventory.put(ItemType.DIRT, 0);

        createWorld();
    }

    private void createWorld() {
        int surface = 9;

        for (int x = 0; x < WORLD_WIDTH; x++) {
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                if (y < surface - 1) {
                    world[x][y] = TileType.DIRT;
                } else if (y == surface - 1) {
                    world[x][y] = TileType.GRASS;
                } else {
                    world[x][y] = TileType.AIR;
                }
            }
        }

        addTree(16, surface);
        addTree(23, surface);
        addTree(31, surface);
        addTree(45, surface);
        addTree(55, surface);
    }

    private void addTree(int baseX, int groundY) {
        for (int y = groundY; y < groundY + 4; y++) {
            if (inBounds(baseX, y)) {
                world[baseX][y] = TileType.WOOD;
            }
        }
    }

    @Override
    public void render(float delta) {
        update(delta);

        ScreenUtils.clear(0.58f, 0.75f, 0.96f, 1f);

        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();

        game.getBatch().draw(backgroundTexture,
                camera.position.x - viewport.getWorldWidth() / 2f,
                camera.position.y - viewport.getWorldHeight() / 2f,
                viewport.getWorldWidth(),
                viewport.getWorldHeight());

        renderWorld();
        game.getBatch().draw(playerTexture, playerBounds.x, playerBounds.y, PLAYER_WIDTH, PLAYER_HEIGHT);
        renderUi();

        game.getBatch().end();
    }

    private void update(float delta) {
        handleInput(delta);

        velocity.y += GRAVITY * delta;
        moveWithCollisions(delta);

        actionCooldown = Math.max(0f, actionCooldown - delta);

        camera.position.set(
                playerBounds.x + playerBounds.width / 2f,
                Math.max(playerBounds.y + playerBounds.height / 2f, viewport.getWorldHeight() / 2f),
                0f
        );
        camera.update();
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            velocity.x = -MOVE_SPEED;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            velocity.x = MOVE_SPEED;
        } else {
            velocity.x = 0;
        }

        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W)) && onGround) {
            velocity.y = JUMP_SPEED;
            onGround = false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) selectedTool = ItemType.PICKAXE;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) selectedTool = ItemType.SWORD;

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && actionCooldown <= 0f) {
            breakTileAtMouse();
            actionCooldown = 0.2f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void breakTileAtMouse() {
        Vector2 worldPos = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

        float px = playerBounds.x + playerBounds.width / 2f;
        float py = playerBounds.y + playerBounds.height / 2f;
        if (Vector2.dst(px, py, worldPos.x, worldPos.y) > TILE_SIZE * 3f) {
            return;
        }

        int tileX = MathUtils.floor(worldPos.x / TILE_SIZE);
        int tileY = MathUtils.floor(worldPos.y / TILE_SIZE);
        if (!inBounds(tileX, tileY)) return;

        TileType tile = world[tileX][tileY];
        if (tile == TileType.WOOD) {
            world[tileX][tileY] = TileType.AIR;
            inventory.put(ItemType.WOOD, inventory.get(ItemType.WOOD) + 1);
        } else if (tile == TileType.DIRT || tile == TileType.GRASS) {
            world[tileX][tileY] = TileType.AIR;
            inventory.put(ItemType.DIRT, inventory.get(ItemType.DIRT) + 1);
        }
    }

    private void moveWithCollisions(float delta) {
        playerBounds.x += velocity.x * delta;
        resolveHorizontal();

        playerBounds.y += velocity.y * delta;
        onGround = false;
        resolveVertical();

        if (playerBounds.y < 0) {
            playerBounds.y = 0;
            velocity.y = 0;
            onGround = true;
        }
    }

    private void resolveHorizontal() {
        int minX = MathUtils.floor(playerBounds.x / TILE_SIZE);
        int maxX = MathUtils.floor((playerBounds.x + playerBounds.width) / TILE_SIZE);
        int minY = MathUtils.floor(playerBounds.y / TILE_SIZE);
        int maxY = MathUtils.floor((playerBounds.y + playerBounds.height - 1) / TILE_SIZE);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (isSolid(x, y)) {
                    Rectangle tileRect = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    if (playerBounds.overlaps(tileRect)) {
                        if (velocity.x > 0) {
                            playerBounds.x = tileRect.x - playerBounds.width;
                        } else if (velocity.x < 0) {
                            playerBounds.x = tileRect.x + tileRect.width;
                        }
                    }
                }
            }
        }
    }

    private void resolveVertical() {
        int minX = MathUtils.floor(playerBounds.x / TILE_SIZE);
        int maxX = MathUtils.floor((playerBounds.x + playerBounds.width - 1) / TILE_SIZE);
        int minY = MathUtils.floor(playerBounds.y / TILE_SIZE);
        int maxY = MathUtils.floor((playerBounds.y + playerBounds.height) / TILE_SIZE);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (isSolid(x, y)) {
                    Rectangle tileRect = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    if (playerBounds.overlaps(tileRect)) {
                        if (velocity.y > 0) {
                            playerBounds.y = tileRect.y - playerBounds.height;
                        } else if (velocity.y <= 0) {
                            playerBounds.y = tileRect.y + tileRect.height;
                            onGround = true;
                        }
                        velocity.y = 0;
                    }
                }
            }
        }
    }

    private void renderWorld() {
        int startX = Math.max(0, MathUtils.floor((camera.position.x - viewport.getWorldWidth() / 2f) / TILE_SIZE) - 1);
        int endX = Math.min(WORLD_WIDTH - 1, MathUtils.ceil((camera.position.x + viewport.getWorldWidth() / 2f) / TILE_SIZE) + 1);

        for (int x = startX; x <= endX; x++) {
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                Texture tileTexture = null;
                switch (world[x][y]) {
                    case GRASS -> tileTexture = grassTexture;
                    case DIRT -> tileTexture = dirtTexture;
                    case WOOD -> tileTexture = treeTexture;
                    case AIR -> {
                    }
                }

                if (tileTexture != null) {
                    game.getBatch().draw(tileTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void renderUi() {
        float left = camera.position.x - viewport.getWorldWidth() / 2f + 20f;
        float top = camera.position.y + viewport.getWorldHeight() / 2f - 20f;

        font.draw(game.getBatch(), "TerraFlora 0.0.1", left, top);
        font.draw(game.getBatch(), "WASD/Стрелки: ходить, Space: прыжок, ЛКМ: ломать", left, top - 28f);
        font.draw(game.getBatch(), "1: Кирка, 2: Меч, Esc: меню", left, top - 52f);

        float hotbarY = camera.position.y - viewport.getWorldHeight() / 2f + 24f;
        float firstSlotX = camera.position.x - 90f;

        drawSlot(firstSlotX, hotbarY, selectedTool == ItemType.PICKAXE);
        game.getBatch().draw(pickaxeTexture, firstSlotX + 12, hotbarY + 12, 40, 40);

        drawSlot(firstSlotX + 74, hotbarY, selectedTool == ItemType.SWORD);
        game.getBatch().draw(swordTexture, firstSlotX + 86, hotbarY + 12, 40, 40);

        font.draw(game.getBatch(), "Дерево: " + inventory.get(ItemType.WOOD), left, top - 82f);
        font.draw(game.getBatch(), "Земля: " + inventory.get(ItemType.DIRT), left, top - 106f);
    }

    private void drawSlot(float x, float y, boolean selected) {
        Color color = selected ? new Color(0.95f, 0.84f, 0.32f, 0.95f) : new Color(0.18f, 0.19f, 0.22f, 0.85f);
        game.getBatch().setColor(color);
        game.getBatch().draw(whitePixel, x, y, 64, 64);
        game.getBatch().setColor(Color.WHITE);
    }

    private boolean isSolid(int x, int y) {
        if (!inBounds(x, y)) return false;
        return world[x][y] != TileType.AIR;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < WORLD_WIDTH && y >= 0 && y < WORLD_HEIGHT;
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
        playerTexture.dispose();
        backgroundTexture.dispose();
        grassTexture.dispose();
        dirtTexture.dispose();
        treeTexture.dispose();
        pickaxeTexture.dispose();
        swordTexture.dispose();
        whitePixel.dispose();
        font.dispose();
    }

    private enum TileType {
        AIR,
        GRASS,
        DIRT,
        WOOD
    }

    private enum ItemType {
        PICKAXE,
        SWORD,
        WOOD,
        DIRT
    }
}
