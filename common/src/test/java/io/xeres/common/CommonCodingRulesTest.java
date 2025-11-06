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

package io.xeres.common;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.xeres.common.id.Identifier;
import org.slf4j.Logger;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

@SuppressWarnings("unused")
@AnalyzeClasses(packagesOf = AppName.class, importOptions = ImportOption.DoNotIncludeTests.class)
class CommonCodingRulesTest
{
	@ArchTest
	private final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

	/**
	 * The serializer uses the 'LENGTH' field of identifiers to be able to deserialize them.
	 * Make sure they all implement one.
	 */
	@ArchTest
	private final ArchRule identifierPublicLengthField = classes()
			.that().areAssignableTo(Identifier.class)
			.and().doNotBelongToAnyOf(Identifier.class)
			.should(new ArchCondition<>("have a public field called LENGTH")
			{
				@Override
				public void check(JavaClass javaClass, ConditionEvents events)
				{
					boolean satisfied = javaClass.getField("LENGTH").getModifiers().contains(JavaModifier.PUBLIC);
					String message = javaClass.getDescription() + (satisfied ? " has" : " does not have")
							+ " a public field called LENGTH";
					events.add(new SimpleConditionEvent(javaClass, satisfied, message));
				}
			});

	@ArchTest
	private final ArchRule loggersShouldBeFinalAndStatic =
			fields().that().haveRawType(Logger.class)
					.should().bePrivate().orShould().beProtected()
					.andShould().beStatic().orShould().beProtected()
					.andShould().beFinal()
					.because("we agreed on this convention");

	@ArchTest
	private final ArchRule utilityClass = classes()
			.that().haveSimpleNameEndingWith("Utils")
			.should(new ArchCondition<>("have a private constructor without parameters")
			        {
				        @Override
				        public void check(JavaClass javaClass, ConditionEvents events)
				        {
					        boolean satisfied = javaClass.getConstructors().stream()
							        .anyMatch(constructor ->
									        constructor.getModifiers().contains(JavaModifier.PRIVATE)
											        && constructor.getParameters().isEmpty()
							        );
					        String message = javaClass.getDescription() + (satisfied ? " has" : " does not have")
							        + " a private constructor without parameters";
					        events.add(new SimpleConditionEvent(javaClass, satisfied, message));
				        }
			        }
			)
			.andShould().haveModifier(JavaModifier.FINAL);
}
