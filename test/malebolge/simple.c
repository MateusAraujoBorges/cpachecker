extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);
extern void exit(int);

/**
success probability:                               0.4999999998835847
  -:                                               4294967295/8589934592
failure probability:                               0.5000000001164153
  -:                                               4294967297/8589934592
grey probability:                                  0.0
  -:                                               0
*/

int main() {

  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();

  if (a > b) {
    __VERIFIER_error();
  } else if (a == b) {
    __VERIFIER_error();
  } else {

  }

  return -1;
}
