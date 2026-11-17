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

import io.xeres.common.mail.MailType;
import io.xeres.common.rest.notification.mail.AddMailMessages;
import io.xeres.common.rest.notification.mail.RemoveMailMessages;
import io.xeres.common.rest.notification.mail.SetMailMessageReadState;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.MailClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.ProgressPane;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.mail.MailMapper;
import io.xeres.ui.model.mail.MailMessage;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.loader.OnDemandLoader;
import io.xeres.ui.support.loader.OnDemandLoaderAction;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.markdown.MarkdownService.Rendering;
import io.xeres.ui.support.util.TextFlowDragSelection;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextFlow;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static io.xeres.common.mail.MailType.*;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_PRECISE_FORMAT;
import static javafx.scene.control.TreeTableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/mail/mail_view.fxml")
public class MailViewController implements Controller, OnDemandLoaderAction<MailFolder>, SmartLifecycle
{
	private static final Logger log = LoggerFactory.getLogger(MailViewController.class);
	private final MarkdownService markdownService;

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

	private MailMessage selectedMailMessage;

	private OnDemandLoader<MailFolder, MailMessage> onDemandLoader;

	private TreeItem<MailMessage> mailMessagesRoot;

	private Disposable notificationDisposable;

	private Long toSelectMsgId;

	private final MailClient mailClient;
	private final WindowManager windowManager;
	private final GeneralClient generalClient;
	private final ImageCache imageCacheService;
	private final NotificationClient notificationClient;
	private final ResourceBundle bundle;

	public MailViewController(MailClient mailClient, WindowManager windowManager, GeneralClient generalClient, ImageCache imageCacheService, NotificationClient notificationClient, ResourceBundle bundle, MarkdownService markdownService)
	{
		this.mailClient = mailClient;
		this.windowManager = windowManager;
		this.generalClient = generalClient;
		this.imageCacheService = imageCacheService;
		this.notificationClient = notificationClient;
		this.bundle = bundle;
		this.markdownService = markdownService;
	}

