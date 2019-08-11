extern void __VERIFIER_error(void);
extern int __VERIFIER_nondet_int(void);
extern int __VERIFIER_assume(int);
extern void exit(int);

/** change = 0
success probability:                               0.3333333333333333
  -:                                               1/3
failure probability:                               0.6666666666666666
  -:                                               2/3
grey probability:                                  0.0
  -:                                               0
smallest domain:                                   4.87890977618477E-19
  -:                                               9/18446744073709551616

change = 1

success probability:                               0.6666666666666666
  -:                                               2/3
failure probability:                               0.3333333333333333
  -:                                               1/3
grey probability:                                  0.0
  -:                                               0
smallest domain:                                   4.87890977618477E-19
  -:                                               9/18446744073709551616

*/

#define CHANGE 1

int change_door(int prize, int choice) {
  if (prize == 0) {
    if (choice == 0) { // open 1
      return 2;
    } else if (choice == 1) { // open 2
      return 0;
    } else { // open 1
      return 0;
    }
  } else if (prize == 1) {
    if (choice == 0) { // open 2
        return 1;
      } else if (choice == 1) { // open 2
        return 0;
      } else { // open 0
        return 1;
    }
  } else {
    if (choice == 0) { // open 1
      return 2;
    } else if (choice == 1) { // open 0
      return 2;
    } else { // open 0
      return 1;
    }
  }
}

int main() {

  int door = __VERIFIER_nondet_int();
  int choice = __VERIFIER_nondet_int();

  __VERIFIER_assume(door >= 0 && door < 3);
  __VERIFIER_assume(choice >= 0 && choice < 3);

  if (CHANGE) {
    choice = change_door(door, choice);
  }

  if (choice != door) {
  ERROR:
    __VERIFIER_error();
  }

  return 0;
}
