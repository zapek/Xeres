/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.help;

import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.uri.ExternalUri;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.Requester;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@Component
@FxmlView(value = "/view/help/help.fxml")
public class HelpWindowController implements WindowController
{
	public static final String INDEX_MD = "00.Index.md";
	public static final String SECTION_SETTINGS = "07";
	public static final String SECTION_SETTINGS_GENERAL = "07a";
	public static final String SECTION_SETTINGS_NOTIFICATIONS = "07b";
	public static final String SECTION_SETTINGS_NETWORK = "07c";
	public static final String SECTION_SETTINGS_TRANSFER = "07d";
	public static final String SECTION_SETTINGS_MEDIA = "07e";
	public static final String SECTION_SETTINGS_SOUND = "07f";
	public static final String SECTION_SETTINGS_REMOTE = "07g";

	private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "es", "fr", "ru", "zh");

	@FXML
	private Button back;

	@FXML
	private Button forward;

	@FXML
	private Button home;

	@FXML
	private TreeView<Resource> sectionTree;

	@FXML
	private EditorView editorView;

	private String language;

	private final MarkdownService markdownService;
	private final ResourcePatternResolver resourcePatternResolver;
	private final UriService uriService;
	private Navigator navigator;

	public HelpWindowController(MarkdownService markdownService, ResourcePatternResolver resourcePatternResolver, UriService uriService)
	{
		this.markdownService = markdownService;
		this.resourcePatternResolver = resourcePatternResolver;
		this.uriService = uriService;
	}

	@Override
	public void initialize() throws IOException
	{
		language = Stream.of(Locale.getDefault().getLanguage())
				.filter(SUPPORTED_LOCALES::contains)
				.findFirst()
				.orElse("en");

		var root = new TreeItem<Resource>(new ClassPathResource(""));

		//noinspection DataFlowIssue
		Arrays.stream(resourcePatternResolver.getResources("classpath:help/" + language + "/*.md"))
				.sorted(Comparator.comparing(Resource::getFilename))
				.filter(resource -> !StringUtils.defaultString(resource.getFilename()).equals(INDEX_MD))
				.forEach(resource -> addToSectionTree(root, resource));

		sectionTree.setRoot(root);
		sectionTree.setShowRoot(false);
		sectionTree.setCellFactory(_ -> new ResourceCell());
		sectionTree.getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> {
					if (newValue != null)
					{
						navigator.navigate(new ExternalUri(newValue.getValue().getFilename()));
					}
				});

		navigator = new Navigator(uri -> {
			if (uri instanceof ExternalUri externalUri)
			{
				var plain = uri.toUriString();

				if (navigator.isNavigable(uri))
				{
					var resource = HelpWindowController.class.getResourceAsStream("/help/" + language + "/" + plain);
					if (resource != null)
					{
						editorView.setMarkdown(resource);
						selectListViewItemIfNeeded(plain);
					}
					else
					{
						Requester.showError("Couldn't find resource for link '" + plain + "'");
					}
				}
				else
				{
					uriService.openUri(externalUri);
				}
			}
			else
			{
				Requester.showError("Unhandled URI '" + uri + "'");
			}
		});

		editorView.setMarkdownService(markdownService);

		back.disableProperty().bind(navigator.backProperty.not());
		forward.disableProperty().bind(navigator.forwardProperty.not());

		home.setOnAction(_ -> navigator.navigate(new ExternalUri(INDEX_MD)));
		back.setOnAction(_ -> navigator.navigateBackwards());
		forward.setOnAction(_ -> navigator.navigateForwards());

		editorView.setUriAction(navigator::navigate);
		navigator.navigate(new ExternalUri(INDEX_MD));
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(sectionTree);
		if (userData != null)
		{
			goToSection((String) userData);
		}
	}

	public void goToSection(String section)
	{
		sectionTree.getRoot().getChildren()
				.stream()
				.flatMap(resourceTreeItem -> resourceTreeItem.isLeaf() ? Stream.of(resourceTreeItem) : Stream.concat(Stream.of(resourceTreeItem), resourceTreeItem.getChildren().stream()))
				.filter(treeItem -> Objects.requireNonNull(treeItem.getValue().getFilename()).startsWith(section))
				.findFirst()
				.ifPresent(resource -> Platform.runLater(() -> sectionTree.getSelectionModel().select(resource)));
	}

	private static void addToSectionTree(TreeItem<Resource> sectionTree, Resource resource)
	{
		//noinspection DataFlowIssue
		if (resource.getFilename().indexOf(".") == 3)
		{
			sectionTree.getChildren().stream()
					.filter(resourceTreeItem -> Objects.requireNonNull(resourceTreeItem.getValue().getFilename()).startsWith(resource.getFilename().substring(0, 2)))
					.findFirst()
					.ifPresent(resourceTreeItem -> resourceTreeItem.getChildren().add(new TreeItem<>(resource)));
		}
		else
		{
			sectionTree.getChildren().add(new TreeItem<>(resource));
		}
	}

	private void selectListViewItemIfNeeded(String url)
	{
		if (url == null)
		{
			return;
		}
		sectionTree.getRoot().getChildren()
				.stream()
				.flatMap(resourceTreeItem -> resourceTreeItem.isLeaf() ? Stream.of(resourceTreeItem) : Stream.concat(Stream.of(resourceTreeItem), resourceTreeItem.getChildren().stream()))
				.filter(treeItem -> Strings.CS.equals(url, treeItem.getValue().getFilename()))
				.findFirst()
				.ifPresentOrElse(resource -> Platform.runLater(() -> sectionTree.getSelectionModel().select(resource)), () ->
						Platform.runLater(() -> sectionTree.getSelectionModel().clearSelection())); // Defer otherwise this breaks TreeItem's state as we're still using the list
	}
}
