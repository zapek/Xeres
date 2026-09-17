/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chess;

import io.xeres.ui.support.preference.PreferenceUtils;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChessSettingsTest
{
	@Test
	void defaultsAndSavedChoicesUseLocationPreferences()
	{
		var preferences = mock(Preferences.class);
		when(preferences.node("Chess")).thenReturn(preferences);
		when(preferences.get("BoardTheme", "BROWN")).thenReturn("BROWN");
		when(preferences.getBoolean("InviteSound", true)).thenReturn(true);
		when(preferences.getBoolean("MoveSound", true)).thenReturn(true);
		when(preferences.getBoolean("CaptureSound", true)).thenReturn(true);
		try (var utility = mockStatic(PreferenceUtils.class))
		{
			utility.when(PreferenceUtils::getPreferences).thenReturn(preferences);
			var settings = new ChessSettings();
			assertEquals(ChessBoardTheme.BROWN, settings.getTheme());
			assertTrue(settings.isInviteEnabled());
			assertTrue(settings.isMoveEnabled());
			assertTrue(settings.isCaptureEnabled());
			settings.save(ChessBoardTheme.CHECKERS, false, true, true, true, true, false);
			assertFalse(settings.isInviteEnabled());
			verify(preferences).putBoolean("InviteSound", false);
			when(preferences.getBoolean("InviteSound", true)).thenReturn(false);
			assertEquals(ChessBoardTheme.CHECKERS, settings.themeProperty().get());
			assertFalse(settings.isMoveEnabled());
			verify(preferences).put("BoardTheme", "CHECKERS");
			verify(preferences).putBoolean("MoveSound", false);
			when(preferences.get("BoardTheme", "BROWN")).thenReturn("CHECKERS");
			when(preferences.getBoolean("MoveSound", true)).thenReturn(false);
			var reloaded = new ChessSettings();
			assertEquals(ChessBoardTheme.CHECKERS, reloaded.getTheme());
			assertFalse(reloaded.isInviteEnabled());
			assertFalse(reloaded.isMoveEnabled());
		}
		assertEquals(ChessBoardTheme.BROWN, ChessBoardTheme.fromPreference("UNKNOWN"));
	}
}