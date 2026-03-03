package com.Laroslav7.TerraFlora.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.Laroslav7.TerraFlora.TerraFloraGame;

public class Lwjgl3Launcher {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        new Lwjgl3Application(new TerraFloraGame(), createConfig());
    }

    private static Lwjgl3ApplicationConfiguration createConfig() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setTitle("TerraFlora");

        // Размер окна
        config.setWindowedMode(1920, 1080);

        // VSync
        config.useVsync(true);
        config.setForegroundFPS(
            Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate
        );

        // Иконку временно убираем, чтобы не падало
        // Если добавишь свою — раскомментируй
        // config.setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png");

        return config;
    }
}
