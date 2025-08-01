# LazyParams <a name="lazyparams"></a> [![javadoc](https://javadoc.io/badge2/org.lazyparams/lazyparams/javadoc.svg)](https://javadoc.io/doc/org.lazyparams/lazyparams/latest/org/lazyparams/package-summary.html)  [![maven](https://javadoc.io/badge2/org.lazyparams/lazyparams/mavencentral.svg)](https://central.sonatype.com/search?namespace=org.lazyparams)
LazyParams is a powerful parametrization solution for JUnit (versions 4 and 5). It allows the test developer to easily convert a regular test into a parameterized test that can have one or many parameters. These parameters are seamlessly combined to facilitate pairwise testing.

Unlike traditional parametrization approaches that rely on annotations, LazyParams provides an imperative API during test execution. This approach allows for "lazy" parametrization, where a test can start as a regular test without parameters and then transition into a parameterized test as parameters are introduced during test execution.

## Best-Practice Feature API [LazyParams#pickValue(...)](https://javadoc.io/doc/org.lazyparams/lazyparams/latest/org/lazyparams/LazyParams.html) and execution path evaluation

### ... One parameter ...
First example:
```java
@Test
public void smallInt() {
  int myInt = LazyParams.pickValue("int", 3, 2, 4);
  assertThat(myInt).isLessThan(5);
}

// With ConsoleLauncher the test result is presented like this:
//  smallInt() ✔
//  ├─ smallInt int=3 ✔
//  ├─ smallInt int=2 ✔
//  └─ smallInt int=4 ✔
```
Above test uses the best practice API of feature class [LazyParams](https://javadoc.io/doc/org.lazyparams/lazyparams/latest/org/lazyparams/LazyParams.html). First argument of above [#pickValue(...)](https://javadoc.io/doc/org.lazyparams/lazyparams/latest/org/lazyparams/LazyParams.html#pickValue(java.lang.String,T,T...)) invocation specifies parameter-name, which will be part of the test-name. Additional arguments define possible parameter values.

Fine-grained customization on how to display parameter value is 
achieved with a separate pickValue-method, which first parameter is of type [ToDisplayFunction](https://javadoc.io/doc/org.lazyparams/lazyparams/latest/org/lazyparams/ToDisplayFunction.html), which method takes parameter value as argument. E.g. instead of parameter name, a lambda expression can be used:
```java
  int myInt = LazyParams.pickValue(
          i -> 1==i ? "1st" : 2==i ? "2nd" : 3==i ? "3rd" : i + "th",
          3, 2, 4);

//  smallInt() ✔
//  ├─ smallInt 3rd ✔
//  ├─ smallInt 2nd ✔
//  └─ smallInt 4th ✔
```
There is one special method [pickValue(Enum...)](https://javadoc.io/doc/org.lazyparams/lazyparams/latest/org/lazyparams/LazyParams.html#pickValue(E...)) that promotes enum parameters. Its only parameter is a vararg Enum array with the parameter values. Empty vararg array means all constants of parameter enum-type are possible values. With this method the enum constant #toString() describes the value in test-name. E.g. if parameter type is [RetentionPolicy](https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/RetentionPolicy.html) then a test could look like this:
```java
@Test
public void policy() {
  RetentionPolicy myPolicy = LazyParams.pickValue();
  assertThat(myPolicy).isNotNull();
}

//  policy() ✔
//  ├─ policy SOURCE ✔
//  ├─ policy CLASS ✔
//  └─ policy RUNTIME ✔
```
### ... Two parameters ...
Let's have values for the above `myInt` and `myPolicy` picked in a single test:
```java
@Test
public void twoParams() {
  int myInt = LazyParams.pickValue("int", 3, 2, 4);
  RetentionPolicy myPolicy = LazyParams.pickValue();
}

// Test-result will contain nine executions:
//  twoParams() ✔
//  ├─ twoParams int=3 SOURCE ✔
//  ├─ twoParams int=2 CLASS ✔
//  ├─ twoParams int=4 RUNTIME ✔
//  ├─ twoParams int=3 CLASS ✔
//  ├─ twoParams int=2 SOURCE ✔
//  ├─ twoParams int=4 SOURCE ✔
//  ├─ twoParams int=3 RUNTIME ✔
//  ├─ twoParams int=2 RUNTIME ✔
//  └─ twoParams int=4 CLASS ✔
```
All parameter combinations are tested. This is because LazyParams attempts to seek out all possible paired combinations of the two parameters' values.

What if test execution only introduces second parameter `myPolicy` when `myInt == 2`? ...
```java
@Test
public void twoParams() {
  int myInt = LazyParams.pickValue("int", 3, 2, 4);
  if (2 == myInt) {
    RetentionPolicy myPolicy = LazyParams.pickValue();
  }
}

//  twoParams() ✔
//  ├─ twoParams int=3 ✔
//  ├─ twoParams int=2 SOURCE ✔
//  ├─ twoParams int=4 ✔
//  ├─ twoParams int=2 CLASS ✔
//  └─ twoParams int=2 RUNTIME ✔
```
LazyParams notices that end-of-the-road is reached when `myInt` has value 3 or 4, making it necessary to choose `int=2` in the last couple of test repetitions to unlock the execution path for evaluating the pending `myPolicy` values.

In the above example, the condition that unlocks the execution path for introducing `myPolicy` is hardwired. However, even without a hardwired condition, it would still be possible for the repetition execution paths to unfold like this if `myInt` values 3 and 4 cause test failure before `myPolicy` is introduced.

Let's instead have the other `myInt` values unlock `myPolicy` introduction:
```java
@Test
public void twoParams() {
  int myInt = LazyParams.pickValue("int", 3, 2, 4);
  if (3 <= myInt) {
    RetentionPolicy myPolicy = LazyParams.pickValue();
  }
}

//  twoParams() ✔
//  ├─ twoParams int=3 SOURCE ✔
//  ├─ twoParams int=2 ✔
//  ├─ twoParams int=4 CLASS ✔
//  ├─ twoParams int=3 RUNTIME ✔
//  ├─ twoParams int=4 SOURCE ✔
//  ├─ twoParams int=3 CLASS ✔
//  └─ twoParams int=4 RUNTIME ✔
```
With `myPolicy` introduction path enabled by two `myInt` values there are again some pairwise combinations for LazyParams to navigate!

### ... Three parameters ...
Now have an additional parameter `extra` combined with both parameters from above section:
```java
@Test
public void threeParams() {
  int myInt = LazyParams.pickValue("int", 3, 2, 4);
  RetentionPolicy myPolicy = LazyParams.pickValue();
  String extra = LazyParams.pickValue("extra", "x","y","z");
}

//  threeParams() ✔
//  ├─ threeParams int=3 SOURCE extra=x ✔
//  ├─ threeParams int=2 CLASS extra=y ✔
//  ├─ threeParams int=4 RUNTIME extra=z ✔
//  ├─ threeParams int=3 CLASS extra=z ✔
//  ├─ threeParams int=4 SOURCE extra=y ✔
//  ├─ threeParams int=2 RUNTIME extra=x ✔
//  ├─ threeParams int=3 RUNTIME extra=y ✔
//  ├─ threeParams int=2 SOURCE extra=z ✔
//  └─ threeParams int=4 CLASS extra=x ✔
```
With 3 parameters each having 3 values, there are actually 27 possible combinations (3x3x3) but there are only 9 repetitions. This is because the pairwise combinatorial reduction has kicked in as here are three parameters that are all introduced in each repetition. LazyParams aims to ensure that each `myInt` value (3,2 & 4) is combined at least once with each value of `myPolicy` and `extra`, and that values of `myPolicy` and `extra` are also combined at least once with each other.

In total, LazyParams identifies 27 value pairs, each to be tried at least once. This time it managed to walk through all pairs with just 9 repetitions, because it managed to try three new pairs in each repetition. (I.e. in the first repetition there are `int=3 SOURCE`, `int=3 extra=x` and `SOURCE extra=x`. In the second repetition there are `int=2 CLASS`, `int=2 extra=y` and `CLASS extra=y` etc.)

What if `extra` is never introduced for `SOURCE`:
```java
@Test
public void threeParams() {
  int myInt = LazyParams.pickValue("int", 3, 2, 4);
  RetentionPolicy myPolicy = LazyParams.pickValue();
  if (RetentionPolicy.SOURCE == myPolicy) {
    return;
  }
  String extra = LazyParams.pickValue("extra", "x","y","z");
}

//  threeParams() ✔
//  ├─ threeParams int=3 SOURCE ✔
//  ├─ threeParams int=2 CLASS extra=x ✔
//  ├─ threeParams int=4 RUNTIME extra=y ✔
//  ├─ threeParams int=2 RUNTIME extra=z ✔
//  ├─ threeParams int=4 CLASS extra=z ✔
//  ├─ threeParams int=2 CLASS extra=y ✔
//  ├─ threeParams int=4 RUNTIME extra=x ✔
//  ├─ threeParams int=3 CLASS extra=y ✔
//  ├─ threeParams int=3 RUNTIME extra=z ✔
//  ├─ threeParams int=3 CLASS extra=x ✔
//  ├─ threeParams int=2 SOURCE ✔
//  └─ threeParams int=4 SOURCE ✔
```
With the conditional introduction of the `extra` parameter, the number of possible parameter combinations is down from 27 (3x3x3) to only 21. This reduction in possible combinations might suggest that fewer repetitions would be required to cover all possible value pairs. However, paradoxically, the number of repetitions increased from 9 to 12.

This increase is necessary because 12 is indeed the minimum number of repetitions required to cover all possible value pairs. `SOURCE` is end-of-the-line, requiring 3 repetitions that have just two parameters (i.e. only one pair per repetition) to combine with all three myInt values. None of these repetitions will cover any pairs involving `extra`, which requires at least 9 separate repetitions (3x3) to satisfy all combinations with `myInt`. Thus, at least 12 repetitions (3x3+3) are here needed. (`extra` does not combine with `SOURCE`, so only the `myPolicy` values `RUNTIME` and `CLASS` need 6 repetitions (3x2) for all possible `myPolicy<->extra` pairs, which are easily covered while navigating the 9 `myInt<->extra` pairs.)

So LazyParams managed to reach the lowest possible number of repetitions (12) to satisfy all pairs here. But do also notice how parameter picks `int=3` and `SOURCE` happened first and thereafter were only repeated after all pairs without them had been covered. I.e. after `int=3` and `SOURCE` were branded as not introducing `extra` then LazyParams prioritized the other values, which didn't prevent parameter `extra`, and had them form all possible pairs with the three `extra` values before revisiting `int=3`, which was then given higher priority as it turned out to not prevent `extra`. - And finally the two remaining pairs `int=2 SOURCE` and `int=4 SOURCE` were checked off.

The purpose of this dynamic prioritization is to uncover potentially hidden execution paths while satisfying parameter pair combinations in the process. First impression of `int=3` and `SOURCE` was that they are a dead end - and since first-impressions-last the other values seemed like better candidates for uncovering hidden execution paths and therefore they were initially entrusted with higher priority.

## Simpliest Possible Parameterization: [FalseOrTrue.pickBoolean(displayOnTrue)](https://javadoc.io/doc/org.lazyparams/lazyparams/latest/org/lazyparams/showcase/FalseOrTrue.html#pickBoolean(java.lang.CharSequence))
Isn't the simpliest possible parameter one that can only have values `true` or `false`? Though being very simple it is nevertheless kind of a big deal when relying on LazyParams to seek out corner cases that require special treatment during a test.
