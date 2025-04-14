/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom;

import io.xeres.common.AppName;
import io.xeres.ui.support.util.UiUtils;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Set;

/**
 * Input area widget.
 * <p>Autogrow system by Dirk Lemmermann, see
 * <a href="https://github.com/dlsc-software-consulting-gmbh/GemsFX/blob/master/gemsfx/src/main/java/com/dlsc/gemsfx/ExpandingTextArea.java">GemsFX</a>
 */
public class InputArea extends TextArea
{
	private static final Logger log = LoggerFactory.getLogger(InputArea.class);

	private static final KeyCodeCombination CTRL_S = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);

	private Text text;

	private double offsetTop;
	private double offsetBottom;

	public InputArea()
	{
		this("");
	}

	public InputArea(String text)
	{
		super(text);
		setWrapText(true);

		sceneProperty().addListener(observable -> {
			if (getScene() != null)
			{
				performBinding();
			}
		});

		skinProperty().addListener(observable -> {
			if (getSkin() != null)
			{
				performBinding();
			}
		});

		addEventFilter(KeyEvent.KEY_PRESSED, this::handleInputKeys);
	}

	public void openStickerSelector()
	{
		handleStickers();
	}

	private void handleInputKeys(KeyEvent event)
	{
		if (CTRL_S.match(event))
		{
			if (handleStickers())
			{
				event.consume();
			}
		}
	}

	private boolean handleStickers()
	{
		var bounds = localToScreen(getBoundsInLocal());
		var popup = new Popup();
		var stickerView = new StickerView();
		popup.getContent().add(stickerView);
		popup.setAnchorX(bounds.getMinX());
		popup.setAnchorY(bounds.getMinY());
		popup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);

		// Proxy the event to the InputArea
		stickerView.addEventHandler(StickerClickedEvent.STICKER_CLICKED, event -> {
			event.consume();
			fireEvent(new StickerClickedEvent(event.getPath()));
			popup.hide();
		});

		popup.show(UiUtils.getWindow(this));
		stickerView.loadStickers(Paths.get(AppDirsFactory.getInstance().getUserDataDir(AppName.NAME, null, null, true), "stickers"));
		popup.setAutoHide(true);
		return true;
	}

	private double computeHeight()
	{
		computeOffsets();

		var bounds = localToScreen(text.getLayoutBounds());
		if (bounds != null)
		{
			var minY = bounds.getMinY();
			var maxY = bounds.getMaxY();

			return maxY - minY + offsetTop + offsetBottom;
		}
		return 0.0;
	}

	private void computeOffsets()
	{
		offsetTop = getInsets().getTop();
		offsetBottom = getInsets().getBottom();

		var scrollPane = (ScrollPane) lookup(".scroll-pane");
		if (scrollPane != null)
		{
			var viewport = (Region) scrollPane.lookup(".viewport");
			var content = (Region) scrollPane.lookup(".content");

			offsetTop += viewport.getInsets().getTop();
			offsetTop += content.getInsets().getTop();

			offsetBottom += viewport.getInsets().getBottom();
			offsetBottom += content.getInsets().getBottom();
		}
	}

	private void performBinding()
	{
		var scrollPane = (ScrollPane) lookup(".scroll-pane");
		if (scrollPane != null)
		{
			scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			scrollPane.skinProperty().addListener(it -> {
				if (scrollPane.getSkin() != null)
				{
					if (text == null)
					{
						text = findTextNode();
						if (text != null)
						{
							prefHeightProperty().bind(Bindings.createDoubleBinding(this::computeHeight, text.layoutBoundsProperty()));
						}
					}
				}
			});
		}
	}

	private Text findTextNode()
	{
		final Set<Node> nodes = lookupAll(".text");
		for (Node node : nodes)
		{
			if (node.getParent() instanceof Group)
			{
				return (Text) node;
			}
		}
		return null;
	}
}
