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

package io.xeres.ui;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.xeres.ui.controller.WindowController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

@SuppressWarnings("unused")
@AnalyzeClasses(packagesOf = JavaFxApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class UiCodingRulesTest
{
	@ArchTest
	private final ArchRule noAccessToStandardStreams = noClasses()
			.should(ACCESS_STANDARD_STREAMS)
			.because("We use loggers");

	@ArchTest
	private final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
			.because("Constructor injection allow detection of cyclic dependencies");

	@ArchTest
	private final ArchRule windowNaming = classes()
			.that().implement(WindowController.class)
			.should().haveSimpleNameEndingWith("WindowController");

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

	@ArchTest
	private final ArchRule noDirectInitialDirectoryCalls = noClasses()
			.should(new ArchCondition<>("not call FileChooser or DirectoryChooser's setInitialDirectory() directly but use ChooserUtils")
			        {
				        @Override
				        public void check(JavaClass javaClass, ConditionEvents events)
				        {
					        for (JavaMethodCall call : javaClass.getMethodCallsFromSelf())
					        {
						        String targetName = call.getTarget().getName();
						        if (!"setInitialDirectory".equals(targetName))
						        {
							        continue;
						        }
						        String owner = call.getTargetOwner().getFullName();
						        if ("javafx.stage.FileChooser".equals(owner) || "javafx.stage.DirectoryChooser".equals(owner))
						        {
							        if ("ChooserUtils".equals(javaClass.getSimpleName()))
							        {
								        continue;
							        }
							        String message = javaClass.getDescription() + " calls " + owner + ".setInitialDirectory";
							        events.add(new SimpleConditionEvent(javaClass, true, message));
						        }
					        }
				        }
			        }
			)
			.because("the Chooser would fail to open if the directory doesn't exist");
}
