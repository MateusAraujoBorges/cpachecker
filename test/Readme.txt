Directory structure for /test:

- config: configuration files for CPAchecker necessary only for tests
- original-sources: unmodified source code from which test cases were made
- programs: test cases in a form that CPAchecker can handle (e.g., example C programs)
- results: all files produced by the benchmark script benchmark.py (not in repository)
- utils: utilities such as scripts that generate test cases or benchmark definitions
- test-sets: files that each specify a set of test cases (which we call a test set)
             and files that specify a whole test-suite (used as input for benchmark.py)
