package fr.ensimag.deca;

import fr.ensimag.deca.codegen.LabelManager;
import fr.ensimag.deca.codegen.RegisterManager;
import fr.ensimag.deca.context.EnvironmentTypes;
import fr.ensimag.deca.syntax.DecaLexer;
import fr.ensimag.deca.syntax.DecaParser;
import fr.ensimag.deca.tools.DecacInternalError;
import fr.ensimag.deca.tools.SymbolTable;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.deca.tree.AbstractProgram;
import fr.ensimag.deca.tree.LocationException;
import fr.ensimag.ima.pseudocode.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import fr.ensimag.ima.pseudocode.multipleinstructions.ErrorInstruction;
import fr.ensimag.ima.pseudocode.multipleinstructions.InstructionList;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.log4j.Logger;

/**
 * Decac compiler instance.
 *
 * This class is to be instantiated once per source file to be compiled. It
 * contains the meta-data used for compiling (source file name, compilation
 * options) and the necessary utilities for compilation (symbol tables, abstract
 * representation of target file, ...).
 *
 * It contains several objects specialized for different tasks. Delegate methods
 * are used to simplify the code of the caller (e.g. call
 * compiler.addInstruction() instead of compiler.getProgram().addInstruction()).
 *
 * @author gl41
 * @date 01/01/2016
 */
public class DecacCompiler implements Callable {
    private static final Logger LOG = Logger.getLogger(DecacCompiler.class);

    public EnvironmentTypes getEnvTypes() {
        return rootEnv;
    }

    public void setEnvTypes(EnvironmentTypes rootEnv) {
        this.rootEnv = rootEnv;
    }

    private EnvironmentTypes rootEnv;

    /**
     * Les symboles du programme.
     */
    private SymbolTable symbols;

    /**
	 * table des registres et données utiles
     */
    private RegisterManager regManager;
    public RegisterManager getRegManager(){
        return this.regManager;
    }
    public void resetTableRegistre(){
        this.regManager.resetTableRegistre();
    }


    private RegisterManager fakeRegManager;
    public RegisterManager getFakeRegManager(){
        return this.fakeRegManager;
    }
    private int maxFakeRegister =0;
    public void addMaxFakeRegister(int ajout){
        if(ajout>maxFakeRegister){
            maxFakeRegister=ajout;
        }
    }
    public void resetMaxFakeRegister(){
        this.maxFakeRegister= 0;
    }
    public int getMaxFakeRegister(){
        return maxFakeRegister;
    }

    /**
     * Gestion des labels
     */
    LabelManager labelManager;
    public LabelManager getLblManager(){
        return this.labelManager;
    }

    /**
     * Portable newline character.
     */
    private static final String nl = System.getProperty("line.separator", "\n");

    public DecacCompiler(CompilerOptions compilerOptions, File source) {
        super();
        this.compilerOptions = compilerOptions;
        this.source = source;
        this.regManager = new RegisterManager(compilerOptions.getRegistre());
        this.labelManager = new LabelManager();
        this.fakeRegManager = new RegisterManager(compilerOptions.getRegistre());

        /**
         * Ajouts des symboles prédéfinis
         */
        symbols = new SymbolTable();

        symbols.create("void");
        symbols.create("int");
        symbols.create("boolean");
        symbols.create("float");
        symbols.create("Object");

    }

    /**
     * Source file associated with this compiler instance.
     */
    public File getSource() {
        return source;
    }

