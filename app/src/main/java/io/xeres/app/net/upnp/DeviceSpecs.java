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

package io.xeres.app.net.upnp;

import java.net.URI;

public interface DeviceSpecs
{
	boolean hasModelName();

	String getModelName();

	void setModelName(String modelName);

	boolean hasManufacturer();

	String getManufacturer();

	void setManufacturer(String manufacturer);

	URI getManufacturerUrl();

	void setManufacturerUrl(String manufacturerUrl);

	boolean hasSerialNumber();

	String getSerialNumber();

	void setSerialNumber(String serialNumber);

	boolean hasControlUrl();

	URI getControlUrl();

	void setControlUrl(String controlUrl);

	boolean hasPresentationUrl();

	URI getPresentationUrl();

	void setPresentationUrl(String presentationUrl);

	String getServiceType();

	void setServiceType(String serviceType);
}
