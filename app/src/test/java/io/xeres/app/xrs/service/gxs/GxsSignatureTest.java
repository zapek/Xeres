/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxs;

import io.netty.buffer.Unpooled;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.RawItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;

import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GxsSignatureTest
{
	private static IdentityGroupItem createIdentityGroupItem()
	{
		return new IdentityGroupItem();
	}

	@Test
	void GxsSignature_Create_And_Verify_OK()
	{
		var gxsIdGroupItem = IdentityGroupItemFakes.createIdentityGroupItem();

		var keyPair = RSA.generateKeys(512);

		gxsIdGroupItem.setAdminKeys(keyPair.getPrivate(), keyPair.getPublic(), Instant.now(), null);

		var data = serializeItemForSignature(gxsIdGroupItem);

		var signature = RSA.sign(data, gxsIdGroupItem.getAdminPrivateKey());
		gxsIdGroupItem.setAdminSignature(signature);

		var rawItem = serializeItem(gxsIdGroupItem);
		assertNotNull(rawItem);

		rawItem.getBuffer().release();
	}

	private RawItem serializeItem(Item item)
	{
		item.setOutgoing(Unpooled.buffer().alloc(), new IdentityRsService(null, null, null, null, null, null, null, null));
		return item.serializeItem(EnumSet.noneOf(SerializationFlags.class));
	}

	private byte[] serializeItemForSignature(Item item)
	{
		item.setOutgoing(Unpooled.buffer().alloc(), new IdentityRsService(null, null, null, null, null, null, null, null));
		var buf = item.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		var data = new byte[buf.writerIndex() - HEADER_SIZE];
		buf.getBytes(HEADER_SIZE, data);
		buf.release();
		return data;
	}
}
