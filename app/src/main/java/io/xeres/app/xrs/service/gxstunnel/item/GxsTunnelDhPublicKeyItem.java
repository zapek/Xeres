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

package io.xeres.app.xrs.service.gxstunnel.item;

import io.xeres.app.xrs.common.SecurityKey;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.serialization.TlvType;

import java.math.BigInteger;

public class GxsTunnelDhPublicKeyItem extends GxsTunnelItem
{
	@RsSerialized
	private BigInteger publicKey;

	@RsSerialized(tlvType = TlvType.SIGNATURE)
	private Signature signature;

	@RsSerialized(tlvType = TlvType.SECURITY_KEY)
	private SecurityKey signerPublicKey;

	@Override
	public int getSubType()
	{
		return 2;
	}

	@SuppressWarnings("unused")
	public GxsTunnelDhPublicKeyItem()
	{
	}

	public GxsTunnelDhPublicKeyItem(BigInteger publicKey, Signature signature, SecurityKey signerPublicKey)
	{
		this.publicKey = publicKey;
		this.signature = signature;
		this.signerPublicKey = signerPublicKey;
	}

	public BigInteger getPublicKey()
	{
		return publicKey;
	}

	public Signature getSignature()
	{
		return signature;
	}

	public SecurityKey getSignerPublicKey()
	{
		return signerPublicKey;
	}

	@Override
	public GxsTunnelDhPublicKeyItem clone()
	{
		return (GxsTunnelDhPublicKeyItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsTunnelDhPublicKeyItem{" +
				"publicKey=" + publicKey +
				", signature=" + signature +
				", signerPublicKey=" + signerPublicKey +
				'}';
	}
}
