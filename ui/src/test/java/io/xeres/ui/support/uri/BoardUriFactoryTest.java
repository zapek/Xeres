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

package io.xeres.ui.support.uri;

import io.xeres.testutils.IdFakes;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static io.xeres.ui.support.uri.UriFactoryUtils.createUriComponentsFromUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(ApplicationExtension.class)
class BoardUriFactoryTest
{
	@Test
	void BoardsUri_WrongParams_MissingGxsId_Fail()
	{
		var url = "retroshare://posted?name=test";

		var factory = new BoardsUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", _ -> {
		});

		assertInstanceOf(ContentText.class, content);
	}

	@Test
	void BoardsUri_WrongParams_MissingName_Fail()
	{
		var gxsId = IdFakes.createGxsId();
		var url = "retroshare://posted?id=" + gxsId;

		var factory = new BoardsUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", _ -> {
		});

		assertInstanceOf(ContentText.class, content);
	}

	@Test
	void BoardsUri_TwoParams_Success()
	{
		var gxsId = IdFakes.createGxsId();

		var url = "retroshare://posted?name=test&id=" + gxsId;

		var factory = new BoardsUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", _ -> {
		});

		assertEquals(url, ((ContentUri) content).getUri());
	}

	@Test
	void BoardsUri_ThreeParams_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var msgId = IdFakes.createMessageId();

		var url = "retroshare://posted?name=test&id=" + gxsId + "&msgid=" + msgId;

		var factory = new BoardsUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", _ -> {
		});

		assertEquals(url, ((ContentUri) content).getUri());
	}

	@Test
	void BoardsUri_Pretty()
	{
		var gxsId = IdFakes.createGxsId();
		var msgId = IdFakes.createMessageId();

		var url = "retroshare://posted?name=Fun%20Board&id=" + gxsId + "&msgid=" + msgId;

		var factory = new BoardsUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", _ -> {
		});

		assertEquals("Fun Board", content.asText());
	}

	@Test
	void BoardsUri_Pretty_FromText()
	{
		var gxsId = IdFakes.createGxsId();
		var msgId = IdFakes.createMessageId();

		var url = "retroshare://posted?name=Fun%20Board&id=" + gxsId + "&msgid=" + msgId;

		var factory = new BoardsUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "Test", _ -> {
		});

		assertEquals("Test", content.asText());
	}
}