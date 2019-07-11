package jit.x64;

import java.util.ArrayList;
import java.util.List;

public class X64Assembler {
	static enum Prefix {
		NONE((byte) 0), REX_W((byte) 0x48), REX_R((byte) 0x44), REX_WR((byte) 0x4C), REX_B((byte) 0x41),
		REX_WB((byte) 0x49), REX_RB((byte) 0x45), REX_WRB((byte) 0x4D);

		private final byte code;

		Prefix(byte code) {
			this.code = code;
		}

		public byte getCode() {
			return code;
		}
	}

	static enum OpCode {
		ADD_RM_R((byte) 0x01), EXTENDED((byte) 0x0F), SUB_RM_R((byte) 0x29), CMP_RM_R((byte) 0x39),
		MOV_RM_R((byte) 0x89), CDQ((byte) 0x99), MOV_R_IMM((byte) 0xB8), PUSH_R((byte) 0x50), POP_R((byte) 0x58),
		RET((byte) 0xC3), JMP_REL32((byte) 0xE9), IDIV_RM((byte) 0xF7), NEG((byte) 0xF7), NOT((byte)0xF7);

		private final byte code;

		OpCode(byte code) {
			this.code = code;
		}

		public byte getCode() {
			return code;
		}
	}

	static enum OpCode2 {
		JE_REL32((byte) 0x84), JNE_REL32((byte) 0x85), JL_REL32((byte) 0x8C), JLE_REL32((byte) 0x8E),
		JGE_REL32((byte)0x8D), JG_REL32((byte) 0x8F), IMUL_R_RM((byte) 0xAF);

		private final byte code;

		OpCode2(byte code) {
			this.code = code;
		}

		public byte getCode() {
			return code;
		}
	}

	private final List<Byte> code = new ArrayList<>();
	private final List<X64Label> labels = new ArrayList<>();

	public byte[] getCode() {
		fixLabels();
		return toArray(code);
	}

	private byte[] toArray(List<Byte> list) {
		byte[] array = new byte[list.size()];
		for (int index = 0; index < list.size(); index++) {
			array[index] = list.get(index);
		}
		return array;
	}

	public X64Label createLabel() {
		var label = new X64Label();
		labels.add(label);
		return label;
	}

	public void setLabel(X64Label label) {
		if (label.getTarget() != null) {
			throw new AssertionError("Label already set");
		}
		label.setTarget(code.size());
	}

	private void fixLabels() {
		for (var label : labels) {
			for (var jump : label.getJumps()) {
				if (label.getTarget() == null) {
					throw new AssertionError("Undefined label target");
				}
				if (jump.getByteSize() == 4) {
					fixJump32(jump, label.getTarget());
				} else {
					throw new AssertionError("Label size not supported");
				}
			}
		}
	}

	private void fixJump32(X64Jump jump, int target) {
		var relative = target - (jump.getPosition() + jump.getByteSize());
		writeLittleEndian(jump.getPosition(), relative);
	}

	public void ADD_RegReg(X64Register target, X64Register source) {
		encodeRegReg(OpCode.ADD_RM_R, null, source, target);
	}

	public void SUB_RegReg(X64Register target, X64Register source) {
		encodeRegReg(OpCode.SUB_RM_R, null, source, target);
	}

	public void IMUL_RegReg(X64Register target, X64Register source) {
		encodeRegReg(OpCode.EXTENDED, OpCode2.IMUL_R_RM, target, source);
	}

	public void CDQ() {
		emit(Prefix.REX_W.getCode());
		emit(OpCode.CDQ.getCode());
	}
	
	public void CMP_RegReg(X64Register rm64, X64Register reg) {
		encodeRegReg(OpCode.CMP_RM_R, null, reg, rm64);
	}

	public void IDIV(X64Register source) {
		encodeRegReg(OpCode.IDIV_RM, null, X64Register.RDI, source);
	}

	public void JE_Rel(X64Label label) {
		encodeJump32(OpCode.EXTENDED, OpCode2.JE_REL32, label);
	}

	public void JNE_Rel(X64Label label) {
		encodeJump32(OpCode.EXTENDED, OpCode2.JNE_REL32, label);
	}

