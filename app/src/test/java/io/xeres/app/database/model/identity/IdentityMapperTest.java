package io.xeres.app.database.model.identity;

import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentityMapperTest
{
	@Test
	void IdentityMapper_NoInstanceOK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(IdentityMapper.class);
	}

	@Test
	void IdentityMapper_toDTO_OK()
	{
		var identity = GxsIdFakes.createOwnIdentity();
		var identityDTO = IdentityMapper.toDTO(identity);

		assertEquals(identity.getId(), identityDTO.id());
		assertEquals(identity.getName(), identityDTO.name());
		assertEquals(identity.getGxsId(), identityDTO.gxsId());
		assertEquals(identity.getPublished(), identityDTO.updated());
		assertEquals(identity.getType(), identityDTO.type());
		assertEquals(identity.hasImage(), identityDTO.hasImage());
	}
}
