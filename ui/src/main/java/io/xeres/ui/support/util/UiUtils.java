/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import io.xeres.common.rest.ErrorResponseEntity;
import io.xeres.ui.JavaFxApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

public final class UiUtils
{
	private UiUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static final PseudoClass dangerPseudoClass = PseudoClass.getPseudoClass("danger");

	private static final String KEY_LISTENER = "listener";
	private static final String KEY_POPUP = "popup";

	/**
	 * Builds an {@code ErrorResponseEntity} from a {@code WebClientResponseException}. Use in {@code doOnError} in the WebClients.
	 *
	 * @param e the WebClientResponseException
	 * @return an ErrorResponseEntity
	 */
	public static ErrorResponseEntity getErrorResponseEntity(WebClientResponseException e)
	{
		ErrorResponseEntity.Builder builder = new ErrorResponseEntity.Builder(e.getStatusCode());
		return builder.fromJson(e.getResponseBodyAsString());
	}

	public static void showAlertError(WebClientResponseException e)
	{
		Platform.runLater(() -> {
			var error = getErrorResponseEntity(e);
			alert(AlertType.ERROR, error.getMessage()); // XXX: use getDetails()? it contains the first exception message
		});
	}

	public static void showError(TextField field, String error)
	{
		field.pseudoClassStateChanged(dangerPseudoClass, true);

		var label = new Label();
		label.setText(error);
		label.setStyle("-fx-border-color: black; -fx-background-color: white;"); // XXX: temporary. we should probably have some proper layout somewhere
		var popup = new Popup();
		popup.getContent().add(label);
		var bounds = field.getBoundsInLocal();
		var location = field.localToScreen(bounds.getMinX(), bounds.getMaxY());
		popup.show(field, location.getX(), location.getY());
		popup.setAutoHide(true);
		field.getProperties().put(KEY_POPUP, popup);

		var listener = (InvalidationListener) observable -> clearError(field);
		field.getProperties().put(KEY_LISTENER, listener);
		field.textProperty().addListener(listener);
	}

	public static void clearError(TextField field)
	{
		var listener = (InvalidationListener) field.getProperties().get(KEY_LISTENER);
		if (listener != null)
		{
			field.textProperty().removeListener(listener);
		}

		var popup = (Popup) field.getProperties().get(KEY_POPUP);
		if (popup != null)
		{
			popup.hide();
		}
		field.pseudoClassStateChanged(dangerPseudoClass, false);
	}

	public static void showError(Node... nodes)
	{
		for (var node : nodes)
		{
			node.pseudoClassStateChanged(dangerPseudoClass, true);
		}
	}

	public static void clearError(Node... nodes)
	{
		for (var node : nodes)
		{
			node.pseudoClassStateChanged(dangerPseudoClass, false);
		}
	}

	public static void alert(AlertType alertType, String message)
	{
		var alert = buildAlert(alertType, message);
		alert.showAndWait();
	}

	public static void alertConfirm(String message, Runnable runnable)
	{
		var alert = buildAlert(AlertType.CONFIRMATION, message);
		alert.showAndWait()
				.filter(response -> response == ButtonType.OK)
				.ifPresent(response -> runnable.run());
	}

	private static Alert buildAlert(AlertType alertType, String message)
	{
		var alert = new Alert(alertType);
		var stage = (Stage) alert.getDialogPane().getScene().getWindow();

		UiUtils.setDefaultIcon(stage); // required for the window's title bar icon
		UiUtils.setDefaultStyle(stage.getScene()); // required for the default styles being applied
		alert.setHeaderText(null); // the header is ugly
		alert.setContentText(message);
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); // Without this, long texts get truncated. Go figure why this isn't the default...
		return alert;
	}

	public static void setDefaultIcon(Stage stage)
	{
		stage.getIcons().add(new Image(Objects.requireNonNull(stage.getClass().getResourceAsStream("/image/icon.png"))));
	}

	public static void setDefaultStyle(Scene scene)
	{
		if (Application.getUserAgentStylesheet() == null) // default, so modena
		{
			scene.getStylesheets().add("/view/javafx.css");
		}
		else
		{
			scene.getStylesheets().add("/view/javafx-atlanta.css");
		}
		//scene.getStylesheets().add("/view/javafx-dark.css");
	}

	/**
	 * Reads a text file and returns it as a string, preserving line endings.
	 *
	 * @param in an input stream
	 * @return the text file
	 * @throws IOException I/O error
	 */
	public static String getResourceFileAsString(InputStream in) throws IOException
	{
		try (var isr = new InputStreamReader(in);
		     var reader = new BufferedReader(isr))
		{
			return reader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	/**
	 * Sets a close window actions easily, for example:
	 * {@snippet :
	 *     closeButton.setOnAction(UiUtils::closeWindow);
	 *}
	 * Beware because not all events contain a node (for example, events from MenuItems).
	 *
	 * @param event the event which needs a node in its source
	 */
	public static void closeWindow(ActionEvent event)
	{
		closeWindow((Node) event.getSource());
	}

	/**
	 * Closes a window using a node.
	 *
	 * @param node the node
	 */
	public static void closeWindow(Node node)
	{
		var stage = (Stage) node.getScene().getWindow();
		stage.close();
	}

	public static Object getUserData(Node node)
	{
		return node.getScene().getRoot().getUserData();
	}

	/**
	 * Makes Hyperlinks actually do something. Slightly recursive.
	 *
	 * @param rootNode the parent node where the hyperlinks are
	 */
	public static void linkify(Node rootNode)
	{
		if (rootNode instanceof TabPane tabPane)
		{
			tabPane.getTabs().forEach(tab -> linkify(tab.getContent()));
		}
		else if (rootNode instanceof ScrollPane scrollPane)
		{
			linkify(scrollPane.getContent());
		}
		else if (rootNode instanceof Parent parent)
		{
			parent.getChildrenUnmodifiable().forEach(UiUtils::linkify);
		}

		if (rootNode instanceof Hyperlink hyperlink)
		{
			if (hyperlink.getOnAction() == null)
			{
				hyperlink.setOnAction(event -> JavaFxApplication.openUrl(hyperlink.getText().contains("@") ? ("mailto:" + hyperlink.getText()) : hyperlink.getText()));
			}
		}
	}

	/**
	 * Gets the window from an event, handles MenuItems as well.
	 *
	 * @param event the event
	 * @return a Window
	 */
	public static Window getWindow(Event event)
	{
		var target = Objects.requireNonNull(event.getTarget(), "event has no target");

		if (target instanceof MenuItem menuItem)
		{
			return menuItem.getParentMenu().getParentPopup().getOwnerWindow();
		}
		else if (target instanceof Node node)
		{
			return getWindow(node);
		}
		else
		{
			throw new IllegalStateException("Cannot find a window from the event " + event);
		}
	}

	public static Window getWindow(Node node)
	{
		return node.getScene().getWindow();
	}
}
