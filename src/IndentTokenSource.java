import org.antlr.v4.runtime.*;
import java.util.*;

public class IndentTokenSource implements TokenSource {
    Stack<Integer> identStack = new Stack<Integer>();

    CommonTokenStream stream;

    /** The queue of tokens */
    ArrayList<Token> tokens = new ArrayList<Token>();
    int lastIndent = 0;

    TokenFactory tokenFactory = new CommonTokenFactory();

    public IndentTokenSource(CommonTokenStream stream) {
        this.stream = stream;
    }

    public int getCharPositionInLine() {
        return -1;
    }

    public CharStream getInputStream() {
        return null;
    }

    public int getLine() {
        return -1;
    }

    public TokenFactory<?> getTokenFactory() {
        return tokenFactory;
    }

    public Token nextToken() {
        // if something in queue, just remove and return it
        if ( tokens.size()>0 ) {
            Token t = (Token)tokens.get(0);
            tokens.remove(0);
            // System.out.println(t);
            return t;
        }

        insertIndentDedentTokens();

        return nextToken();
    }

    public void setTokenFactory(TokenFactory<?> factory) {
        this.tokenFactory = factory;
    }

    public void insertIndentDedentTokens() {
        Token t = stream.LT(1);
        stream.consume();

        if ( t.getType()!=MagicLexer.NL ) {
            if (t.getType()!=MagicLexer.LEADING_WS) {
                tokens.add(t);
            }
            return;
        }

        tokens.add(t);

        t = stream.LT(1);

        if (t.getType() == Token.EOF) {
             while (!identStack.empty()) {
                identStack.pop();
                tokens.add(tokenFactory.create(MagicParser.DEDENT,"l=0"));
            }
            tokens.add(t);
            return;
        }

        stream.consume();

        String w = "";
        if ( t.getType() == MagicLexer.LEADING_WS ) {
            w = t.getText();
        }

        int l = w.length();

        if (l>lastIndent) {
            identStack.push(l);
            tokens.add(tokenFactory.create(MagicParser.INDENT,"I:l="+identStack.size()+" s="+l));
            lastIndent = l;
        } else if (l < lastIndent) {
            while (!identStack.empty() && identStack.peek()>l) {
                int cl = identStack.pop();
                tokens.add(tokenFactory.create(MagicParser.DEDENT,"D:l="+identStack.size()+" s="+cl));
            }
            lastIndent = l;
        }

        if ( t.getType()!=MagicLexer.LEADING_WS ) {
            tokens.add(t);
            return;
        }
    }

    public String getSourceName() {
        return stream.getSourceName();
    }

}
