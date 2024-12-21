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

package io.xeres.app.xrs.serialization;

import io.xeres.common.id.LocationIdentifier;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class SerialAll
{
	@RsSerialized
	private int intPrimitiveField;

	@RsSerialized
	private Integer integerField;

	@RsSerialized
	private short shortPrimitiveField;

	@RsSerialized
	private Short shortField;

	@RsSerialized
	private byte bytePrimitiveField;

	@RsSerialized
	private Byte byteField;

	@RsSerialized
	private long longPrimitiveField;

	@RsSerialized
	private Long longField;

	@RsSerialized
	private float floatPrimitiveField;

	@RsSerialized
	private Float floatField;

	@RsSerialized
	private double doublePrimitiveField;

	@RsSerialized
	private Double doubleField;

	@RsSerialized
	private boolean booleanPrimitiveField;

	@RsSerialized
	private Boolean booleanField;

	@RsSerialized
	private byte[] bytes;

	@RsSerialized
	private LocationIdentifier locationIdentifier;

	@RsSerialized
	private List<String> stringList;

	@RsSerialized
	private Map<Integer, String> stringMap;

	@RsSerialized
	private SerialEnum serialEnum;

	@RsSerialized
	private EnumSet<SerialEnum> enumSet;

	@RsSerialized(fieldSize = FieldSize.SHORT)
	private EnumSet<SerialEnum> enumSetShort;

	@RsSerialized(fieldSize = FieldSize.BYTE)
	private EnumSet<SerialEnum> enumSetByte;

	@RsSerialized(tlvType = TlvType.STR_NAME)
	private String tlvName;

	public int getIntPrimitiveField()
	{
		return intPrimitiveField;
	}

	public void setIntPrimitiveField(int intPrimitiveField)
	{
		this.intPrimitiveField = intPrimitiveField;
	}

	public Integer getIntegerField()
	{
		return integerField;
	}

	public void setIntegerField(Integer integerField)
	{
		this.integerField = integerField;
	}

	public short getShortPrimitiveField()
	{
		return shortPrimitiveField;
	}

	public void setShortPrimitiveField(short shortPrimitiveField)
	{
		this.shortPrimitiveField = shortPrimitiveField;
	}

	public Short getShortField()
	{
		return shortField;
	}

	public void setShortField(Short shortField)
	{
		this.shortField = shortField;
	}

	public byte getBytePrimitiveField()
	{
		return bytePrimitiveField;
	}

	public void setBytePrimitiveField(byte bytePrimitiveField)
	{
		this.bytePrimitiveField = bytePrimitiveField;
	}

	public Byte getByteField()
	{
		return byteField;
	}

	public void setByteField(Byte byteField)
	{
		this.byteField = byteField;
	}

	public long getLongPrimitiveField()
	{
		return longPrimitiveField;
	}

	public void setLongPrimitiveField(long longPrimitiveField)
	{
		this.longPrimitiveField = longPrimitiveField;
	}

	public Long getLongField()
	{
		return longField;
	}

	public void setLongField(Long longField)
	{
		this.longField = longField;
	}

	public float getFloatPrimitiveField()
	{
		return floatPrimitiveField;
	}

	public void setFloatPrimitiveField(float floatPrimitiveField)
	{
		this.floatPrimitiveField = floatPrimitiveField;
	}

	public Float getFloatField()
	{
		return floatField;
	}

	public void setFloatField(Float floatField)
	{
		this.floatField = floatField;
	}

	public double getDoublePrimitiveField()
	{
		return doublePrimitiveField;
	}

	public void setDoublePrimitiveField(double doublePrimitiveField)
	{
		this.doublePrimitiveField = doublePrimitiveField;
	}

	public Double getDoubleField()
	{
		return doubleField;
	}

	public void setDoubleField(Double doubleField)
	{
		this.doubleField = doubleField;
	}

	public boolean isBooleanPrimitiveField()
	{
		return booleanPrimitiveField;
	}

	public void setBooleanPrimitiveField(boolean booleanPrimitiveField)
	{
		this.booleanPrimitiveField = booleanPrimitiveField;
	}

	public Boolean getBooleanField()
	{
		return booleanField;
	}

	public void setBooleanField(Boolean booleanField)
	{
		this.booleanField = booleanField;
	}

	public byte[] getBytes()
	{
		return bytes;
	}

	public void setBytes(byte[] bytes)
	{
		this.bytes = bytes;
	}

	public LocationIdentifier getLocationIdentifier()
	{
		return locationIdentifier;
	}

	public void setLocationIdentifier(LocationIdentifier locationIdentifier)
	{
		this.locationIdentifier = locationIdentifier;
	}

	public List<String> getStringList()
	{
		return stringList;
	}

	public void setStringList(List<String> stringList)
	{
		this.stringList = stringList;
	}

	public Map<Integer, String> getStringMap()
	{
		return stringMap;
	}

	public void setStringMap(Map<Integer, String> stringMap)
	{
		this.stringMap = stringMap;
	}

	public SerialEnum getSerialEnum()
	{
		return serialEnum;
	}

	public void setSerialEnum(SerialEnum serialEnum)
	{
		this.serialEnum = serialEnum;
	}

	public EnumSet<SerialEnum> getEnumSet()
	{
		return enumSet;
	}

	public void setEnumSet(EnumSet<SerialEnum> enumSet)
	{
		this.enumSet = enumSet;
	}

	public String getTlvName()
	{
		return tlvName;
	}

	public void setTlvName(String tlvName)
	{
		this.tlvName = tlvName;
	}

	public EnumSet<SerialEnum> getEnumSetShort()
	{
		return enumSetShort;
	}

	public void setEnumSetShort(EnumSet<SerialEnum> enumSetShort)
	{
		this.enumSetShort = enumSetShort;
	}

	public EnumSet<SerialEnum> getEnumSetByte()
	{
		return enumSetByte;
	}

	public void setEnumSetByte(EnumSet<SerialEnum> enumSetByte)
	{
		this.enumSetByte = enumSetByte;
	}
}
