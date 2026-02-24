/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UriUtilsTest
{
	@ParameterizedTest
	@ValueSource(strings = {
			"https://zapek.com",
			"https://xeres.io/docs/",
			"https://01.com",
			"mailto:foo@bar.com",
			"mailto:admin",
			"tel:+12345678",
			"retroshare://forum?name=Xeres&id=1eff9350b5d8eca8feef04fd914fc365",
			"01.Main"
	})
	void isSafeUrl(String url)
	{
		assertTrue(UriUtils.isSafeEnough(url));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"http://zapek.com",
			"https://localhost",
			"https://127.0.0.1",
			"https://127.0.0.2",
			"https://192.168.1.1",
			"https://10.0.0.1",
			"https://172.16.0.1",
			"https://127.1",
			"https://127.2",
			"https://[::1]",
			"https://[2001:0db8:85a3:0000:8a2e:0370:7334]",
			"https://124.2.4.58",
			"https://zapek.com:8080",
			"ftp://some.site.com",
			"file:///etc/passwd",
			"file:///C:/Users/Name/file.txt",
			"https://0x7f000001/",
			"https://0X7f000001/",
			"https://0x7F.0.0000.00000001/",
			"https://0X7F.0.0000.00000001/"
	})
	void isMaliciousUrl(String url)
	{
		assertFalse(UriUtils.isSafeEnough(url));
	}
}