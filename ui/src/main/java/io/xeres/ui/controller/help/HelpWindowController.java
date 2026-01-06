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
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Component
@FxmlView(value = "/view/help/help.fxml")
public class HelpWindowController implements WindowController
{
	public static final String INDEX_MD = "00.Index.md";

	private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "es", "fr", "ru");

	@FXML
	private Button back;

	@FXML
	private Button forward;

	@FXML
	private Button home;

	@FXML
	private ListView<Resource> indexList;

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

		var resources = Arrays.stream(resourcePatternResolver.getResources("classpath:help/" + language + "/*.md"))
				.filter(resource -> !StringUtils.defaultString(resource.getFilename()).equals(INDEX_MD))
				.toList();
		indexList.getItems().addAll(resources);
		indexList.setCellFactory(_ -> new IndexCell());
		indexList.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
			if (newValue != null)
			{
				navigator.navigate(new ExternalUri(newValue.getFilename()));
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
						UiUtils.showAlert(Alert.AlertType.ERROR, "Couldn't find resource for link '" + plain + "'");
					}
				}
				else
				{
					uriService.openUri(externalUri);
				}
			}
			else
			{
				UiUtils.showAlert(Alert.AlertType.ERROR, "Unhandled URI '" + uri + "'");
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

	private void selectListViewItemIfNeeded(String url)
	{
		if (url == null)
		{
			return;
		}

		indexList.getItems().stream()
				.filter(resource -> url.equals(resource.getFilename()))
				.findFirst()
				.ifPresentOrElse(resource -> indexList.getSelectionModel().select(resource), () -> indexList.getSelectionModel().clearSelection());
	}
}
