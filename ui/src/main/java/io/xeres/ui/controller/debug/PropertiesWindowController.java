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

package io.xeres.ui.controller.debug;

import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.contextmenu.XContextMenu;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static javafx.scene.control.TableColumn.SortType.ASCENDING;

@Component
@FxmlView(value = "/view/debug/properties.fxml")
public class PropertiesWindowController implements WindowController
{
	private static final String COPY_MENU_ID = "copy";

	@FXML
	private TableView<Map.Entry<String, String>> propertiesTableView;

	@FXML
	private TableColumn<Map.Entry<String, String>, String> tableName;

	@FXML
	private TableColumn<Map.Entry<String, String>, String> tableValue;

	@FXML
	private MenuItem copyAll;

	@Override
	public void initialize() throws IOException
	{
		createPropertiesTableViewContextMenu();

		tableName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey()));
		tableValue.setCellValueFactory(param -> new SimpleStringProperty(showLineSeparator(param.getValue().getValue())));

		propertiesTableView.getItems().addAll(FXCollections.observableArrayList(getSortedProperties().entrySet()));

		propertiesTableView.getSortOrder().add(tableName);
		tableName.setSortType(ASCENDING);

		copyAll.setOnAction(event -> {
			var clipboardContent = new ClipboardContent();
			var sb = new StringBuilder();
			getSortedProperties().forEach((k, v) -> sb.append(k).append(": ").append(showLineSeparator(v)).append("\n"));
			clipboardContent.putString(showLineSeparator(sb.toString()));
			Clipboard.getSystemClipboard().setContent(clipboardContent);
		});
	}

	private LinkedHashMap<String, String> getSortedProperties()
	{
		var properties = System.getProperties();

		return properties.entrySet().stream()
				.collect(Collectors.toMap(k -> (String) k.getKey(), e -> (String) e.getValue()))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	private String showLineSeparator(String in)
	{
		in = in.replace("\n", "\\n");
		in = in.replace("\r", "\\r");
		return in;
	}

	private void createPropertiesTableViewContextMenu()
	{
		var copyItem = new MenuItem("Copy entry");
		copyItem.setId(COPY_MENU_ID);
		copyItem.setOnAction(event -> {
			@SuppressWarnings("unchecked") var entry = (Map.Entry<String, String>) event.getSource();
			if (entry != null)
			{
				var clipboardContent = new ClipboardContent();
				clipboardContent.putString(entry.getKey() + " = " + showLineSeparator(entry.getValue()));
				Clipboard.getSystemClipboard().setContent(clipboardContent);
			}
		});
		new XContextMenu<Map.Entry<String, String>>(propertiesTableView, copyItem);
	}
}
