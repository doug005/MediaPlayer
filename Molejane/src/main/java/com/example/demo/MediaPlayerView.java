package com.example.demo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.util.Duration;

import java.io.File;

public class MediaPlayerView {
    private static final String ACCENT = "#f0b64f";
    private static final String TEXT = "#f8f2ef";
    private static final String MUTED = "#c4b5b4";
    private final BorderPane root = new BorderPane();
    private final ObservableList<File> mediaFiles = FXCollections.observableArrayList();
    private final FilteredList<File> filteredFiles = new FilteredList<>(mediaFiles, file -> true);
    private final ListView<File> playlist = new ListView<>(filteredFiles);
    private final VBox playlistPanel = new VBox(12);
    private final StackPane mediaArea = new StackPane();
    private final MediaView mediaView = new MediaView();
    private final Label displayMessage = new Label("Add media files to start playing");
    private final Label currentFileLabel = new Label("Nothing playing");
    private final Label statusLabel = new Label("Ready");
    private final Label currentTimeLabel = new Label("0:00");
    private final Label durationLabel = new Label("0:00");
    private final Label playlistCountLabel = new Label("0 tracks");
    private final Slider progressSlider = new Slider(0, 1, 0);
    private final Slider volumeSlider = new Slider(0, 1, 0.7);
    private final Button addButton = new Button("+  Add files");
    private final Button removeButton = new Button("Remove");
    private final Button previousButton = new Button("|◀");
    private final Button playPauseButton = new Button("▶");
    private final Button stopButton = new Button("■");
    private final Button nextButton = new Button("▶|");
    private final Button replayButton = new Button("↻");
    private final Button muteButton = new Button("🔊");
    private final Button fullscreenButton = new Button("⤢");
    private final TextField searchField = new TextField();
    private final CheckBox autoplayCheckBox = new CheckBox("Autoplay next");
    private final Label nowPlayingHeading = new Label("NOW PLAYING");
    private File currentFile;

    public MediaPlayerView() {
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #641d2b 0%, #451923 52%, #211317 100%);");
        root.setTop(createHeader());
        root.setCenter(createMainContent());
        root.setBottom(createPlaybackControls());
        stylePlaylist();
        mediaFiles.addListener((javafx.collections.ListChangeListener<File>) change -> updatePlaylistCount());
    }

