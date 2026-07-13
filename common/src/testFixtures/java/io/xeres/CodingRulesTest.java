/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.xeres.common.annotation.VisibleForTesting;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import org.slf4j.Logger;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * ArchUnit rules shared between modules.
 */
public class CodingRulesTest
{
	public static final ArchRule LOGGERS_SHOULD_BE_FINAL_AND_STATIC =
			fields().that().haveRawType(Logger.class)
					.should().bePrivate().orShould().beProtected()
					.andShould().beStatic().orShould().beProtected()
					.andShould().beFinal()
					.because("we agreed on this convention");

	public static final ArchRule UTILITY_CLASS_SHOULD_HAVE_A_PRIVATE_CONSTRUCTOR_AND_BE_FINAL =
			classes()
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

	public static final ArchRule STRING_UTILS_SHOULD_BE_FROM_APACHE_COMMONS =
			noClasses().should()
					.dependOnClassesThat().resideInAnyPackage("io.micrometer.common.util")
					.because("We use StringUtils from apache.commons.lang3");

	// nicked from apache flink, see https://github.com/apache/flink/blob/master/flink-architecture-tests/flink-architecture-tests-production/src/main/java/org/apache/flink/architecture/rules/ApiAnnotationRules.java
	public static final ArchRule NO_CROSS_CALLS_TO_VISIBLE_FOR_TESTING_METHODS = noClasses().should()
			.callMethodWhere(new DescribedPredicate<>("the target is annotated @"
					+ VisibleForTesting.class.getSimpleName())
			{
				@Override
				public boolean test(JavaMethodCall call)
				{
					final JavaClass targetOwner = call.getTargetOwner();
					final JavaClass originOwner = call.getOriginOwner();

					// no violation for caller annotated with
					// @VisibleForTesting
					if (call.getOrigin()
							.isAnnotatedWith(VisibleForTesting.class))
					{
						return false;
					}

					if (originOwner.equals(targetOwner))
					{
						return false;
					}
					if (originOwner
							.getEnclosingClass()
							.map(targetOwner::equals)
							.orElse(false))
					{
						return false;
					}
					if (targetOwner
							.getEnclosingClass()
							.map(originOwner::equals)
							.orElse(false))
					{
						return false;
					}

					return call.getTarget()
							.isAnnotatedWith(VisibleForTesting.class);
				}
			});

	public static final ArchRule GXS_ID_FIELD_NAMING = fields().that().haveRawType(GxsId.class)
			.should().haveNameEndingWith("GxsId")
			.orShould().haveName("gxsId")
			.because("The name could be confused with database IDs");

	public static final ArchRule MSG_ID_FIELD_NAMING = fields().that().haveRawType(MsgId.class)
			.should().haveNameEndingWith("MsgId")
			.orShould().haveName("msgId")
			.because("The name could be confused with database IDs");
}
