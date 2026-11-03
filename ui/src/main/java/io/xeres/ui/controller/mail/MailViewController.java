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

package io.xeres.ui.controller.mail;

import io.xeres.ui.client.MailClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.ProgressPane;
import io.xeres.ui.model.mail.MailMessage;
import io.xeres.ui.support.loader.OnDemandLoader;
import io.xeres.ui.support.loader.OnDemandLoaderAction;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextFlow;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

import static io.xeres.common.mail.MailType.*;
import static javafx.scene.control.TreeTableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/mail/mail_view.fxml")
public class MailViewController implements Controller, OnDemandLoaderAction<MailFolder>, SmartLifecycle
{
	private static final Logger log = LoggerFactory.getLogger(MailViewController.class);

	private boolean running;

	@FXML
	private ListView<MailFolder> folderList;

	@FXML
	private SplitPane splitPaneVertical;

	@FXML
	private Button newMail;

	@FXML
	private SplitPane splitPaneHorizontal;

	@FXML
	private ProgressPane mailMessagesProgress;

	@FXML
	private TreeTableView<MailMessage> mailMessagesTreeTableView;

	@FXML
	private TreeTableColumn<MailMessage, String> treeTableSubject;

	@FXML
	private TreeTableColumn<MailMessage, MailMessage> treeTableAuthor;

	@FXML
	private TreeTableColumn<MailMessage, Instant> treeTableDate;

	@FXML
	private GridPane messageHeader;

	@FXML
	private Label messageAuthor;

	@FXML
	private Label messageDate;

	@FXML
	private Label messageSubject;

	@FXML
	private ScrollPane messagePane;

	@FXML
	private TextFlow messageContent;

	private final ObservableList<MailMessage> messages = FXCollections.observableArrayList();

	private OnDemandLoader<MailFolder, MailMessage> onDemandLoader;

	private TreeItem<MailMessage> mailMessagesRoot;

	private final MailClient mailClient;
	private final WindowManager windowManager;
	private final ResourceBundle bundle;

	public MailViewController(MailClient mailClient, WindowManager windowManager, ResourceBundle bundle)
	{
		this.mailClient = mailClient;
		this.windowManager = windowManager;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		folderList.setCellFactory(_ -> new MailFolderCell());
		folderList.getItems().addAll(
				new MailFolder(INBOX, new FontIcon(MaterialDesignI.INBOX_ARROW_DOWN)),
				new MailFolder(DRAFT, new FontIcon(MaterialDesignE.EMAIL_EDIT)),
				new MailFolder(OUTBOX, new FontIcon(MaterialDesignI.INBOX_ARROW_UP)),
				new MailFolder(SENT, new FontIcon(MaterialDesignE.EMAIL_ARROW_RIGHT)),
				new MailFolder(TRASH, new FontIcon(MaterialDesignT.TRASH_CAN_OUTLINE))
		);

		folderList.getSelectionModel().selectFirst();
		folderList.getSelectionModel().selectedItemProperty().addListener((_, oldSelection, newSelection) -> {
			if (newSelection == null)
			{
				folderList.getSelectionModel().select(oldSelection);
			}
			else
			{
				onDemandLoader.changeSelection(newSelection);
			}
		});

		mailMessagesTreeTableView.setRowFactory(_ -> new MailMessageCell());
		// XXX: context menu
		treeTableSubject.setCellValueFactory(new TreeItemPropertyValueFactory<>("subject"));
		// XXX: author, etc...

		mailMessagesRoot = new TreeItem<>(new MailMessage());
		mailMessagesTreeTableView.setRoot(mailMessagesRoot);
		mailMessagesTreeTableView.setShowRoot(false);

		mailMessagesTreeTableView.getSortOrder().add(treeTableDate);
		treeTableDate.setSortType(DESCENDING);
		treeTableDate.setSortable(true);

		// XXX: change selected message
		mailMessagesTreeTableView.getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> changeSelectedMailMessage(newValue != null ? newValue.getValue() : null));

		onDemandLoader = new OnDemandLoader<>(mailMessagesTreeTableView, messages, mailClient, this);

		newMail.setOnAction(_ -> createNewMail(false));
	}

	@Override
	public void start()
	{
		running = true;
	}

	@Override
	public void stop()
	{
		running = false;

		// XXX: dispose notifications
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	@Override
	public void onMessagesLoaded(MailFolder group)
	{
		refreshMessageList();
	}

	private List<TreeItem<MailMessage>> toTreeItemMailMessages(List<MailMessage> mailMessages)
	{
		return mailMessages.stream()
				.map(TreeItem::new)
				.toList();
	}

	private void selectMessageIfNeeded()
	{
		// XXX
	}

	private void mailMessagesState(boolean loading)
	{
		Platform.runLater(() -> mailMessagesProgress.showProgress(loading));
	}

	private void refreshMessageList()
	{
		saveSelection();
		mailMessagesRoot.getChildren().clear();
		mailMessagesRoot.getChildren().addAll(toTreeItemMailMessages(messages));
		mailMessagesTreeTableView.sort();
		selectMessageIfNeeded();

		mailMessagesState(false);
	}

	private void saveSelection()
	{
		// XXX
	}

	private void changeSelectedMailMessage(MailMessage mailMessage)
	{
		// XXX
	}

	private void createNewMail(boolean replyTo)
	{
		var replyToId = 0L;

		// XXX: check selected message, etc... like ForumView

		var mailRequest = new CreateMailRequest(replyToId);
		windowManager.openMailEditor(mailRequest);
	}

}
