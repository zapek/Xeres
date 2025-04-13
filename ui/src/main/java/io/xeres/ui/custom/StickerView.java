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

package io.xeres.ui.custom;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class StickerView extends VBox
{
	private static final Logger log = LoggerFactory.getLogger(StickerView.class);

	private static final int IMAGE_COLLECTION_WIDTH = 32;
	private static final int IMAGE_COLLECTION_HEIGHT = 32;

	private static final int IMAGE_WIDTH = 96; // XXX: should vary depending on the image size... use the collection image to guess it?
	private static final int IMAGE_HEIGHT = 96;

	@FXML
	private TabPane tabPane;

	public StickerView()
	{
		var loader = new FXMLLoader(getClass().getResource("/view/custom/stickerview.fxml"));
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

	public void loadStickers(final Path path)
	{
		Task<List<StickerCollectionEntry>> task = new Task<>()
		{
			@Override
			protected List<StickerCollectionEntry> call() throws Exception
			{
				List<StickerCollectionEntry> stickerCollections = new ArrayList<>();

				if (Files.isDirectory(path))
				{
					log.debug("Found sticker collections directory");
					try (var stream = Files.find(path, 1, (dirPath, bfa) -> bfa.isDirectory()))
					{
						return stream
								.filter(filePath -> !filePath.equals(path)) // filter out root directory
								.map(filePath -> new StickerCollectionEntry(filePath.getFileName().toString(), filePath, getStickerMainImage(filePath)))
								.sorted(Comparator.comparing(StickerCollectionEntry::name))
								.toList();
					}
				}
				return stickerCollections;
			}
		};
		task.setOnSucceeded(event -> {
			@SuppressWarnings("unchecked") var stickers = (List<StickerCollectionEntry>) event.getSource().getValue();

			tabPane.getTabs().addAll(stickers.stream()
					.map(sticker -> {
						Tab tab = null;
						if (sticker.image() != null && !sticker.image().isError())
						{
							tab = new Tab();
							tab.setTooltip(new Tooltip(sticker.name()));
							var imageView = new ImageView(sticker.image());
							imageView.setFitWidth(IMAGE_COLLECTION_WIDTH);
							imageView.setFitHeight(IMAGE_COLLECTION_HEIGHT);
							tab.setGraphic(imageView);
							tab.setUserData(sticker.path());
						}
						return tab;
					})
					.filter(Objects::nonNull)
					.toList());

			setupTabSelection();
		});
		Thread.ofVirtual().name("Stickers Directory Loader").start(task);
	}

	private void setupTabSelection()
	{
		if (!tabPane.getTabs().isEmpty())
		{
			loadTab(tabPane.getSelectionModel().getSelectedIndex());
		}
		tabPane.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> loadTab(newValue.intValue()));
	}

	private void loadTab(int index)
	{
		var tab = tabPane.getTabs().get(index);

		if (tab.getContent() == null)
		{
			var path = (Path) tab.getUserData();
			var textFlow = new TextFlow();
			textFlow.setPrefWidth(600.0);
			textFlow.setOnMouseClicked(mouseEvent -> {
				log.debug("source: {}, target: {}", mouseEvent.getSource(), mouseEvent.getTarget());
				if (mouseEvent.getTarget() instanceof ImageView imageView)
				{
					fireEvent(new StickerClickedEvent((Path) imageView.getUserData()));
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
						try (var stream = Files.find(path, 1, (dirPath, bfa) -> bfa.isRegularFile()))
						{
							stream
									.filter(filePath -> !filePath.equals(path))
									.sorted(Comparator.comparing(filePath -> filePath.getFileName().toString()))
									.forEach(filePath -> {
										var image = openImage(filePath, IMAGE_WIDTH, IMAGE_HEIGHT);
										if (image != null && !image.isError())
										{
											Platform.runLater(() -> {
												var imageView = new ImageView(image);
												imageView.setFitWidth(IMAGE_WIDTH);
												imageView.setFitHeight(IMAGE_HEIGHT);
												imageView.setUserData(filePath);
												textFlow.getChildren().add(imageView);
											});
										}
									});
						}
					}
					return null;
				}
			};
			Thread.ofVirtual().name("Stickers Loader").start(task);
		}
	}

	private static Image getStickerMainImage(Path directory)
	{
		try (var stream = Files.find(directory, 1, (path, bfa) -> bfa.isRegularFile()))
		{
			return stream
					.findFirst()
					.map(path -> openImage(path, IMAGE_COLLECTION_WIDTH, IMAGE_COLLECTION_HEIGHT))
					.orElse(null);
		}
		catch (IOException e)
		{
			log.error("Couldn't get sticker main image from {}: {}", directory, e.getMessage());
			return null;
		}
	}

	private static Image openImage(Path path, int width, int height)
	{
		try
		{
			return new Image(new FileInputStream(path.toFile()), width * 1.5, height * 1.5, false, true); // Less than 1.5 and it's blurred, go figure...
		}
		catch (FileNotFoundException e)
		{
			log.debug("Couldn't open image {}: {}", path, e.getMessage());
			return null;
		}
	}

	private record StickerCollectionEntry(String name, Path path, Image image)
	{
	}
}
