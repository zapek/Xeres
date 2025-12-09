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

import io.xeres.ui.client.BoardClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.board.BoardGroup;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/board/board_view.fxml")
public class BoardViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(BoardViewController.class);

	private static final String SUBSCRIBE_MENU_ID = "subscribe";
	private static final String UNSUBSCRIBE_MENU_ID = "unsubscribe";
	private static final String COPY_LINK_MENU_ID = "copyLink";

	private static final String OPEN_OWN = "OpenOwn";
	private static final String OPEN_SUBSCRIBED = "OpenSubscribed";
	private static final String OPEN_POPULAR = "OpenPopular";
	private static final String OPEN_OTHER = "OpenOther";

	@FXML
	private TreeTableView<BoardGroup> boardTree;

	@FXML
	private TreeTableColumn<BoardGroup, String> boardNameColumn;

	@FXML
	private TreeTableColumn<BoardGroup, Integer> boardCountColumn;

	@FXML
	private SplitPane splitPaneVertical;

	@FXML
	private Button createBoard;

	private final ResourceBundle bundle;

	private final BoardClient boardClient;
	private final NotificationClient notificationClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	private final TreeItem<BoardGroup> ownBoards;
	private final TreeItem<BoardGroup> subscribedBoards;
	private final TreeItem<BoardGroup> popularBoards;
	private final TreeItem<BoardGroup> otherBoards;

	public BoardViewController(BoardClient boardClient, ResourceBundle bundle, NotificationClient notificationClient, GeneralClient generalClient, ImageCache imageCache)
	{
		this.boardClient = boardClient;
		this.bundle = bundle;

		ownBoards = new TreeItem<>(new BoardGroup(bundle.getString("forum.tree.own")));
		subscribedBoards = new TreeItem<>(new BoardGroup(bundle.getString("forum.tree.subscribed")));
		popularBoards = new TreeItem<>(new BoardGroup(bundle.getString("forum.tree.popular")));
		otherBoards = new TreeItem<>(new BoardGroup(bundle.getString("forum.tree.other")));

		this.notificationClient = notificationClient;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
	}


	@Override
	public void initialize()
	{
		log.debug("Trying to get boards list...");

		var root = new TreeItem<>(new BoardGroup(""));
		//noinspection unchecked
		root.getChildren().addAll(ownBoards, subscribedBoards, popularBoards, otherBoards);
		root.setExpanded(true);
		boardTree.setRoot(root);
		boardTree.setShowRoot(false);
		boardTree.setRowFactory(_ -> new BoardCell(generalClient, imageCache));
		boardNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		boardCountColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("unreadCount"));
		boardCountColumn.setCellFactory(_ -> new BoardCellCount());

		// XXX: add the rest...

	}
}
