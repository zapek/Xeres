/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class StringExpression implements Expression
{
	public enum Operator
	{
		CONTAINS_ANY,
		CONTAINS_ALL,
		EQUALS
	}

	abstract String getValue(File file);

	abstract String getType();

	abstract String getFieldName();

	private final Operator operator;
	private final List<String> words;
	private final boolean caseSensitive;

	protected StringExpression(Operator operator, String template, boolean caseSensitive)
	{
		this.operator = operator;
		this.caseSensitive = caseSensitive;
		template = caseSensitive ? template : template.toLowerCase(Locale.ENGLISH);
		words = Arrays.stream(template.split(" ")).toList();
	}

	@Override
	public boolean evaluate(File file)
	{
		var value = getValue(file);
		if (!caseSensitive)
		{
			value = value.toLowerCase(Locale.ENGLISH);
		}

		return switch (operator)
		{
			case EQUALS -> String.join(" ", words).equals(value);
			case CONTAINS_ALL -> words.stream().allMatch(value::contains);
			case CONTAINS_ANY -> words.stream().anyMatch(value::contains);
		};
	}

	@Override
	public Predicate toPredicate(CriteriaBuilder cb, Root<File> root)
	{
		return switch (operator)
		{
			case EQUALS -> equals(cb, root);
			case CONTAINS_ALL -> contains(cb, root, true);
			case CONTAINS_ANY -> contains(cb, root, false);
		};
	}

	private Predicate equals(CriteriaBuilder cb, Root<File> root)
	{
		if (caseSensitive)
		{
			return cb.equal(root.get(getFieldName()), String.join(" ", words));
		}
		else
		{
			return cb.equal(cb.lower(root.get(getFieldName())), String.join(" ", words).toLowerCase(Locale.ROOT));
		}
	}

	private Predicate contains(CriteriaBuilder cb, Root<File> root, boolean all)
	{
		List<Predicate> predicates = new ArrayList<>();
		words.forEach(s -> predicates.add(like(cb, root.get(getFieldName()), s)));
		var array = predicates.toArray(new Predicate[0]);
		return all ? cb.and(array) : cb.or(array);
	}

	private Predicate like(CriteriaBuilder cb, jakarta.persistence.criteria.Expression<String> x, String pattern)
	{
		if (caseSensitive)
		{
			return cb.like(x, "%" + pattern + "%");
		}
		else
		{
			return cb.like(cb.lower(x), "%" + pattern.toLowerCase(Locale.ROOT) + "%");
		}
	}

	@Override
	public void linearize(List<Byte> tokens, List<Integer> ints, List<String> strings)
	{
		tokens.add(ExpressionType.getTokenValueByClass(getClass()));
		ints.add(operator.ordinal());
		ints.add(caseSensitive ? 0 : 1);
		ints.add(words.size());
		strings.addAll(words);
	}

	@Override
	public String toString()
	{
		return switch (operator)
		{
			case CONTAINS_ALL -> getType() + " CONTAINS ALL " + String.join(" ", words);
			case CONTAINS_ANY ->
			{
				if (words.size() == 1)
				{
					yield getType() + " CONTAINS " + words.getFirst();
				}
				else
				{
					yield getType() + " CONTAINS ONE OF " + String.join(" ", words);
				}
			}
			case EQUALS ->
			{
				if (words.size() == 1)
				{
					yield getType() + " IS " + words.getFirst();
				}
				else
				{
					yield getType() + " IS ONE OF " + String.join(" ", words);
				}
			}
		};
	}
}
