/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.hmac.sha1;

import io.xeres.app.crypto.hmac.AbstractHMac;

import javax.crypto.SecretKey;

/// Uses SHA1 for HMAC.
///
/// HMAC-SHA1 remains secure because its security relies on the secret key and the HMAC construction, not on the collision resistance of the underlying SHA-1 hash function.  While SHA-1 is vulnerable to collision attacks (where two different inputs produce the same hash), HMAC-SHA1 uses a nested structure with an inner and outer key that prevents attackers from exploiting these collisions to forge messages or recover the key.
///
/// Key reasons for its continued security include:
///
/// - Keyed vs. Unkeyed: Plain SHA-1 is unkeyed, meaning anyone can compute the hash and exploit collisions. HMAC-SHA1 requires a secret key, ensuring that only authorized parties can generate or verify the authentication tag.
/// - Collision Resistance Not Required: The security proof for HMAC only requires the underlying hash function to behave as a Pseudorandom Function (PRF), which SHA-1 still effectively does when a secret key is used.
/// - Protection Against Attacks: The HMAC structure masks the internal state of the hash function from the attacker, rendering chosen-prefix collision attacks and length-extension attacks ineffective against the MAC itself.
public class Sha1HMac extends AbstractHMac
{
	public Sha1HMac(SecretKey secretKey)
	{
		super(secretKey, "HmacSHA1");
	}
}
