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

package io.xeres.ui.controller.board;

import atlantafx.base.controls.Tab;
import atlantafx.base.controls.TabLine;
import io.xeres.ui.client.BoardClient;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.custom.ImageSelectorView;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/board/board_editor_view.fxml")
public class BoardEditorWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(BoardEditorWindowController.class);

	@FXML
	private GridPane gridPane;

	@FXML
	private TextField boardName;

	@FXML
	private TextField title;

	@FXML
	private TabLine tabLine;

	@FXML
	private Tab textTab;

	@FXML
	private Tab imageTab;

	@FXML
	private Tab linkTab;

	@FXML
	private EditorView editorView;

	@FXML
	private Button send;

	private final List<Node> addedNodes = new ArrayList<>();

	private ImageSelectorView imageSelectorView;

	private Label linkLabel;

	private TextField linkTextField;

	private final BoardClient boardClient;
	private final LocationClient locationClient;
	private final MarkdownService markdownService;
	private final ResourceBundle bundle;

	public BoardEditorWindowController(BoardClient boardClient, LocationClient locationClient, MarkdownService markdownService, ResourceBundle bundle)
	{
		this.boardClient = boardClient;
		this.locationClient = locationClient;
		this.markdownService = markdownService;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		imageSelectorView = new ImageSelectorView(240.0, 180.0, "mdi2i-image", true);
		linkLabel = new Label("URL");
		linkTextField = new TextField();

		tabLine.setTabClosingPolicy(Tab.ClosingPolicy.NO_TABS);
		tabLine.setTabDragPolicy(Tab.DragPolicy.FIXED);
		tabLine.setTabResizePolicy(Tab.ResizePolicy.ADAPTIVE);

		tabLine.getSelectionModel().selectedItemProperty().subscribe(tab -> {
			clearNodes();
			if (tab == textTab)
			{
				// Nothing to add here
			}
			else if (tab == imageTab)
			{
				gridPane.add(addNode(imageSelectorView), 0, 3, 2, 1);
			}
			else if (tab == linkTab)
			{
				gridPane.add(addNode(linkLabel), 0, 3);
				gridPane.add(addNode(linkTextField), 1, 3);
			}
		});

		Platform.runLater(() -> title.requestFocus());
	}

	private Node addNode(Node node)
	{
		addedNodes.add(node);
		return node;
	}

	private void clearNodes()
	{
		addedNodes.forEach(node -> gridPane.getChildren().remove(node));
		addedNodes.clear();
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(title);
		if (userData == null)
		{
			throw new IllegalArgumentException("Missing board id");
		}

		long boardId = (long) userData;

		boardClient.getBoardGroupById(boardId)
				.doOnSuccess(boardGroup -> Platform.runLater(() -> boardName.setText(boardGroup.getName())))
				.subscribe();

		// XXX: add discard prevention...
	}
}
