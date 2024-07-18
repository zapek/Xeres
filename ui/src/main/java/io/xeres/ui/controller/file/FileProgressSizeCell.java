package io.xeres.ui.controller.file;

import io.xeres.common.util.ByteUnitUtils;
import javafx.scene.control.TableCell;

class FileProgressSizeCell extends TableCell<FileProgressDisplay, Long>
{
	@Override
	protected void updateItem(Long value, boolean empty)
	{
		super.updateItem(value, empty);
		setText(empty ? null : ByteUnitUtils.fromBytes(value));
	}
}
