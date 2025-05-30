/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.HostServices;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.xeres.ui.support.util.DateUtils.DATE_TIME_DISPLAY;
import static javafx.scene.control.Alert.AlertType.ERROR;
import static javafx.scene.control.Alert.AlertType.WARNING;

/**
 * Supplements JavaFX with handy functions for UI operations.
 */
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
	 * Will not block.
	 *
	 * @param t the throwable
	 */
	public static void showAlertError(Throwable t)
	{
		showAlertError(t, null);
	}

	/**
	 * Shows a generic alert error and allows to run an action afterwards. Is supposed to be used in
	 * {@code doOnError} in the WebClients. Will not block.
	 *
	 * @param t      the throwable
	 * @param action the action to perform after the alert has been dismissed
	 */
	public static void showAlertError(Throwable t, Runnable action)
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
				alert(ERROR, "Error", t.getClass().getSimpleName() + ": " + t.getMessage(), ExceptionUtils.getStackTrace(t));
			}
			if (action != null)
			{
				action.run();
			}
		});
		log.error("Error: {}", t.getMessage(), t);
	}

	/**
	 * Highlights the specified nodes. Add the 'danger' CSS style to them.
	 *
	 * @param nodes the nodes to highlight with errors
	 */
	public static void showError(Node... nodes)
	{
		for (var node : nodes)
		{
			node.pseudoClassStateChanged(dangerPseudoClass, true);
		}
	}

	/**
	 * Clears out the highlighting of the specified nodes. Removes the 'danger' CSS styles to them.
	 *
	 * @param nodes the nodes to un-highlight with errors
	 */
	public static void clearError(Node... nodes)
	{
		for (var node : nodes)
		{
			node.pseudoClassStateChanged(dangerPseudoClass, false);
		}
	}

	/**
	 * Shows an alert. Is supposed to run in the UI thread and will block.
	 *
	 * @param alertType the type of the alert
	 * @param message   the message
	 */
	public static void alert(AlertType alertType, String message)
	{
		var alert = buildAlert(alertType, null, message, null);
		alert.showAndWait();
	}

	/**
	 * Shows an alert with a confirmation. Is supposed to run in the UI thread and will block.
	 *
	 * @param message  the message to display
	 * @param runnable the action to run after the confirmation
	 */
	public static void alertConfirm(String message, Runnable runnable)
	{
		var alert = buildAlert(AlertType.CONFIRMATION, null, message, null);
		alert.showAndWait()
				.filter(response -> response == ButtonType.OK)
				.ifPresent(response -> runnable.run());
	}

	private static void alert(AlertType alertType, String title, String message, String stackTrace)
	{
		var alert = buildAlert(alertType, title, message, stackTrace);
		alert.showAndWait();
	}

	private static Alert buildAlert(AlertType alertType, String title, String message, String stackTrace)
	{
		var alert = new Alert(alertType);
		var stage = (Stage) alert.getDialogPane().getScene().getWindow();

		// Try to intelligently set the owner window to indicate to the
		// user that there's some action needed if he clicks it
		var defaultOwnerWindow = WindowManager.getDefaultOwnerWindow();
		if (defaultOwnerWindow != null)
		{
			alert.initOwner(defaultOwnerWindow);
		}

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
		if (stackTrace != null)
		{
			var copyButton = new Button(null, new FontIcon(MaterialDesignC.CLIPBOARD_OUTLINE));
			copyButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
			TooltipUtils.install(copyButton, "Copy as a bug report to the clipboard");
			hbox.getChildren().add(copyButton);
			copyButton.setOnAction(event -> ClipboardUtils.copyTextToClipboard(generateAlertErrorString(alertType, title, message, stackTrace)));
		}

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

	/**
	 * Sets the default icon of a stage (once per window).
	 *
	 * @param stage the stage to set the default icon to
	 */
	public static void setDefaultIcon(Stage stage)
	{
		stage.getIcons().add(new Image(Objects.requireNonNull(stage.getClass().getResourceAsStream("/image/icon.png"))));
	}

	/**
	 * Sets the default style of a scene (once per window).
	 *
	 * @param scene the scene to set the default icon to
	 */
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

	/**
	 * Gets the user data set to a particular node.
	 *
	 * @param node the node to get the userdata from
	 * @return the user data
	 */
	public static Object getUserData(Node node)
	{
		return node.getScene().getRoot().getUserData();
	}

	/**
	 * Makes Hyperlinks actually do something. Slightly recursive.
	 *
	 * @param rootNode the parent node where the hyperlinks are
	 * @param hostServices the host services
	 */
	public static void linkify(Node rootNode, HostServices hostServices)
	{
		if (hostServices == null)
		{
			return;
		}

		if (rootNode instanceof TabPane tabPane)
		{
			tabPane.getTabs().forEach(tab -> linkify(tab.getContent(), hostServices));
		}
		else if (rootNode instanceof ScrollPane scrollPane)
		{
			linkify(scrollPane.getContent(), hostServices);
		}
		else if (rootNode instanceof Parent parent)
		{
			parent.getChildrenUnmodifiable().forEach(node -> linkify(node, hostServices));
		}

		if (rootNode instanceof DisclosedHyperlink disclosedHyperlink)
		{
			if (disclosedHyperlink.getOnAction() == null)
			{
				disclosedHyperlink.setOnAction(event -> hostServices.showDocument(disclosedHyperlink.getUri()));
			}
		}
		else if (rootNode instanceof Hyperlink hyperlink && hyperlink.getOnAction() == null)
		{
			hyperlink.setOnAction(event -> hostServices.showDocument(hyperlink.getText().contains("@") ? ("mailto:" + hyperlink.getText()) : hyperlink.getText()));
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

	/**
	 * Gets the window from a node.
	 *
	 * @param node the node to get the window from
	 * @return the window
	 */
	public static Window getWindow(Node node)
	{
		return node.getScene().getWindow();
	}

	/**
	 * Sets the presence of a node, that is, if it's visible and takes up space.
	 *
	 * @param node the node
	 * @param present true if visible, false if gone
	 */
	public static void setPresent(Node node, boolean present)
	{
		node.setManaged(present);
		node.setVisible(present);
	}

	public static void setAbsent(Node node, boolean absent)
	{
		setPresent(node, !absent);
	}

	/**
	 * Puts a node as present, that is, is visible and takes up space.
	 *
	 * @param node the node
	 */
	public static void setPresent(Node node)
	{
		setPresent(node, true);
	}

	/**
	 * Puts a node as absent, that is, is gone.
	 *
	 * @param node the node
	 */
	public static void setAbsent(Node node)
	{
		setPresent(node, false);
	}

	public static void setOnPrimaryMouseClicked(Node node, Consumer<MouseEvent> consumer)
	{
		node.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY)
			{
				consumer.accept(event);
				event.consume();
			}
		});
	}

	public static void setOnPrimaryMouseDoubleClicked(Node node, Consumer<MouseEvent> consumer)
	{
		node.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY)
			{
				consumer.accept(event);
				event.consume();
			}
		});
	}
}
