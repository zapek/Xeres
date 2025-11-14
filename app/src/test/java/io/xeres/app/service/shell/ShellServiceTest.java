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

package io.xeres.app.service.shell;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.xeres.common.mui.ShellAction.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ShellServiceTest
{
	@InjectMocks
	private ShellService shellService;

	@Test
	void translateCommandLine_OK()
	{
		var res = ShellService.translateCommandline("hello world");
		assertEquals("hello", res[0]);
		assertEquals("world", res[1]);
	}

	@Test
	void sendCommand_Cls()
	{
		var res = shellService.sendCommand("cls");
		assertEquals(CLS, res.getAction());
	}

	@Test
	void sendCommand_Alias_Clear()
	{
		var res = shellService.sendCommand("clear");
		assertEquals(CLS, res.getAction());
	}

	@Test
	void sendCommand_Exit()
	{
		var res = shellService.sendCommand("exit");
		assertEquals(EXIT, res.getAction());
	}

	@Test
	void sendCommand_Help()
	{
		var res = shellService.sendCommand("help");
		assertEquals(SUCCESS, res.getAction());
		assertTrue(res.getOutput().contains("Available commands:"));
	}

	@Test
	void sendCommand_Unknown()
	{
		var res = shellService.sendCommand("yabadabadoo");
		assertEquals(UNKNOWN_COMMAND, res.getAction());
	}

	@Test
	void sendCommand_NoOp()
	{
		var res = shellService.sendCommand("");
		assertEquals(NO_OP, res.getAction());
	}
}