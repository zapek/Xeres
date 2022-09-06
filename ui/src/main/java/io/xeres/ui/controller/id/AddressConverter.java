package io.xeres.ui.controller.id;

import javafx.util.StringConverter;

public class AddressConverter extends StringConverter<AddressCountry>
{
	@Override
	public String toString(AddressCountry object)
	{
		if (object != null)
		{
			return object.address();
		}
		return null;
	}

	@Override
	public AddressCountry fromString(String string)
	{
		return null;
	}
}
