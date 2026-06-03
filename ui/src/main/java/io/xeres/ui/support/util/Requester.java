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

package io.xeres.ui.support.util;

import atlantafx.base.theme.Styles;
import io.xeres.common.AppName;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.ByteUnitUtils;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Locale;

import static io.xeres.ui.support.util.DateUtils.DATE_FORMAT;
import static javafx.scene.control.Alert.AlertType.ERROR;

/**
 * Enhanced requester functions for JavaFX. They're all supposed to run in the UI thread.
 */
public final class Requester
{
	private Requester()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Shows an informative alert.
	 *
	 * @param message the message
	 */
	public static void showInfo(String message)
	{
		show(AlertType.INFORMATION, message);
	}

	/**
	 * Shows a warning.
	 *
	 * @param message the message
	 */
	public static void showWarning(String message)
	{
		show(AlertType.WARNING, message);
	}

	/**
	 * Shows an error.
	 *
	 * @param message the message
	 */
	public static void showError(String message)
	{
		show(AlertType.ERROR, message);
	}

	/**
	 * Shows an alert with a confirmation.
	 *
	 * @param message  the message to display
	 * @param runnable the action to run if OK was selected
	 */
	public static void confirm(String message, Runnable runnable)
	{
		var alert = buildAlert(AlertType.CONFIRMATION, null, message, null);
		alert.showAndWait()
				.filter(response -> response == ButtonType.OK)
				.ifPresent(_ -> runnable.run());
	}

	/**
	 * Shows an alert with a confirmation.
	 *
	 * @param message the message to display
	 * @return true if OK was selected, false if cancel
	 */
	public static boolean confirm(String message)
	{
		var alert = buildAlert(AlertType.CONFIRMATION, null, message, null);
		var result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.OK;
	}

	/**
	 * Shows an alert with a yes/no button.
	 *
	 * @param message the message to display
	 * @return true if 'yes' was pressed, false if 'no' was pressed
	 */
	public static boolean ask(String message)
	{
		var alert = buildAlert(AlertType.CONFIRMATION, null, message, null);
		alert.getButtonTypes().clear();
		alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
		var result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.YES;
	}

	/**
	 * Shows an alert with two buttons. Is supposed to run in the UI thread and will block.
	 *
	 * @param message  the message to display
	 * @param positive the text for the positive button
	 * @param negative the text for the negative button
	 * @return true if the positive button was pressed, false if the negative button was pressed
	 */
	public static boolean ask(String message, String positive, String negative)
	{
		var alert = buildAlert(AlertType.CONFIRMATION, null, message, null);
		alert.getButtonTypes().clear();
		alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
		var yesButton = (Button) alert.getDialogPane().lookupButton(ButtonType.YES);
		yesButton.setText(positive);
		var noButton = (Button) alert.getDialogPane().lookupButton(ButtonType.NO);
		noButton.setText(negative);
		var result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.YES;
	}

	/**
	 * Shows an alert to get a string.
	 *
	 * @param message the message to display
	 * @return the string input by the user, or an empty string if none or cancel
	 */
	public static String getString(String message)
	{
		var dialog = new TextInputDialog();
		setCommonDialog(dialog, null);
		dialog.setContentText(message);
		var result = dialog.showAndWait();
		return result.orElse("");
	}

	private static void setCommonDialog(Dialog<?> dialog, String title)
	{
		var stage = (Stage) dialog.getDialogPane().getScene().getWindow();

		// Try to intelligently set the owner window to indicate to the
		// user that there's some action needed if he clicks it
		var defaultOwnerWindow = WindowManager.getDefaultOwnerWindow();
		if (defaultOwnerWindow != null)
		{
			dialog.initOwner(defaultOwnerWindow);
		}

		UiUtils.setDefaultIcon(stage); // required for the window's title bar icon
		UiUtils.setDefaultStyle(stage.getScene()); // required for the default styles being applied
		// Setting dark borders doesn't work because dialogs aren't in JavaFX's built-in windows list
		if (title != null)
		{
			dialog.setTitle(title);
		}
		dialog.setHeaderText(null); // the header is ugly
	}

	static Alert buildAlert(AlertType alertType, String title, String message, String stackTrace)
	{
		var alert = new Alert(alertType);

		setCommonDialog(alert, title);

		// The default doesn't allow cut & pasting and doesn't have scrollbars when needed,
		// so instead we use a TextArea with similar styling.
		var vbox = new VBox();
		var hbox = new HBox();
		hbox.setAlignment(Pos.TOP_RIGHT);
		if (stackTrace != null)
		{
			var copyButton = new Button(null, new FontIcon(MaterialDesignC.CLIPBOARD_OUTLINE));
			copyButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
			TooltipUtils.install(copyButton, I18nUtils.getBundle().getString("requester.bug-report.tip"));
			hbox.getChildren().add(copyButton);
			copyButton.setOnAction(_ -> ClipboardUtils.copyTextToClipboard(generateAlertErrorString(alertType, message, stackTrace)));
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
			ssTextArea.getStyleClass().add("fixed-font");
			ssTextArea.setWrapText(false);
			ssTextArea.setEditable(false);
			ssTextArea.setMaxWidth(Double.MAX_VALUE);
			ssTextArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setHgrow(ssTextArea, Priority.ALWAYS);
			GridPane.setVgrow(ssTextArea, Priority.ALWAYS);

			var content = new GridPane();
			content.setMaxWidth(Double.MAX_VALUE);
			content.add(new Label(I18nUtils.getBundle().getString("requester.stacktrace.title")), 0, 0);
			content.add(ssTextArea, 0, 1);

			alert.getDialogPane().setExpandableContent(content);
		}
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); // Without this, long texts get truncated. Go figure why this isn't the default...

		return alert;
	}

	private static void show(AlertType alertType, String message)
	{
		var alert = buildAlert(alertType, null, message, null);
		alert.showAndWait();
	}

	private static String generateAlertErrorString(AlertType alertType, String message, String stackTrace)
	{
		String version;
		try (var resource = UiUtils.class.getClassLoader().getResourceAsStream("META-INF/build-info.properties"))
		{
			if (resource != null)
			{
				try (var buildInfo = new BufferedReader(new InputStreamReader(resource)))
				{
					version = buildInfo.lines()
							.filter(s -> s.startsWith("build.version="))
							.map(s -> s.substring("build.version=".length()))
							.findFirst().orElse("unknown");
				}
			}
			else
			{
				version = "unknown";
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		return AppName.NAME + " Error Report\n" +
				"\nVersion: " + version +
				"\nOS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")" +
				"\nJRE: " + System.getProperty("java.vendor") + " (" + System.getProperty("java.version") + ")" +
				"\nCharset: " + Charset.defaultCharset() +
				"\nLanguage: " + Locale.getDefault().getLanguage() +
				"\nTCP/IP stack state: " + (StringUtils.defaultString(System.getProperty("java.net.preferIPv4Stack")).equals("true") ? "sane" : "broken") +
				"\nNumber of processor threads: " + Runtime.getRuntime().availableProcessors() +
				"\nMemory allocated for the JVM: " + ByteUnitUtils.fromBytes(Runtime.getRuntime().totalMemory()) +
				"\nMaximum allocatable memory: " + ByteUnitUtils.fromBytes(Runtime.getRuntime().maxMemory()) +
				"\nDate: " + DATE_FORMAT.format(Instant.now()) +
				"\nSource: requester" +
				"\nType: " + (alertType == ERROR ? "Error" : "Warning") +
				"\nMessage: " + message +
				"\n\nStack Trace:\n" + stackTrace +
				"\n\n";
	}
}
