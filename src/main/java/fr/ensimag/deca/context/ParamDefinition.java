package fr.ensimag.deca.context;

import fr.ensimag.deca.tree.Location;

/**
 * Definition of a method parameter.
 *
 * @author gl41
 * @date 01/01/2016
 */
public class ParamDefinition extends NonTypeDefinition {

    public ParamDefinition(Type type, Location location) {
        super(type, location);
    }

    @Override
    public String getKind() {
        return "parameter";
    }

    @Override
    public boolean isExpression() {
        return true;
    }

    @Override
    public boolean isParam() {
        return true;
    }

}
