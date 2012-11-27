#!/usr/bin/env bash
set -u
#set -x
base=$(cd $(dirname $0)/..; pwd)

CYAN="\033[0;36m"
RED="\033[0;31m"
GREEN="\033[0;32m"
NONE="\033[0m"

function compare {
  test_name="$1"
  val1=$2
  val2=$3
  
  if [[ $val1 != $val2 ]]; then
      echo -e >&2 "${RED}Test FAILED: $test_name: $val1 != $val2${NONE}"
      return 1
  else
      echo -e >&2 "${GREEN}Test succeeded: $test_name: $val1 == $val2${NONE}"
      return 0
  fi
}

errors=0
tests=0

for iRun in {0..3}; do
    for iRef in {0..3}; do
        refs=$base/reg-test/set1/set1.ref${iRef}
        hyps=$base/reg-test/set1/set1.out.${iRun}
        me=$($base/multeval.sh eval --metrics bleu --refs $refs --hyps-baseline $hyps --bleu.verbosity 1 --verbosity 1 --boot-samples 1 |& awk '/BLEU: OptRun 0/{printf "%.2f\n",$6}')
        mb=$($base/reg-test/multi-bleu.perl $refs < $hyps |& awk -F'( |,)' '{print $3}')
        compare "Set 1 -- multi-bleu.pl Single Ref $iRef Run $iRun" $me $mb        
        errors=$(( $errors + $? ))
        tests=$(( $tests + 1 ))

        # Create dummy SGML files for NIST mt-eval
        sgml=sgml
        $base/reg-test/write-sgm.py en $hyps $sgml $refs
        nist=$($base/reg-test/mteval-v13m.pl -r $sgml/ref -s $sgml/src -t $sgml/hyps --no-norm -b |& awk '/^BLEU score/{printf "%.2f\n",$4*100}')
        compare "Set 1 -- mteval-v13m.pl Single Ref $iRef Run $iRun" $me $nist
        errors=$(( $errors + $? ))
        tests=$(( $tests + 1 ))
    done
done

for iRun in {0..3}; do
    refs_stem=$base/reg-test/set1/set1.ref
    hyps=$base/reg-test/set1/set1.out.${iRun}
    me=$($base/multeval.sh eval --metrics bleu --refs ${refs_stem}? --hyps-baseline $hyps --bleu.verbosity 1 --verbosity 1 --boot-samples 1 |& awk '/BLEU: OptRun 0/{printf "%.2f\n",$6}')
    mb=$($base/reg-test/multi-bleu.perl $refs_stem < $hyps |& awk -F'( |,)' '{print $3}')
    compare "Set 1 -- multi-bleu.pl MultiRef Run $iRun" $me $mb
    errors=$(( $errors + $? ))
    tests=$(( $tests + 1 ))

    # Create dummy SGML files for NIST mt-eval
    sgml=sgml
    $base/reg-test/write-sgm.py en $hyps $sgml ${refs_stem}?
    nist=$($base/reg-test/mteval-v13m.pl -r $sgml/ref -s $sgml/src -t $sgml/hyps --no-norm -b |& awk '/^BLEU score/{printf "%.2f\n",$4*100}')
    compare "Set 1 -- mteval-v13m.pl MultiRef Run $iRun" $me $nist
    errors=$(( $errors + $? ))
    tests=$(( $tests + 1 ))
done

if (( $errors == 0 )); then
    echo -e >&2 "${GREEN}SUMMARY: Ran $tests tests total${NONE}"
    echo -e >&2 "${GREEN}SUMMARY: All tests passed${NONE}"
else
    echo -e >&2 "${RED}SUMMARY: Ran $tests tests total${NONE}"
    echo -e >&2 "${RED}SUMMARY: $errors tests failed${NONE}"
fi

exit $errors
