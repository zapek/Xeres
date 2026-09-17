/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.ui.support.chess;

import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.common.rest.PathConfig;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.ChessClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.custom.asyncimage.PlaceholderImageView;
import io.xeres.ui.support.util.Requester;
import io.xeres.ui.support.util.UiUtils;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/// Desktop toaster notification for incoming chess invitations.
public class ChessInvitationToaster
{
	private static final Logger log = LoggerFactory.getLogger(ChessInvitationToaster.class);

	private static final double TOAST_WIDTH = 350;
	private static final double TOAST_HEIGHT = 125;
	private static final int AUTO_CLOSE_SECONDS = 25;

	private static final String GREEN_BUTTON_NORMAL =
			"-fx-background-color: linear-gradient(to bottom, #32b34a 0%, #1e8734 100%);" +
			"-fx-text-fill: white;" +
			"-fx-font-weight: bold;" +
			"-fx-font-size: 13px;" +
			"-fx-background-radius: 5px;" +
			"-fx-padding: 5px 14px;" +
			"-fx-cursor: hand;" +
			"-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 3, 0, 0, 1);";

	private static final String GREEN_BUTTON_HOVER =
			"-fx-background-color: linear-gradient(to bottom, #38c853 0%, #239c3d 100%);" +
			"-fx-text-fill: white;" +
			"-fx-font-weight: bold;" +
			"-fx-font-size: 13px;" +
			"-fx-background-radius: 5px;" +
			"-fx-padding: 5px 14px;" +
			"-fx-cursor: hand;" +
			"-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 4, 0, 0, 2);";

	private static final String CLOSE_BUTTON_NORMAL =
			"-fx-background-color: transparent;" +
			"-fx-text-fill: -color-fg-muted;" +
			"-fx-font-size: 11px;" +
			"-fx-font-weight: bold;" +
			"-fx-padding: 2px 7px;" +
			"-fx-cursor: hand;" +
			"-fx-background-radius: 4px;";

	private static final String CLOSE_BUTTON_HOVER =
			"-fx-background-color: #dc3545;" +
			"-fx-text-fill: white;" +
			"-fx-font-size: 11px;" +
			"-fx-font-weight: bold;" +
			"-fx-padding: 2px 7px;" +
			"-fx-cursor: hand;" +
			"-fx-background-radius: 4px;";

	private final Stage stage;
	private final VBox root;
	private final PauseTransition autoCloseTimer;
	private final Runnable onCloseCallback;
	private boolean closing;

