package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.DecacFatalError;
import fr.ensimag.deca.context.*;
import fr.ensimag.deca.tools.DecacInternalError;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.ima.pseudocode.*;
import fr.ensimag.ima.pseudocode.instructions.*;
import org.apache.commons.lang.Validate;

import java.io.PrintStream;

public class InstanceOf extends AbstractExpr {

    AbstractIdentifier className;
    AbstractExpr var;

    public InstanceOf(AbstractIdentifier className, AbstractExpr var) {
        Validate.notNull(className);
        Validate.notNull(var);

        this.className = className;
        this.var = var;
    }

    @Override
    public Type verifyExpr(DecacCompiler compiler, EnvironmentExp localEnv, ClassDefinition currentClass) throws ContextualError {

        className.verifyClass(compiler);
        var.verifyExpr(compiler, localEnv, currentClass);

        Type t = compiler.getEnvTypes().get(compiler.getSymbols().create("boolean")).getType();
        setType(t);
        return t;
    }

    @Override
    public void codegenExpr(DecacCompiler compiler, GPRegister register) {
        if(var.getDval()==null){
            throw new DecacInternalError("element vide");
        }
        else{
            boolean[] backup =compiler.getRegManager().getTableRegistre();
            GPRegister bypass = compiler.getRegManager().getGBRegister();
            GPRegister stock = compiler.getRegManager().getGBRegister();
            int i=compiler.getLblManager().getIf();
            compiler.getLblManager().incrementIf();
            compiler.addInstruction(new LOAD(var.getDval(),bypass));
            compiler.addInstruction(new LOAD(className.getClassDefinition().getOperand(),stock));
            compiler.addLabel(new Label("debut.instanceof"+i));
            compiler.addInstruction(new CMP(bypass,stock));
            compiler.addInstruction(new BEQ(new Label("true.instanceof."+i))); //test si egal
            compiler.addInstruction(new LOAD(new RegisterOffset(0,bypass),bypass)); // on descend
            compiler.addInstruction(new CMP(new NullOperand(),bypass)); //si object instance
            compiler.addInstruction(new BNE(new Label("debut.instanceof"+i))); //non, on remonte
            compiler.addInstruction(new LOAD(new ImmediateInteger(0),register));
            compiler.addInstruction(new BRA(new Label("fin.instanceof"+i)));
            compiler.addLabel(new Label("true.instanceof."+i));
            compiler.addInstruction(new LOAD(new ImmediateInteger(1),register));
            compiler.addLabel(new Label("fin.instanceof"+i));
            compiler.getRegManager().setTableRegistre(backup);
        }

    }

    @Override
    public DVal getDval() {
        return null;
    }

    @Override
    public void decompile(IndentPrintStream s) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    protected void prettyPrintChildren(PrintStream s, String prefix) {
        var.prettyPrint(s, prefix, false);
        className.prettyPrint(s, prefix, true);
    }

    @Override
    protected void iterChildren(TreeFunction f) {
        var.iter(f);
        className.iter(f);
    }
}
