package io.xeres.ui.controller.id;

import io.xeres.common.geoip.Country;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Locale;

public class AddressCell extends ListCell<AddressCountry>
{
	public AddressCell(ListView<AddressCountry> listView)
	{
		super();
	}

	@Override
	protected void updateItem(AddressCountry item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.address());
		setGraphic(empty ? null : getFlag(item.country()));
	}

	private ImageView getFlag(Country country)
	{
		if (country != null)
		{
			var flagPath = getClass().getResourceAsStream("/image/flags/" + country.name().toLowerCase(Locale.ENGLISH) + ".png");
			if (flagPath != null)
			{
				return new ImageView(new Image(flagPath));
			}
		}
		return null; // XXX: fix! return some default.. also if flagpath is null
	}
}
