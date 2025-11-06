/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

package io.xeres.app;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.xeres.app.application.environment.CommandArgument;
import io.xeres.app.service.UiBridgeService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import jakarta.persistence.Entity;
import org.slf4j.Logger;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.GeneralCodingRules.*;

@SuppressWarnings("unused")
@AnalyzeClasses(packagesOf = XeresApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class AppCodingRulesTest
{
	@ArchTest
	private final ArchRule noAccessToStandardStreams = noClasses()
			.should(ACCESS_STANDARD_STREAMS)
			.andShould()
			.notBe(CommandArgument.class)
			.because("We use loggers");

	@ArchTest
	private final ArchRule noJavaUtilLogging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

	@ArchTest
	private final ArchRule loggersShouldBeFinalAndStatic =
			fields().that().haveRawType(Logger.class)
					.should().bePrivate().orShould().beProtected()
					.andShould().beStatic().orShould().beProtected()
					.andShould().beFinal()
					.because("we agreed on this convention");

	@ArchTest
	private final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
			.because("Constructor injection allow detection of cyclic dependencies");

	@ArchTest
	private final ArchRule rsServiceNaming = classes()
			.that().areAssignableTo(RsService.class)
			.should().haveSimpleNameEndingWith("RsService");

	/**
	 * Items should have a public no-arg constructor and have an empty clone method
	 * that returns their own type.
	 */
	@ArchTest
	private final ArchRule rsItem = classes()
			.that().areAssignableTo(Item.class)
			.and().doNotBelongToAnyOf(Item.class)
			.should(new ArchCondition<>("have a public constructor without parameters")
			{
				@Override
				public void check(JavaClass javaClass, ConditionEvents events)
				{
					boolean satisfied = javaClass.getConstructors().stream()
							.anyMatch(constructor ->
									constructor.getModifiers().contains(JavaModifier.PUBLIC)
											&& constructor.getParameters().isEmpty()
							);
					String message = javaClass.getDescription() + (satisfied ? " has" : " does not have")
							+ " a public constructor without parameters";
					events.add(new SimpleConditionEvent(javaClass, satisfied, message));
				}
			})
			.andShould(new ArchCondition<>("have a clone() method that returns their class")
			{
				@Override
				public void check(JavaClass javaClass, ConditionEvents events)
				{
					boolean satisfied = javaClass.getMethods().stream()
							.anyMatch(method ->
									method.getName().equals("clone")
											&& method.getParameters().isEmpty()
											&& method.getReturnType().equals(javaClass)
											&& method.getModifiers().contains(JavaModifier.PUBLIC));
					String message = javaClass.getDescription() + (satisfied ? " has" : " does not have")
							+ " a clone() method returning its own type";
					events.add(new SimpleConditionEvent(javaClass, satisfied, message));
				}
			})
			.andShould(new ArchCondition<>("have a toString() method that returns a meaningful description")
			{
				@Override
				public void check(JavaClass javaClass, ConditionEvents events)
				{
					boolean satisfied = javaClass.getMethods().stream()
							.anyMatch(method ->
									method.getName().equals("toString")
											&& method.getParameters().isEmpty()
											&& method.getModifiers().contains(JavaModifier.PUBLIC)
											&& method.getReturnType().getName().equals("java.lang.String")
											&& method.getOwner().equals(javaClass)
							);
					String message = javaClass.getDescription() + (satisfied ? " has" : " does not have")
							+ " a toString() method returning a meaningful description";
					events.add(new SimpleConditionEvent(javaClass, satisfied, message));
				}
			});

	/**
	 * JPA entities should have a public or protected no-arg constructor.
	 */
	@ArchTest
	private final ArchRule jpaEntitiesEmptyConstructor = classes()
			.that().areAnnotatedWith(Entity.class)
			.should(new ArchCondition<>("have a public constructor without parameters")
			{
				@Override
				public void check(JavaClass javaClass, ConditionEvents events)
				{
					boolean satisfied = javaClass.getConstructors().stream()
							.anyMatch(constructor ->
									(constructor.getModifiers().contains(JavaModifier.PUBLIC) || constructor.getModifiers().contains(JavaModifier.PROTECTED))
											&& constructor.getParameters().isEmpty()
							);
					String message = javaClass.getDescription() + (satisfied ? " has" : " does not have")
							+ " a public constructor without parameters";
					events.add(new SimpleConditionEvent(javaClass, satisfied, message));
				}
			});

	/**
	 * The following rule helps avoid dependencies from app to the UI. Everything should be done with
	 * server notifications, message queues and, if strictly necessary, UiBridgeService.
	 */
	@ArchTest
	private final ArchRule noUiAccess = noClasses()
			.that().resideInAPackage("..app..")
			.and().doNotBelongToAnyOf(XeresApplication.class, UiBridgeService.class)
			.should().accessClassesThat().resideInAPackage("..ui..");

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
