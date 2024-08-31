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

package io.xeres.app.crypto.rsid;

import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.xeres.common.rsid.Type.CERTIFICATE;
import static org.junit.jupiter.api.Assertions.*;

class RSCertificateTest
{
	@Test
	void Build_Success()
	{
		var profile = ProfileFakes.createProfile("Nemesis", 0x9F00B21277698D8DL, Id.toBytes("60049f670534eab17dda2e6d9f00b21277698d8d"), Id.toBytes("984d0461fd80400102008e20511e623f662693d054e1aeb26a007e17f745d4616a6a647d22313b67111ce5f45db22fb670bb5e05f4846ad6d686224acc22966f28e1a50d99d4afb295fb0011010001b4084e656d6573697320885c041001020006050261fd8040000a09109f00b21277698d8d97e401ff688d2b9b73551587858994309485909a36b5401518716698131e1811d8f8204348392c89e99fcb21651d7490e9877b80ced7e11aabbb7c0538853954d77d047b"));
		var location = LocationFakes.createLocation("Home", profile, new LocationId("738ea192064e3f20e766438cc9305bd5"));

		var rsId = new RSIdBuilder(CERTIFICATE)
				.setName(profile.getName().getBytes())
				.setProfile(profile)
				.setLocationId(location.getLocationId())
				.addLocator(Connection.from(PeerAddress.fromAddress("192.168.1.50:1234")))
				.addLocator(Connection.from(PeerAddress.fromAddress("85.1.2.3:1234")))
				.addLocator(Connection.from(PeerAddress.fromHostname("foo.bar.com")))
				.addLocator(Connection.from(PeerAddress.fromAddress("85.1.2.4:1234")))
				.build();

		var armored = rsId.getArmored();

		assertEquals("""
				CQEGAbeYTQRh/YBAAQIAjiBRHmI/ZiaT0FThrrJqAH4X90XUYWpqZH0iMTtnERzl
				9F2yL7Zwu14F9IRq1taGIkrMIpZvKOGlDZnUr7KV+wARAQABtAhOZW1lc2lzIIhc
				BBABAgAGBQJh/YBAAAoJEJ8AshJ3aY2Nl+QB/2iNK5tzVRWHhYmUMJSFkJo2tUAV
				GHFmmBMeGBHY+CBDSDksiemfyyFlHXSQ6Yd7gM7X4Rqru3wFOIU5VNd9BHsCBlUB
				AgME0gMGwKgBMgTSBA1mb28uYmFyLmNvbQTSBgdOZW1lc2lzBRBzjqGSBk4/IOdm
				Q4zJMFvVCgZVAQIEBNIHA90yoQ==""", armored);
	}

	@Test
	void Parse_Success()
	{
		var string = """
				CQEGAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2
				gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU
				9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBa
				M+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P
				6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCg
				NwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChH
				ZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE
				3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD
				2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz
				2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIY
				HIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkE
				zS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfi
				F9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhv
				bWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIFEHql0D0fEzStg7Xe+nDb7wEH
				AxnSjw==""";

		var rsId = RSId.parse(string, CERTIFICATE);

		assertTrue(rsId.isPresent());
		assertNotNull(rsId.get().getPgpPublicKey());
		assertFalse(rsId.get().getInternalIp().isPresent()); // RS put 169.254.67.38 in my certificate...
		assertTrue(rsId.get().getExternalIp().isPresent());
		assertNotNull(rsId.get().getLocationId());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			// Empty
			"",
			// Wrong certificate version
			"CQEFAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIFEHql0D0fEzStg7Xe+nDb7wEHAxnSjw==",
			// No version
			"AcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIFEHql0D0fEzStg7Xe+nDb7wEHAxnSjw==",
			// Wrong checksum
			"CQEGAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIFEHql0D0fEzStg7Xe+nDb7wEHAxnSjg==",
			// Wrong checksum length
			"CQEGAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIFEHql0D0fEzStg7Xe+nDb7wEHAhnSjw==",
			// Missing checksum
			"CQEGAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIFEHql0D0fEzStg7Xe+nDb7wE=",
			// Packet shorter than advertised length
			"CQEGAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIFEHql0D0fEzStg7Xe+nDb7wEHBBnSjw==",
			// Missing location id
			"CQEGAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQYLTXkgY29tcHV0ZXIHA9kC3w==",
			// Missing name
			"CQEGAcGWxsBNBFpq3M0BCADEQWXjoNmUNDo/RSfYwlSavOQoTllnlLv7bmRHXRP2gRxBlCjp185VyI+mW9uWbNnv8TpMsScjKvS+x0uE3QoqjW9seSxq1hIu5ba3cDbU9CzhKfAyycreIWtjZn18IqfvQ3qg3yJ+JLYptA10AGO0ErCmMyhtXAeDthCD3JBaM+jCXi0KGg5k2SkQq9OS+/ktD3/izLX5Zeo5z41s9pSRe5nGQd0vpcwSHTLCUK9P6okDXLNG5jjcLfHD6ap74oTb/My/XOCqprLHIcm00/Byabd9HsZ2Z63KK9ZJ8NCgNwAX1dBwTx1dESdre7+GxUaE3aMYCBon2ZwsTvKv4mjPABEBAAHNInphcGVrIChHZW5lcmF0ZWQgYnkgUmV0cm9TaGFyZSkgPD7CwF8EEwECABMFAlpq3M0JEBM+UlCE3l1NAhkBAAD4mwf/RH/aoFKos9gNCOts9d8TcLhwzvIA++Ah2gmfBcdD9yS7bfiD2cR+qazwhl8GFuCldrUIs+oX0MpN7u2eBX26IH9qwszRQLsEgxETvTxc+0lSE/uz2j+YDQ3fU++ARu5/FKH6HwYspxE+NDnxnaqjkZNAtJmUUBnp9wW2LfkEvHLVnmIYHIQQSSalA2yOzVd0Onf6WJJshctiBbglEZMViN3sypMeoYDct3qhGNCk0E3yojkEzS/CSzueXKS2jucYaybaouACvQ/hlyJeGuv0Ba//lupYn6xRonNzuS8oMcJmUBfiF9pVssvzvyfTIoyD8WGEI3COvthDhKDzF+5rOgIGVEvWwHwOAwap/kMmfA4EEmhvbWUuZHluLnphcGVrLmNvbQUQeqXQPR8TNK2Dtd76cNvvAQcDtYSn",
			// Missing PGP key
			"CQEGAgZUS9bAfA4DBqn+QyZ8DgQSaG9tZS5keW4uemFwZWsuY29tBgtNeSBjb21wdXRlcgUQeqXQPR8TNK2Dtd76cNvvAQcDHrnJ"
	})
	void Parse_Error(String string)
	{
		var rsId = RSId.parse(string, CERTIFICATE);

		assertFalse(rsId.isPresent());
	}
}
