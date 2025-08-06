/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.util.expression;

import io.xeres.app.database.model.file.File;
import io.xeres.common.util.FileNameUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches the extension of a file. This implementation deviates a little by matching over
 * the whole name but uses some tricks to make it acceptable in most common cases.
 */
public class ExtensionExpression extends StringExpression
{
	public ExtensionExpression(@SuppressWarnings("unused") Operator operator, String template, boolean caseSensitive)
	{
		super(Operator.CONTAINS_ANY, template, caseSensitive);
	}

	@Override
	String getType()
	{
		return "EXTENSION";
	}

	@Override
	String getDatabaseColumnName()
	{
		return "name";
	}

	@Override
	public Predicate toPredicate(CriteriaBuilder cb, Root<File> root)
	{
		return contains(cb, root);
	}

	private Predicate contains(CriteriaBuilder cb, Root<File> root)
	{
		List<Predicate> predicates = new ArrayList<>();
		words.forEach(s -> predicates.add(like(cb, root.get(getDatabaseColumnName()), "." + s)));
		var array = predicates.toArray(new Predicate[0]);
		return cb.or(array);
	}

	@Override
	String getValue(File file)
	{
		return FileNameUtils.getExtension(file.getName()).orElse("");
	}
}
