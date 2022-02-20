/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.chatcipher;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatChallengeTest
{
	@Test
	void ChatChallenge_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ChatChallenge.class);
	}

	@Test
	void ChatChallenge_Code_OK()
	{
		var gxsId = new GxsId(Id.toBytes("01dc22f128d9495541f780a254b89630"));
		var code = ChatChallenge.code(gxsId, Long.parseUnsignedLong("10949563242187165295"), Long.parseUnsignedLong("140257447151802099"));
		assertEquals(Long.parseUnsignedLong("1540395435043678632"), code);

		gxsId = new GxsId(Id.toBytes("01dc22f128d9495541f780a254b89630"));
		code = ChatChallenge.code(gxsId, Long.parseUnsignedLong("10949563242187165295"), Long.parseUnsignedLong("3128845210392038968"));
		assertEquals(Long.parseUnsignedLong("9133905927926710723"), code);

		gxsId = new GxsId(Id.toBytes("01dc22f128d9495541f780a254b89630"));
		code = ChatChallenge.code(gxsId, Long.parseUnsignedLong("10949563242187165295"), Long.parseUnsignedLong("15552989625937603562"));
		assertEquals(Long.parseUnsignedLong("2213486716447545487"), code);

		gxsId = new GxsId(Id.toBytes("01dc22f128d9495541f780a254b89630"));
		code = ChatChallenge.code(gxsId, Long.parseUnsignedLong("10949563242187165295"), Long.parseUnsignedLong("140257447151802099"));
		assertEquals(Long.parseUnsignedLong("1540395435043678632"), code);

		gxsId = new GxsId(Id.toBytes("01dc22f128d9495541f780a254b89630"));
		code = ChatChallenge.code(gxsId, Long.parseUnsignedLong("10949563242187165295"), Long.parseUnsignedLong("3128845210392038968"));
		assertEquals(Long.parseUnsignedLong("9133905927926710723"), code);

		gxsId = new GxsId(Id.toBytes("01dc22f128d9495541f780a254b89630"));
		code = ChatChallenge.code(gxsId, Long.parseUnsignedLong("10949563242187165295"), Long.parseUnsignedLong("15552989625937603562"));
		assertEquals(Long.parseUnsignedLong("2213486716447545487"), code);

		gxsId = new GxsId(Id.toBytes("01dc22f128d9495541f780a254b89630"));
		code = ChatChallenge.code(gxsId, Long.parseUnsignedLong("10949563242187165295"), Long.parseUnsignedLong("140257447151802099"));
		assertEquals(Long.parseUnsignedLong("1540395435043678632"), code);
	}
}
