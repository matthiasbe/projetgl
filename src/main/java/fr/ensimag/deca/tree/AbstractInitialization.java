package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;

/**
 * Initialization (of variable, field, ...)
 *
 * @author gl41
 * @date 01/01/2016
 */
public abstract class AbstractInitialization extends Tree {
    
    protected abstract void verifyInitialization(DecacCompiler compiler,
            Type t, EnvironmentExp localEnv, ClassDefinition currentClass)
            throws ContextualError;

    protected abstract void codeGenInit(DecacCompiler compiler);
    protected abstract void codeGenInitFieldFloat(DecacCompiler compiler);
    protected abstract void codeGenInitFieldInt(DecacCompiler compiler);
    protected abstract AbstractExpr getExpression();
}
