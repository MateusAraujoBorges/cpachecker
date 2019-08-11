extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);
extern void exit(int);

/**
with depthLimit = 50 && quantifyGrey = true

success probability:                               3.259629011154175E-9
  -:                                               7/2147483648
failure probability:                               0.9999999767169356
  -:                                               1073741799/1073741824
grey probability:                                  2.0023435354232788E-8
  -:                                               43/2147483648
domain size:                                       1.0
  -:                                               1

#success paths:                                    14
#failure paths:                                    2
#grey    paths:                                    1

*/

int main() {

  int a = __VERIFIER_nondet_int();

  if (a < 0 || a >= 100) {
    __VERIFIER_error();
  }

  while (a >= 0) {
    a--;
  }

  return 0;
}
