#!/usr/bin/env perl
######################################################################
# Copyright 2024 the original author or authors.
#
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License v2.0 which
# accompanies this distribution and is available at
#
# https://www.eclipse.org/legal/epl-v20.html
######################################################################

@features = (Combiner,Identifier,Silencer);

sub factory_interface_name_for_already_applied {
  my %already_applied = map { $_ => 1 } @_;
  return "FactoryRoot<T>" if not %already_applied;
  if (2 <= @features - @_) {
    return join("_and_", grep { not $already_applied{$_} } @features)
        . "_Factory<T>";
  } else {
    for (@features) {
      return $_ . "<T, BasicFactory<T>>" if not $already_applied{$_};
    }
  }
  die "Cannot determine proper name for factory interface after "
      . "features @_ have already been applied!";
}

sub print_factory_progress_definitions {
  my %already_applied = map { $_ => 1 } @_;
  my @still_pending = grep { not $already_applied{$_} } @features;
  if (3 <= @still_pending) {
    for (@features) {
      last if $already_applied{$_};
      print_factory_progress_definitions($_,@_);
    }
  }

  if (not %already_applied) {
    print "\n/**"
        . "\n * \@see #from(Object[]) from(T[])"
	. "\n * \@see #from(Object, Object...) from(T, T...)"
	. "\n * \n * ";
  } else {
    print "/**";
  }
  print "\@see " . join("\n * \@see ", "BasicFactory", @still_pending)
      . " */\npublic interface " . factory_interface_name_for_already_applied(@_)
      . " extends BasicFactory<T>";
  for (@still_pending) {
    print ",\n        $_<T, "
        . factory_interface_name_for_already_applied($_,@_) . ">";
  }
  print " {}\n";
}

print_factory_progress_definitions

