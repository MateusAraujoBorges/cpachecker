extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);
extern void exit(int);

/**
success probability:                               9.0E-4
  -:                                               9/10000
failure probability:                               0.9991
  -:                                               9991/10000
grey probability:                                  0.0
  -:                                               0
smallest domain:                                   2.938735877055719E-35
  -:                                               625/21267647932558653966460912964485513216
*/

int main() {
  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();
  int c = __VERIFIER_nondet_int();
  int d = __VERIFIER_nondet_int();

  __VERIFIER_assume(a >= 0 && a < 10);
  __VERIFIER_assume(b >= 0 && b < 10);
  __VERIFIER_assume(c >= 0 && c < 10);
  __VERIFIER_assume(d >= 0 && d < 10);

  if (a) {
    __VERIFIER_error();
  }

  if (!b) {
    __VERIFIER_error();
  }

  if (c && d) {
    __VERIFIER_error();
  }

  if (c || d) {
    __VERIFIER_error();
  }

}