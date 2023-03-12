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

import io.xeres.ui.controller.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/forum/forumview.fxml")
public class ForumViewController implements Controller
{
	@FXML
	private TreeView<ForumHolder> forumTree;

	@FXML
	private SplitPane splitPane;

	private final ResourceBundle bundle;

	private final TreeItem<ForumHolder> ownForums;
	private final TreeItem<ForumHolder> subscribedForums;
	private final TreeItem<ForumHolder> popularForums;
	private final TreeItem<ForumHolder> otherForums;

	public ForumViewController(ResourceBundle bundle)
	{
		this.bundle = bundle;

		ownForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.own")));
		subscribedForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.subscribed")));
		popularForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.popular")));
		otherForums = new TreeItem<>(new ForumHolder(bundle.getString("forum.tree.other")));
	}

	@Override
	public void initialize() throws IOException
	{

		var root = new TreeItem<>(new ForumHolder());
		//noinspection unchecked
		root.getChildren().addAll(ownForums, subscribedForums, popularForums, otherForums);
		root.setExpanded(true);
		forumTree.setRoot(root);
		forumTree.setShowRoot(false);

	}
}
