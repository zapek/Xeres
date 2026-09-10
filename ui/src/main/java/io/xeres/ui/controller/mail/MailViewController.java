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

import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.ProgressPane;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextFlow;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static io.xeres.ui.controller.mail.MailFolderType.*;

@Component
@FxmlView(value = "/view/mail/mail_view.fxml")
public class MailViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(MailViewController.class);

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
	private TableView<String> mailMessagesTableView; // XXX: not a string

	@FXML
	private TableColumn<String, String> tableSubject; // XXX: first not a string

	@FXML
	private TableColumn<String, String> tableAuthor; // XXX: first not a string, 2nd not either?

	@FXML
	private TableColumn<String, String> tableDate; // XXX: idtto

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

	private final ResourceBundle bundle;

	public MailViewController(ResourceBundle bundle)
	{
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		folderList.setCellFactory(_ -> new MailFolderCell());
		folderList.getItems().addAll(
				new MailFolder(bundle.getString("mail.view.inbox"), INBOX, new FontIcon(MaterialDesignI.INBOX_ARROW_DOWN)),
				new MailFolder(bundle.getString("mail.view.draft"), DRAFT, new FontIcon(MaterialDesignE.EMAIL_EDIT)),
				new MailFolder(bundle.getString("mail.view.outbox"), OUTBOX, new FontIcon(MaterialDesignI.INBOX_ARROW_UP)),
				new MailFolder(bundle.getString("mail.view.sent"), SENT, new FontIcon(MaterialDesignE.EMAIL_ARROW_RIGHT)),
				new MailFolder(bundle.getString("mail.view.trash"), TRASH, new FontIcon(MaterialDesignT.TRASH_CAN_OUTLINE))
		);

		folderList.getSelectionModel().selectFirst();
		folderList.getSelectionModel().selectedItemProperty().addListener((_, oldSelection, newSelection) -> {
			if (newSelection == null)
			{
				folderList.getSelectionModel().select(oldSelection);
			}
		});
	}
}
