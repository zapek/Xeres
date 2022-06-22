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

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class NicknameCompleterTest
{
	@Mock
	private NicknameCompleter.UsernameFinder usernameFinder;

	@InjectMocks
	private NicknameCompleter nicknameCompleter;

	@Test
	public void NicknameCompleter_Complete_Empty_Start()
	{
		Consumer<String> action = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq(""), eq(0))).thenReturn(null);

		nicknameCompleter.complete("", 0, action);
		verify(action, times(0)).accept("");
	}

	@Test
	public void NicknameCompleter_Complete_Empty()
	{
		Consumer<String> action = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq(""), eq(0))).thenReturn(null);

		nicknameCompleter.complete("Hello ", 6, action);
		verify(action, times(0)).accept("");
	}

	@Test
	public void NicknameCompleter_Complete_Single_Start()
	{
		Consumer<String> action = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq(""), eq(0))).thenReturn("Nicolas");

		nicknameCompleter.complete("", 0, action);

		verify(action).accept("Nicolas: ");
	}

	@Test
	public void NicknameCompleter_Complete_Multiple_Start()
	{
		Consumer<String> action1 = Mockito.mock(Consumer.class);
		Consumer<String> action2 = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq(""), eq(0))).thenReturn("Alceste");
		when(usernameFinder.getUsername(eq(""), eq(1))).thenReturn("Nicolas");

		nicknameCompleter.complete("", 0, action1);
		nicknameCompleter.complete("Alceste: ", 9, action2);

		verify(action1).accept("Alceste: ");
		verify(action2).accept("Nicolas: ");
	}

	@Test
	public void NicknameCompleter_Complete_MultipleWithPrefix_Start()
	{
		Consumer<String> action1 = Mockito.mock(Consumer.class);
		Consumer<String> action2 = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq("A"), eq(0))).thenReturn("Agnan");
		when(usernameFinder.getUsername(eq("A"), eq(1))).thenReturn("Alceste");

		nicknameCompleter.complete("A", 1, action1);
		nicknameCompleter.complete("Agnan: ", 7, action2);

		verify(action1).accept("Agnan: ");
		verify(action2).accept("Alceste: ");
	}

	@Test
	public void NicknameCompleter_Complete_Single()
	{
		Consumer<String> action = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq(""), eq(0))).thenReturn("Nicolas");

		nicknameCompleter.complete("This is some text for ", 22, action);

		verify(action).accept("This is some text for Nicolas");
	}

	@Test
	public void NicknameCompleter_Complete_Multiple()
	{
		Consumer<String> action1 = Mockito.mock(Consumer.class);
		Consumer<String> action2 = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq(""), eq(0))).thenReturn("Alceste");
		when(usernameFinder.getUsername(eq(""), eq(1))).thenReturn("Nicolas");

		nicknameCompleter.complete("This is some text for ", 22, action1);
		nicknameCompleter.complete("This is some text for Alceste", 29, action2);

		verify(action1).accept("This is some text for Alceste");
		verify(action2).accept("This is some text for Nicolas");
	}

	@Test
	public void NicknameCompleter_Complete_MultipleWithPrefix()
	{
		Consumer<String> action1 = Mockito.mock(Consumer.class);
		Consumer<String> action2 = Mockito.mock(Consumer.class);

		when(usernameFinder.getUsername(eq("A"), eq(0))).thenReturn("Agnan");
		when(usernameFinder.getUsername(eq("A"), eq(1))).thenReturn("Alceste");

		nicknameCompleter.complete("This is some text for A", 23, action1);
		nicknameCompleter.complete("This is some text for Agnan", 27, action2);

		verify(action1).accept("This is some text for Agnan");
		verify(action2).accept("This is some text for Alceste");
	}
}
