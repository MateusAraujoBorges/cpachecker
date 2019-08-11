extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);
extern void exit(int);

int main() {
  int a = __VERIFIER_nondet_int();
  int b = __VERIFIER_nondet_int();

  long c = a;

  if (c < 0) {
    __VERIFIER_error();
  }

  unsigned int d = b;

  if (d < 0) {
      __VERIFIER_error();
  }
}
