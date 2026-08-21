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

package io.xeres.ui.custom.sticker;

import com.lottie4j.fxplayer.LottiePlayer;
import io.xeres.common.util.LottieUtils;
import io.xeres.ui.custom.event.StickerSelectedEvent;
import io.xeres.ui.custom.event.StickerSelectedEvent.StickerType;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class StickerView extends VBox
{
	private static final Logger log = LoggerFactory.getLogger(StickerView.class);

	public static final int IMAGE_MAIN_WIDTH = 32;
	public static final int IMAGE_MAIN_HEIGHT = 32;

	public static final int IMAGE_WIDTH = 80;
	public static final int IMAGE_HEIGHT = 80;

	@FXML
	private TabPane tabPane;

	private final int stickerSizeLimit;

	public StickerView(int sizeLimit)
	{
		stickerSizeLimit = sizeLimit;

		var loader = new FXMLLoader(StickerView.class.getResource("/view/custom/sticker_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void loadStickers(Path localPath, Path userPath)
	{
		Task<List<StickerCollectionEntry>> task = new Task<>()
		{
			@Override
			protected List<StickerCollectionEntry> call() throws Exception
			{
				List<StickerCollectionEntry> stickerCollections = new ArrayList<>();

				if (Files.isDirectory(localPath))
				{
					try (var stream = Files.find(localPath, 1, (dirPath, bfa) -> bfa.isDirectory() && !dirPath.equals(localPath)))
					{
						stickerCollections.addAll(processStickers(stream));
					}
				}

				if (Files.isDirectory(userPath))
				{
					log.debug("Found sticker collections directory in {}", userPath);
					try (var stream = Files.find(userPath, 1, (dirPath, bfa) -> bfa.isDirectory() && !dirPath.equals(userPath)))
					{
						stickerCollections.addAll(processStickers(stream));
					}
				}
				return stickerCollections.stream()
						.sorted(Comparator.comparing(StickerCollectionEntry::name))
						.toList();
			}
		};
		task.setOnSucceeded(event -> {
			@SuppressWarnings("unchecked") var stickers = (List<StickerCollectionEntry>) event.getSource().getValue();

			tabPane.getTabs().addAll(stickers.stream()
					.map(stickerCollectionEntry -> {
						Tab tab = null;
						if (stickerCollectionEntry.sticker().hasNode())
						{
							tab = new Tab();
							var tooltipName = new StickerNameBuilder()
									.name(stickerCollectionEntry.name())
									.build();
							if (StringUtils.isNotEmpty(tooltipName))
							{
								tab.setTooltip(new Tooltip(tooltipName));
							}
							tab.setGraphic(stickerCollectionEntry.sticker().createMainNode());
							tab.setUserData(stickerCollectionEntry.path());
						}
						return tab;
					})
					.filter(Objects::nonNull)
					.toList());

			Tab emptyTab = new EmptyStickerTab(userPath);

			tabPane.getTabs().add(emptyTab);

			setupTabSelection();
		});
		Thread.ofVirtual().name("Stickers Collection Directory Loader").start(task);
	}

	private List<StickerCollectionEntry> processStickers(Stream<Path> stream)
	{
		return stream
				.map(filePath -> new StickerCollectionEntry(filePath.getFileName().toString(), filePath, getMainSticker(filePath)))
				.toList();
	}

	private void setupTabSelection()
	{
		if (!tabPane.getTabs().isEmpty())
		{
			loadTab(tabPane.getSelectionModel().getSelectedIndex());
		}
		tabPane.getSelectionModel().selectedIndexProperty().addListener((_, _, newValue) -> loadTab(newValue.intValue()));
	}

	private void loadTab(int index)
	{
		var tab = tabPane.getTabs().get(index);

		if (tab.getContent() == null)
		{
			var path = (Path) tab.getUserData();
			var textFlow = new TextFlow();
			textFlow.setPrefWidth(600.0);
			textFlow.setPadding(new Insets(8.0));
			UiUtils.setOnPrimaryMouseClicked(textFlow, event -> {
				if (event.getTarget() instanceof ImageView imageView)
				{
					fireEvent(new StickerSelectedEvent((Sticker) imageView.getUserData(), StickerType.IMAGE));
				}
				else if (event.getTarget() instanceof LottiePlayer lottiePlayer)
				{
					fireEvent(new StickerSelectedEvent((Sticker) lottiePlayer.getUserData(), StickerType.LOTTIE));
				}
			});
			var scrollPane = new ScrollPane(textFlow);
			scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			tab.setContent(scrollPane);

			Task<Void> task = new Task<>()
			{
				@Override
				protected Void call() throws Exception
				{
					if (Files.isDirectory(path))
					{
						try (var stream = Files.find(path, 1, (path, bfa) -> bfa.isRegularFile() && (!LottieUtils.isLottieSubSet(path) || LottieUtils.isLottieSizeSmallEnough(bfa.size(), stickerSizeLimit))))
						{
							stream
									.sorted(Comparator.comparing(filePath -> filePath.getFileName().toString()))
									.map(StickerFactory::create)
									.filter(Sticker::hasNode)
									.forEach(sticker -> Platform.runLater(() -> {
										var pane = new StackPane(sticker.createNode());
										pane.setPrefWidth(IMAGE_WIDTH + 16.0);
										pane.setPrefHeight(IMAGE_HEIGHT + 16.0);
										textFlow.getChildren().add(pane);
									}));
						}
					}
					return null;
				}
			};
			Thread.ofVirtual().name("Stickers Collection Content Loader").start(task);
		}
	}

	private static Sticker getMainSticker(Path directory)
	{
		try (var stream = Files.find(directory, 1, (_, bfa) -> bfa.isRegularFile()))
		{
			return stream.min(Comparator.comparing(Path::getFileName))
					.map(StickerFactory::create)
					.filter(Sticker::hasNode)
					.orElse(null);
		}
		catch (IOException e)
		{
			log.error("Couldn't get sticker main image from {}: {}", directory, e.getMessage());
			return null;
		}
	}

	private record StickerCollectionEntry(String name, Path path, Sticker sticker)
	{
	}
}
