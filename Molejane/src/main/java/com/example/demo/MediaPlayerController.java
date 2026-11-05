package com.example.demo;

import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.List;

public class MediaPlayerController {
    private final Stage stage;
    private final MediaPlayerView view;
    private MediaPlayer mediaPlayer;
    private Stage videoFullscreenStage;
    private StackPane fullscreenVideoArea;
    private File currentFile;
    private boolean updatingProgress;
    private double previousVolume = 0.7;

    public MediaPlayerController(Stage stage, MediaPlayerView view) {
        this.stage = stage;
        this.view = view;
        connectControls();
    }

    private void connectControls() {
        view.getAddButton().setOnAction(event -> addMediaFiles());
        view.getRemoveButton().setOnAction(event -> removeSelectedFile());
        view.getPreviousButton().setOnAction(event -> playPrevious());
        view.getPlayPauseButton().setOnAction(event -> togglePlayPause());
        view.getStopButton().setOnAction(event -> stopPlayback());
        view.getNextButton().setOnAction(event -> playNext());
        view.getReplayButton().setOnAction(event -> replayCurrent());
        view.getMuteButton().setOnAction(event -> toggleMute());
        view.getFullscreenButton().setOnAction(event -> toggleVideoFullscreen());

        view.getPlaylist().getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
            if (newFile != null && !newFile.equals(currentFile)) {
                loadMedia(newFile, true);
            }
        });
        view.getProgressSlider().valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingProgress && !view.getProgressSlider().isValueChanging()) {
                seekToSliderPosition();
            }
        });
        view.getProgressSlider().valueChangingProperty().addListener((obs, oldValue, changing) -> {
            if (!changing) {
                seekToSliderPosition();
            }
        });
        view.getProgressSlider().setOnMouseReleased(event -> seekToSliderPosition());
        view.getVolumeSlider().valueProperty().addListener((obs, oldValue, newValue) -> {
            double volume = newValue.doubleValue();
            if (volume > 0) {
                previousVolume = volume;
            }
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume);
            }
            view.getMuteButton().setText(volume == 0 ? "🔇" : "🔊");
        });
    }

    public void installKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (scene.getFocusOwner() instanceof TextInputControl) {
                return;
            }

            KeyCode key = event.getCode();
            if (key == KeyCode.SPACE) {
                togglePlayPause();
            } else if (key == KeyCode.S) {
                stopPlayback();
            } else if (key == KeyCode.N || key == KeyCode.RIGHT) {
                playNext();
            } else if (key == KeyCode.P || key == KeyCode.LEFT) {
                playPrevious();
            } else if (key == KeyCode.UP) {
                changeVolume(0.1);
            } else if (key == KeyCode.DOWN) {
                changeVolume(-0.1);
            } else if (key == KeyCode.M) {
                toggleMute();
            } else if (key == KeyCode.F) {
                toggleVideoFullscreen();
            } else {
                return;
            }
            event.consume();
        });
    }

    private void addMediaFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add audio or video files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Common audio and video", "*.mp3", "*.wav", "*.aiff",
                        "*.aif", "*.m4a", "*.aac", "*.mp4", "*.m4v", "*.mov", "*.avi", "*.mkv", "*.webm"),
                new FileChooser.ExtensionFilter("All files", "*.*"));

        List<File> selectedFiles = chooser.showOpenMultipleDialog(stage);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        int added = 0;
        for (File file : selectedFiles) {
            if (file.isFile() && !view.getMediaFiles().contains(file)) {
                view.getMediaFiles().add(file);
                added++;
            }
        }
        view.getStatusLabel().setText(added == 0
                ? "No new files were added" : added + " file(s) added");
    }

    private void toggleVideoFullscreen() {
        if (videoFullscreenStage != null) {
            videoFullscreenStage.close();
            return;
        }
        if (!isVideoFile(currentFile)) {
            return;
        }

        StackPane fullscreenArea = new StackPane();
        fullscreenArea.setStyle("-fx-background-color: black;");
        Stage fullscreenStage = new Stage();
        fullscreenStage.initOwner(stage);
        fullscreenStage.setTitle("Video fullscreen");
        fullscreenStage.setFullScreenExitHint("Press Esc to exit video fullscreen");
        fullscreenStage.setScene(new Scene(fullscreenArea));
        fullscreenArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                fullscreenStage.close();
                event.consume();
            }
        });
        view.showVideoFullscreen(fullscreenArea);
        videoFullscreenStage = fullscreenStage;
        fullscreenVideoArea = fullscreenArea;
        fullscreenStage.setOnHidden(event -> {
            view.restoreVideoFromFullscreen(fullscreenVideoArea);
            fullscreenVideoArea = null;
            videoFullscreenStage = null;
        });
        fullscreenStage.setFullScreen(true);
        fullscreenStage.show();
    }

    private void removeSelectedFile() {
        File selected = view.getPlaylist().getSelectionModel().getSelectedItem();
        if (selected == null) {
            view.getStatusLabel().setText("Select a playlist item to remove");
            return;
        }

        int removedIndex = view.getMediaFiles().indexOf(selected);
        boolean removedCurrent = selected.equals(currentFile);
        view.getPlaylist().getSelectionModel().clearSelection();
        view.getMediaFiles().remove(selected);
        if (removedCurrent) {
            disposePlayer();
            clearCurrentMedia();
        }

        if (view.getMediaFiles().isEmpty()) {
            view.getPlaylist().getSelectionModel().clearSelection();
            view.getStatusLabel().setText("Playlist is empty");
        } else {
            int nextIndex = Math.min(removedIndex, view.getMediaFiles().size() - 1);
            File next = view.getMediaFiles().get(nextIndex);
            if (removedCurrent) {
                view.getPlaylist().getSelectionModel().select(next);
            }
            view.getStatusLabel().setText("Removed " + selected.getName());
        }
    }

    private void loadMedia(File file, boolean autoplay) {
        if (file == null || !file.isFile()) {
            view.getStatusLabel().setText("The selected file is unavailable");
            return;
        }

        disposePlayer();
        currentFile = file;
        view.setCurrentFile(file);
        view.getPlaylist().getSelectionModel().select(file);
        boolean video = isVideoFile(file);
        if (!video && videoFullscreenStage != null) {
            toggleVideoFullscreen();
        }
        view.getCurrentFileLabel().setText(file.getName());
        view.getCurrentTimeLabel().setText("0:00");
        view.getDurationLabel().setText("0:00");
        view.getProgressSlider().setValue(0);
        view.getProgressSlider().setDisable(true);
        view.getPlayPauseButton().setText(autoplay ? "Pause" : "Play");
        view.getStatusLabel().setText("Loading media…");
        view.getDisplayMessage().setText("Loading " + file.getName());
        view.setVideoMode(video);
        if (video) {
            view.getDisplayMessage().setText("Loading " + file.getName());
            view.getDisplayMessage().setVisible(true);
        }

        Media media;
        try {
            media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
        } catch (IllegalArgumentException exception) {
            showPlaybackError("Cannot open this file: " + exception.getMessage());
            return;
        }

        MediaPlayer player = mediaPlayer;
        MediaView mediaView = view.getMediaView();
        if (video) {
            mediaView.setMediaPlayer(player);
            mediaView.setVisible(true);
        } else {
            mediaView.setMediaPlayer(null);
        }

        media.setOnError(() -> {
            if (mediaPlayer == player) {
                showPlaybackError(errorMessage(media.getError()));
            }
        });
        player.setOnReady(() -> {
            if (mediaPlayer != player) {
                return;
            }
            Duration duration = player.getTotalDuration();
            view.getDurationLabel().setText(formatTime(duration));
            boolean canSeek = duration != null && !duration.isUnknown()
                    && duration.greaterThan(Duration.ZERO);
            view.getProgressSlider().setDisable(!canSeek);
            if (video) {
                view.getDisplayMessage().setVisible(false);
            } else {
                view.getDisplayMessage().setText("♫  " + file.getName());
                view.getDisplayMessage().setVisible(true);
            }
            if (autoplay) {
                player.play();
            } else {
                view.getStatusLabel().setText("Ready");
            }
            view.getPlayPauseButton().setText(autoplay ? "Pause" : "Play");
        });
        player.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (mediaPlayer != player) {
                return;
            }
            view.getCurrentTimeLabel().setText(formatTime(newTime));
            Duration duration = player.getTotalDuration();
            if (!view.getProgressSlider().isValueChanging() && duration != null
                    && !duration.isUnknown() && duration.greaterThan(Duration.ZERO)) {
                updatingProgress = true;
                view.getProgressSlider().setValue(newTime.toMillis() / duration.toMillis());
                updatingProgress = false;
            }
        });
        player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (mediaPlayer != player) {
                return;
            }
            if (newStatus == MediaPlayer.Status.PLAYING) {
                view.getStatusLabel().setText("Playing");
                view.getPlayPauseButton().setText("Pause");
            } else if (newStatus == MediaPlayer.Status.PAUSED) {
                view.getStatusLabel().setText("Paused");
                view.getPlayPauseButton().setText("Play");
            } else if (newStatus == MediaPlayer.Status.STOPPED) {
                view.getStatusLabel().setText("Stopped");
                view.getPlayPauseButton().setText("Play");
            }
        });
        player.setOnEndOfMedia(() -> {
            if (mediaPlayer == player) {
                playNextAfterEnd();
            }
        });
        player.setOnError(() -> {
            if (mediaPlayer == player) {
                showPlaybackError(errorMessage(player.getError()));
            }
        });
        player.setVolume(view.getVolumeSlider().getValue());
        view.getPlaylist().refresh();
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            File selected = view.getPlaylist().getSelectionModel().getSelectedItem();
            if (selected == null && !view.getMediaFiles().isEmpty()) {
                selected = view.getMediaFiles().get(0);
            }
            if (selected == null) {
                view.getStatusLabel().setText("Add media files to start playback");
            } else {
                loadMedia(selected, true);
            }
            return;
        }

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.STOPPED) {
                mediaPlayer.seek(Duration.ZERO);
            }
            mediaPlayer.play();
        }
    }

    private void stopPlayback() {
        if (mediaPlayer == null) {
            view.getStatusLabel().setText("No media is loaded");
            return;
        }
        mediaPlayer.stop();
        mediaPlayer.seek(Duration.ZERO);
        view.getCurrentTimeLabel().setText("0:00");
        view.getProgressSlider().setValue(0);
        view.getStatusLabel().setText("Stopped");
        view.getPlayPauseButton().setText("Play");
    }

    private void playNext() {
        int index = view.getMediaFiles().indexOf(currentFile);
        if (index < 0) {
            index = view.getMediaFiles().indexOf(view.getPlaylist().getSelectionModel().getSelectedItem());
        }
        int nextIndex = index + 1;
        if (nextIndex >= 0 && nextIndex < view.getMediaFiles().size()) {
            loadMedia(view.getMediaFiles().get(nextIndex), true);
        } else {
            view.getStatusLabel().setText("No next media file");
        }
    }

    private void playNextAfterEnd() {
        if (!view.getAutoplayCheckBox().isSelected()) {
            view.getStatusLabel().setText("Finished");
            view.getPlayPauseButton().setText("Play");
            return;
        }
        int index = view.getMediaFiles().indexOf(currentFile);
        if (index >= 0 && index + 1 < view.getMediaFiles().size()) {
            loadMedia(view.getMediaFiles().get(index + 1), true);
        } else if (!view.getMediaFiles().isEmpty()) {
            loadMedia(view.getMediaFiles().get(0), true);
        }
    }

    private void playPrevious() {
        int index = view.getMediaFiles().indexOf(currentFile);
        if (index < 0) {
            index = view.getMediaFiles().indexOf(view.getPlaylist().getSelectionModel().getSelectedItem());
        }
        if (index > 0) {
            loadMedia(view.getMediaFiles().get(index - 1), true);
        } else if (index == 0 && mediaPlayer != null) {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
        } else if (!view.getMediaFiles().isEmpty()) {
            loadMedia(view.getMediaFiles().get(0), true);
        } else {
            view.getStatusLabel().setText("Playlist is empty");
        }
    }

    private void changeVolume(double amount) {
        double volume = view.getVolumeSlider().getValue() + amount;
        view.getVolumeSlider().setValue(Math.max(0, Math.min(1, volume)));
    }

    private void toggleMute() {
        if (view.getVolumeSlider().getValue() > 0) {
            previousVolume = view.getVolumeSlider().getValue();
            view.getVolumeSlider().setValue(0);
        } else {
            view.getVolumeSlider().setValue(previousVolume > 0 ? previousVolume : 0.7);
        }
    }

    private void replayCurrent() {
        if (mediaPlayer == null) {
            view.getStatusLabel().setText("No media is loaded");
            return;
        }
        mediaPlayer.seek(Duration.ZERO);
        mediaPlayer.play();
    }

    private void seekToSliderPosition() {
        if (mediaPlayer == null || view.getProgressSlider().isDisabled()) {
            return;
        }
        Duration duration = mediaPlayer.getTotalDuration();
        if (duration != null && !duration.isUnknown() && duration.greaterThan(Duration.ZERO)) {
            mediaPlayer.seek(duration.multiply(view.getProgressSlider().getValue()));
        }
    }

    private void clearCurrentMedia() {
        currentFile = null;
        if (videoFullscreenStage != null) {
            toggleVideoFullscreen();
        }
        view.setCurrentFile(null);
        view.setEmptyState();
        view.getMediaView().setMediaPlayer(null);
        view.getMediaView().setVisible(false);
        view.getDisplayMessage().setText("Add a media file to get started");
        view.getDisplayMessage().setVisible(true);
        view.getCurrentFileLabel().setText("Nothing playing");
        view.getCurrentTimeLabel().setText("0:00");
        view.getDurationLabel().setText("0:00");
        view.getProgressSlider().setValue(0);
        view.getProgressSlider().setDisable(true);
        view.getPlayPauseButton().setText("Play");
    }

    private void showPlaybackError(String message) {
        view.getStatusLabel().setText("Playback error: " + message);
        view.getDisplayMessage().setText("Cannot play this file: " + message);
        view.getDisplayMessage().setVisible(true);
        view.getProgressSlider().setDisable(true);
        view.getPlayPauseButton().setText("Play");
    }

    private String errorMessage(MediaException error) {
        return error == null || error.getMessage() == null
                ? "Unsupported or unreadable media file" : error.getMessage();
    }

    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.lessThan(Duration.ZERO)) {
            return "0:00";
        }
        long totalSeconds = (long) duration.toSeconds();
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private boolean isVideoFile(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".mp4") || name.endsWith(".m4v") || name.endsWith(".mov")
                || name.endsWith(".avi") || name.endsWith(".mkv") || name.endsWith(".webm");
    }

    private void disposePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        view.getMediaView().setMediaPlayer(null);
    }

    public void dispose() {
        if (videoFullscreenStage != null) {
            toggleVideoFullscreen();
        }
        disposePlayer();
    }
}
