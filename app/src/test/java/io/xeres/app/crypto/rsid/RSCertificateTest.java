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

package io.xeres.app.crypto.rsid;

import org.junit.jupiter.api.Test;

import java.security.cert.CertificateParsingException;

import static org.junit.jupiter.api.Assertions.*;

class RSCertificateTest
{
	@Test
	void RSCertificate_Parse_OK() throws CertificateParsingException
	{
		String string = """
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

		RSId rsId = RSId.parse(string);

		assertNotNull(rsId);
		assertNotNull(rsId.getPgpPublicKey());
		assertFalse(rsId.hasInternalIp()); // RS put 169.254.67.38 in my certificate...
		assertNotNull(rsId.getInternalIp()); // which means it has an internal IP but it's invalid
		assertFalse(rsId.getInternalIp().isValid());
		assertTrue(rsId.hasExternalIp());
		assertNotNull(rsId.getExternalIp());
		assertNotNull(rsId.getLocationId());
	}
}
