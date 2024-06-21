/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

@AnalyzeClasses(packagesOf = XeresApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class CodingRulesTest
{
	@ArchTest
	private final ArchRule no_access_to_standard_streams = noClasses()
			.should(ACCESS_STANDARD_STREAMS)
			.andShould()
			.notBe(CommandArgument.class)
			.because("We use loggers");

	@ArchTest
	private final ArchRule no_field_injection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
			.because("Constructor injection allow detection of cyclic dependencies");

	@ArchTest
	private final ArchRule rs_service_naming = classes()
			.that().areAssignableTo(RsService.class)
			.should().haveSimpleNameEndingWith("RsService");

	/**
	 * Items should have a public no-arg constructor.
	 */
	@ArchTest
	private final ArchRule rs_item_empty_constructor = classes()
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
			});

	/**
	 * JPA entities should have a public or protected no-arg constructor.
	 */
	@ArchTest
	private final ArchRule jpa_entities_empty_constructor = classes()
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
	private final ArchRule no_ui_access = noClasses()
			.that().resideInAPackage("..app..")
			.and().doNotBelongToAnyOf(XeresApplication.class, UiBridgeService.class)
			.should().accessClassesThat().resideInAPackage("..ui..");
}
