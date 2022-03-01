/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.common.AppName;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Popup;
import javafx.stage.Stage;

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

	private static final PseudoClass errorTextField = PseudoClass.getPseudoClass("error");
	private static final PseudoClass warningTextField = PseudoClass.getPseudoClass("warning");

	private static final String KEY_LISTENER = "listener";
	private static final String KEY_POPUP = "popup";

	// XXX: fix later
	//public static ErrorResponseEntity getErrorResponseEntity(Throwable throwable)
	//{
	//	WebClientResponseException e = (WebClientResponseException) throwable;

	//	ErrorResponseEntity.Builder builder = new ErrorResponseEntity.Builder(e.getStatusCode());
	//	return builder.fromJson(e.getResponseBodyAsString());
	//}

	public static void showError(TextField field, String error)
	{
		field.pseudoClassStateChanged(errorTextField, true);

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
		field.pseudoClassStateChanged(errorTextField, false);
	}

	public static void showError(TextArea textArea)
	{
		textArea.pseudoClassStateChanged(errorTextField, true);
	}

	public static void showWarning(TextArea textArea)
	{
		textArea.pseudoClassStateChanged(warningTextField, true);
	}

	public static void clearError(TextArea textArea)
	{
		textArea.pseudoClassStateChanged(errorTextField, false);
	}

	public static void showAlertError(String title, String header, String message)
	{
		var errorAlert = new Alert(Alert.AlertType.ERROR);
		if (title != null)
		{
			errorAlert.setTitle(title);
		}
		if (header != null)
		{
			errorAlert.setHeaderText(header);
		}
		errorAlert.setContentText(message);
		errorAlert.showAndWait();
	}

	public static void showAlertInfo(String message)
	{
		showAlertInfo(AppName.NAME, null, message);
	}

	public static void showAlertInfo(String title, String header, String message)
	{
		var errorAlert = new Alert(Alert.AlertType.INFORMATION);
		errorAlert.setTitle(title);
		errorAlert.setHeaderText(header);
		errorAlert.setContentText(message);
		errorAlert.showAndWait();
	}

	public static void showAlertInfoConfirm(String title, String header, String message, Runnable runnable)
	{
		var alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(message);
		alert.showAndWait()
				.filter(response -> response == ButtonType.OK)
				.ifPresent(response -> runnable.run());
	}

	public static void setDefaultIcon(Stage stage)
	{
		stage.getIcons().add(new Image(Objects.requireNonNull(stage.getClass().getResourceAsStream("/image/icon.png"))));
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
	 * <pre>
	 *     closeButton.setOnAction(UiUtils::closeWindow);
	 * </pre>
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
}
