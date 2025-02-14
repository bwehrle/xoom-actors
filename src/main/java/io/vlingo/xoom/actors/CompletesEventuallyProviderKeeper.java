// Copyright © 2012-2022 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.actors;

public interface CompletesEventuallyProviderKeeper {
  CompletesEventuallyProvider providerFor(final String name);
  void close();
  CompletesEventuallyProvider findDefault();
  void keep(final String name, final CompletesEventuallyProvider completesEventuallyProvider);
}
