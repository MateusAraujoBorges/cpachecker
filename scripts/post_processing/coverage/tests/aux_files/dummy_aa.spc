OBSERVER AUTOMATON AssumptionAutomaton

INITIAL STATE DummyAA;

STATE __TRUE :
    TRUE -> GOTO __TRUE;

STATE __FALSE :
    TRUE -> GOTO __FALSE;

STATE USEFIRST DummyAA :
    MATCH "" -> GOTO __FALSE;
    TRUE -> GOTO __TRUE;

END AUTOMATON
