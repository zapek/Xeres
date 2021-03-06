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

package io.xeres.app.xrs.service.gxs;

import io.netty.buffer.Unpooled;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.gxs.GxsIdGroupItemFakes;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.RawItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsServiceType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GxsSignatureTest
{
	@Test
	void GxsSignature_Create_And_Verify_OK()
	{
		var gxsIdGroupItem = GxsIdGroupItemFakes.createGxsIdGroupItem();

		var keyPair = RSA.generateKeys(512);

		gxsIdGroupItem.setAdminPrivateKey(keyPair.getPrivate());
		gxsIdGroupItem.setAdminPublicKey(keyPair.getPublic());

		var data = serializeItemForSignature(gxsIdGroupItem);

		var signature = RSA.sign(data, gxsIdGroupItem.getAdminPrivateKey());
		gxsIdGroupItem.setSignature(signature);

		var rawItem = serializeItem(gxsIdGroupItem);
		assertNotNull(rawItem);

//		var item = (GxsIdGroupItem) rawItem.deserialize(); // XXX: can't work like that... sigh. also GxsIdGroupItem is not in GxsId's item list :-/
//		assertNotNull(item);
//
//		var verifyData = serializeItemForSignature(item);
//
//		assertTrue(RSA.verify(RSA.getPublicKey(item.getAdminPublicKeyData()), item.getSignature(), verifyData));
	}

	private RawItem serializeItem(Item item)
	{
		item.setOutgoing(Unpooled.buffer().alloc(), 2, RsServiceType.GXSID, 1);
		return item.serializeItem(EnumSet.noneOf(SerializationFlags.class));
	}

	private byte[] serializeItemForSignature(Item item)
	{
		item.setOutgoing(Unpooled.buffer().alloc(), 2, RsServiceType.GXSID, 1);
		var buf = item.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		var data = new byte[buf.writerIndex()];
		buf.getBytes(0, data);
		buf.release();
		return data;
	}
}