	public ChessInvitationToaster(ChessGameDTO game,
			ChessClient chessClient,
			GeneralClient generalClient,
			ImageCache imageCache,
			ResourceBundle bundle,
			int stackIndex,
			Consumer<ChessGameDTO> onOpen,
			Runnable onCloseCallback)
	{
		this.onCloseCallback = onCloseCallback;

		stage = new Stage();
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setAlwaysOnTop(true);
		stage.setResizable(false);
		stage.setTitle(bundle.getString("chess.invitation-toast.title"));

		root = new VBox(6);
		root.setPrefWidth(TOAST_WIDTH);
		root.setMaxWidth(TOAST_WIDTH);
		root.setPrefHeight(TOAST_HEIGHT);
		root.setMaxHeight(TOAST_HEIGHT);
		root.setPadding(new Insets(8, 10, 10, 10));
		root.getStyleClass().add("card");
		root.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 6px; -fx-border-color: -color-border-default; -fx-border-width: 1px; -fx-border-radius: 6px;");
		root.setEffect(new DropShadow(8, 0, 2, Color.rgb(0, 0, 0, 0.3)));

		// Header: Xeres app icon, Title, Spacer, Close [X] button
		var iconStream = ChessInvitationToaster.class.getResourceAsStream("/image/icon.png");
		var titleIcon = new ImageView();
		if (iconStream != null)
		{
			titleIcon.setImage(new Image(iconStream));
		}
		titleIcon.setFitWidth(18);
		titleIcon.setFitHeight(18);
		titleIcon.setPreserveRatio(true);

		var titleLabel = new Label(bundle.getString("chess.invitation-toast.title"));
		titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-fg-default;");

		var spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		var closeButton = new Button("✕");
		closeButton.setStyle(CLOSE_BUTTON_NORMAL);
		closeButton.setOnMouseEntered(_ -> closeButton.setStyle(CLOSE_BUTTON_HOVER));
		closeButton.setOnMouseExited(_ -> closeButton.setStyle(CLOSE_BUTTON_NORMAL));
		closeButton.setOnAction(event -> {
			event.consume();
			closeButton.setDisable(true);
			chessClient.action(game.peer(), "decline").subscribe(
					_ -> Platform.runLater(this::closeImmediately),
					_ -> Platform.runLater(this::closeImmediately)
			);
		});

		var header = new HBox(6, titleIcon, titleLabel, spacer, closeButton);
		header.setAlignment(Pos.CENTER_LEFT);

		// Body: Avatar on left, Message & Green Accept button on right
		var avatar = new PlaceholderImageView(url -> generalClient.getImage(url).block(), "mdi2a-account", imageCache);
		avatar.setFitWidth(68);
		avatar.setFitHeight(68);
		avatar.setPreserveRatio(true);
		avatar.setUrl(RemoteUtils.getControlUrl() + PathConfig.IDENTITIES_PATH + "/image?find=true&gxsId=" + game.peer());

		var avatarFrame = new StackPane(avatar);
		avatarFrame.setPrefSize(72, 72);
		avatarFrame.setMaxSize(72, 72);
		avatarFrame.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-subtle; -fx-border-width: 1px; -fx-border-radius: 2px; -fx-padding: 2px;");

		var messageBox = new VBox(6);
		HBox.setHgrow(messageBox, Priority.ALWAYS);
		messageBox.setAlignment(Pos.CENTER_LEFT);

		var messageLabel = new Label(MessageFormat.format(bundle.getString("chess.invitation-toast.message"), game.name()));
		messageLabel.setWrapText(true);
		messageLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: -color-fg-default;");

		var acceptButton = new Button(bundle.getString("chess.invitation-toast.accept"));
		acceptButton.setMaxWidth(Double.MAX_VALUE);
		acceptButton.setStyle(GREEN_BUTTON_NORMAL);
		acceptButton.setOnMouseEntered(_ -> acceptButton.setStyle(GREEN_BUTTON_HOVER));
		acceptButton.setOnMouseExited(_ -> acceptButton.setStyle(GREEN_BUTTON_NORMAL));
		acceptButton.setOnAction(event -> {
			event.consume();
			acceptButton.setDisable(true);
			chessClient.action(game.peer(), "accept").subscribe(
					acceptedGame -> Platform.runLater(() -> {
						closeImmediately();
						onOpen.accept(acceptedGame);
					}),
					failure -> Platform.runLater(() -> {
						closeImmediately();
						Requester.showError(bundle.getString("chess.error") + " " + failure.getMessage());
					})
			);
		});

		messageBox.getChildren().addAll(messageLabel, acceptButton);

		var content = new HBox(10, avatarFrame, messageBox);
		content.setAlignment(Pos.CENTER_LEFT);
		VBox.setVgrow(content, Priority.ALWAYS);

		root.getChildren().addAll(header, content);

		// Clicking on the toast body outside buttons opens the game window
		root.setOnMouseClicked(event -> {
			if (!event.isConsumed())
			{
				close();
				onOpen.accept(game);
			}
		});

		// Auto-close timer with pause on hover
		autoCloseTimer = new PauseTransition(Duration.seconds(AUTO_CLOSE_SECONDS));
		autoCloseTimer.setOnFinished(_ -> close());

		root.setOnMouseEntered(_ -> autoCloseTimer.pause());
		root.setOnMouseExited(_ -> autoCloseTimer.play());

		var scene = new Scene(root);
		UiUtils.setDefaultStyle(scene);
		stage.setScene(scene);

		// Calculate screen coordinates (bottom right of visual bounds)
		positionStage(stackIndex);
	}

	private void positionStage(int stackIndex)
	{
		var visualBounds = Screen.getPrimary().getVisualBounds();
		double margin = 16;
		double x = visualBounds.getMaxX() - TOAST_WIDTH - margin;
		double y = visualBounds.getMaxY() - TOAST_HEIGHT - margin - (stackIndex * (TOAST_HEIGHT + 10));

		stage.setX(x);
		stage.setY(y);
	}

	/// Shows the toast notification with a fade-in transition.
	public void show()
	{
		root.setOpacity(0.0);
		stage.show();
		autoCloseTimer.play();

		var fadeIn = new FadeTransition(Duration.millis(200), root);
		fadeIn.setFromValue(0.0);
		fadeIn.setToValue(1.0);
		fadeIn.play();
	}

	/// Closes the toast notification immediately without fade animation.
	public void closeImmediately()
	{
		if (closing)
		{
			return;
		}
		closing = true;
		autoCloseTimer.stop();
		stage.close();
		if (onCloseCallback != null)
		{
			onCloseCallback.run();
		}
	}

	/// Closes the toast notification with a fade-out transition.
	public void close()
	{
		if (closing)
		{
			return;
		}
		closing = true;
		autoCloseTimer.stop();

		var fadeOut = new FadeTransition(Duration.millis(180), root);
		fadeOut.setFromValue(root.getOpacity());
		fadeOut.setToValue(0.0);
		fadeOut.setOnFinished(_ -> {
			stage.close();
			if (onCloseCallback != null)
			{
				onCloseCallback.run();
			}
		});
		fadeOut.play();
	}
}
