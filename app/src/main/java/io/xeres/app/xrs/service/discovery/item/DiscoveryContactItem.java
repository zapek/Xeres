/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.discovery.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.protocol.NetMode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;
import static io.xeres.app.xrs.serialization.TlvType.*;

public class DiscoveryContactItem extends Item implements RsSerializable
{
	private long pgpIdentifier;
	private LocationIdentifier locationIdentifier;
	private String locationName;
	private String version;
	private Set<NetMode> netMode; // 1: UDP, 2: UPNP, 3: EXT, 4: HIDDEN, 5: UNREACHABLE
	private short vsDisc; // 0: off, 1: minimal (never implemented I think), 2: full
	private short vsDht; // 0: off, 1: passive (never implemented too?!), 2: full
	private int lastContact;

	private String hiddenAddress;
	private short hiddenPort;

	private PeerAddress localAddressV4;
	private PeerAddress externalAddressV4;
	private PeerAddress localAddressV6;
	private PeerAddress externalAddressV6;
	private PeerAddress currentConnectAddress;
	private String hostname;
	private List<PeerAddress> localAddressList = new ArrayList<>();
	private List<PeerAddress> externalAddressList = new ArrayList<>();

	@SuppressWarnings("unused")
	public DiscoveryContactItem()
	{
	}

	private DiscoveryContactItem(Builder builder)
	{
		pgpIdentifier = builder.pgpIdentifier;
		locationIdentifier = builder.locationIdentifier;
		locationName = builder.location;
		version = builder.version;
		netMode = EnumSet.of(builder.netMode);
		vsDisc = builder.vsDisc;
		vsDht = builder.vsDht;
		lastContact = builder.lastContact;
		hiddenAddress = builder.hiddenAddress;
		hiddenPort = builder.hiddenPort;
		localAddressV4 = builder.localAddressV4;
		externalAddressV4 = builder.externalAddressV4;
		localAddressV6 = builder.localAddressV6;
		externalAddressV6 = builder.externalAddressV6;
		currentConnectAddress = builder.currentConnectAddress;
		hostname = builder.hostname;
		if (builder.localAddressList != null)
		{
			localAddressList = builder.localAddressList;
		}
		if (builder.externalAddressList != null)
		{
			externalAddressList = builder.externalAddressList;
		}
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.GOSSIP_DISCOVERY.getType();
	}

	@Override
	public int getSubType()
	{
		return 5;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.BACKGROUND.getPriority();
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, pgpIdentifier);
		size += serialize(buf, locationIdentifier, LocationIdentifier.class);
		size += serialize(buf, STR_LOCATION, locationName);
		size += serialize(buf, STR_VERSION, version);
		size += serialize(buf, netMode, FieldSize.INTEGER);
		size += serialize(buf, vsDisc);
		size += serialize(buf, vsDht);
		size += serialize(buf, lastContact);

		if (hiddenAddress != null)
		{
			size += serialize(buf, STR_DOM_ADDR, hiddenAddress);
			size += serialize(buf, hiddenPort);
		}
		else
		{
			size += serialize(buf, ADDRESS, localAddressV4);
			size += serialize(buf, ADDRESS, externalAddressV4);
			size += serialize(buf, ADDRESS, localAddressV6);
			size += serialize(buf, ADDRESS, externalAddressV6);
			size += serialize(buf, ADDRESS, currentConnectAddress);
			size += serialize(buf, STR_DYNDNS, hostname);

			size += serialize(buf, ADDRESS_SET, localAddressList);
			size += serialize(buf, ADDRESS_SET, externalAddressList);
		}
		return size;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void readObject(ByteBuf buf)
	{
		pgpIdentifier = deserializeLong(buf);
		locationIdentifier = (LocationIdentifier) deserializeIdentifier(buf, LocationIdentifier.class);
		locationName = (String) deserialize(buf, STR_LOCATION);
		version = (String) deserialize(buf, STR_VERSION);
		netMode = deserializeEnumSet(buf, NetMode.class, FieldSize.INTEGER);
		vsDisc = deserializeShort(buf);
		vsDht = deserializeShort(buf);
		lastContact = deserializeInt(buf);

		if (buf.getUnsignedShort(buf.readerIndex()) == STR_DOM_ADDR.getValue()) // RS uses a hack to parse hidden addresses, so we do the same :/
		{
			// is hidden address
			hiddenAddress = (String) deserialize(buf, STR_DOM_ADDR);
			hiddenPort = deserializeShort(buf);
		}
		else
		{
			// is normal address
			localAddressV4 = (PeerAddress) deserialize(buf, ADDRESS);
			externalAddressV4 = (PeerAddress) deserialize(buf, ADDRESS);
			localAddressV6 = (PeerAddress) deserialize(buf, ADDRESS);
			externalAddressV6 = (PeerAddress) deserialize(buf, ADDRESS);
			currentConnectAddress = (PeerAddress) deserialize(buf, ADDRESS);
			hostname = (String) deserialize(buf, STR_DYNDNS);

			localAddressList = (List<PeerAddress>) deserialize(buf, ADDRESS_SET);
			externalAddressList = (List<PeerAddress>) deserialize(buf, ADDRESS_SET);
		}
	}

	public long getPgpIdentifier()
	{
		return pgpIdentifier;
	}

	public LocationIdentifier getLocationIdentifier()
	{
		return locationIdentifier;
	}

	public String getLocationName()
	{
		return locationName;
	}

	public String getVersion()
	{
		return version;
	}

