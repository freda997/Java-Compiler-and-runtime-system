package bytecode;

public enum OpCode {
	LDC, // load constant boolean, int or string (operand const) // ... -> ..., value
	ACONST_NULL, // load constant null // ... -> ..., null

	IADD, // integer add // ..., value1, value2 -> ..., result
	ISUB, // integer subtract // ..., value1, value2 -> ..., result (value1 - value2)
	IMUL, // integer multiply // ..., value1, value2 -> ..., result
	IDIV, // integer divide // ..., value1, value2 -> ..., result (value1 / value2)
	IREM, // integer remainder // ..., value1, value2 -> ..., result (value1 % value2)
	INEG, // integer negate  // ..., value -> ..., result
	BNEG, // boolean negate // ..., value -> ..., result

	CMPEQ, // compare equal // ..., value1, value2 -> ..., result (boolean)
	CMPNE, // compare not equal // ..., value1, value2 -> ..., result (boolean)
	ICMPLT, // integer compare less than // ..., value1, value2 -> ..., result (value1 < value2)
	ICMPLE, // integer compare less equal // ..., value1, value2 -> ..., result (value1 <= value2)
	ICMPGT, // integer compare greater than // ..., value1, value2 -> ..., result (value1 > value2)
	ICMPGE, // integer compare greater equal // ..., value1, value2 -> ..., result (value1 >= value2)

	IF_TRUE, // conditional branch if true (operand label or relative instruction offset) // ..., value -> ...
	IF_FALSE, // conditional branch if false (operand label or relative instruction offset) // ..., value -> ...
	GOTO, // unconditional branch (operand label or relative instruction offset) // ... -> ...

	INSTANCEOF, // type test (operand class symbol) // ..., objref -> ..., result (boolean)
	CHECKCAST, // type cast (operand class symbol) // ..., objref -> ... (if successful)

	LOAD, // load argument or local variable (operand index, "this": 0, arguments: 1..n, locals: n+1..) // ... -> ..., value
	STORE, // store argument or local variable (operand index, arguments: 1..n, locals: n+1..) // ..., value -> ...

	GETFIELD, // read field (operand field symbol or field index) // ..., objref -> ..., value
	PUTFIELD, // store field (operand field symbol or field index) // ..., objref, value -> ...

	ALOAD, // array element load // ..., arrayref, index -> ..., value
	ASTORE, // array element store // ..., arrayref, index, value -> ...

	NEW, // new object (operand class symbol) // ... -> ..., objref
	ARRAYLENGTH, // load array length // ..., arrayref -> ..., length
	NEWARRAY, // new array (operand array type symbol) // ..., length -> ..., arrayref

	INVOKESTATIC, // call static inbuilt method (operand method symbol) // ..., arg1, .., argn -> ..., result (if any)
	INVOKEVIRTUAL, // virtual method call (operand method symbol) // ..., objref, arg1, .., argn -> ..., result (if any)
	RETURN // return // retval (if any) -> (empty)
}
