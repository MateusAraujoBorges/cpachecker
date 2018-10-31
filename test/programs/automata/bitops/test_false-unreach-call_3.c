static inline int find_next_bit(const unsigned long *vaddr, int size, int offset);

static inline int find_first_bit(const unsigned long *vaddr, unsigned size) {
	int res;
	return res < size ? res : size;
}

void main(void)
{
	unsigned long size, offset, res_1, res_2;
	const unsigned long *addr;

	if (size <= offset)
		return;
	res_1 = find_first_bit(addr, size);
	
	// check return value of find_first_bit
	res_2 = find_next_bit(addr, res_1, offset);

}

