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

package io.xeres.app.service;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.database.model.identity.GxsIdFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.repository.GxsIdentityRepository;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.ProfileFingerprint;
import jakarta.persistence.EntityNotFoundException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class IdentityServiceTest
{
	@Mock
	private SettingsService settingsService;

	@Mock
	private ProfileService profileService;

	@Mock
	private GxsIdentityRepository gxsIdentityRepository;

	@Mock
	private GxsExchangeService gxsExchangeService;

	@InjectMocks
	private IdentityService identityService;

	@BeforeAll
	static void setup()
	{
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	void IdentityService_CreateOwnIdentity_Anonymous_OK() throws PGPException, CertificateException, IOException
	{
		var NAME = "test";

		when(settingsService.isOwnProfilePresent()).thenReturn(true);
		when(settingsService.hasOwnLocation()).thenReturn(true);
		when(gxsIdentityRepository.save(any(IdentityGroupItem.class))).thenAnswer(invocation -> {
			var gxsIdGroupItem = (IdentityGroupItem) invocation.getArguments()[0];
			gxsIdGroupItem.setPublished(Instant.now());
			return gxsIdGroupItem;
		});
		doNothing().when(gxsExchangeService).setLastServiceUpdate(any(), any());

		var id = identityService.createOwnIdentity(NAME, false);

		var gxsIdGroupItem = ArgumentCaptor.forClass(IdentityGroupItem.class);
		verify(gxsIdentityRepository).save(gxsIdGroupItem.capture());
		assertEquals(NAME, gxsIdGroupItem.getValue().getName());
	}

	@Test
	void IdentityService_CreateOwnIdentity_Signed_OK() throws PGPException, CertificateException, IOException
	{
		var NAME = "test";

		var encodedKey = new byte[]{-107, 1, 30, 4, 96, -83, 89, -119, 1, 2, 0, -124, 36, -16, 89, 77, 70, 111, 82, 42, 104, 115, 27, 52, -67, 56, -116, 80, 71, 109, -9,
				78, -113, 115, -22, -35, 97, 121, 34, -118, 90, -6, -68, 113, 78, -58, -120, -4, -123, -1, 46, 10, -19, 122, -84, 21, -24, 118, 82, 12, -1, 45, -56, -94, -21, -25, -3, -68, 17, 45,
				9, -26, -33, 86, -53, 0, 17, 1, 0, 1, -2, 3, 3, 2, 120, 82, -62, 47, -20, 15, -47, -114, 96, -60, -67, 67, 56, -82, 79, -17, 82, -40, 17, 72, 39, -53, -72, 25, 52, -94, 103, -31,
				92, -51, 53, -29, 119, -26, 20, 81, 94, -29, -20, 104, 103, 56, -53, -53, 28, 6, -82, -33, 92, -31, -18, -4, 73, 55, 97, -89, 38, -21, 123, 30, -28, 76, -122, 20, 89, -28, -112,
				-29, 32, -116, -75, -19, -113, 123, -23, -42, 122, 13, 1, -46, -70, -69, 87, -41, -104, -49, 101, 22, 79, -63, -112, -120, 79, 25, 16, -2, -77, 118, 110, -109, -33, -100, -11,
				-126, -73, -64, 125, 56, 101, 49, -89, 19, -61, 125, 103, 121, 82, -15, 109, 2, 105, -103, -11, 31, -68, -117, -81, -14, 7, -9, 98, 18, 96, -26, 70, 66, -64, 108, -2, -6, 114, -13,
				44, -103, 81, -28, 80, 115, 124, 74, -28, -53, 53, -44, -118, 20, -94, -113, -43, 109, 111, 82, -21, 34, 80, -50, 62, 127, -38, -10, 108, -49, -123, 44, -39, 116, -90, 61, 41, -40,
				-127, -84, 111, -127, -68, -75, 106, -9, -81, 37, -40, -120, 36, 62, 12, 45, 15, -88, 9, -51, -24, -96, 68, -38, 125, -76, 4, 116, 101, 115, 116, -120, 92, 4, 16, 1, 2, 0, 6, 5, 2,
				96, -83, 89, -119, 0, 10, 9, 16, -119, -55, 33, -4, 60, -108, 116, -23, -92, -19, 1, -4, 10, -89, 1, 44, 82, -29, 24, 104, -128, -73, -96, 122, -38, 67, -120, 18, 62, 10, 3, 95, 27,
				-51, -45, -114, -113, -93, 118, 13, -20, 3, -35, 8, 15, 97, 27, 76, 20, 9, 78, 74, -24, 27, -99, -58, -125, -69, -103, -13, 50, -83, -117, -115, -123, 25, 52, 39, -122, -22, 81, 46,
				84, 22, -52, 17};

		var secretKey = PGP.getPGPSecretKey(encodedKey);
		var publicKey = secretKey.getPublicKey();
		var fingerprint = publicKey.getFingerprint();

		var ownProfile = ProfileFakes.createProfile(NAME, PGP.getPGPIdentifierFromFingerprint(fingerprint), fingerprint, publicKey.getEncoded());

		ownProfile.setProfileFingerprint(new ProfileFingerprint(secretKey.getPublicKey().getFingerprint()));
		ownProfile.setPgpPublicKeyData(secretKey.getPublicKey().getEncoded());

		when(settingsService.isOwnProfilePresent()).thenReturn(true);
		when(settingsService.hasOwnLocation()).thenReturn(true);
		when(profileService.getOwnProfile()).thenReturn(ownProfile);
		when(settingsService.getSecretProfileKey()).thenReturn(encodedKey);
		when(gxsIdentityRepository.save(any(IdentityGroupItem.class))).thenAnswer(invocation -> {
			var gxsIdGroupItem = (IdentityGroupItem) invocation.getArguments()[0];
			gxsIdGroupItem.setPublished(Instant.now());
			return gxsIdGroupItem;
		});
		doNothing().when(gxsExchangeService).setLastServiceUpdate(any(), any());

		var id = identityService.createOwnIdentity(NAME, true);

		var gxsIdGroupItem = ArgumentCaptor.forClass(IdentityGroupItem.class);
		verify(gxsIdentityRepository).save(gxsIdGroupItem.capture());
		assertEquals(NAME, gxsIdGroupItem.getValue().getName());
		assertNotNull(gxsIdGroupItem.getValue().getProfileHash());
		assertNotNull(gxsIdGroupItem.getValue().getProfileSignature());
	}

	@Test
	void IdentityService_SaveIdentityImage_OK() throws IOException
	{
		var id = 1L;
		var identity = GxsIdFakes.createOwnIdentity();
		var file = new MockMultipartFile("file", getClass().getResourceAsStream("/image/leguman.jpg"));

		when(gxsIdentityRepository.findById(id)).thenReturn(Optional.of(identity));

		identityService.saveIdentityImage(id, file);

		assertNotNull(identity.getImage());

		verify(gxsIdentityRepository).findById(id);
		verify(gxsIdentityRepository).save(identity);
	}

	@Test
	void IdentityService_SaveIdentityImage_NotOwn_Error()
	{
		var id = 2L;
		var file = mock(MultipartFile.class);

		assertThrows(EntityNotFoundException.class, () -> identityService.saveIdentityImage(id, file));
	}

	@Test
	void IdentityService_SaveIdentityImage_EmptyImage_Error()
	{
		var id = 1L;

		assertThrows(IllegalArgumentException.class, () -> identityService.saveIdentityImage(id, null));
	}

	@Test
	void IdentityService_SaveIdentityImage_ImageTooBig_Error()
	{
		var id = 1L;
		var file = mock(MultipartFile.class);
		when(file.getSize()).thenReturn(1024 * 1024 * 11L);

		assertThrows(IllegalArgumentException.class, () -> identityService.saveIdentityImage(id, file), "Avatar image size is bigger than " + (1024 * 1024 * 10) + " bytes");
	}

	@Test
	void IdentityService_DeleteIdentityImage_OK()
	{
		var id = 1L;
		var identity = GxsIdFakes.createOwnIdentity();
		identity.setImage(new byte[1]);

		when(gxsIdentityRepository.findById(id)).thenReturn(Optional.of(identity));

		identityService.deleteIdentityImage(id);

		assertNull(identity.getImage());

		verify(gxsIdentityRepository).findById(id);
		verify(gxsIdentityRepository).save(identity);
	}

	@Test
	void IdentityService_DeleteIdentityImage_NotOwn_Error()
	{
		var id = 2L;

		assertThrows(EntityNotFoundException.class, () -> identityService.deleteIdentityImage(id));
	}
}
