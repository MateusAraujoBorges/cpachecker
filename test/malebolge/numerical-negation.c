extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);
extern void exit(int);

/**
 from email_spec0_product19_true-unreach-call_true-termination.cil_reach_bobKeyAdd.partitionWithGap.1.c

success probability:                               0.9999999064020856
  -:                                               715827849/715827916
failure probability:                               9.359791439036306E-8
  -:                                               67/715827916
grey probability:                                  0.0
  -:                                               0
smallest domain:                                   0.5000000232830644
  -:                                               536870937/1073741824

#success paths:                                    1
#failure paths:                                    1
#grey    paths:                                    0

*/

int main() {
  int a = __VERIFIER_nondet_int();

  __VERIFIER_assume(-a - a <= 200);

  if (a > 100) {
    return 0;
  } else {
    __VERIFIER_error();
  }
  return -1;
}

