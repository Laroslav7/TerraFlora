package com.Laroslav7.TerraFlora;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.EnumMap;
import java.util.Map;

/** TerraFlora Alpha 0.0.3 playable screen. */
public class WorldScreen implements Screen {
    private static final int WORLD_WIDTH = 140;
    private static final int WORLD_HEIGHT = 36;
    private static final int TILE_SIZE = 32;

    private static final float PLAYER_SCALE = 1.3f;
    private static final float PLAYER_WIDTH = 32f * PLAYER_SCALE;
    private static final float PLAYER_HEIGHT = 48f * PLAYER_SCALE;

    private static final float GRAVITY = -1700f;
    private static final float MOVE_SPEED = 240f;
    private static final float JUMP_SPEED = 650f;

    private final TerraFloraGame game;
    private final TileType[][] world = new TileType[WORLD_WIDTH][WORLD_HEIGHT];
    private final Rectangle playerBounds = new Rectangle(200, 0, PLAYER_WIDTH, PLAYER_HEIGHT);
    private final Vector2 velocity = new Vector2();
    private final Map<ItemType, Integer> inventory = new EnumMap<>(ItemType.class);

    private boolean onGround;
    private boolean facingRight = true;
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
    private Texture hotbarTexture;
    private Texture whitePixel;
    private BitmapFont font;

    private int selectedSlot = 0;

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

        if (Gdx.files.internal("assets/ui/FirstHotBar.png").exists()) {
            hotbarTexture = new Texture(Gdx.files.internal("assets/ui/FirstHotBar.png"));
            hotbarTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        playerTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        grassTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        dirtTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        treeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pickaxeTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        swordTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        font = createUiFont();
        font.getData().setScale(0.55f);

        inventory.put(ItemType.WOOD, 0);
        inventory.put(ItemType.DIRT, 0);

        createWorld();
        int surface = findSurfaceY(MathUtils.floor(playerBounds.x / TILE_SIZE));
        playerBounds.y = surface * TILE_SIZE;
        velocity.setZero();
        onGround = true;
    }

