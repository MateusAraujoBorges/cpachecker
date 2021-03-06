struct mutex;

extern void mutex_lock(struct mutex *lock);
extern void mutex_unlock(struct mutex *lock);
void check_final_state(void);

void main(void)
{
	struct mutex *mutex_1, *mutex_2;

	mutex_lock(&mutex_1);
	mutex_lock(&mutex_2); // no double lock
	mutex_unlock(&mutex_2);
	mutex_unlock(&mutex_1);

	check_final_state();
}

