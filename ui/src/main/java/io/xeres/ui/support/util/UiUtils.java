/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.custom.DisclosedHyperlink;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

	private static final PseudoClass warningPseudoClass = PseudoClass.getPseudoClass("warning");
	private static final PseudoClass dangerPseudoClass = PseudoClass.getPseudoClass("danger");

	/**
	 * Shows a generic alert error. Is supposed to be used in {@code doOnError} in the WebClients.
	 * Will not block.
	 *
	 * @param t the throwable
	 */
	public static void webAlertError(Throwable t)
	{
		webAlertError(t, null);
	}

	/**
	 * Shows a generic alert error and allows to run an action afterwards. Is supposed to be used in
	 * {@code doOnError} in the WebClients. Will not block.
	 *
	 * @param t      the throwable
	 * @param action the action to perform after the alert has been dismissed, null if no action
	 */
	public static void webAlertError(Throwable t, Runnable action)
	{
		var bundle = I18nUtils.getBundle();
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
					detail = StringUtils.defaultString(problem.getDetail());
					var properties = problem.getProperties();
					if (properties != null)
					{
						stackTrace = (String) properties.get("trace");
					}
				}
				else
				{
					title = bundle.getString("requester.error");
					detail = bundle.getString("requester.unknown-error");
				}
				showAlert(e.getStatusCode().isError() ? ERROR : WARNING, title, detail, stackTrace);
			}
			else
			{
				showAlert(ERROR, bundle.getString("requester.error"), t.getClass().getSimpleName() + ": " + t.getMessage(), ExceptionUtils.getStackTrace(t));
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
	public static void highlightError(Node... nodes)
	{
		for (var node : nodes)
		{
			node.pseudoClassStateChanged(dangerPseudoClass, true);
		}
	}

	public static void highlightWarning(Node... nodes)
	{
		for (var node : nodes)
		{
			node.pseudoClassStateChanged(warningPseudoClass, true);
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
	 * Makes Hyperlinks actually do something. Slightly recursive.
	 *
	 * @param rootNode     the parent node where the hyperlinks are
	 * @param hostServices the host services
	 */
	public static void linkify(Node rootNode, HostServices hostServices)
	{
		if (hostServices == null)
		{
			return;
		}

		switch (rootNode)
		{
			case TabPane tabPane -> tabPane.getTabs().forEach(tab -> linkify(tab.getContent(), hostServices));
			case ScrollPane scrollPane -> linkify(scrollPane.getContent(), hostServices);
			case Parent parent -> parent.getChildrenUnmodifiable().forEach(node -> linkify(node, hostServices));
			default ->
			{
			}
		}

		if (rootNode instanceof DisclosedHyperlink disclosedHyperlink)
		{
			if (disclosedHyperlink.getOnAction() == null)
			{
				disclosedHyperlink.setOnAction(_ -> askBeforeOpeningIfNeeded(disclosedHyperlink, () -> hostServices.showDocument(disclosedHyperlink.getUri())));
			}
		}
		else if (rootNode instanceof Hyperlink hyperlink && hyperlink.getOnAction() == null)
		{
			hyperlink.setOnAction(_ -> hostServices.showDocument(hyperlink.getText().contains("@") ? ("mailto:" + hyperlink.getText()) : hyperlink.getText()));
		}
	}

	/**
	 * Asks before opening an hyperlink, if the link is suspicious.
	 * @param hyperlink the hyperlink
	 * @param action the action to do if OK was pressed
	 */
	public static void askBeforeOpeningIfNeeded(DisclosedHyperlink hyperlink, Runnable action)
	{
		if (hyperlink.isMalicious())
		{
			Requester.confirm(MessageFormat.format(I18nUtils.getBundle().getString("uri.malicious-link.confirm"), hyperlink.getUri()), action);
		}
		else if (hyperlink.isUnsafe())
		{
			Requester.confirm(MessageFormat.format(I18nUtils.getBundle().getString("uri.unsafe-link.confirm"), hyperlink.getUri()), action);
		}
		else
		{
			action.run();
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
	 * @param node    the node
	 * @param present true if visible, false if gone
	 */
	public static void setPresent(Node node, boolean present)
	{
		node.setManaged(present);
		node.setVisible(present);
	}

	/**
	 * Sets the absence of a node, that is, if it's not visible and not taking up any space.
	 *
	 * @param node    the node
	 * @param absent true if gone, false if visible
	 */
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

	/**
	 * Sets a left mouse click event on a node.
	 * @param node the node
	 * @param consumer the consumer
	 */
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

	/**
	 * Sets a left mouse double click event on a node.
	 * @param node the node
	 * @param consumer the consumer
	 */
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

	/**
	 * Gets the user data set to a particular node.
	 *
	 * @param node the node to get the userdata from
	 * @return the user data, can be null
	 */
	public static Object getUserData(Node node)
	{
		Objects.requireNonNull(node, "node cannot be null");
		var scene = node.getScene();
		if (scene != null)
		{
			var root = scene.getRoot();
			if (root != null)
			{
				return root.getUserData();
			}
		}
		return null;
	}

	/**
	 * Sets the size of a {@link FontIcon}.
	 * <p>ikonli and AtlantaFX don't work well together so this utility method has to be used instead.
	 * <p>
	 * See {@link <a href="https://github.com/kordamp/ikonli/issues/150">this issue</a>}.
	 *
	 * @param icon the FontIcon
	 * @param size the size
	 */
	public static void setIconSize(FontIcon icon, int size)
	{
		String normalizedStyle = normalizeStyle(icon.getStyle(), "-fx-font-size", size + "px");
		normalizedStyle = normalizeStyle(normalizedStyle, "-fx-icon-size", size + "px");
		icon.setStyle(normalizedStyle);
	}

	// Taken from FontIcon()
	private static String normalizeStyle(String style, String key, String value)
	{
		int start = style.indexOf(key);
		if (start != -1)
		{
			int end = style.indexOf(";", start);
			end = end >= start ? end : style.length() - 1;
			style = style.substring(0, start) + style.substring(end + 1);
		}
		return style + key + ": " + value + ";";
	}

	private static void showAlert(AlertType alertType, String title, String message, String stackTrace)
	{
		var alert = Requester.buildAlert(alertType, title, message, stackTrace);
		alert.showAndWait();
	}
}
