grammar Smalltalk;

script : rootSequence ws EOF;
rootSequence : temps? ws rootStatements ws terminatingExpression?;
rootStatements : (rootTerminatedExpression | methodGroup ws)* ;
rootTerminatedExpression : expression ws PERIOD ws;
terminatingExpression : expression | answer;

sequence : temps? ws statements? ws;
ws : (SEP | EOL | COMMENT)*;
temps : ws PIPE (ws IDENTIFIER)+ ws PIPE;
statements : answer ws # StatementAnswer
           | expressions ws PERIOD ws answer # StatementExpressionsAnswer
           | expressions PERIOD? ws # StatementExpressions
           ;
answer : CARROT ws expression ws PERIOD?;
expression : assignment | cascade | keywordSend | binarySend | primitive;
expressions : expression expressionList*;
expressionList : PERIOD ws expression;
cascade : (keywordSend | binarySend) (ws SEMI_COLON ws message)+;
message : binaryMessage | unaryMessage | keywordMessage;
assignment : variable ws ASSIGNMENT ws expression;
variable : IDENTIFIER;
binarySend : unarySend binaryTail?;
unarySend : operand ws unaryTail?;
keywordSend : binarySend keywordMessage;
keywordMessage : ws (keywordPair ws)+;
keywordPair : KEYWORD ws binarySend ws;
operand : literal | reference | subexpression;
subexpression : OPEN_PAREN ws expression ws CLOSE_PAREN;
literal : runtimeLiteral | parsetimeLiteral;
runtimeLiteral : dynamicDictionary | dynamicArray | block;
block : BLOCK_START blockParamList? ws (PIPE ws)? sequence BLOCK_END;
blockParamList : (ws BLOCK_PARAM)+;
dynamicDictionary : DYNDICT_START ws expressions? ws DYNARR_END;
dynamicArray : DYNARR_START ws expressions? ws DYNARR_END;
parsetimeLiteral : charConstant | pseudoVariable | number | literalArray | string | symbol;
number : numberExp | hex | stFloat | stInteger;
numberExp : (stFloat | stInteger) EXP stInteger;
charConstant : CHARACTER_CONSTANT;
hex : MINUS? HEX;
stInteger : MINUS? DIGIT+;
stFloat : MINUS? DIGIT+ PERIOD DIGIT+;
pseudoVariable : RESERVED_WORD;
string : STRING;
symbol : HASH bareSymbol;
primitive : LT ws KEYWORD ws DIGIT+ ws GT;
bareSymbol : (IDENTIFIER | binarySelector | RESERVED_WORD) | KEYWORD+ | string | PIPE+;
literalArray : LITARR_START literalArrayRest;
literalArrayRest : (ws (parsetimeLiteral | bareLiteralArray | bareSymbol))* ws CLOSE_PAREN;
bareLiteralArray : OPEN_PAREN literalArrayRest;
unaryTail : unaryMessage ws unaryTail? ws;
unaryMessage : ws unarySelector;
unarySelector : IDENTIFIER;
keywords : KEYWORD+;
reference : variable;
binaryTail : binaryMessage binaryTail?;
binaryMessage : ws binarySelector ws (unarySend | operand);
binarySelector : (BINARY_SELECTOR_CHAR | PIPE | MINUS)+;

//Methods declaration
methodGroup : EXCLAMATION SEP* className SEP+ classSelector? methodHeaderKeywords EXCLAMATION
              ws (methodDeclaration ws)+
              EXCLAMATION
             ;
className : IDENTIFIER;
classSelector : IDENTIFIER SEP+;
methodHeaderKeywords: KEYWORD SEP* STRING (SEP+ KEYWORD SEP* STRING)*;
methodDeclaration : methodHeader (SEP | COMMENT)* EOL
                    sequence
                    ws EXCLAMATION
                   ;
methodHeader : IDENTIFIER | binaryMethodHeader | keywordMethodHeader;
binaryMethodHeader : binarySelector SEP* IDENTIFIER;
keywordMethodHeader : KEYWORD SEP* IDENTIFIER (SEP+ KEYWORD SEP* IDENTIFIER)*;

EXCLAMATION : '!';
EOL : '\r'? '\n';
SEP : [ \t];

STRING : '\'' (~[\\'] | '\\\\' | '\\\'' | '\'\'' | '\\t' | '\\n' )* '\'';
COMMENT : '"' (.)*? '"';
BLOCK_START : '[';
BLOCK_END : ']';
CLOSE_PAREN : ')';
OPEN_PAREN : '(';
PIPE : '|';
PERIOD : '.';
SEMI_COLON : ';';
BINARY_SELECTOR_CHAR : ('\\' | '+' | '*' | '/' | '=' | '>' | '<' | ',' | '@' | '%' | '~' | '&' | '?');
LT : '<';
GT : '>';
MINUS : '-';
RESERVED_WORD : 'nil' | 'true' | 'false' | 'self' | 'super';
IDENTIFIER : [a-zA-Z]+[a-zA-Z0-9_]*;
CARROT : '^';
COLON : ':';
ASSIGNMENT : ':=';
HASH : '#';
DOLLAR : '$';
EXP : 'e';
HEX : '16r' ([0-9a-fA-F] [0-9a-fA-F])+;
LITARR_START : '#(';
DYNDICT_START : '#{';
DYNARR_END : '}';
DYNARR_START : '{';
DIGIT : [0-9];
KEYWORD : IDENTIFIER COLON;
BLOCK_PARAM : COLON IDENTIFIER;
CHARACTER_CONSTANT : DOLLAR ('!!' | .); //Symbol $!! should be parsed as $! inside of method declaration
