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

import atlantafx.base.theme.Styles;
import io.xeres.common.AppName;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.support.uri.UriService;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.xeres.ui.support.util.DateUtils.DATE_TIME_DISPLAY;
import static javafx.scene.control.Alert.AlertType.ERROR;
import static javafx.scene.control.Alert.AlertType.WARNING;

public final class UiUtils
{
	private static final Logger log = LoggerFactory.getLogger(UiUtils.class);

	private UiUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static final PseudoClass dangerPseudoClass = PseudoClass.getPseudoClass("danger");

	/**
	 * Shows a generic alert error. Is supposed to be used in {@code doOnError} in the WebClients.
	 *
	 * @param t the throwable
	 */
	public static void showAlertError(Throwable t)
	{
		Platform.runLater(() -> {
			if (t instanceof WebClientResponseException e)
			{
				var problem = e.getResponseBodyAs(ProblemDetail.class);
				String title;
				String detail;
				String stackTrace = null;

				if (problem != null)
				{
					title = problem.getTitle();
					detail = problem.getDetail();
					var properties = problem.getProperties();
					if (properties != null)
					{
						stackTrace = (String) properties.get("trace");
					}
				}
				else
				{
					title = "Error";
					detail = "Unknown error";
				}
				alert(e.getStatusCode().isError() ? ERROR : WARNING, title, detail, stackTrace);
			}
			else
			{
				alert(ERROR, "Error", t.getMessage(), ExceptionUtils.getStackTrace(t));
			}
			log.error("Error: {}", t.getMessage(), t);
		});
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

	public static void alert(AlertType alertType, String title, String message, String stackTrace)
	{
		var alert = buildAlert(alertType, title, message, stackTrace);
		alert.showAndWait();
	}

	public static void alert(AlertType alertType, String message)
	{
		var alert = buildAlert(alertType, null, message, null);
		alert.showAndWait();
	}

	public static void alertConfirm(String message, Runnable runnable)
	{
		var alert = buildAlert(AlertType.CONFIRMATION, null, message, null);
		alert.showAndWait()
				.filter(response -> response == ButtonType.OK)
				.ifPresent(response -> runnable.run());
	}

	private static Alert buildAlert(AlertType alertType, String title, String message, String stackTrace)
	{
		var alert = new Alert(alertType);
		var stage = (Stage) alert.getDialogPane().getScene().getWindow();

		UiUtils.setDefaultIcon(stage); // required for the window's title bar icon
		UiUtils.setDefaultStyle(stage.getScene()); // required for the default styles being applied
		// Setting dark borders doesn't work because dialogs aren't in JavaFX' built-in windows list
		if (title != null)
		{
			alert.setTitle(title);
		}
		alert.setHeaderText(null); // the header is ugly

		// The default doesn't allow cut & pasting and doesn't have scrollbars when needed,
		// so instead we use a TextArea with similar styling.
		var vbox = new VBox();
		var hbox = new HBox();
		hbox.setAlignment(Pos.TOP_RIGHT);
		var copyButton = new Button(null, new FontIcon(MaterialDesignC.CLIPBOARD_OUTLINE));
		copyButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
		TooltipUtils.install(copyButton, "Copy as a bug report to the clipboard");
		hbox.getChildren().add(copyButton);

		var textArea = new TextArea();
		textArea.setWrapText(true);
		textArea.setEditable(false);
		textArea.setText(message);
		textArea.getStyleClass().add("alert-textarea");
		textArea.setPrefHeight(StringUtils.defaultString(message).length() < 120 ? 60 : 100); // Should be good enough
		vbox.setPadding(new Insets(14.0));
		vbox.getChildren().addAll(hbox, textArea);
		alert.getDialogPane().setContent(vbox);

		if (stackTrace != null)
		{
			var ssTextArea = new TextArea(stackTrace);
			ssTextArea.setWrapText(false);
			ssTextArea.setEditable(false);
			ssTextArea.setMaxWidth(Double.MAX_VALUE);
			ssTextArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setHgrow(ssTextArea, Priority.ALWAYS);
			GridPane.setVgrow(ssTextArea, Priority.ALWAYS);

			var content = new GridPane();
			content.setMaxWidth(Double.MAX_VALUE);
			content.add(new Label("Full stacktrace:"), 0, 0);
			content.add(ssTextArea, 0, 1);

			alert.getDialogPane().setExpandableContent(content);
		}
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); // Without this, long texts get truncated. Go figure why this isn't the default...

		copyButton.setOnAction(event -> {
			var clipboardContent = new ClipboardContent();
			clipboardContent.putString(generateAlertErrorString(alertType, title, message, stackTrace));
			Clipboard.getSystemClipboard().setContent(clipboardContent);
		});

		return alert;
	}

	private static String generateAlertErrorString(AlertType alertType, String title, String message, String stackTrace)
	{
		String version;
		var resource = UiUtils.class.getClassLoader().getResourceAsStream("META-INF/build-info.properties");

		if (resource != null)
		{
			var buildInfo = new BufferedReader(new InputStreamReader(resource));
			version = buildInfo.lines()
					.filter(s -> s.startsWith("build.version="))
					.map(s -> s.substring("build.version=".length()))
					.findFirst().orElse("unknown");
		}
		else
		{
			version = "unknown";
		}

		return AppName.NAME + " Requester Error Report\n\nVersion: " + version +
				"\nTime: " +
				DATE_TIME_DISPLAY.format(Instant.now()) +
				"\nType: " + (alertType == ERROR ? "Error" : "Warning") +
				"\nTitle: " +
				title +
				"\n\n" +
				message +
				"\n\nStack Trace:\n" +
				stackTrace +
				"\n\n";
	}

	public static void setDefaultIcon(Stage stage)
	{
		stage.getIcons().add(new Image(Objects.requireNonNull(stage.getClass().getResourceAsStream("/image/icon.png"))));
	}

	public static void setDefaultStyle(Scene scene)
	{
		scene.getStylesheets().add("/view/default.css");
		if (SystemUtils.IS_OS_WINDOWS)
		{
			scene.getStylesheets().add("/view/windows.css");
		}
		else if (SystemUtils.IS_OS_LINUX)
		{
			scene.getStylesheets().add("/view/linux.css");
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			scene.getStylesheets().add("/view/mac.css");
		}
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

		if (rootNode instanceof DisclosedHyperlink disclosedHyperlink)
		{
			if (disclosedHyperlink.getOnAction() == null)
			{
				disclosedHyperlink.setOnAction(event -> UriService.openUri(disclosedHyperlink.getUri()));
			}
		}
		else if (rootNode instanceof Hyperlink hyperlink && hyperlink.getOnAction() == null)
		{
			hyperlink.setOnAction(event -> UriService.openUri(hyperlink.getText().contains("@") ? ("mailto:" + hyperlink.getText()) : hyperlink.getText()));
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

		switch (target)
		{
			case MenuItem menuItem ->
			{
				if (menuItem.getParentMenu() != null)
				{
					return menuItem.getParentMenu().getParentPopup().getOwnerWindow();
				}
				return menuItem.getParentPopup().getOwnerWindow();
			}
			case Node node ->
			{
				return getWindow(node);
			}
			default -> throw new IllegalStateException("Cannot find a window from the event " + event);
		}
	}

	public static Window getWindow(Node node)
	{
		return node.getScene().getWindow();
	}

	public static void setPresent(Node node, boolean present)
	{
		node.setManaged(present);
		node.setVisible(present);
	}

	public static void setPresent(Node node)
	{
		setPresent(node, true);
	}

	public static void setAbsent(Node node)
	{
		setPresent(node, false);
	}
}
