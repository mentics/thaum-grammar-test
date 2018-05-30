package org.thaum.parser;

import static org.junit.jupiter.api.Assertions.fail;
import static org.thaum.parser.ThaumParser.RuleNames.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thaum.parser.ThaumParser;
import org.thaum.parser.ThaumParser.RuleNames;

import apg.Ast;
import apg.Parser;
import apg.Parser.Result;
import apg.Trace;

class ThaumParserTest {
    private Parser parser;
    private Ast ast;

    @BeforeEach
    void setUp() throws Exception {
        parser = new Parser(ThaumParser.getInstance());
        ast = parser.enableAst(true);

        for (RuleNames rule : ThaumParser.RuleNames.values()) {
            if (Character.isUpperCase(rule.ruleName().charAt(0))) {
                ast.enableRuleNode(rule.ruleID(), true);
            }
        }
    }

    @Test
    void testLiterals() throws Exception {
        testInput("'3'", true, LITASCIISTRING);
        testInput("1231", true, LITNUMBER);
        testInput("'asdf'", true, LITASCIISTRING);
        testInput("\"  $%#&@sdfgds3498\"", true, LITUNICODESTRING);

        testInput("[ 1,  2,3,42324]", true, LITLIST);
        testInput("[0,0]", true, LITLIST);
        testInput("(a,  1, 'blue', 32 )", true, LITTUPLE);
        testInput("[ \t \t 1=2,a=2,\t'var'=test\t ]", true, LITMAP);
    }

    @Test
    void testInfixExpressions() throws Exception {
        testInput("a+2+3", true, EXPRREPEAT);
        testInput("2*a/5+2+3", true, EXPRREPEAT);

        testInput("a=2", true, EXPRACTION);
        testInput("a :=2", true, EXPRACTION);
        testInput("a@=  2", true, EXPRACTION, EXPRINFIX);

        testInput("a ==   2", true, EXPRCOMPARE);
        testInput("2  >asdfasdfasdasdf", true, EXPRCOMPARE);
        testInput("3<  2", true, EXPRCOMPARE);
        testInput("a == 2 + 2", true, EXPRCOMPARE);

        testInput("2*a + 4*(x*t - a)", true, EXPRREPEAT);
        testInput("2*a + 4*x*t - a", true, EXPRREPEAT);
    }

    @Test
    void testControlExpressions() throws Exception {
        testInput("if { a == 2 + 2 } then b = 4;", true, IFEXPR);
        testInput("if a == 2 then { g = 2+3* 22 }", true, IFEXPR);
        testInput("if a == 2 then b = 4;", true, IFEXPR);
        // testInput("# Naive Fibonacci implementation\r\n" + "fibonacci : Int64\r\n" +
        // " 0 = 1\r\n" + " 1 = 1\r\n"
        // + " n = fibonacci(n - 1) + fibonacci(n - 2)", true,
        // ThaumParser.RuleNames.FUNCTIONDEFINITION);
    }

    @Test
    void testFunctionCalls() throws Exception {
        testInput("print 'blue'", true, CALL);
        testInput("print 5 'blue' 5", true, CALL);
        testInput("print (1+2) 5", true, CALL);
        testInput("print aas 45 csdf", true, CALL);
        testInput("print (go 'a' b)", true, CALL);
        testInput("print 5 4 54 (5+5) (go 'do' 'that') 'blue'", true, CALL);
    }

    @Test
    void testExpressions() throws Exception {
        testInput("{ print 'blue'; }", true, CODEBLOCK);
        testInput("{}", true, CODEBLOCK);
        testInput("{ print 'blue' }", true, CODEBLOCK);
        testInput("{ print 'blue'; [0,0]; }", true, CODEBLOCK);
        testInput("[3,_] = { print 'blue'; [0,0]; }", true, EXPRACTION);
        testInput("{ a = 5; b = 2; }", true, CODEBLOCK);
    }

    @Test
    void testStatements() throws Exception {
        testInput("a:=5;", false, STMT);
        testInput("print 'blue';", false, STMT);
        testInput("[a,a];", false, STMT);
        testInput("[2,a] = [5,2];", false, STMT);
        testInput("[0,0];", true, STMT);
    }

    @Test
    void testFunctionDefinitions() throws Exception {
        testInput("f { a = 5; b = 2; }", false, FUNCTIONDEFINITION);
        testInput("f : List { [2,a] = [a,a]; }", false, FUNCTIONDEFINITION);
        testInput("f : Number { [_,a] = [5,2]; }", false, FUNCTIONDEFINITION);
        testInput("f { [_,a] = [5,2]; }", false, FUNCTIONDEFINITION);
    }

    @Test
    void testFull() throws Exception {
        testInput("module std { f : List { [1,a] = [a,1]; [2,a] = [a,a]; [3,_] = { print 'blue'; [0,0]; } } }", true);
        testInput("module std { a := 5; }", false);
        testInput("module org.omni;", false);
    }

    private void testInput(String input, boolean showAst) throws Exception {
        testInput(input, showAst, TOP);
    }

    private void testInput(String input, boolean showAst, ThaumParser.RuleNames... asRule) throws Exception {
        for (ThaumParser.RuleNames rule : asRule) {
            System.out.println("-- Parsing as " + rule.ruleName() + " --\n" + input);
            parser.setStartRule(rule.ruleID());
            parser.setInputString(input);

            // Trace trace = parser.enableTrace(true);
            // trace.setOut(System.err);
            try {
                Result result = parser.parse();

                if (result.success()) {
                    if (showAst) {
                        ast.display(System.out);
                    }
                } else {
                    System.err.println("Failed to parse. Matched length = " + result.getMatchedPhraseLength());
                    result.displayResult(System.err);
                    Trace trace = parser.enableTrace(true);
                    trace.setOut(limited());
                    parser.parse();
                    fail("Failed to parse input:\n" + input);
                }
            } catch (StackOverflowError err) {
                System.err.println("**** STACK OVERFLOW ****");
                Trace trace = parser.enableTrace(true);
                trace.setOut(limited());
                parser.parse();
                fail("Failed to parse input:\n" + input);
            }
        }
    }

    private static PrintStream limited() {
        return new PrintStream(new OutputStream() {
            int count = 0;

            @Override
            public void write(int b) throws IOException {
                if (count++ > 100000) {
                    System.err.println("Output exceeded, exiting.");
                    System.exit(-1);
                }
                System.err.write(b);
            }
        });
    }
}
