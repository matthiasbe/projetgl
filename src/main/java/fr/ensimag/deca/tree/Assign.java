package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.ima.pseudocode.*;
import fr.ensimag.ima.pseudocode.instructions.*;

/**
 * Assignment, i.e. lvalue = expr.
 *
 * @author gl41
 * @date 01/01/2016
 */
public class Assign extends AbstractBinaryExpr {

    @Override
    public AbstractLValue getLeftOperand() {
        // The cast succeeds by construction, as the leftOperand has been set
        // as an AbstractLValue by the constructor.
        return (AbstractLValue)super.getLeftOperand();
    }

    public Assign(AbstractLValue leftOperand, AbstractExpr rightOperand) {
        super(leftOperand, rightOperand);
    }

    protected void codePreGenInst(DecacCompiler compiler){
        boolean[] table = compiler.getFakeRegManager().getTableRegistre(); //on verifie les registre
        compiler.getFakeRegManager().getGBRegister();
        compiler.addMaxFakeRegister(compiler.getFakeRegManager().getLastregistre());
        getRightOperand().codePreGenExpr(compiler);
        if(getLeftOperand().getDefinition().isField()){
            compiler.getFakeRegManager().getGBRegister();
            compiler.addMaxFakeRegister(compiler.getFakeRegManager().getLastregistre());
        }
        compiler.getFakeRegManager().setTableRegistre(table);
    }
    @Override
    protected void codeGenInst(DecacCompiler compiler){
        boolean[] table=compiler.getRegManager().getTableRegistre(); //on verifie les registre
        GPRegister register;
        if(compiler.getRegManager().noFreeRegister()){
            int i =compiler.getRegManager().getGBRegisterInt();
            compiler.addInstruction(new TSTO(1));
            compiler.addInstruction(new BOV(new Label("stack_overflow")));
            compiler.addInstruction(new PUSH(Register.getR(i)));
            register = Register.getR(i);
            setPush();
        }
        else{
            register = compiler.getRegManager().getGBRegister();

        }

        getRightOperand().codegenExpr(compiler, register);
        if(getLeftOperand().getDefinition().isField()){
            GPRegister stock;
            if(compiler.getRegManager().noFreeRegister()){
                int i =compiler.getRegManager().getGBRegisterInt(register.getNumber());
                compiler.addInstruction(new PUSH(Register.getR(i)));
                stock = Register.getR(i);
                setPush();
            }
            else{
                stock = compiler.getRegManager().getGBRegister();

            }
            compiler.addInstruction(new LOAD(new RegisterOffset(-2, Register.LB),stock));
            compiler.addInstruction(new STORE(register,
                    new RegisterOffset(getLeftOperand().getFieldDefinition().getIndex(),stock))); // on store
            if(getPop()){
                compiler.addInstruction(new POP(stock));
                popDone();
            }
        }
        else if(getLeftOperand().getDefinition().isClass()){

        }
        else if(getLeftOperand().getDefinition().isMethod()){

        }
        else if(getLeftOperand().getDefinition().isParam()){

        }
        else if(getLeftOperand().getDefinition().isExpression()){
            DAddr adress = this.getLeftOperand().getNonTypeDefinition().getOperand();
            compiler.addInstruction(new STORE(register, adress));
            compiler.getRegManager().resetTableRegistre();
        }
        else{

        }
        if(getPop()){
            compiler.addInstruction(new POP(register));
            popDone();
        }
        compiler.getRegManager().setTableRegistre(table);
    }
    @Override
    public void codegenExpr(DecacCompiler compiler, GPRegister register){
        this.codeGenInst(compiler);
        compiler.addInstruction(new LOAD(getLeftOperand().getNonTypeDefinition().getOperand(),register));
    }

    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass) throws ContextualError {
        Type rightType = getRightOperand().verifyExpr(compiler, localEnv, currentClass);
        Type leftType = getLeftOperand().verifyExpr(compiler, localEnv, currentClass);

        if(!assignCompatible(localEnv, leftType, rightType)) {
            throw new ContextualError("Cannot assign " + rightType + " to " + leftType, getLocation());
        }

        if(rightType.isInt() && leftType.isFloat()) {
            // Conversion du rightoperand
            ConvFloat conv = new ConvFloat(getRightOperand());
            conv.setLocation(getLocation());
            setRightOperand(conv);
            getRightOperand().verifyExpr(compiler, localEnv, currentClass);
        }
        else if (!leftType.sameType(rightType)) {
            // On crée le type et on met sa location
            Identifier typeIdentifier = new Identifier(leftType.getName());
            typeIdentifier.setLocation(getLocation());
            // On crée le cast et on met sa location
            Cast cast = new Cast(typeIdentifier, getRightOperand());
            cast.setLocation(getLocation());

            // On relie le tout à l'affectation
            setRightOperand(cast);
            getRightOperand().verifyExpr(compiler, localEnv, currentClass);
        }

        Type t = leftType;
        setType(t);
        return t;
    }


    @Override
    protected String getOperatorName() {
        return "=";
    }

}
