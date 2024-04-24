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

package io.xeres.ui.support.window;

import io.xeres.common.AppName;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.theme.AppThemeManager;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxWeaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.apache.commons.lang3.StringUtils.isEmpty;

final class UiWindow
{
	private static final Logger log = LoggerFactory.getLogger(UiWindow.class);

	private static final String KEY_WINDOW_X = "PosX";
	private static final String KEY_WINDOW_Y = "PosY";
	private static final String KEY_WINDOW_WIDTH = "Width";
	private static final String KEY_WINDOW_HEIGHT = "Height";
	public static final String NODE_WINDOWS = "Windows";

	private static double borderTop;
	private static double borderBottom;
	private static double borderLeft;
	private static double borderRight;

	final Scene scene;
	final Stage stage;

	static void setWindowDecorationSizes(double top, double bottom, double left, double right)
	{
		borderTop = top;
		borderBottom = bottom;
		borderLeft = left;
		borderRight = right;
	}

	private UiWindow(Builder builder)
	{
		scene = new Scene(builder.root);
		UiUtils.setDefaultStyle(scene);
		stage = Objects.requireNonNullElseGet(builder.stage, Stage::new);
		UiUtils.setDefaultIcon(stage);

		if (builder.parent != null)
		{
			stage.initOwner(builder.parent);
			stage.initModality(Modality.WINDOW_MODAL);
		}
		if (builder.localId != null)
		{
			if (!builder.root.getId().contains(":"))
			{
				throw new IllegalArgumentException("LocalId used for unique window " + builder.root.getId());
			}
			var tokens = builder.root.getId().split(":");
			builder.root.setId(tokens[0] + ":" + builder.localId);
		}
		else
		{
			if (builder.root.getId().contains(":"))
			{
				throw new IllegalArgumentException("Missing localId for non unique window " + builder.root.getId());
			}
		}
		if (builder.userData != null)
		{
			builder.root.setUserData(builder.userData);
		}

		// Set the minimums to the root's minimums + decorations.
		stage.setMinWidth(builder.root.minWidth(-1) + (int) borderLeft + (int) borderRight); // There's some rounding errors in JavaFX somewhere. int is a bit better
		stage.setMinHeight(builder.root.minHeight(-1) + (int) borderTop + (int) borderBottom);

		stage.setTitle(builder.title);
		stage.setScene(scene);

		loadWindowPreferences(stage, builder);

		if (!builder.resizeable)
		{
			stage.setResizable(false);
		}

		stage.setOnShowing(event -> builder.controller.onShowing());
		stage.setOnShown(event -> {
			builder.controller.onShown();
			UiBorders.setDarkModeOnOpeningWindow(builder.appThemeManager.getCurrentTheme().isDark());
		});
		stage.setOnHiding(event -> {
			saveWindowPreferences(stage, builder);
			builder.controller.onHiding();
		});
		stage.setOnHidden(event -> builder.controller.onHidden());

		scene.getWindow().setUserData(builder.controller);
	}

	private void loadWindowPreferences(Stage stage, Builder builder)
	{
		var id = builder.root.getId();

		if (!builder.rememberEnvironment)
		{
			return;
		}

		if (isEmpty(id))
		{
			throw new IllegalArgumentException("A Window requires an ID");
		}

		boolean preferencesExist;
		try
		{
			preferencesExist = builder.preferences.nodeExists(NODE_WINDOWS + "/" + id);
		}
		catch (BackingStoreException e)
		{
			log.debug("Error while trying to retrieve Windows' preferences: {}", e.getMessage());
			preferencesExist = false;
		}

		if (preferencesExist)
		{
			var preferences = builder.preferences.node(NODE_WINDOWS).node(id);
			stage.setX(preferences.getDouble(KEY_WINDOW_X, 0));
			stage.setY(preferences.getDouble(KEY_WINDOW_Y, 0));
			stage.setWidth(preferences.getDouble(KEY_WINDOW_WIDTH, 0));
			stage.setHeight(preferences.getDouble(KEY_WINDOW_HEIGHT, 0));
		}
	}

