// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors.plugin.mailbox.agronampscarrayqueue;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.vlingo.xoom.actors.Actor;
import io.vlingo.xoom.actors.ActorsTest;
import io.vlingo.xoom.actors.LocalMessage;
import io.vlingo.xoom.actors.Mailbox;
import io.vlingo.xoom.actors.testkit.AccessSafely;
import io.vlingo.xoom.common.SerializableConsumer;

public class ManyToOneConcurrentArrayQueueDispatcherTest extends ActorsTest {
  private static final int MailboxSize = 64;

  @Test
  public void testClose() throws Exception {
    final TestResults testResults = new TestResults(MailboxSize);

    final ManyToOneConcurrentArrayQueueDispatcher dispatcher =
            new ManyToOneConcurrentArrayQueueDispatcher(MailboxSize, 2, false, 4, 10);

    dispatcher.start();

    final Mailbox mailbox = dispatcher.mailbox();

    final CountTakerActor actor = new CountTakerActor(testResults);

    for (int count = 1; count <= MailboxSize; ++count) {
      final int countParam = count;
      final SerializableConsumer<CountTaker> consumer = (consumerActor) -> consumerActor.take(countParam);
      final LocalMessage<CountTaker> message = new LocalMessage<>(actor, CountTaker.class, consumer, "take(int)");

      mailbox.send(message);
    }

    assertEquals(MailboxSize, testResults.getHighest());

    dispatcher.close();

    final int neverReceived = MailboxSize * 2;

    for (int count = MailboxSize + 1; count <= neverReceived; ++count) {
      final int countParam = count;
      final SerializableConsumer<CountTaker> consumer = (consumerActor) -> consumerActor.take(countParam);
      final LocalMessage<CountTaker> message = new LocalMessage<>(actor, CountTaker.class, consumer, "take(int)");

      mailbox.send(message);
    }

    assertEquals(MailboxSize, testResults.getHighest());
  }

  @Test
  public void testBasicDispatch() {
    final int mailboxSize = 64;
    final TestResults testResults = new TestResults(MailboxSize);

    final ManyToOneConcurrentArrayQueueDispatcher dispatcher =
            new ManyToOneConcurrentArrayQueueDispatcher(mailboxSize, 2, false, 4, 10);

    dispatcher.start();

    final Mailbox mailbox = dispatcher.mailbox();

    final CountTakerActor actor = new CountTakerActor(testResults);

    for (int count = 1; count <= mailboxSize; ++count) {
      final int countParam = count;
      final SerializableConsumer<CountTaker> consumer = (consumerActor) -> consumerActor.take(countParam);
      final LocalMessage<CountTaker> message = new LocalMessage<>(actor, CountTaker.class, consumer, "take(int)");

      mailbox.send(message);
    }

    assertEquals(mailboxSize, testResults.getHighest());
  }

  @Test
  public void testNotifyOnSendDispatch() throws Exception {
    final int mailboxSize = 64;
    final TestResults testResults = new TestResults(mailboxSize);

    final ManyToOneConcurrentArrayQueueDispatcher dispatcher =
            new ManyToOneConcurrentArrayQueueDispatcher(mailboxSize, 1000, true, 4, 10);

    dispatcher.start();

    final Mailbox mailbox = dispatcher.mailbox();

    final CountTakerActor actor = new CountTakerActor(testResults);

    for (int count = 1; count <= mailboxSize; ++count) {
      final int countParam = count;
      final SerializableConsumer<CountTaker> consumer = (consumerActor) -> consumerActor.take(countParam);
      final LocalMessage<CountTaker> message = new LocalMessage<>(actor, CountTaker.class, consumer, "take(int)");

      // notify if in back off
      mailbox.send(message);

      // every third message give time for dispatcher to back off
      if (count % 3 == 0) {
        Thread.sleep(50);
      }
    }

    assertEquals(mailboxSize, testResults.getHighest());
  }

  public static interface CountTaker {
    void take(final int count);
  }

  public static class CountTakerActor extends Actor implements CountTaker {
    private final TestResults testResults;
    @SuppressWarnings("unused")
    private final CountTaker self;

    public CountTakerActor(final TestResults testResults) {
      this.testResults = testResults;
      this.self = selfAs(CountTaker.class);
    }

    @Override
    public void take(final int count) {
      if (testResults.isHighest(count)) {
        testResults.setHighest(count);
      }
    }
  }

  private static class TestResults {
    private final AccessSafely accessSafely;

    private TestResults(final int happenings) {
      final AtomicInteger highest = new AtomicInteger(0);
      this.accessSafely = AccessSafely
              .afterCompleting(happenings)
              .writingWith("highest", highest::set)
              .readingWith("highest", highest::get)
              .readingWith("isHighest", (Integer count) -> count > highest.get());
    }

    void setHighest(Integer value){
      this.accessSafely.writeUsing("highest", value);
    }

    int getHighest(){
      return this.accessSafely.readFrom("highest");
    }

    boolean isHighest(Integer value){
      return this.accessSafely.readFromNow("isHighest", value);
    }
  }
}
