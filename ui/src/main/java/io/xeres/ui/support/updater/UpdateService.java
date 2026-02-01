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

package io.xeres.ui.support.updater;

import io.xeres.common.AppName;
import io.xeres.common.util.OsUtils;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.client.update.ReleaseAsset;
import io.xeres.ui.client.update.ReleaseResponse;
import io.xeres.ui.client.update.UpdateClient;
import io.xeres.ui.controller.MainWindowController;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import jakarta.annotation.Nullable;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static java.lang.Boolean.TRUE;

@Service
public class UpdateService
{
	private static final Logger log = LoggerFactory.getLogger(UpdateService.class);

	private static final String XERES_DOWNLOAD_URL = "https://xeres.io/download";

	private static final String WINDOWS_INSTALLER_EXTENSION = ".msi";

	private VersionChecker versionChecker;

	private final MainWindowController mainWindowController;
	private final UpdateClient updateClient;
	private final ConfigClient configClient;
	private final BuildProperties buildProperties;
	private final HostServices hostServices;
	private final TrayService trayService;
	private final ResourceBundle bundle;

	public UpdateService(MainWindowController mainWindowController, UpdateClient updateClient, ConfigClient configClient, BuildProperties buildProperties, @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") @Nullable HostServices hostServices, TrayService trayService, ResourceBundle bundle)
	{
		this.mainWindowController = mainWindowController;
		this.updateClient = updateClient;
		this.configClient = configClient;
		this.buildProperties = buildProperties;
		this.hostServices = hostServices;
		this.trayService = trayService;
		this.bundle = bundle;
	}

	public void startBackgroundChecksIfEnabled()
	{
		versionChecker = new VersionChecker();
		versionChecker.scheduleVersionCheck(this::checkForUpdateInBackground);
	}

	public void checkForUpdate()
	{
		updateClient.getLatestVersion()
				.doOnSuccess(releaseResponse -> Platform.runLater(() -> {
					assert releaseResponse != null;
					if (versionChecker.isVersionMoreRecent(releaseResponse.tagName(), buildProperties.getVersion()))
					{
						UiUtils.showAlertConfirm(MessageFormat.format(bundle.getString("update.new-version"), releaseResponse.tagName().substring(1)), () -> download(releaseResponse));
					}
					else
					{
						UiUtils.showAlert(Alert.AlertType.INFORMATION, bundle.getString("update.latest-already"));
					}
				}))
				.doOnError(UiUtils::webAlertError)
				.subscribe();
	}

	public void skipUpdate(String tagName)
	{
		versionChecker.skipUpdate(tagName);
	}

	public boolean isAutomaticallyCheckingForUpdates(Preferences preferences)
	{
		return VersionChecker.isCheckForUpdates(preferences);
	}

	public void setAutomaticCheckForUpdates(Preferences preferences, boolean check)
	{
		VersionChecker.setCheckForUpdates(preferences, check);
	}

	private void checkForUpdateInBackground()
	{
		updateClient.getLatestVersion()
				.doOnSuccess(releaseResponse -> Platform.runLater(() -> {
					assert releaseResponse != null;
					if (versionChecker.isVersionMoreRecent(releaseResponse.tagName(), buildProperties.getVersion()))
					{
						mainWindowController.showUpdate(MessageFormat.format(bundle.getString("update.new-version-auto"), releaseResponse.tagName().substring(1)), releaseResponse.tagName(), () -> download(releaseResponse));
					}
				}))
				.subscribe();
	}

	private void download(ReleaseResponse releaseResponse)
	{
		if (SystemUtils.IS_OS_WINDOWS)
		{
			var url = releaseResponse.assets().stream()
					.filter(asset -> asset.name().startsWith(AppName.NAME) && asset.name().endsWith(WINDOWS_INSTALLER_EXTENSION))
					.findAny()
					.map(ReleaseAsset::url)
					.orElse(null);

			var signingUrl = releaseResponse.assets().stream()
					.filter(asset -> asset.name().startsWith(AppName.NAME) && asset.name().endsWith(WINDOWS_INSTALLER_EXTENSION + ".sig"))
					.findAny()
					.map(ReleaseAsset::url)
					.orElse(null);

			if (url != null && signingUrl != null)
			{
				download(url, signingUrl);
			}
			else
			{
				log.debug("Couldn't download url and/or signing url");
				UiUtils.showAlert(Alert.AlertType.ERROR, bundle.getString("update.download-failure"));
			}
		}
		else
		{
			if (hostServices != null)
			{
				hostServices.showDocument(XERES_DOWNLOAD_URL);
			}
		}
	}

	private void download(String url, String signingUrl)
	{
		log.debug("Downloading {}", url);
		Path tempFile;
		try
		{
			tempFile = Files.createTempFile(AppName.NAME + "_update_", WINDOWS_INSTALLER_EXTENSION);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		var progressBar = new ProgressBar(0);
		var dialogPane = new DialogPane();
		dialogPane.setHeaderText(bundle.getString("update.download-file"));
		dialogPane.getButtonTypes().addAll(ButtonType.CANCEL); // XXX: how can I make it do something?
		dialogPane.setContent(progressBar);
		dialogPane.setMinHeight(Region.USE_PREF_SIZE);
		var dialog = new Dialog<Void>();
		var defaultOwnerWindow = WindowManager.getDefaultOwnerWindow();
		if (defaultOwnerWindow != null)
		{
			dialog.initOwner(defaultOwnerWindow);
		}
		dialog.setDialogPane(dialogPane);
		dialog.setWidth(320);

		dialog.setTitle(bundle.getString("update.download.title"));

		var stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		UiUtils.setDefaultIcon(stage);
		UiUtils.setDefaultStyle(stage.getScene());

		dialog.show();

		updateClient.downloadFile(signingUrl)
				.publishOn(Schedulers.boundedElastic())
				.doOnSuccess(signature -> updateClient.downloadFileWithProgress(url, tempFile, progress -> Platform.runLater(() -> {
							progressBar.setProgress(progress.getProgress());
							log.debug("Progress: {}", progress.getProgress());
						}))
						.doOnComplete(() -> Platform.runLater(() -> {
							log.debug("Download complete");
							dialogPane.getButtonTypes().clear();
							var installButtonType = new ButtonType(bundle.getString("update.download.install"));
							dialogPane.getButtonTypes().addAll(installButtonType);

							var installButton = dialogPane.lookupButton(installButtonType);
							installButton.setDisable(true);
							dialogPane.setHeaderText(bundle.getString("update.download.verifying"));
							progressBar.setProgress(-1);

							UiUtils.setAbsent(progressBar);

							configClient.verifyUpdate(tempFile.toAbsolutePath().toString(), signature)
									.doOnSuccess(signingResult -> Platform.runLater(() -> {
										if (TRUE.equals(signingResult))
										{
											log.debug("File verified successfully");
											dialogPane.setHeaderText(bundle.getString("update.download.install-ready"));
											installButton.setDisable(false);
											installButton.setOnMouseReleased(_ -> install(tempFile.toFile()));
										}
										else
										{
											dialogPane.setHeaderText(bundle.getString("update.download-verification-failed"));
											log.debug("Verification failed!");
											// XXX: set button as either retry or close...
										}
									}))
									.subscribe();
						}))
						.doOnError(UiUtils::webAlertError)
						.subscribe())
				.subscribe();
	}

	private void install(File file)
	{
		OsUtils.shellOpen(file);
		trayService.exitApplication();
	}
}
