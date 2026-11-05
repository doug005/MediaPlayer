package com.example.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private MediaPlayerController controller;

    @Override
    public void start(Stage stage) {
        MediaPlayerView view = new MediaPlayerView();
        controller = new MediaPlayerController(stage, view);

        Scene scene = new Scene(view.getRoot(), 1000, 680);
        controller.installKeyboardShortcuts(scene);

        stage.setTitle("PulseWave Studio Player");
        stage.setMinWidth(760);
        stage.setMinHeight(540);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