    private HBox createHeader() {
        Label brandMark = new Label("◈");
        brandMark.setTextFill(Color.web(ACCENT));
        brandMark.setFont(Font.font("System", FontWeight.BOLD, 27));
        Label pageTitle = new Label("PulseWave");
        pageTitle.setTextFill(Color.web(TEXT));
        pageTitle.setFont(Font.font("System", FontWeight.BOLD, 23));
        Label subtitle = new Label("LOCAL PLAYER");
        subtitle.setTextFill(Color.web("#d9aeb0"));
        subtitle.setFont(Font.font("System", FontWeight.BOLD, 9));
        VBox brand = new VBox(1, pageTitle, subtitle);
        HBox logo = new HBox(9, brandMark, brand);
        logo.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search your playlist");
        searchField.setMaxWidth(310);
        searchField.setStyle("-fx-background-color: rgba(25,9,13,0.34); -fx-text-fill: " + TEXT
            + "; -fx-prompt-text-fill: #d0aeb0; -fx-background-radius: 18; -fx-padding: 10 16;");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(java.util.Locale.ROOT);
            filteredFiles.setPredicate(file -> query.isEmpty()
                    || file.getName().toLowerCase(java.util.Locale.ROOT).contains(query));
        });
        styleButton(addButton, true);
        addButton.setAccessibleText("Add audio or video files");

        HBox header = new HBox(26, logo, searchField, addButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(19, 30, 19, 30));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        header.setStyle("-fx-background-color: rgba(69,18,29,0.52); -fx-border-color: rgba(255,255,255,0.08); "
                + "-fx-border-width: 0 0 1 0;");
        return header;
    }

    private HBox createMainContent() {
        mediaArea.setStyle("-fx-background-color: linear-gradient(to bottom right, #9a263d 0%, #511524 38%, #120d12 100%); "
            + "-fx-background-radius: 8; -fx-border-color: rgba(255,220,201,0.13); -fx-border-radius: 8;");
        mediaArea.setMinSize(0, 280);

        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(mediaArea.widthProperty());
        mediaView.fitHeightProperty().bind(mediaArea.heightProperty());
        mediaView.setVisible(false);

        displayMessage.setTextFill(Color.web(TEXT));
        displayMessage.setFont(Font.font("System", FontWeight.BOLD, 16));
        displayMessage.setStyle("-fx-background-color: rgba(28,10,16,0.48); -fx-background-radius: 8; "
            + "-fx-padding: 12 18;");
        nowPlayingHeading.setTextFill(Color.web(ACCENT));
        nowPlayingHeading.setFont(Font.font("System", FontWeight.BOLD, 10));

        VBox presentation = new VBox(12, nowPlayingHeading, mediaArea);
        presentation.setPadding(new Insets(26, 28, 24, 30));
        VBox.setVgrow(mediaArea, Priority.ALWAYS);

        styleButton(fullscreenButton, false);
        fullscreenButton.setAccessibleText("Toggle video fullscreen");
        fullscreenButton.setTooltip(new Tooltip("Toggle fullscreen"));
        fullscreenButton.setVisible(false);
        fullscreenButton.setManaged(false);
        StackPane.setAlignment(fullscreenButton, Pos.TOP_RIGHT);
        StackPane.setMargin(fullscreenButton, new Insets(14));
        mediaArea.getChildren().addAll(mediaView, displayMessage, fullscreenButton);

        buildPlaylistPanel();
        HBox content = new HBox(presentation, playlistPanel);
        HBox.setHgrow(presentation, Priority.ALWAYS);
        return content;
    }

    private void buildPlaylistPanel() {
        Label title = new Label("Playlist");
        title.setTextFill(Color.web(TEXT));
        title.setFont(Font.font("System", FontWeight.BOLD, 17));
        playlistCountLabel.setTextFill(Color.web(MUTED));
        playlistCountLabel.setFont(Font.font("System", 11));
        styleButton(removeButton, false);
        removeButton.setAccessibleText("Remove selected playlist item");

        HBox heading = new HBox(title, removeButton);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        playlist.setPlaceholder(new Label("Add media files to your playlist"));
        VBox.setVgrow(playlist, Priority.ALWAYS);
        autoplayCheckBox.setTextFill(Color.web(MUTED));
        autoplayCheckBox.setSelected(true);
        autoplayCheckBox.setFont(Font.font("System", 11));
        playlistPanel.getChildren().addAll(heading, playlistCountLabel, playlist, autoplayCheckBox);
        playlistPanel.setPadding(new Insets(24, 28, 18, 18));
        playlistPanel.setPrefWidth(390);
        playlistPanel.setMinWidth(300);
        playlistPanel.setMaxWidth(460);
        playlistPanel.setStyle("-fx-background-color: rgba(47,17,25,0.48); -fx-border-color: rgba(255,255,255,0.08); "
                + "-fx-border-width: 0 0 0 1;");
    }

    private VBox createPlaybackControls() {
        currentFileLabel.setTextFill(Color.web(TEXT));
        currentFileLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        statusLabel.setTextFill(Color.web(MUTED));
        statusLabel.setFont(Font.font("System", 10));
        VBox trackInfo = new VBox(4, currentFileLabel, statusLabel);
        trackInfo.setMinWidth(150);
        trackInfo.setMaxWidth(270);

        progressSlider.setMaxWidth(Double.MAX_VALUE);
        progressSlider.setDisable(true);
        progressSlider.setStyle("-fx-accent: " + ACCENT + ";");
        currentTimeLabel.setTextFill(Color.web(TEXT));
        durationLabel.setTextFill(Color.web(MUTED));
        currentTimeLabel.setFont(Font.font("System", 10));
        durationLabel.setFont(Font.font("System", 10));
        HBox progressRow = new HBox(10, currentTimeLabel, progressSlider, durationLabel);
        progressRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        styleButton(previousButton, false);
        styleButton(playPauseButton, true);
        styleButton(stopButton, false);
        styleButton(nextButton, false);
        styleButton(replayButton, false);
        previousButton.setAccessibleText("Play previous media file");
        playPauseButton.setAccessibleText("Play or pause");
        stopButton.setAccessibleText("Stop playback");
        nextButton.setAccessibleText("Play next media file");
        replayButton.setAccessibleText("Replay current track");
        HBox transport = new HBox(8, previousButton, playPauseButton, stopButton, nextButton, replayButton);
        transport.setAlignment(Pos.CENTER);

        styleButton(muteButton, false);
        muteButton.setAccessibleText("Mute or unmute");
        volumeSlider.setPrefWidth(120);
        volumeSlider.setStyle("-fx-accent: " + ACCENT + ";");
        HBox volume = new HBox(8, muteButton, volumeSlider);
        volume.setAlignment(Pos.CENTER_RIGHT);

        HBox bottomRow = new HBox(18, trackInfo, transport, volume);
        bottomRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(trackInfo, Priority.ALWAYS);
        VBox controls = new VBox(8, progressRow, bottomRow);
        controls.setPadding(new Insets(14, 30, 18, 30));
        controls.setStyle("-fx-background-color: linear-gradient(to bottom, #251318, #160f12); "
            + "-fx-border-color: rgba(255,255,255,0.09); "
                + "-fx-border-width: 1 0 0 0;");
        return controls;
    }

    private void stylePlaylist() {
        playlist.setCellFactory(list -> new ListCell<>() {
            private final RotateTransition artworkSpin = new RotateTransition(Duration.seconds(5));

            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                artworkSpin.stop();
                if (empty || file == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                StackPane artwork = new StackPane();
                artwork.setMinSize(48, 48);
                artwork.setPrefSize(48, 48);
                artwork.setMaxSize(48, 48);
                artwork.setStyle("-fx-background-color: linear-gradient(to bottom right, #c13c4b, #671c30 52%, #21121b); "
                    + "-fx-background-radius: 24; -fx-border-color: rgba(255,255,255,0.16); -fx-border-radius: 24;");
                artwork.setRotate(0);
                Label coverMark = new Label(file.equals(currentFile) ? "▶" : "✦");
                coverMark.setTextFill(Color.web(file.equals(currentFile) ? TEXT : "#f2b85a"));
                coverMark.setFont(Font.font("System", FontWeight.BOLD, 15));
                artwork.getChildren().add(coverMark);
                if (file.equals(currentFile)) {
                    artworkSpin.setNode(artwork);
                    artworkSpin.setFromAngle(0);
                    artworkSpin.setToAngle(360);
                    artworkSpin.setInterpolator(Interpolator.LINEAR);
                    artworkSpin.setCycleCount(RotateTransition.INDEFINITE);
                    artworkSpin.play();
                }
                Label name = new Label(file.getName());
                name.setTextFill(Color.web(file.equals(currentFile) ? ACCENT : TEXT));
                name.setFont(Font.font("System", FontWeight.BOLD, 12));
                name.setMaxWidth(Double.MAX_VALUE);
                name.setEllipsisString("…");
                HBox row = new HBox(13, artwork, name);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(9, 11, 9, 9));
                HBox.setHgrow(name, Priority.ALWAYS);
                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color: " + (isSelected()
                        ? "rgba(255,213,161,0.13)" : "transparent")
                        + "; -fx-background-radius: 7;");
            }
        });
        playlist.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) ->
                playlist.refresh());
        playlist.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; "
                + "-fx-background-insets: 0; -fx-border-color: transparent; -fx-padding: 0;");
    }

    private void updatePlaylistCount() {
        int count = mediaFiles.size();
        playlistCountLabel.setText(count + (count == 1 ? " track" : " tracks"));
    }

    private void styleButton(Button button, boolean primary) {
        button.setTextFill(Color.web(primary ? "#17110f" : TEXT));
        button.setStyle("-fx-background-color: " + (primary ? ACCENT : "rgba(255,255,255,0.11)")
            + "; -fx-background-radius: 18; -fx-padding: 9 14; -fx-cursor: hand;");
    }

    public BorderPane getRoot() { return root; }
    public ListView<File> getPlaylist() { return playlist; }
    public ObservableList<File> getMediaFiles() { return mediaFiles; }
    public CheckBox getAutoplayCheckBox() { return autoplayCheckBox; }
    public Button getMuteButton() { return muteButton; }
    public Button getReplayButton() { return replayButton; }

    public void setCurrentFile(File file) {
        currentFile = file;
        playlist.refresh();
    }

    public void setVideoMode(boolean video) {
        mediaView.setVisible(video);
        fullscreenButton.setVisible(video);
        fullscreenButton.setManaged(video);
        displayMessage.setVisible(!video);
    }

    public void showVideoFullscreen(StackPane fullscreenArea) {
        mediaArea.getChildren().removeAll(mediaView, fullscreenButton);
        fullscreenArea.getChildren().setAll(mediaView, fullscreenButton);
        mediaView.fitWidthProperty().unbind();
        mediaView.fitHeightProperty().unbind();
        mediaView.fitWidthProperty().bind(fullscreenArea.widthProperty());
        mediaView.fitHeightProperty().bind(fullscreenArea.heightProperty());
        fullscreenButton.setText("⤡");
    }

    public void restoreVideoFromFullscreen(StackPane fullscreenArea) {
        fullscreenArea.getChildren().removeAll(mediaView, fullscreenButton);
        mediaArea.getChildren().setAll(mediaView, displayMessage, fullscreenButton);
        mediaView.fitWidthProperty().unbind();
        mediaView.fitHeightProperty().unbind();
        mediaView.fitWidthProperty().bind(mediaArea.widthProperty());
        mediaView.fitHeightProperty().bind(mediaArea.heightProperty());
        fullscreenButton.setText("⤢");
    }

    public void setEmptyState() {
        mediaView.setVisible(false);
        fullscreenButton.setVisible(false);
        fullscreenButton.setManaged(false);
        displayMessage.setText("Add media files to start playing");
        displayMessage.setVisible(true);
    }

    public MediaView getMediaView() { return mediaView; }
    public Label getDisplayMessage() { return displayMessage; }
    public Label getCurrentFileLabel() { return currentFileLabel; }
    public Label getStatusLabel() { return statusLabel; }
    public Label getCurrentTimeLabel() { return currentTimeLabel; }
    public Label getDurationLabel() { return durationLabel; }
    public Slider getProgressSlider() { return progressSlider; }
    public Slider getVolumeSlider() { return volumeSlider; }
    public Button getAddButton() { return addButton; }
    public Button getRemoveButton() { return removeButton; }
    public Button getPreviousButton() { return previousButton; }
    public Button getPlayPauseButton() { return playPauseButton; }
    public Button getStopButton() { return stopButton; }
    public Button getNextButton() { return nextButton; }
    public Button getFullscreenButton() { return fullscreenButton; }
}
