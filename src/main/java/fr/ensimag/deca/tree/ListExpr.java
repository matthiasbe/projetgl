package fr.ensimag.deca.tree;

import fr.ensimag.deca.context.Type;
import fr.ensimag.deca.DecacCompiler;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ContextualError;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.Signature;
import fr.ensimag.deca.tools.IndentPrintStream;

import java.util.Iterator;

/**
 * List of expressions (eg list of parameters).
 *
 * @author gl41
 * @date 01/01/2016
 */
public class ListExpr extends TreeList<AbstractExpr> {
    public void verifyParams(DecacCompiler compiler, EnvironmentExp localEnv,
                                  ClassDefinition currentClass, Signature signature, Location location)
            throws ContextualError {

        // Erreur si nombres de paramètres différents
        if(signature.size() != this.size()) {
            throw new ContextualError("Wrong argument number.", location);
        }

        // Erreur si types différents pour un certain paramètre
        int i = 0;
        for (AbstractExpr expr : getList()) {
            if(!signature.paramNumber(i).sameType(expr.verifyExpr(compiler, localEnv, currentClass))) {
                throw new ContextualError("Wrong type for param " + i, location);
            }

            i++;
        }
    }

    public Signature verifySignature(DecacCompiler compiler, EnvironmentExp localEnv,
                                  ClassDefinition currentClass)
            throws ContextualError {
        Signature s = new Signature();
        // Erreur si types différents pour un certain paramètre
        for (AbstractExpr expr : getList()) {
            s.add(expr.verifyExpr(compiler, localEnv, currentClass));
        }
        return s;
    }

    @Override
    public void decompile(IndentPrintStream s) {
        for(AbstractExpr a : getList()){
            a.decompile(s);
        }
    }
}
