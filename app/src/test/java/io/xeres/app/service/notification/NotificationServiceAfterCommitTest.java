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

package io.xeres.app.service.notification;

import io.xeres.app.configuration.AsynchronousEventsConfiguration;
import io.xeres.common.rest.notification.Notification;
import io.xeres.common.rest.notification.file.FileTrendNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(NotificationServiceAfterCommitTest.TestConfiguration.class)
class NotificationServiceAfterCommitTest
{
	@Autowired
	private RecordingNotificationService notificationService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp()
	{
		notificationService.getSentNotifications().clear();
	}

	@Test
	void NotificationWithinTransaction_IsSentAfterCommit()
	{
		var notification = new FileTrendNotification("sender", "keywords");
		var transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.executeWithoutResult(_ ->
		{
			notificationService.publishNotification(notification);
			assertTrue(notificationService.getSentNotifications().isEmpty());
		});

		assertEquals(List.of(notification), notificationService.getSentNotifications());
	}

	@Test
	void NotificationWithinRolledBackTransaction_IsNotSent()
	{
		var notification = new FileTrendNotification("sender", "keywords");
		var transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.executeWithoutResult(status ->
		{
			notificationService.publishNotification(notification);
			status.setRollbackOnly();
		});

		assertTrue(notificationService.getSentNotifications().isEmpty());
	}

	@Test
	void NotificationWithoutTransaction_IsSentImmediately()
	{
		var notification = new FileTrendNotification("sender", "keywords");

		notificationService.publishNotification(notification);

		assertEquals(List.of(notification), notificationService.getSentNotifications());
	}

	@Configuration
	@EnableTransactionManagement
	@Import(AsynchronousEventsConfiguration.class)
	static class TestConfiguration
	{
		@Bean
		DataSource dataSource()
		{
			return new DriverManagerDataSource("jdbc:h2:mem:notification-test;DB_CLOSE_DELAY=-1", "sa", "");
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource)
		{
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		RecordingNotificationService notificationService(ApplicationEventPublisher publisher)
		{
			return new RecordingNotificationService(publisher);
		}
	}

	static class RecordingNotificationService extends NotificationService
	{
		private final List<Notification> sentNotifications = new CopyOnWriteArrayList<>();

		RecordingNotificationService(ApplicationEventPublisher publisher)
		{
			super(publisher);
		}

		List<Notification> getSentNotifications()
		{
			return sentNotifications;
		}

		void publishNotification(Notification notification)
		{
			publish(notification);
		}

		@Override
		protected void sendNotification(Notification notification)
		{
			sentNotifications.add(notification);
		}

		@TransactionalEventListener(fallbackExecution = true)
		public void onFileTrendNotification(FileTrendNotification notification)
		{
			sendNotification(notification);
		}
	}
}