    private BitmapFont createUiFont() {
        if (!Gdx.files.internal("assets/ui/Newfont.otf").exists()) {
            return new BitmapFont();
        }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("assets/ui/Newfont.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS +
                "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
                "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        BitmapFont generated = generator.generateFont(parameter);
        generator.dispose();
        return generated;
    }

    private void createWorld() {
        int surface = 6;
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

    private int findSurfaceY(int tileX) {
        int clampedX = MathUtils.clamp(tileX, 0, WORLD_WIDTH - 1);
        for (int y = WORLD_HEIGHT - 1; y >= 0; y--) {
            if (world[clampedX][y] != TileType.AIR) {
                return y + 1;
            }
        }
        return 1;
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
                0,
                viewport.getWorldWidth(),
                viewport.getWorldHeight());

        renderWorld();
        renderPlayer();
        renderUi();
        game.getBatch().end();
    }

    private void renderPlayer() {
        game.getBatch().draw(playerTexture,
                MathUtils.floor(playerBounds.x),
                MathUtils.floor(playerBounds.y),
                PLAYER_WIDTH,
                PLAYER_HEIGHT);

        Texture handTexture = selectedSlot == 1 ? swordTexture : pickaxeTexture;
        float handX = facingRight ? playerBounds.x + PLAYER_WIDTH - 8f : playerBounds.x - 24f;
        float handY = playerBounds.y + PLAYER_HEIGHT * 0.45f;
        game.getBatch().draw(handTexture, MathUtils.floor(handX), MathUtils.floor(handY), 30f, 30f);
    }

    private void update(float delta) {
        handleInput();
        velocity.y += GRAVITY * delta;
        moveWithCollisions(delta);
        actionCooldown = Math.max(0f, actionCooldown - delta);

        camera.position.set(MathUtils.floor(playerBounds.x + playerBounds.width / 2f), viewport.getWorldHeight() / 2f, 0f);
        camera.update();
    }

    private void handleInput() {
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            velocity.x = -MOVE_SPEED;
            facingRight = false;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            velocity.x = MOVE_SPEED;
            facingRight = true;
        } else {
            velocity.x = 0;
        }

        if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W)) && onGround) {
            velocity.y = JUMP_SPEED;
            onGround = false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) selectedSlot = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) selectedSlot = 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) selectedSlot = 2;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) selectedSlot = 3;

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
        if (Vector2.dst(px, py, worldPos.x, worldPos.y) > TILE_SIZE * 3f) return;

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
        int maxX = MathUtils.floor((playerBounds.x + playerBounds.width - 1f) / TILE_SIZE);
        int minY = MathUtils.floor(playerBounds.y / TILE_SIZE);
        int maxY = MathUtils.floor((playerBounds.y + playerBounds.height - 1f) / TILE_SIZE);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!isSolid(x, y)) continue;
                Rectangle tileRect = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                if (!playerBounds.overlaps(tileRect)) continue;
                if (velocity.x > 0) playerBounds.x = tileRect.x - playerBounds.width;
                else if (velocity.x < 0) playerBounds.x = tileRect.x + tileRect.width;
            }
        }
    }

    private void resolveVertical() {
        int minX = MathUtils.floor(playerBounds.x / TILE_SIZE);
        int maxX = MathUtils.floor((playerBounds.x + playerBounds.width - 1f) / TILE_SIZE);
        int minY = MathUtils.floor(playerBounds.y / TILE_SIZE);
        int maxY = MathUtils.floor((playerBounds.y + playerBounds.height - 1f) / TILE_SIZE);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!isSolid(x, y)) continue;
                Rectangle tileRect = new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                if (!playerBounds.overlaps(tileRect)) continue;

                if (velocity.y > 0) {
                    playerBounds.y = tileRect.y - playerBounds.height;
                } else {
                    playerBounds.y = tileRect.y + tileRect.height;
                    onGround = true;
                }
                velocity.y = 0;
            }
        }
    }

    private void renderWorld() {
        int startX = Math.max(0, MathUtils.floor((camera.position.x - viewport.getWorldWidth() / 2f) / TILE_SIZE) - 1);
        int endX = Math.min(WORLD_WIDTH - 1, MathUtils.ceil((camera.position.x + viewport.getWorldWidth() / 2f) / TILE_SIZE) + 1);

        for (int x = startX; x <= endX; x++) {
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                Texture tileTexture = switch (world[x][y]) {
                    case GRASS -> grassTexture;
                    case DIRT -> dirtTexture;
                    case WOOD -> treeTexture;
                    case AIR -> null;
                };

                if (tileTexture != null) {
                    game.getBatch().draw(tileTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void renderUi() {
        float topY = camera.position.y + viewport.getWorldHeight() / 2f - 6f;
        float topX = camera.position.x - 80f;
        font.draw(game.getBatch(), "TerraFlora Alpha 0.0.3", topX, topY);

        float hotbarY = camera.position.y - viewport.getWorldHeight() / 2f + 18f;
        float hotbarX = camera.position.x - 128f;

        if (hotbarTexture != null) {
            game.getBatch().draw(hotbarTexture, hotbarX, hotbarY, 256, 64);
        } else {
            drawFallbackHotbar(hotbarX, hotbarY);
        }

        drawSelectedSlot(hotbarX, hotbarY);

        drawHotbarItem(0, hotbarX, hotbarY, pickaxeTexture, -1);
        drawHotbarItem(1, hotbarX, hotbarY, swordTexture, -1);
        drawHotbarItem(2, hotbarX, hotbarY, treeTexture, inventory.get(ItemType.WOOD));
        drawHotbarItem(3, hotbarX, hotbarY, dirtTexture, inventory.get(ItemType.DIRT));

        drawSlotNumber(hotbarX, hotbarY, 0, "1");
        drawSlotNumber(hotbarX, hotbarY, 1, "2");
        drawSlotNumber(hotbarX, hotbarY, 2, "3");
        drawSlotNumber(hotbarX, hotbarY, 3, "4");
    }

    private void drawHotbarItem(int slot, float hotbarX, float hotbarY, Texture texture, int amount) {
        float itemX = hotbarX + slot * 64f + 13f;
        float itemY = hotbarY + 13f;
        game.getBatch().draw(texture, itemX, itemY, 38f, 38f);

        if (amount >= 0) {
            font.draw(game.getBatch(), String.valueOf(amount), hotbarX + slot * 64f + 40f, hotbarY + 16f);
        }
    }

    private void drawSlotNumber(float hotbarX, float hotbarY, int slot, String number) {
        font.draw(game.getBatch(), number, hotbarX + slot * 64f + 5f, hotbarY + 60f);
    }

    private void drawSelectedSlot(float hotbarX, float hotbarY) {
        game.getBatch().setColor(new Color(1f, 0.92f, 0.35f, 0.45f));
        game.getBatch().draw(whitePixel, hotbarX + selectedSlot * 64f, hotbarY, 64f, 64f);
        game.getBatch().setColor(Color.WHITE);
    }

    private void drawFallbackHotbar(float hotbarX, float hotbarY) {
        for (int i = 0; i < 4; i++) {
            game.getBatch().setColor(new Color(0.16f, 0.18f, 0.22f, 0.9f));
            game.getBatch().draw(whitePixel, hotbarX + i * 64f, hotbarY, 62, 62);
            game.getBatch().setColor(Color.WHITE);
        }
    }

    private boolean isSolid(int x, int y) {
        return inBounds(x, y) && world[x][y] != TileType.AIR;
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
        if (hotbarTexture != null) hotbarTexture.dispose();
        whitePixel.dispose();
        font.dispose();
    }

    private enum TileType {
        AIR, GRASS, DIRT, WOOD
    }

    private enum ItemType {
        WOOD, DIRT
    }
}