	@Override
	public void start()
	{
		running = true;
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

		folderList.getSelectionModel().selectedItemProperty().addListener((_, oldSelection, newSelection) -> {
			if (newSelection == null)
			{
				folderList.getSelectionModel().select(oldSelection);
			}
			else
			{
				mailMessagesState(true);
				onDemandLoader.changeSelection(newSelection);
			}
		});

		mailMessagesTreeTableView.setRowFactory(_ -> new MailMessageCell());
		createMailMessageTableViewContextMenu();
		treeTableSubject.setCellValueFactory(new TreeItemPropertyValueFactory<>("subject"));
		treeTableAuthor.setCellFactory(_ -> new MailCellAuthor(generalClient, imageCacheService));
		treeTableAuthor.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));

		treeTableDate.setCellFactory(_ -> new DateCell());
		treeTableDate.setCellValueFactory(new TreeItemPropertyValueFactory<>("published"));

		mailMessagesRoot = new TreeItem<>(new MailMessage());
		mailMessagesTreeTableView.setRoot(mailMessagesRoot);
		mailMessagesTreeTableView.setShowRoot(false);

		mailMessagesTreeTableView.getSortOrder().add(treeTableDate);
		treeTableDate.setSortType(DESCENDING);
		treeTableDate.setSortable(true);

		mailMessagesTreeTableView.getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> changeSelectedMailMessage(newValue != null ? newValue.getValue() : null));

		onDemandLoader = new OnDemandLoader<>(mailMessagesTreeTableView, messages, mailClient, this);

		newMail.setOnAction(_ -> createNewMail(ReplyMode.NONE));

		folderList.getSelectionModel().selectFirst();

		setupMailNotifications();

		TextFlowDragSelection.enableSelection(messageContent, messagePane);
	}

	@Override
	public void stop()
	{
		running = false;

		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
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

	private void createMailMessageTableViewContextMenu()
	{
		var replyItem = new MenuItem(bundle.getString("mail.view.reply"));
		replyItem.setGraphic(new FontIcon(MaterialDesignR.REPLY));
		replyItem.setOnAction(_ -> createNewMail(ReplyMode.SENDER));

		var replyAllItem = new MenuItem(bundle.getString("mail.view.reply-all"));
		replyAllItem.setGraphic(new FontIcon(MaterialDesignR.REPLY_ALL));
		replyAllItem.setOnAction(_ -> createNewMail(ReplyMode.ALL));

		var forwardItem = new MenuItem(bundle.getString("mail.view.forward"));
		forwardItem.setGraphic(new FontIcon(MaterialDesignF.FORWARD));
		forwardItem.setOnAction(_ -> createNewMail(ReplyMode.FORWARD));

		var markUnreadItem = new MenuItem(bundle.getString("mark-unread"));
		markUnreadItem.setGraphic(new FontIcon(MaterialDesignE.EMAIL_MARK_AS_UNREAD));
		markUnreadItem.setOnAction(_ -> markAsUnread());

		var deleteItem = new MenuItem(bundle.getString("delete"));
		deleteItem.setGraphic(new FontIcon(MaterialDesignE.EMAIL_REMOVE));
		deleteItem.setOnAction(_ -> delete());

		var xContextMenu = new XContextMenu<TreeItem<MailMessage>>(replyItem, replyAllItem, forwardItem, new SeparatorMenuItem(), markUnreadItem, deleteItem);
		xContextMenu.setOnShowing((contextMenu, treeItem) -> {
			if (treeItem == null)
			{
				return false;
			}
			// XXX: add conditions?
			return true;
		});
		xContextMenu.addToNode(mailMessagesTreeTableView);

	}

	private List<TreeItem<MailMessage>> toTreeItemMailMessages(List<MailMessage> mailMessages)
	{
		return mailMessages.stream()
				.map(TreeItem::new)
				.toList();
	}

	private void setMessageToSelect(Long msgId)
	{
		if (msgId != null)
		{
			toSelectMsgId = msgId;
		}
	}

	private void selectMessageIfNeeded()
	{
		if (toSelectMsgId != null)
		{
			mailMessagesRoot.getChildren().stream()
					.filter(mailMessageTreeItem -> Objects.equals(mailMessageTreeItem.getValue().getId(), toSelectMsgId))
					.findFirst()
					.ifPresent(mailMessageTreeItem -> Platform.runLater(() -> mailMessagesTreeTableView.getSelectionModel().select(mailMessageTreeItem)));
		}
	}

	private void mailMessagesState(boolean loading)
	{
		Platform.runLater(() -> mailMessagesProgress.showProgress(loading));
	}

	private void saveSelection()
	{
		var selectedItem = mailMessagesTreeTableView.getSelectionModel().getSelectedItem();
		if (selectedItem != null)
		{
			setMessageToSelect(selectedItem.getValue().getId());
			mailMessagesTreeTableView.getSelectionModel().clearSelection();
		}
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

	private void addMessageContent(String input)
	{
		messageContent.getChildren().addAll(markdownService.parse(input, EnumSet.noneOf(Rendering.class)).stream()
				.map(Content::getNode).toList());
	}

	private void setCommonMessageAttributes(MailMessage mailMessage)
	{
		messageContent.getChildren().clear();
		messagePane.setVvalue(messagePane.getVmin()); // Reset scroll position
		addMessageContent(mailMessage.getContent());
		messageDate.setText(DATE_TIME_PRECISE_FORMAT.format(mailMessage.getPublished()));
		messageSubject.setText(mailMessage.getSubject());
	}

	private void changeSelectedMailMessage(MailMessage mailMessage)
	{
		selectedMailMessage = mailMessage;

		if (mailMessage != null)
		{
			mailClient.getMessage(mailMessage.getId())
					.doOnSuccess(message -> Platform.runLater(() -> {
						assert message != null;
						setCommonMessageAttributes(message);
						messageAuthor.setText(message.getAuthorName());
						// XXX: create author context menu
						UiUtils.setPresent(messageHeader);
						if (!message.isRead())
						{
							mailClient.setMailMessageReadState(message.getId(), true)
									.subscribe();
						}
					}))
					.doOnError(UiUtils::webAlertError)
					.subscribe();
		}
		else
		{
			clearMessage();
		}
	}

	private void clearMessage()
	{
		UiUtils.setAbsent(messageHeader);
		messageAuthor.setText(null);
		messageAuthor.setContextMenu(null);
		messageDate.setText(null);
		messageSubject.setText(null);
		messageContent.getChildren().clear();
	}

	private void createNewMail(ReplyMode replyMode)
	{
		var replyToId = 0L;

		if (selectedMailMessage != null)
		{
			replyToId = replyMode != ReplyMode.NONE ? selectedMailMessage.getId() : 0L;
		}

		var mailRequest = new CreateMailRequest(replyToId, replyMode);
		windowManager.openMailEditor(mailRequest);
	}

	private void markAsUnread()
	{
		if (selectedMailMessage == null) // Should not happen
		{
			return;
		}

		mailClient.setMailMessageReadState(selectedMailMessage.getId(), false)
				.subscribe();
	}

	private void delete()
	{
		if (selectedMailMessage == null) // Should not happen
		{
			return;
		}

		// XXX: add a delete and/or move to trash to mailClient?
	}

	private void setupMailNotifications()
	{
		notificationDisposable = notificationClient.getMailNotifications()
				.doOnError(UiUtils::webAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					switch (sse.data())
					{
						case AddMailMessages action -> addMailMessages(action.mailMessages().stream()
								.map(MailMapper::fromDTO)
								.toList());
						case RemoveMailMessages action ->
						{
						}
						case SetMailMessageReadState action -> setMessageReadState(action.mailType(), action.messageId(), action.read());
						case null -> throw new IllegalArgumentException("Mail notifications have not been set");
					}
				}))
				.subscribe();
	}

	private void addMailMessages(List<MailMessage> mailMessages)
	{
		// XXX: add update like addForumMessages()

		for (MailMessage mailMessage : mailMessages)
		{
			onDemandLoader.insertMessage(mailMessage);
			// XXX
		}
		// XXX: unread count, etc...
		refreshMessageList();
	}

	private void setMessageReadState(MailType mailType, long messageId, boolean read)
	{
		// Avoids flickering because of some current Flowless limitation
		if (selectedMailMessage != null && selectedMailMessage.getId() == messageId && selectedMailMessage.isRead() != read)
		{
			// XXX: mailTree.setUnreadCount(mailType, read);
			selectedMailMessage.setRead(read);
			mailMessagesTreeTableView.refresh();
		}
	}
}
