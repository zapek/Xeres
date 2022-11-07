package io.xeres.ui.controller.id;

import io.xeres.common.geoip.Country;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Locale;
import java.util.Objects;

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
			var flagPath = getClass().getResourceAsStream("/image/flags/" + country.name().toLowerCase(Locale.ROOT) + ".png");
			if (flagPath != null)
			{
				TooltipUtils.install(this, country::toString, null);
				return new ImageView(new Image(flagPath));
			}
		}
		return new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/flags/_unknown.png"))));
	}
}
