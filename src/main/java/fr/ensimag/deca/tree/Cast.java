package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.*;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.DVal;
import fr.ensimag.ima.pseudocode.GPRegister;
import fr.ensimag.ima.pseudocode.Register;
import fr.ensimag.ima.pseudocode.instructions.*;
import org.apache.commons.lang.Validate;

import java.io.PrintStream;

/**
 *
 * @author gl41
 * @date 01/01/2016
 */
public class Cast extends AbstractCast {

    AbstractIdentifier type;
    AbstractExpr expr;

    public Cast (AbstractIdentifier type, AbstractExpr expr) {
        this.expr = expr;
        this.type = type;
    }

    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv,
            ClassDefinition currentClass) throws ContextualError {

        Type castType = type.verifyType(compiler);
        Type opType = expr.verifyExpr(compiler, localEnv, currentClass);

        if(!castCompatible(localEnv, opType, castType)) {
            throw new ContextualError("Incompatible cast from " + opType + " to " + castType, getLocation());
        }

        setType(castType);
        return castType;
    }


    @Override
    public void decompile(IndentPrintStream s) {
        s.print("(");
        type.decompile(s);
        s.print(") (");
        expr.decompile(s);
        s.println(")");


    }

    @Override
    protected void iterChildren(TreeFunction f) {
        type.iter(f);
        expr.iter(f);
    }

    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        type.prettyPrint(s, prefix, false);
        expr.prettyPrint(s, prefix, true);
    }

    @Override
    protected void codeGenInst(DecacCompiler compiler) {
        expr.codeGenInst(compiler); // Calcul de l'expression
        // Récupération de l'expression calculée
        GPRegister target = Register.getR(compiler.getRegManager().getLastregistre());
        compiler.getRegManager().setEtatRegistreTrue(compiler.getRegManager().getLastregistre());
        // Cast de l'expression
        if(this.getType().isFloat())
            compiler.addInstruction(new FLOAT(expr.getdValue(),target));
        else
            compiler.addInstruction(new INT(expr.getdValue(),target));
        this.setdValue(target);

    }

    @Override
    public void codegenExpr(DecacCompiler compiler, GPRegister register) {
        expr.codegenExpr(compiler, register);
        // Récupération de l'expression calculée
        // Cast de l'expression
        if(this.getType().isFloat())
            compiler.addInstruction(new FLOAT(register, register));
        else
            compiler.addInstruction(new INT(register, register));
    }

    @Override
    public DVal getDval() {
        return null;
    }

    @Override
    protected void codeGenPrint(DecacCompiler compiler){
        GPRegister reg = compiler.getRegManager().getGBRegister();
        expr.codegenExpr(compiler,reg);
        if(this.getType().isFloat()) {
            compiler.addInstruction(new FLOAT(reg, Register.R1));
            compiler.addInstruction(new WFLOAT());
        }else {
            compiler.addInstruction(new INT(reg, Register.R1));
            compiler.addInstruction(new WINT());
        }
    }

    @Override
    protected void codeGenPrintX(DecacCompiler compiler){
        Validate.isTrue(this.getType().isFloat());
        GPRegister reg = compiler.getRegManager().getGBRegister();
        expr.codegenExpr(compiler,reg);
        compiler.addInstruction(new FLOAT(reg, Register.R1));
        compiler.addInstruction(new WFLOATX());
    }
}
