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

use utf8;
use open qw (:std :utf8);

# Details about the nomenclature can be found on this link:
# https://en.wikipedia.org/wiki/Musical_ensemble

$lazyEnsembleDocs = "/**
             * For most situations this is a shortcut to use instead of the
             * more tedious alternative ...<pre><code>
             * .asParameter(parameterName).pickValue()
             * </code></pre>... but the result Trio from this function differs in two ways:
             * <ul><li>
             * It is lazy, i.e. the parameter ensemble-pick will be postponed until first usage.
             * </li><li>
             * It does not conserve picked ensemble when execution enters a child scope
             * and will instead make a separate ensemble-pick on first use in each scope.
             * </li></ul>
             * The above differences are probably in favour of this function.
             * However, a drawback is how this function eliminates the opportunity
             * to decorate the parameter ensemble-pick (e.g. with {\@link Combiner} and
             * {\@link Silencer}). Instead this function will always force
             * pairwise combining with other parameters and the qronic display
             * strategy is not available (i.e. the ensemble pick will be displayed
             * in the same manner regardless of whether the test succeeds or fails).*/";

foreach $ensemble (Duo,Trio,Quartet,Quintet,Sextet,Septet,Octet,Nonet,Decet) {
  $typeParams = substr 'T,U,V,W,X,Y,Z,Å,Ä,Ö', 0, 1 + 2 * ++$ensembleCount;

  ($methodParams = $typeParams) =~ s/(.)/',' eq $1 ? ', ' : "$1 " . lc $1/ge;
  ($wildcardParams = $typeParams) =~ s/(?=\w)/? super /g;
  ($parameterNameParams = $methodParams) =~ s/\w (\w)/String $1Name/g;
  $methodArgs = lc $typeParams;
  ($methodParamsT = $methodArgs) =~ s/(?=\w)/T /g;
  ($allTypeParamsT = $methodParamsT) =~ s/ .//g;

  ($lazySingleArgDocs = $lazyEnsembleDocs) =~ s/Trio/$ensemble/;
  ($lazyMultiArgDocs = $lazySingleArgDocs) =~ s/parameterName/tName, uName .../;
  ($lazyToDisplayArgDocs = $lazySingleArgDocs) =~ s/parameterName/toDisplayFunction/;

  $fluentFactoryDefinitions .= "
    public static <$typeParams,R> $ensemble.ArgumentOptionsRoot<$typeParams,R>
            asArgumentsTo($ensemble.Function<$typeParams,R,?> recordFactory) {
        return newArgumentsRoot($ensemble.ArgumentOptionsRoot.class,
                $ensemble.Function.class, recordFactory);
    }
    public static <$typeParams> $ensemble.EnsembleOptions<$typeParams>
            use($methodParams) {
        return newRecordOptions($ensemble.EnsembleOptions.class, $ensemble.class, $methodArgs);
    }
";

  $typeDefinitions .= "
    public interface $ensemble<$typeParams> {
        <R,E extends Throwable> R applyOn(Function<$wildcardParams,R,E> function) throws E;
        <E extends Throwable> void execute(Consumer<$wildcardParams,E>  consumer) throws E;

        interface ArgumentOptionsRoot<$typeParams,R> {
            <O extends ArgumentOptions<$typeParams,R,O>> O use($methodParams);
        }
        interface ArgumentOptions<$typeParams,R,O extends ArgumentOptions<$typeParams,R,O>>
        extends ScopedLazyParameter.FactoryRoot<R> {
            O or($methodParams);
        }
        interface EnsembleOptions<$typeParams>
        extends ArgumentOptions<$typeParams,$ensemble<$typeParams>,EnsembleOptions<$typeParams>> {
            ScopedLazyParameter<$ensemble<$typeParams>> asParameter($parameterNameParams);
            $lazySingleArgDocs
            $ensemble<$typeParams> asLazy$ensemble(String parameterName);
            $lazyMultiArgDocs
            $ensemble<$typeParams> asLazy$ensemble($parameterNameParams);
            $lazyToDisplayArgDocs
            $ensemble<$typeParams> asLazy$ensemble(Function<$wildcardParams,? extends CharSequence,?> toDisplayFunction);
            <R, RF extends ScopedLazyParameter.FactoryRoot<R>>
                    RF asArgumentsTo(Function<$typeParams,R,?> recordFactory);
        }

        \@FunctionalInterface interface Function<$typeParams,R,E extends Throwable>
        {   R apply($methodParams) throws E;}
        \@FunctionalInterface interface Consumer<$typeParams,E extends Throwable>
        {void accept($methodParams) throws E;}
    }
";

    push @allFunctionInterfaces, "$ensemble.Function<$allTypeParamsT,R,E>";

    $allEnsemblesFunctionImpl .= "
            \@Override public R apply($methodParamsT) throws E {
                List<T> arguments = new ArrayList();
                arguments.addAll(Arrays.asList($methodArgs));
                return groupFunction.apply(arguments);
            }";
}

print $fluentFactoryDefinitions;
print $typeDefinitions;
print "
    public interface AllEnsemblesFunction<T,R,E extends Throwable>
    extends ", join(",
            ", @allFunctionInterfaces), " {}
";
print "
    /**
     * \@deprecated
     * There is doubt whether this is a good idea.
     */
    \@Deprecated
    public static AllEnsemblesFunction<Object,String,RuntimeException> joining(final CharSequence delimiter) {
        return groupBy(new GroupFunction<Object,String,RuntimeException>() {
            public String apply(List<Object> arguments) {
                StringBuilder sb = new StringBuilder();
                for (Object eachArg : arguments) {
                    sb.append(delimiter).append(eachArg);
                }
                return sb.substring(delimiter.length());
            }
        });
    }

    /**
     * \@deprecated
     * There is doubt whether this is a good idea.
     */
    \@Deprecated
    public static <T,R,E extends Throwable> AllEnsemblesFunction<T,R,E> groupBy(
            final GroupFunction<T,R,E> groupFunction) {
        return new AllEnsemblesFunction<T,R,E>() {",
            $allEnsemblesFunctionImpl,"
        };
    }
";

