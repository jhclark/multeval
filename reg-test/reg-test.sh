#!/usr/bin/env bash
set -u
set -x
base=$(cd $(dirname $0)/..; pwd)

CYAN="\033[0;36m"
RED="\033[0;31m"
GREEN="\033[0;32m"
NONE="\033[0m"

function compare {
  test_name="$1"
  val1="$2"
  val2="$3"
  
  if [ -e "$val1" ]; then
      echo -e >&2 "${RED}ERROR: $test_name: val1 empty"
      return 1
  fi

  if [ -e "$val2" ]; then
      echo -e >&2 "${RED}ERROR: $test_name: val2 empty"
      return 1
  fi
  
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

# Test Set 1 (from Collin Cherry) -- multiple reference system with relatively high scores, requiring no smoothing
# ...but test it as a single reference system
for iRun in {0..3}; do
    for iRef in {0..3}; do
        refs=$base/reg-test/set1/set1.ref${iRef}
        hyps=$base/reg-test/set1/set1.out.${iRun}
        me=$($base/multeval.sh eval --metrics bleu --refs $refs --hyps-baseline $hyps --bleu.verbosity 1 --verbosity 1 --boot-samples 1 |& tee /dev/stderr | awk '/BLEU: OptRun 0/{printf "%.2f\n",$6}')
        mb=$($base/reg-test/multi-bleu.perl $refs < $hyps |& tee /dev/stderr | awk -F'( |,)' '{print $3}')
        compare "Set 1 -- multi-bleu.pl Single Ref $iRef Run $iRun" "$me" "$mb"
        errors=$(( $errors + $? ))
        tests=$(( $tests + 1 ))

        # Create dummy SGML files for NIST mt-eval
        sgml=sgml
        $base/reg-test/write-sgm.py en $hyps $sgml $refs
        nist=$($base/reg-test/mteval-v13m.pl -r $sgml/ref -s $sgml/src -t $sgml/hyps --no-norm -b |& tee /dev/stderr | awk '/^BLEU score/{printf "%.2f\n",$4*100}')
        compare "Set 1 -- mteval-v13m.pl Single Ref $iRef Run $iRun" "$me" "$nist"
        errors=$(( $errors + $? ))
        tests=$(( $tests + 1 ))
    done
done

# Continue testing set 1, but now actually using it as a multi-ref system
for iRun in {0..3}; do
    refs_stem=$base/reg-test/set1/set1.ref
    hyps=$base/reg-test/set1/set1.out.${iRun}
    me=$($base/multeval.sh eval --metrics bleu --refs ${refs_stem}? --hyps-baseline $hyps --bleu.verbosity 1 --verbosity 1 --boot-samples 1 |& tee /dev/stderr | awk '/BLEU: OptRun 0/{printf "%.2f\n",$6}')
    mb=$($base/reg-test/multi-bleu.perl $refs_stem < $hyps |& tee /dev/stderr | awk -F'( |,)' '{print $3}')
    compare "Set 1 -- multi-bleu.pl MultiRef Run $iRun" $me $mb
    errors=$(( $errors + $? ))
    tests=$(( $tests + 1 ))

    # Create dummy SGML files for NIST mt-eval
    sgml=sgml
    $base/reg-test/write-sgm.py en $hyps $sgml ${refs_stem}?
    nist=$($base/reg-test/mteval-v13m.pl -r $sgml/ref -s $sgml/src -t $sgml/hyps --no-norm -b |& tee /dev/stderr | awk '/^BLEU score/{printf "%.2f\n",$4*100}')
    compare "Set 1 -- mteval-v13m.pl MultiRef Run $iRun" $me $nist
    errors=$(( $errors + $? ))
    tests=$(( $tests + 1 ))
done

# Test set 2 from Yulia Tsvetkov (single ref, single run)
ref=$base/reg-test/set2/set2.ref0
hyps=$base/reg-test/set2/set2.out.0
me=$($base/multeval.sh eval --metrics bleu --refs ${ref} --hyps-baseline $hyps --bleu.verbosity 1 --verbosity 1 --boot-samples 1 |& tee /dev/stderr | awk '/BLEU: OptRun 0/{printf "%.2f\n",$6}')
mb=$($base/reg-test/multi-bleu.perl $ref < $hyps |& tee /dev/stderr | awk -F'( |,)' '{print $3}')
compare "Set 2 -- multi-bleu.pl Single Ref, Single Run" $me $mb
errors=$(( $errors + $? ))
tests=$(( $tests + 1 ))

# Create dummy SGML files for NIST mt-eval
sgml=sgml
$base/reg-test/write-sgm.py en $hyps $sgml ${ref}
nist=$($base/reg-test/mteval-v13m.pl -r $sgml/ref -s $sgml/src -t $sgml/hyps --no-norm -b |& tee /dev/stderr | awk '/^BLEU score/{printf "%.2f\n",$4*100}')
compare "Set 2 -- mteval-v13m.pl Single Ref, Single Run" $me $nist
errors=$(( $errors + $? ))
tests=$(( $tests + 1 ))

if (( $errors == 0 )); then
    echo -e >&2 "${GREEN}SUMMARY: Ran $tests tests total${NONE}"
    echo -e >&2 "${GREEN}SUMMARY: All tests passed${NONE}"
else
    echo -e >&2 "${RED}SUMMARY: Ran $tests tests total${NONE}"
    echo -e >&2 "${RED}SUMMARY: $errors tests failed${NONE}"
fi

exit $errors