	private void saveWindowPreferences(Stage stage, Builder builder)
	{
		var id = builder.root.getId();

		if (!builder.rememberEnvironment)
		{
			return;
		}

		if (isEmpty(id))
		{
			throw new IllegalArgumentException("A Window requires an ID");
		}

		var preferences = builder.preferences.node(NODE_WINDOWS).node(id);
		preferences.putDouble(KEY_WINDOW_X, stage.getX());
		preferences.putDouble(KEY_WINDOW_Y, stage.getY());
		preferences.putDouble(KEY_WINDOW_WIDTH, stage.getWidth());
		preferences.putDouble(KEY_WINDOW_HEIGHT, stage.getHeight());
		log.debug("Saving Window {}, x: {}, y: {}, width: {}, height: {}", id, stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
	}

	static Optional<Window> getOpenedWindow(Class<? extends WindowController> controllerClass)
	{
		return Window.getWindows().stream()
				.filter(window -> Objects.equals(window.getScene().getRoot().getId(), controllerClass.getName()))
				.findFirst();
	}

	static Optional<Window> getOpenedWindow(Class<? extends WindowController> controllerClass, String localId)
	{
		return Window.getWindows().stream()
				.filter(window -> Objects.equals(window.getScene().getRoot().getId(), controllerClass.getName() + ":" + localId))
				.findFirst();
	}

	static List<Window> getOpenedWindows()
	{
		return Window.getWindows();
	}

	void open()
	{
		stage.show();
	}

	void close()
	{
		stage.close();
	}

	static Builder builder(Class<? extends WindowController> controllerClass, FxWeaver fxWeaver, ResourceBundle bundle, Preferences preferences, AppThemeManager appThemeManager)
	{
		var parent = (Parent) fxWeaver.loadView(controllerClass, bundle);
		parent.setId(controllerClass.getName());
		return new Builder(parent, fxWeaver.getBean(controllerClass), preferences, appThemeManager);
	}

	static Builder builder(String resource, WindowController controller, ResourceBundle bundle, Preferences preferences, AppThemeManager appThemeManager)
	{
		var fxmlLoader = new FXMLLoader(UiWindow.class.getResource(resource), bundle);
		fxmlLoader.setController(controller);
		Parent parent;
		try
		{
			parent = fxmlLoader.load();
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Failed to load FXML: " + e.getMessage(), e);
		}
		parent.setId(controller.getClass().getName() + ":" + UUID.randomUUID()); // This is a default ID to enforce uniqueness
		return new Builder(parent, controller, preferences, appThemeManager);
	}

	static final class Builder
	{
		private Stage stage;
		private final Parent root;
		private final WindowController controller;
		private final Preferences preferences;
		private final AppThemeManager appThemeManager;
		private Window parent;
		private String title = AppName.NAME;
		private String localId;
		private Object userData;
		private boolean rememberEnvironment;
		private boolean resizeable = true;

		private Builder(Parent root, WindowController controller, Preferences preferences, AppThemeManager appThemeManager)
		{
			this.root = root;
			this.controller = controller;
			this.preferences = preferences;
			this.appThemeManager = appThemeManager;
		}

		Builder setParent(Window parent)
		{
			this.parent = parent;
			return this;
		}

		Builder setStage(Stage stage)
		{
			this.stage = stage;
			return this;
		}

		Builder setTitle(String title)
		{
			this.title = title;
			return this;
		}

		Builder setLocalId(String id)
		{
			localId = id;
			return this;
		}

		/**
		 * Remembers the window size and position.
		 *
		 * @param remember true if remembering is needed (defaults to false)
		 * @return the builder
		 */
		Builder setRememberEnvironment(boolean remember)
		{
			rememberEnvironment = remember;
			return this;
		}

		Builder setResizeable(boolean resizeable)
		{
			this.resizeable = resizeable;
			return this;
		}

		Builder setUserData(Object userData)
		{
			this.userData = userData;
			return this;
		}

		UiWindow build()
		{
			return new UiWindow(this);
		}
	}
}
