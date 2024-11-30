/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.version;

import io.micrometer.common.util.StringUtils;
import io.xeres.ui.support.preference.PreferenceService;

import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import static io.xeres.ui.support.preference.PreferenceService.UPDATE_CHECK;

public class VersionChecker
{
	private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d{1,5})\\.(\\d{1,5})\\.(\\d{1,5})$");

	private static final String LAST_CHECK = "LastCheck";
	private static final String ENABLED = "Enabled";
	private static final String SKIP = "Skip";

	private static final Duration TIME_BETWEEN_CHECKS = Duration.ofDays(1);
	private static final long SKEW_SECONDS = 240; // Seconds around the checks to avoid tracking

	private final Preferences preferences;

	public VersionChecker(PreferenceService preferenceService)
	{
		preferences = preferenceService.getPreferences().node(UPDATE_CHECK);
	}

	public boolean isVersionMoreRecent(String newVersionString, String currentVersionString)
	{
		if (StringUtils.isBlank(newVersionString))
		{
			return false;
		}

		var currentVersion = createVersion(currentVersionString);
		if (currentVersion.isNotARelease())
		{
			return false;
		}

		var newVersion = createVersion(newVersionString);

		var versionToSkip = createVersion(preferences.get(SKIP, "0.0.0"));
		if (newVersion.equals(versionToSkip))
		{
			return false;
		}
		return newVersion.compareTo(currentVersion) > 0;
	}

	private static Version createVersion(String versionString)
	{
		if (!versionString.startsWith("v"))
		{
			versionString = "v" + versionString;
		}
		var matcher = VERSION_PATTERN.matcher(versionString);

		if (matcher.matches())
		{
			return new Version((Integer.parseInt(matcher.group(1))), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
		}
		return new Version(0, 0, 0);
	}

	public void scheduleVersionCheck(Runnable check)
	{
		var timer = new Timer("Version Update Checker", true);

		Runnable task = () -> {
			if (shouldCheckForUpdate(preferences))
			{
				check.run();
			}
		};

		var versionCheckTask = new VersionCheckTask(task);
		var skew = ThreadLocalRandom.current().nextLong(SKEW_SECONDS) - SKEW_SECONDS / 2;
		timer.scheduleAtFixedRate(versionCheckTask, 0L, Duration.ofDays(1).plus(Duration.ofSeconds(skew)).toMillis());
	}

	public void skipUpdate(String versionString)
	{
		var versionToSkip = createVersion(versionString);
		if (versionToSkip.isNotARelease())
		{
			return;
		}
		preferences.put(SKIP, versionToSkip.toString());
	}

	private static boolean shouldCheckForUpdate(Preferences preferences)
	{
		var now = Instant.now();
		var shouldCheck = preferences.getBoolean(ENABLED, false) && Duration.between(Instant.ofEpochMilli(preferences.getLong(LAST_CHECK, 0)), now).abs().compareTo(TIME_BETWEEN_CHECKS.minus(Duration.ofSeconds(SKEW_SECONDS))) > 0;
		if (shouldCheck)
		{
			preferences.putLong(LAST_CHECK, now.toEpochMilli());
		}
		return shouldCheck;
	}

	public static boolean isCheckForUpdates(Preferences preferences)
	{
		return preferences.getBoolean(ENABLED, false);
	}

	public static void setCheckForUpdates(Preferences preferences, boolean check)
	{
		preferences.putBoolean(ENABLED, check);
	}
}
