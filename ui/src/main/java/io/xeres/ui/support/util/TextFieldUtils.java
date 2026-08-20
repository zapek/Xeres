/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import atlantafx.base.controls.PasswordTextField;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.support.tooltip.TooltipUtils;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import me.gosimple.nbvcxz.Nbvcxz;
import me.gosimple.nbvcxz.resources.ConfigurationBuilder;
import me.gosimple.nbvcxz.resources.Dictionary;
import me.gosimple.nbvcxz.resources.DictionaryBuilder;
import me.gosimple.nbvcxz.scoring.Result;
import me.gosimple.nbvcxz.scoring.TimeEstimate;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public final class TextFieldUtils
{
	private static final Pattern HOST_PATTERN = Pattern.compile("^([a-zA-Z0-9])?[a-zA-Z0-9.-]{0,253}$");

	public static final int MINIMUM_PASSWORD_LENGTH = 3; // 4 would be better but RS uses that, and we can import profiles from it so...
	public static final int MAXIMUM_PASSWORD_LENGTH = 128;
	public static final int PASSWORD_DEBOUNCE_MILLIS = 500;

	private static final PseudoClass riskyPseudoClass = PseudoClass.getPseudoClass("risky");
	private static final PseudoClass modestPseudoClass = PseudoClass.getPseudoClass("modest");
	private static final PseudoClass strongPseudoClass = PseudoClass.getPseudoClass("strong");

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	private TextFieldUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void setNumeric(TextField textField, int minChars, int maxChars)
	{
		if (minChars < 0 || maxChars < 0)
		{
			throw new IllegalArgumentException("Negative char limits are not supported");
		}
		if (maxChars < minChars)
		{
			throw new IllegalArgumentException("maxChars cannot be smaller than minChars");
		}

		var textFormatter = new TextFormatter<String>(change -> {
			var text = change.getControlNewText();

			if (isEmpty(text))
			{
				return change;
			}
			try
			{
				Integer.parseInt(change.getControlNewText());
				if (change.getControlNewText().length() >= minChars && change.getControlNewText().length() <= maxChars)
				{
					return change;
				}
			}
			catch (NumberFormatException _)
			{
				// nothing to do
			}
			return null;
		});
		textField.setTextFormatter(textFormatter);
	}

	public static void setHost(TextField textField)
	{
		var textFormatter = new TextFormatter<String>(change -> HOST_PATTERN.matcher(change.getControlNewText()).matches() ? change : null);
		textField.setTextFormatter(textFormatter);
	}

	public static String getString(TextField textField)
	{
		return isBlank(textField.getText()) ? null : textField.getText();
	}

	public static int getAsNumber(TextField textField)
	{
		try
		{
			return Integer.parseInt(textField.getText());
		}
		catch (NumberFormatException _)
		{
			return 0;
		}
	}

	public static void setPasswordReveal(PasswordTextField password)
	{
		var icon = new FontIcon("mdi2e-eye-off");
		icon.setCursor(Cursor.HAND);
		UiUtils.setOnPrimaryMouseClicked(icon, _ -> {
			icon.setIconLiteral(password.getRevealPassword() ? "mdi2e-eye-off" : "mdi2e-eye");
			password.setRevealPassword(!password.getRevealPassword());
		});
		password.setRight(icon);
	}

	public static Nbvcxz checkPasswordStrength(Nbvcxz nbvcxz, List<String> bannedWords, PasswordTextField password, Label passwordStrengthLabel, ProgressBar passwordStrength)
	{
		if (nbvcxz == null)
		{
			List<Dictionary> dictionaryList = ConfigurationBuilder.getDefaultDictionaries();
			dictionaryList.add(new DictionaryBuilder()
					.setDictionaryName("exclude")
					.setExclusion(true)
					.addWords(bannedWords, 0)
					.createDictionary()
			);

			BigDecimal mooreMultiplier = ConfigurationBuilder.getMooresMultiplier();
			long crackingHardwareCost = ConfigurationBuilder.getDefaultCrackingHardwareCost();
			BigDecimal costMultiplier = BigDecimal.valueOf(crackingHardwareCost).divide(BigDecimal.valueOf(crackingHardwareCost), 5, RoundingMode.HALF_UP);

			var configuration = new ConfigurationBuilder()
					.setLocale(Locale.getDefault())
					.setDictionaries(dictionaryList)
					.setMaxLength(MAXIMUM_PASSWORD_LENGTH)
					.setGuessTypes(Map.of("PGP", costMultiplier.multiply(mooreMultiplier.multiply(BigDecimal.valueOf(3_900_000_000L / 65_536))).longValue()))
					.createConfiguration();

			nbvcxz = new Nbvcxz(configuration);
		}

		String pass = password.getPassword();

		if (pass.isEmpty())
		{
			TooltipUtils.uninstall(passwordStrength);
			TooltipUtils.uninstall(passwordStrengthLabel);
			TooltipUtils.uninstall(password);
			passwordStrength.setProgress(0.0);
			passwordStrengthLabel.setText(null);
		}
		else
		{
			var finalNbvcxz = nbvcxz;
			CompletableFuture.supplyAsync(() -> finalNbvcxz.estimate(pass))
					.thenAccept(result -> Platform.runLater(() -> {
						var secondsToCrack = TimeEstimate.getTimeToCrack(result, "PGP");

						var strength = PasswordStrength.getStrength(secondsToCrack);
						passwordStrength.setProgress((1.0 + strength.ordinal()) / PasswordStrength.values().length);
						setPasswordStrength(passwordStrength, strength);
						passwordStrengthLabel.setText(strength.toString());
						var recommendations = getPasswordRecommendations(result);
						TooltipUtils.install(passwordStrength, recommendations);
						TooltipUtils.install(passwordStrengthLabel, recommendations);
						TooltipUtils.install(password, recommendations);
					}));
		}
		return nbvcxz;
	}

	public static boolean isPasswordCompliant(PasswordTextField password, ProgressBar passwordStrength)
	{
		var pass = password.getPassword();
		if (pass.length() > MAXIMUM_PASSWORD_LENGTH)
		{
			Requester.showError(MessageFormat.format(bundle.getString("account.password-too-long"), MAXIMUM_PASSWORD_LENGTH));
			return false;
		}
		else if (passwordStrength.getPseudoClassStates().contains(TextFieldUtils.riskyPseudoClass))
		{
			return Requester.ask(bundle.getString("account.password.weak-warning"));
		}
		return true;
	}

	private static void setPasswordStrength(ProgressBar passwordStrength, PasswordStrength strength)
	{
		passwordStrength.pseudoClassStateChanged(riskyPseudoClass, strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK);
		passwordStrength.pseudoClassStateChanged(modestPseudoClass, strength == PasswordStrength.GOOD);
		passwordStrength.pseudoClassStateChanged(strongPseudoClass, strength == PasswordStrength.STRONG);
	}

	private static String getPasswordRecommendations(Result result)
	{
		var sb = new StringBuilder();

		var feedback = result.getFeedback();

		if (feedback.getWarning() != null)
		{
			sb.append(bundle.getString("account.password.warning")).append(" ").append(feedback.getWarning()).append("\n");
		}
		for (String suggestion : feedback.getSuggestion())
		{
			sb.append(bundle.getString("account.password.suggestion")).append(" ").append(suggestion).append("\n");
		}
		sb.append(bundle.getString("account.password.time-to-crack")).append(" ").append(TimeEstimate.getTimeToCrackFormatted(result, "PGP"));
		return sb.toString();
	}
}