	public void JG_Rel(X64Label label) {
		encodeJump32(OpCode.EXTENDED, OpCode2.JG_REL32, label);
	}
	
	public void JGE_Rel(X64Label label) {
		encodeJump32(OpCode.EXTENDED, OpCode2.JGE_REL32, label);
	}

	public void JL_Rel(X64Label label) {
		encodeJump32(OpCode.EXTENDED, OpCode2.JL_REL32, label);
	}

	public void JLE_Rel(X64Label label) {
		encodeJump32(OpCode.EXTENDED, OpCode2.JLE_REL32, label);
	}

	public void JMP_Rel(X64Label label) {
		encodeJump32(OpCode.JMP_REL32, null, label);
	}

	public void MOV_RegReg(X64Register target, X64Register source) {
		encodeRegReg(OpCode.MOV_RM_R, null, source, target);
	}

	public void MOV_RegImm(X64Register target, long immediate) {
		encodeRegNoModImm(OpCode.MOV_R_IMM, target, (int) (immediate & 0xFFFFFFFF), (int) (immediate >>> 32));
	}

	public void NEG(X64Register source) {
		encodeRegReg(OpCode.NEG, null, X64Register.RBX, source);
	}
	
	public void NOT(X64Register source) {
		encodeRegReg(OpCode.NOT, null, X64Register.RDX, source);
	}

	public void PUSH(X64Register reg) {
		encodeRegNoMod(Prefix.NONE, OpCode.PUSH_R, reg);
	}

	public void POP(X64Register reg) {
		encodeRegNoMod(Prefix.NONE, OpCode.POP_R, reg);
	}

	public void RET() {
		emit(OpCode.RET.getCode());
	}

	private void encodeJump32(OpCode opCode1, OpCode2 opCode2, X64Label label) {
		emit(opCode1.getCode());
		if (opCode2 != null) {
			emit(opCode2.getCode());
		}
		var jump = new X64Jump(4, code.size());
		label.getJumps().add(jump);
		emitLittleEndian(0);
	}

	private void encodeRegNoModImm(OpCode opCode, X64Register reg, int lowImm, Integer highImm) {
		var prefix = highImm != null ? Prefix.REX_W : Prefix.NONE;
		encodeRegNoMod(prefix, opCode, reg);
		emitLittleEndian(lowImm);
		if (highImm != null) {
			emitLittleEndian(highImm);
		}
	}

	private void encodeRegNoMod(Prefix prefix, OpCode opCode, X64Register reg) {
		var code = prefix.getCode();
		if (reg.getCode() >= X64Register.R8.getCode()) {
			code |= Prefix.REX_B.getCode();
		}
		byte actualOpCode = (byte) (opCode.getCode() + (reg.getCode() & 7));
		if (code != Prefix.NONE.getCode()) {
			emit(code);
		}
		emit(actualOpCode);
	}

	private void encodeRegReg(OpCode opCode1, OpCode2 opCode2, X64Register reg, X64Register rm) {
		var prefix = Prefix.NONE.getCode();
		prefix |= Prefix.REX_W.getCode();
		if (reg.getCode() >= X64Register.R8.getCode()) {
			prefix |= Prefix.REX_R.getCode();
		}
		if (rm.getCode() >= X64Register.R8.getCode()) {
			prefix |= Prefix.REX_B.getCode();
		}
		if (prefix != Prefix.NONE.getCode()) {
			emit(prefix);
		}
		emit(opCode1.getCode());
		if (opCode2 != null) {
			emit(opCode2.getCode());
		}
		emit(modRM(rm, reg));
	}

	private byte modRM(X64Register rm, X64Register reg) {
		return (byte) (0xC0 | (reg.getCode() & 7) << 3 | rm.getCode() & 7);
	}

	private void emitLittleEndian(int number) {
		int value = number;
		for (int offset = 0; offset < 4; offset++) {
			emit((byte) (value & 0xFF));
			value >>>= 8;
		}
	}

	private void emit(byte value) {
		code.add(value);
	}

	private void writeLittleEndian(int position, int number) {
		int value = number;
		for (int offset = 0; offset < 4; offset++) {
			code.set(position + offset, (byte) (value & 0xFF));
			value >>>= 8;
		}
	}
}
