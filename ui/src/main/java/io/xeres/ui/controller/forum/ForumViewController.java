/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.forum;

import io.xeres.ui.client.ForumClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.model.forum.Forum;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/forum/forumview.fxml")
public class ForumViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ForumViewController.class);

	@FXML
	private TreeView<ForumHolder> forumTree;

	@FXML
	private SplitPane splitPane;

	private final ResourceBundle bundle;

	private final ForumClient forumClient;

	private final TreeItem<ForumHolder> ownForums;
	private final TreeItem<ForumHolder> subscribedForums;
	private final TreeItem<ForumHolder> popularForums;
	private final TreeItem<ForumHolder> otherForums;

	public ForumViewController(ForumClient forumClient, ResourceBundle bundle)
	{
		this.forumClient = forumClient;
		this.bundle = bundle;

		ownForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.own")));
		subscribedForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.subscribed")));
		popularForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.popular")));
		otherForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.other")));
	}

	@Override
	public void initialize() throws IOException
	{
		log.debug("Trying to get forums list...");

		var root = new TreeItem<>(new ForumHolder());
		//noinspection unchecked
		root.getChildren().addAll(ownForums, subscribedForums, popularForums, otherForums);
		root.setExpanded(true);
		forumTree.setRoot(root);
		forumTree.setShowRoot(false);

		getForums();
	}

	private void getForums()
	{
		forumClient.getForums().collectList()
				.doOnSuccess(forums -> forums.forEach(this::addForum))
				.subscribe();
	}

	private void addForum(Forum forum)
	{
		var subscribedTree = subscribedForums.getChildren();
		var popularTree = popularForums.getChildren();

		// XXX: add the forums...
		log.debug("Would add forum: {}", forum);
	}
}