	public NetMode getNetMode()
	{
		// TODO: find if there's a better way to handle that netmode... RS used a flag even though it really should be a value...
		if (netMode.contains(NetMode.HIDDEN))
		{
			return NetMode.HIDDEN;
		}
		else if (netMode.contains(NetMode.EXT))
		{
			return NetMode.EXT;
		}
		else if (netMode.contains(NetMode.UPNP))
		{
			return NetMode.UPNP;
		}
		else if (netMode.contains(NetMode.UDP))
		{
			return NetMode.UDP;
		}
		else if (netMode.contains(NetMode.UNREACHABLE))
		{
			return NetMode.UNREACHABLE;
		}
		else
		{
			return NetMode.UNKNOWN;
		}
	}

	public short getVsDisc()
	{
		return vsDisc;
	}

	public short getVsDht()
	{
		return vsDht;
	}

	public int getLastContact()
	{
		return lastContact;
	}

	public String getHiddenAddress()
	{
		return hiddenAddress;
	}

	public short getHiddenPort()
	{
		return hiddenPort;
	}

	public PeerAddress getLocalAddressV4()
	{
		return localAddressV4;
	}

	public PeerAddress getExternalAddressV4()
	{
		return externalAddressV4;
	}

	public PeerAddress getLocalAddressV6()
	{
		return localAddressV6;
	}

	public PeerAddress getExternalAddressV6()
	{
		return externalAddressV6;
	}

	public PeerAddress getCurrentConnectAddress()
	{
		return currentConnectAddress;
	}

	public String getHostname()
	{
		return hostname;
	}

	public List<PeerAddress> getLocalAddressList()
	{
		return localAddressList;
	}

	public List<PeerAddress> getExternalAddressList()
	{
		return externalAddressList;
	}

	@Override
	public DiscoveryContactItem clone()
	{
		return (DiscoveryContactItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "DiscoveryContactItem{" +
				"pgpIdentifier=" + Id.toString(pgpIdentifier) +
				", locationIdentifier=" + locationIdentifier +
				", location='" + locationName + '\'' +
				", version='" + version + '\'' +
				", netMode=" + netMode +
				", vsDisc=" + vsDisc +
				", vsDht=" + vsDht +
				", lastContact=" + lastContact +
				", hiddenAddress='" + hiddenAddress + '\'' +
				", hiddenPort=" + hiddenPort +
				", localAddressV4=" + localAddressV4 +
				", externalAddressV4=" + externalAddressV4 +
				", localAddressV6=" + localAddressV6 +
				", externalAddressV6=" + externalAddressV6 +
				", currentConnectAddress=" + currentConnectAddress +
				", hostname='" + hostname + '\'' +
				", localAddressList=" + localAddressList +
				", externalAddressList=" + externalAddressList +
				'}';
	}


	public static final class Builder
	{
		private long pgpIdentifier;
		private LocationIdentifier locationIdentifier;
		private String location;
		private String version;
		private NetMode netMode;
		private short vsDisc;
		private short vsDht;
		private int lastContact;
		private String hiddenAddress;
		private short hiddenPort;
		private PeerAddress localAddressV4;
		private PeerAddress externalAddressV4;
		private PeerAddress localAddressV6;
		private PeerAddress externalAddressV6;
		private PeerAddress currentConnectAddress;
		private String hostname;
		private List<PeerAddress> localAddressList;
		private List<PeerAddress> externalAddressList;

		private Builder()
		{
		}

		public Builder setPgpIdentifier(long pgpIdentifier)
		{
			this.pgpIdentifier = pgpIdentifier;
			return this;
		}

		public Builder setLocationIdentifier(LocationIdentifier locationIdentifier)
		{
			this.locationIdentifier = locationIdentifier;
			return this;
		}

		public Builder setLocationName(String locationName)
		{
			location = locationName;
			return this;
		}

		public Builder setVersion(String version)
		{
			this.version = version;
			return this;
		}

		public Builder setNetMode(NetMode netMode)
		{
			this.netMode = netMode;
			return this;
		}

		public Builder setVsDisc(int vsDisc)
		{
			this.vsDisc = (short) vsDisc;
			return this;
		}

		public Builder setVsDht(int vsDht)
		{
			this.vsDht = (short) vsDht;
			return this;
		}

		public Builder setLastContact(int lastContact)
		{
			this.lastContact = lastContact;
			return this;
		}

		public Builder setHiddenAddress(String hiddenAddress)
		{
			this.hiddenAddress = hiddenAddress;
			return this;
		}

		public Builder setHiddenPort(short hiddenPort)
		{
			this.hiddenPort = hiddenPort;
			return this;
		}

		public Builder setLocalAddressV4(PeerAddress localAddressV4)
		{
			this.localAddressV4 = localAddressV4;
			return this;
		}

		public Builder setExternalAddressV4(PeerAddress externalAddressV4)
		{
			this.externalAddressV4 = externalAddressV4;
			return this;
		}

		public Builder setLocalAddressV6(PeerAddress localAddressV6)
		{
			this.localAddressV6 = localAddressV6;
			return this;
		}

		public Builder setExternalAddressV6(PeerAddress externalAddressV6)
		{
			this.externalAddressV6 = externalAddressV6;
			return this;
		}

		public Builder setCurrentConnectAddress(PeerAddress currentConnectAddress)
		{
			this.currentConnectAddress = currentConnectAddress;
			return this;
		}

		public Builder setHostname(String hostname)
		{
			this.hostname = hostname;
			return this;
		}

		public Builder setLocalAddressList(List<PeerAddress> localAddressList)
		{
			this.localAddressList = localAddressList;
			return this;
		}

		public Builder setExternalAddressList(List<PeerAddress> externalAddressList)
		{
			this.externalAddressList = externalAddressList;
			return this;
		}

		public DiscoveryContactItem build()
		{
			return new DiscoveryContactItem(this);
		}
	}
}
