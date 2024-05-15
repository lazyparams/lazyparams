#!/bin/bash
######################################################################
# Copyright 2024 the original author or authors.
#
# All rights reserved. This program and the accompanying materials are
# made available under the terms of the Eclipse Public License v2.0 which
# accompanies this distribution and is available at
#
# https://www.eclipse.org/legal/epl-v20.html
######################################################################

### Navigate to project directory ...
cd "$(dirname "$0")"
ls pom.xml > /dev/null || exit $?

### Clean build ...
mvn clean package -ff -Dsurefire.reportNameSuffix= -Dfailsafe.reportNameSuffix=JUnit4 || exit $?
mvn jacoco:report

jdk_options="$(
  grep -oP '(?<![^> ])\d++(?![^< ])' $HOME/.m2/toolchains.xml | sort -rn | uniq | while read jv; do
    if (( 6 <= jv )) && (( jv < 30 )); then
      prfl=jdk_$jv
      mvn toolchains:toolchain -ff -P $prfl &> /dev/null && echo $prfl
    fi
  done
)"

echo Available toolchain JDK profiles:
echo $jdk_options

if ! [ "$jdk_options" ]; then
  echo Unable to resolve any toolchain profiles to test! >&2
  exit 1
fi

prepare_profiles_parameter() {
  param="$1"
  [ "$2" ] && pattern="$2" || pattern="$param"'_[\.0-9a-z]++'
  filter="${3-cat}"
  separator="var $param"'s = new String[] {"'
  grep -oP "(?<=<id>)$pattern(?=</id>)" pom.xml | $filter | while read eachprfl; do
    echo -n "$separator$eachprfl"
    separator='", "'
  done
  echo '"};'
}

{
  separator='var jdks = new String[] {"'
  for eachjdk in $jdk_options; do
    echo -n "$separator$eachjdk"
    separator='", "'
  done
  echo '"};'

  prepare_profiles_parameter newbuddie 'bytebuddy_(default|1.14.*)'
  prepare_profiles_parameter mediumbuddie 'bytebuddy_1.1[12].*'
  prepare_profiles_parameter oldbuddie 'bytebuddy_1.(7|8|9|10).*'
  prepare_profiles_parameter junit

  echo '
  var bytebuddies = new String[][] {newbuddies, mediumbuddies, oldbuddies};

  var lazer = new org.lazyparams.core.Lazer();
  do {
    lazer.startNew();

    var profiles = new java.util.ArrayList<String>() {
      String pickNextFrom(String[] values, boolean combinePairwise) {
        var picked = values[lazer.pick(values, combinePairwise, values.length)];
        add(picked);
        return picked;
      } 
    };

    var jdkPick = profiles.pickNextFrom(jdks,true);
    boolean jdkPost8 = jdkPick.compareTo("jdk_6") < 0 || "jdk_9".equals(jdkPick);
    boolean jdkPost17 = jdkPost8 && "jdk_17".compareTo(jdkPick) < 0;
    boolean junit5support = false == "jdk_6".equals(jdkPick)
                         && false == "jdk_7".equals(jdkPick);
    if (junit5support) {
      profiles.pickNextFrom(junits,true);
    }

    int buddyIndexPick = lazer.pick(bytebuddies, true, jdkPost17 ? 1 : jdkPost8 ? 2 : 3);
    var buddyRange = bytebuddies[buddyIndexPick];
    profiles.pickNextFrom(buddyRange, junit5support && newbuddies == buddyRange);

    System.out.print("mvn test");
    profiles.stream().map(" -P "::concat).forEach(System.out::print);
    System.out.println();
  } while (lazer.pendingCombinations());'

} | jshell -q --class-path target/classes > target/surefire-reports/testcmds.txt

while read cmdLine; do
  [ "$cmdLine" ] && mkdir "target/surefire-reports/$cmdLine"
done < target/surefire-reports/testcmds.txt

failedBuildLog=target/surefire-reports/build-failure.log

runbuilds() {
  buildThreadLog="target/surefire-reports/build-thread-$1.log"
  while read cmdLine; do
    if [ "$cmdLine" ] && rmdir "target/surefire-reports/$cmdLine" 2> /dev/null \
        && ! [ -f $failedBuildLog ]; then
      linePrefix="$(echo $cmdLine | perl -pe 's/\D*-P +/_/g; s/^_//;')"
      $cmdLine -Dmaven.main.skip=true -Dmaven.resources.skip=true -DtempDir=test-thread$1.tmp -ff &> >(tee "$buildThreadLog" | sed "s/^/$linePrefix/")
      if [ 0 -ne $? ]; then
        {
          {
            echo '>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>'
            echo '>>>>' $cmdLine '>>>>>'
          } | grep --color=always .
          perl -ne 'print if /T E S T S/ .. 0' < "$buildThreadLog"
          {
            echo '<<<<<' $cmdLine '<<<<<'
            echo '<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<'
          } | grep --color=always .
        } >> $failedBuildLog 
      fi
    fi
  done < target/surefire-reports/testcmds.txt
  echo Exit test-tread $1
}

numberOfTestThreadsToLaunch=3
while (( 0 < numberOfTestThreadsToLaunch )); do
  runbuilds $numberOfTestThreadsToLaunch &
  (( --numberOfTestThreadsToLaunch ))
done
time wait

mvn jacoco:report
if [ -f $failedBuildLog ]; then
  cat $failedBuildLog
  false
else
  echo Successfully performed these test executions:
  echo -- '--------------------------------------------'
  grep . target/surefire-reports/testcmds.txt
  cat target/surefire-reports/org.*.txt | perl -ne '
    ($successCount += $1) & ($skipCount += $2) if /.*Tests run: *(\d++).*Skipped: *(\d++).*/;
    print "TESTS RUN: $successCount - SKIPPED: $skipCount\n" if eof;
  '
  true
fi
