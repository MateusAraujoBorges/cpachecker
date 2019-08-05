extern void __VERIFIER_assume(int);
int aca_input_var_int_0;
int aca_input_num_0 = 0;
int aca_input_counter_0 = 0;
int aca_input_arr_int_0[0];
void initialize_reads()
{
    for (int i = 0; i < aca_input_num_0; i++)
    {
        int tmp_aca_input_arr_int_0 = __VERIFIER_nondet_int();
        aca_input_arr_int_0[i] = tmp_aca_input_arr_int_0;
        __VERIFIER_assume((aca_input_arr_int_0[i] - 1073741823) <= 0);
        __VERIFIER_assume(0 <= (aca_input_arr_int_0[i] + 1073741823));
    }
}
void block_subspace()
{
    ;
}
extern void __VERIFIER_error() __attribute__((__noreturn__));
extern int __VERIFIER_nondet_int();
int main()
{
    initialize_reads();
    block_subspace();
    {
        if (aca_input_counter_0 < aca_input_num_0)
        {
            aca_input_var_int_0 = aca_input_arr_int_0[aca_input_counter_0++];
        }
        else
        {
            aca_input_var_int_0 = __VERIFIER_nondet_int();
        }
        int x = aca_input_var_int_0;
        if (x < 0)
        {
            __VERIFIER_error();
        }
    }
}