    /**
     * Compilation options (e.g. when to stop compilation, number of registers
     * to use, ...).
     */
    public CompilerOptions getCompilerOptions() {
        return compilerOptions;
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#add(fr.ensimag.ima.pseudocode.AbstractLine)
     */
    public void add(AbstractLine line) {
        program.add(line);
    }

    /**
     * @see fr.ensimag.ima.pseudocode.IMAProgram#addComment(java.lang.String)
     */
    public void addComment(String comment) {
        program.addComment(comment);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addLabel(fr.ensimag.ima.pseudocode.Label)
     */
    public void addLabel(Label label) {
        program.addLabel(label);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction)
     */
    public void addInstruction(Instruction instruction) {
        program.addInstruction(instruction);
    }

    /**
     * @return ligne courante dans le fichier assembleur
     */
    public int getCurrentLine(){
        return program.totalLineNumber();
    }

    /**
     * Add a list of instructions to the program
     * @param list list of instructions to be added to the program
     */
    public void addInstructionList(InstructionList list){
        program.addInstructionList(list);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction,
     * java.lang.String)
     */
    public void addInstruction(Instruction instruction, String comment) {
        program.addInstruction(instruction, comment);
    }
    
    /**
     * @see 
     * fr.ensimag.ima.pseudocode.IMAProgram#display()
     */
    public String displayIMAProgram() {
        return program.display();
    }
    
    private final CompilerOptions compilerOptions;
    private final File source;
    /**
     * The main program. Every instruction generated will eventually end up here.
     */
    private final IMAProgram program = new IMAProgram();
 

    /**
     * Run the compiler (parse source file, generate code)
     *
     * @return true on error
     */
    public boolean compile() {
        String sourceFile = source.getAbsolutePath();
        String destFile = null;
		Integer finNom=sourceFile.lastIndexOf(".deca"); //pour fin nom
		destFile=sourceFile.substring(0,finNom); //on a récupéré nom
        destFile=destFile.concat(".ass");

        PrintStream err = System.err;
        PrintStream out = System.out;
        LOG.debug("Compiling file " + sourceFile + " to assembly file " + destFile);
        try {
            return doCompile(sourceFile, destFile, out, err);
        } catch (LocationException e) {
            e.display(err);
            return true;
        } catch (DecacFatalError e) {
            err.println(e.getMessage());
            return true;
        } catch (StackOverflowError e) {
            LOG.debug("stack overflow", e);
            err.println("Stack overflow while compiling file " + sourceFile + ".");
            return true;
        } catch (Exception e) {
            LOG.fatal("Exception raised while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        } catch (AssertionError e) {
            LOG.fatal("Assertion failed while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        }
    }

    /**
     * Internal function that does the job of compiling (i.e. calling lexer,
     * verification and code generation).
     *
     * @param sourceName name of the source (deca) file
     * @param destName name of the destination (assembly) file
     * @param out stream to use for standard output (output of decac -p)
     * @param err stream to use to display compilation errors
     *
     * @return true on error
     */
    private boolean doCompile(String sourceName, String destName,
            PrintStream out, PrintStream err)
            throws DecacFatalError, LocationException {
        AbstractProgram prog = doLexingAndParsing(sourceName, err);

        if (prog == null) {
            LOG.info("Parsing failed");
            return true;
        }
        if(this.compilerOptions.getParse()){
            IndentPrintStream istream= new IndentPrintStream(out);
            prog.decompile(istream);
            return false;
        }
        else{
            assert(prog.checkAllLocations());
            prog.verifyProgram(this);
            if(this.compilerOptions.getVerification()){
                return false;
            }
            else{
                assert(prog.checkAllDecorations());

                /* Code du programme principal */
                addComment("start main program");
                prog.codeGenProgram(this);
                addComment("end main program");

                /* Code des erreurs */
                addLabel(new Label("arith_overflow"));
                addInstructionList(new ErrorInstruction("Error : overflow during arithmetic operation"));
                addLabel(new Label("stack_overflow"));
                addInstructionList(new ErrorInstruction("Error : stack overflow"));
                addLabel(new Label("heap_overflow"));
                addInstructionList(new ErrorInstruction("Error : heap overflow"));
                addLabel(new Label("dereferencement.null"));
                addInstructionList(new ErrorInstruction("Error : dereferencing null pointer"));

                LOG.debug("Generated assembly code:" + nl + program.display());
                LOG.info("Output file assembly file is: " + destName);

                FileOutputStream fstream = null;
                try {
                    fstream = new FileOutputStream(destName);
                } catch (FileNotFoundException e) {
                    throw new DecacFatalError("Failed to open output file: " + e.getLocalizedMessage());
                }

                LOG.info("Writing assembler file ...");

                program.display(new PrintStream(fstream));
                LOG.info("Compilation of " + sourceName + " successful.");
                return false;
            }
        }
    }

    /**
     * Build and call the lexer and parser to build the primitive abstract
     * syntax tree.
     *
     * @param sourceName Name of the file to parse
     * @param err Stream to send error messages to
     * @return the abstract syntax tree
     * @throws DecacFatalError When an error prevented opening the source file
     * @throws DecacInternalError When an inconsistency was detected in the
     * compiler.
     * @throws LocationException When a compilation error (incorrect program)
     * occurs.
     */
    protected AbstractProgram doLexingAndParsing(String sourceName, PrintStream err)
            throws DecacFatalError, DecacInternalError {
        DecaLexer lex;
        try {
            lex = new DecaLexer(new ANTLRFileStream(sourceName));
        } catch (IOException ex) {
            throw new DecacFatalError("Failed to open input file: " + ex.getLocalizedMessage());
        }
        lex.setDecacCompiler(this);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        DecaParser parser = new DecaParser(tokens);
        parser.setDecacCompiler(this);
        return parser.parseProgramAndManageErrors(err);
    }

    /**
     * Accesseurs de la table des symbols.
     */

    public SymbolTable getSymbols() {
        return symbols;
    }

    @Override
    public Object call() throws Exception {
        return compile();
    }
}
