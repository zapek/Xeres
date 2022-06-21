/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NicknameCompleterTest
{
	@Mock
	private NicknameCompleter.UsernameFinder usernameFinder;

	@InjectMocks
	private NicknameCompleter nicknameCompleter;

	@Test
	public void NicknameCompleter_Complete_Empty()
	{
		@SuppressWarnings("unchecked")
		Consumer<String> action = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(anyString(), anyInt())).thenReturn(null);

		nicknameCompleter.complete("", 0, action);
		verify(action, times(0)).accept("");
	}

	@Test
	public void NicknameCompleter_Complete_Single()
	{
		when(usernameFinder.getUsername(anyString(), anyInt())).thenReturn("Nicolas");

		nicknameCompleter.complete("", 0, s -> assertEquals("Nicolas: ", s));
	}

	@Test
	public void NicknameCompleter_Complete_Multiple()
	{
		when(usernameFinder.getUsername(anyString(), anyInt())).thenReturn("Nicolas", "Alceste");

		nicknameCompleter.complete("", 0, s -> assertEquals("Nicolas: ", s));
		nicknameCompleter.complete("Nicolas: ", 0, s -> assertEquals("Alceste: ", s));
	}
}
