package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.*;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.*;
import fr.ensimag.ima.pseudocode.instructions.*;

/**
 *
 * @author gl41
 * @date 01/01/2016
 */
public abstract class AbstractOpCmp extends AbstractBinaryExpr {

    public AbstractOpCmp(AbstractExpr leftOperand, AbstractExpr rightOperand) {
        super(leftOperand, rightOperand);
    }

    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass) throws ContextualError {

        Type leftType = getLeftOperand().verifyExpr(compiler, localEnv, currentClass);
        Type rightType = getRightOperand().verifyExpr(compiler, localEnv, currentClass);

        if (leftType.sameType(rightType)) {
            if (!leftType.isInt() && !rightType.isFloat() && (this instanceof AbstractOpIneq)) {
                throw new ContextualError("Inequality on non-numbers. Left : " + leftType.getName() + " Right : " + rightType.getName(), getLocation());
            }
        } else {
            if (leftType.isInt() && rightType.isFloat()) {
                // on convertit le leftoperand int -> float
                getLeftOperand().verifyExpr(compiler, localEnv, currentClass);
                setLeftOperand(new ConvFloat(getLeftOperand()));
                getLeftOperand().verifyExpr(compiler, localEnv, currentClass);
            } else if (leftType.isFloat() && rightType.isInt()) {
                // on convertit le rightoperand int -> float
                getRightOperand().verifyExpr(compiler, localEnv, currentClass);
                setRightOperand(new ConvFloat(getRightOperand()));
                getRightOperand().verifyExpr(compiler, localEnv, currentClass);
            } else {
                throw new ContextualError("Comparison on variables which types are differents and non-castable. Left : " + leftType.getName() + " Right : " + rightType.getName(), getLocation());
            }
        }

        Type t = new BooleanType(compiler.getSymbols().create("boolean"));

        setType(t);
        return t;
    }

    @Override
    protected void codeGenCMP(DecacCompiler compiler){
        this.getLeftOperand().codegenExpr(compiler, Register.R0);
        this.getRightOperand().codegenExpr(compiler, Register.R1);
        compiler.addInstruction(new CMP(Register.R1, Register.R0));
        codeGenCMPOP(compiler);
    }

    @Override
    public void codegenExpr(DecacCompiler compiler, GPRegister register) {
        this.getLeftOperand().codegenExpr(compiler, Register.R0);
        this.getRightOperand().codegenExpr(compiler, Register.R1);
        compiler.addInstruction(new CMP(Register.R1, Register.R0));
        this.fetchCond(compiler, register);
    }

    @Override
    protected void codeGenInst(DecacCompiler compiler){
        boolean[] table=compiler.getRegManager().getTableRegistre(); //on verifie les registre
        this.codegenExpr(compiler,null);
        int i=compiler.getLblManager().getIf();
        compiler.getLblManager().incrementIf();
        Label finTest = new Label("endTest"+i);
        Label suiteTest = new Label("suiteTest"+i);
        compiler.getLblManager().setLabelFalse(suiteTest);
        GPRegister stock;
        if(compiler.getRegManager().noFreeRegister()){
            int j =compiler.getRegManager().getGBRegisterInt();
            compiler.addInstruction(new TSTO(1));
            compiler.addInstruction(new BOV(new Label("stack_overflow")));
            compiler.addInstruction(new PUSH(Register.getR(j)));
            stock = Register.getR(j);
            setPush();
        }
        else{
            stock = compiler.getRegManager().getGBRegister();

        }
        this.codeGenCMPOP(compiler);

        compiler.addInstruction(new LOAD(new ImmediateInteger(1),stock));
        compiler.addInstruction(new BRA(finTest));
        compiler.addLabel(suiteTest);
        compiler.addInstruction(new LOAD(new ImmediateInteger(0),stock));
        compiler.addLabel(finTest);
        if(getPop()){
            compiler.addInstruction(new POP(stock));
            popDone();
        }
        compiler.getRegManager().setTableRegistre(table);
    }

    @Override
    public void decompile(IndentPrintStream s){
        getLeftOperand().decompile(s);
        s.print(this.getOperatorName());
        getRightOperand().decompile(s);
    }

    @Override
    public DVal getDval() {
        return null;
    }

    /**
     *
     * @param compiler
     * @param register
     */
    protected abstract void fetchCond(DecacCompiler compiler, GPRegister register);

}
