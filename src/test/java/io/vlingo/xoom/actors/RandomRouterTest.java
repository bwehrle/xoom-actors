// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.
package io.vlingo.xoom.actors;

import io.vlingo.xoom.actors.testkit.AccessSafely;
import io.vlingo.xoom.common.Completes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SmallestMailboxRouterTest tests {@link SmallestMailboxRouter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RandomRouterTest extends ActorsTest {

  @Test
  public void testTwoArgConsumerProtocol() {
    final int messagesToSend = 3;

    final Results results = new Results(messagesToSend);

    // this seeded Random generates 1,0,2
    // with 3 as its bound: rnd.nextInt(3)
    Random rnd = new Random(0x2FDDA356L);
    final SubscriptionProtocol router = world.actorFor(
            SubscriptionProtocol.class,
            Definition.has(TestRouterActor.class, Definition.parameters(rnd)));

    final String FirstWorkerAddress = "1f7b13b6-3631-415e-88f2-ea3496f1a70d";
    SubscriptionProtocol FirstWorker = world.actorFor(
            SubscriptionProtocol.class,
            Definition.has(TestRouteeActor.class, Definition.NoParameters, FirstWorkerAddress));

    final String SecondWorkerAddress = "761b8c2e-0cfc-40c8-af97-50eff09fb850";
    SubscriptionProtocol SecondWorker = world.actorFor(
            SubscriptionProtocol.class,
            Definition.has(TestRouteeActor.class, Definition.NoParameters, SecondWorkerAddress));

    final String ThirdWorkerAddress = "2f6d29ca-0313-4e75-86a2-876bbfb6fd29";
    SubscriptionProtocol ThirdWorker = world.actorFor(
            SubscriptionProtocol.class,
            Definition.has(TestRouteeActor.class, Definition.NoParameters, ThirdWorkerAddress));

    router.subscribe(FirstWorker);
    router.subscribe(SecondWorker);
    router.subscribe(ThirdWorker);

    for (int j = 0; j < 10; j++) {
      for (int i = 0; i < messagesToSend; i++) {
        router
                .getWorkerName()
                .andFinallyConsume(answer -> results.access.writeUsing("answers", answer));
      }

      final List<String> allExpected = new ArrayList<>();
      allExpected.add("761b8c2e-0cfc-40c8-af97-50eff09fb850");
      allExpected.add("1f7b13b6-3631-415e-88f2-ea3496f1a70d");
      allExpected.add("2f6d29ca-0313-4e75-86a2-876bbfb6fd29");

      for (int round = 0; round < messagesToSend; round++) {
        String actual = results.access.readFrom("answers", round);
        assertTrue(allExpected.remove(actual));
      }
      assertEquals(0, allExpected.size());
    }
  }

  public interface SubscriptionProtocol {
    void subscribe(SubscriptionProtocol routeeActor);
    void unsubscribe(SubscriptionProtocol routeeActor);
    Completes<String> getWorkerName();
  }

  public static class TestRouterActor extends RandomRouter<SubscriptionProtocol>
          implements SubscriptionProtocol {

    public TestRouterActor(final Random seededRandom) {
      super(new RouterSpecification<>(
              0,
              Definition.has(TestRouteeActor.class, Definition.NoParameters),
              SubscriptionProtocol.class
      ), seededRandom);
    }

    @Override
    public void subscribe(SubscriptionProtocol routeeActor) {
      subscribe(Routee.of(routeeActor));
    }

    @Override
    public void unsubscribe(SubscriptionProtocol routeeActor) {
      unsubscribe(Routee.of(routeeActor));
    }

    @Override
    public Completes<String> getWorkerName() {
      return dispatchQuery((subscriptionProtocol, a1) -> subscriptionProtocol.getWorkerName(), null);
    }
  }

  public static class TestRouteeActor extends Actor implements SubscriptionProtocol {
    TestRouteeActor() { }

    @Override
    public void subscribe(SubscriptionProtocol routeeActor) { }

    @Override
    public void unsubscribe(SubscriptionProtocol routeeActor) { }

    @Override
    public Completes<String> getWorkerName() {
      return completes().with(this.lifeCycle.environment.address.name());
    }
  }

  public static class Results {
    public AccessSafely access;
    private final String[] answers;
    private int index;

    Results(final int totalAnswers) {
      this.answers = new String[totalAnswers];
      this.index = 0;
      this.access = afterCompleting(totalAnswers);
    }

    private AccessSafely afterCompleting(final int steps) {
      access = AccessSafely
              .afterCompleting(steps)
              .writingWith("answers", (String answer) -> answers[index++] = answer)
              .readingWith("answers", (Integer index) -> answers[index]);
      return access;
    }
  }
}
