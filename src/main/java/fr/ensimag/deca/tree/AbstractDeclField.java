package fr.ensimag.deca.tree;

import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.Type;

/**
 * Individual variable declaration (e.g. "x = 42" in "int x = 42, z;")
 *
 * @author gl41
 * @date 01/01/2016
 */
public abstract class AbstractDeclField extends Tree {

    protected abstract void verifyMembers(Type typeDeclaration,
                                          Visibility visibility, DecacCompiler compiler, EnvironmentExp localEnv,
                                          ClassDefinition currentClass) throws ContextualError;

    protected abstract void verifyBody(Type typeDeclaration,
                                          Visibility visibility, DecacCompiler compiler, EnvironmentExp localEnv,
                                          ClassDefinition currentClass) throws ContextualError;

    /**
     * Generates the code for the declaration
     * @param compiler
     */

    protected abstract void codeGenDecl(DecacCompiler compiler);

    protected abstract void codeGenFieldFloat(DecacCompiler compiler);

    protected abstract void codeGenFieldInt(DecacCompiler compiler);
    protected abstract void codePreGenField(DecacCompiler compiler);

    public abstract AbstractIdentifier getVarName();

}
