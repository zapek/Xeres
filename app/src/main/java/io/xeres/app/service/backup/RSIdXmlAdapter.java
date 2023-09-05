/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.service.backup;

import io.xeres.app.crypto.rsid.RSId;
import io.xeres.common.rsid.Type;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class RSIdXmlAdapter extends XmlAdapter<String, RSId>
{
	@Override
	public RSId unmarshal(String v)
	{
		return RSId.parse(v, Type.CERTIFICATE).orElseThrow(() -> new IllegalArgumentException("Couldn't parse certificate"));
	}

	@Override
	public String marshal(RSId v)
	{
		return v.getArmored();
	}
}
