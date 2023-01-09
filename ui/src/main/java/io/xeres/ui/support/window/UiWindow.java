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

	private static final String WINDOW_X = "PosX";
	private static final String WINDOW_Y = "PosY";
	private static final String WINDOW_WIDTH = "Width";
	private static final String WINDOW_HEIGHT = "Height";

	private static FxWeaver fxWeaver;
	private static ResourceBundle bundle;

	final Scene scene;
	final Stage stage;

	static void setFxWeaver(FxWeaver fxWeaver, ResourceBundle bundle)
	{
		UiWindow.fxWeaver = fxWeaver;
		UiWindow.bundle = bundle;
	}

	private UiWindow(Builder builder)
	{
		scene = new Scene(builder.root);
		scene.getStylesheets().add("/view/javafx.css");
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
		stage.setMinWidth(builder.minWidth);
		stage.setMinHeight(builder.minHeight);
		stage.setTitle(builder.title);
		stage.setScene(scene);

		if (builder.rememberEnvironment)
		{
			setWindowPreferences(stage, builder.root.getId());
		}

		if (!builder.resizeable)
		{
			stage.setResizable(false);
		}

		stage.setOnShowing(event -> builder.controller.onShowing());
		stage.setOnShown(event -> builder.controller.onShown());
		stage.setOnHiding(event -> builder.controller.onHiding());
		stage.setOnHidden(event -> builder.controller.onHidden());

		scene.getWindow().setUserData(builder.controller);
	}

	private void setWindowPreferences(Stage stage, String id)
	{
		if (isEmpty(id))
		{
			throw new IllegalArgumentException("A Window requires an ID");
		}

		boolean preferencesExist;
		try
		{
			preferencesExist = Preferences.userRoot().nodeExists("/Windows/" + id);
		}
		catch (BackingStoreException e)
		{
			log.debug("Error while trying to retrieve Windows' preferences: {}", e.getMessage());
			preferencesExist = false;
		}

		if (preferencesExist)
		{
			var preferences = Preferences.userRoot().node("Windows").node(id);
			stage.setX(preferences.getDouble(WINDOW_X, 0));
			stage.setY(preferences.getDouble(WINDOW_Y, 0));
			stage.setWidth(preferences.getDouble(WINDOW_WIDTH, 0));
			stage.setHeight(preferences.getDouble(WINDOW_HEIGHT, 0));
		}

		stage.setOnCloseRequest(event -> {
			var preferences = Preferences.userRoot().node("Windows").node(id);
			preferences.putDouble(WINDOW_X, stage.getX());
			preferences.putDouble(WINDOW_Y, stage.getY());
			preferences.putDouble(WINDOW_WIDTH, stage.getWidth());
			preferences.putDouble(WINDOW_HEIGHT, stage.getHeight());
			log.debug("Saving Window {}, x: {}, y: {}, width: {}, height: {}", id, stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
		});
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

	Window getWindow()
	{
		return scene.getWindow();
	}

	void open()
	{
		stage.show();
	}

	void close()
	{
		stage.close();
	}

	static Builder builder(Class<? extends WindowController> controllerClass)
	{
		var parent = (Parent) fxWeaver.loadView(controllerClass, bundle);
		parent.setId(controllerClass.getName());
		return new Builder(parent, fxWeaver.getBean(controllerClass));
	}

	static Builder builder(String resource, WindowController controller)
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
		return new Builder(parent, controller);
	}

	static final class Builder
	{
		private Stage stage;
		private final Parent root;
		private final WindowController controller;
		private Window parent;
		private double minWidth = 240;
		private double minHeight = 200;
		private String title = AppName.NAME;
		private String localId;
		private Object userData;
		private boolean rememberEnvironment;
		private boolean resizeable = true;

		private Builder(Parent root, WindowController controller)
		{
			this.root = root;
			this.controller = controller;
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

		Builder setMinWidth(double minWidth)
		{
			this.minWidth = minWidth;
			return this;
		}

		Builder setMinHeight(double minHeight)
		{
			this.minHeight = minHeight;
			return this;
		}

		Builder setTitle(String title)
		{
			this.title = title;
			return this;
		}

		Builder setLocalId(String id)
		{
			this.localId = id;
			return this;
		}

		Builder setRememberEnvironment(boolean remember)
		{
			this.rememberEnvironment = remember;
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
