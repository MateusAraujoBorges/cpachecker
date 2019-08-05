
extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);
extern void exit(int);

void error() {
  ERROR:
    __VERIFIER_error();
}

int main() {

  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();
  int c[2];
  c[0] = __VERIFIER_nondet_int();
  c[1] = __VERIFIER_nondet_int();

  __VERIFIER_assume(a <= b);

//  while (1) {
    if (a > b) {
      error();
    } else if (a == b) {
      error();
    } else {

    }
//  }

  return -1;

}

/*
extern void __VERIFIER_error();
extern unsigned char __VERIFIER_nondet_uchar();

int main() {
  unsigned char a = __VERIFIER_nondet_uchar();
  unsigned char b = __VERIFIER_nondet_uchar();
  unsigned char c = b + 1;
  unsigned char i = 0;
  while (a < 5) {
    if (__VERIFIER_nondet_uchar()) {
      a++;
      b++;
    } else {
      i++;
    }
  }

  if (c == b) {
    ERROR:
    __VERIFIER_error();
    }
}
*/