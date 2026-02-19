grammar SQLiteSimple;

// Define o package para os arquivos gerados
@header {
package lib.parser;
}

// ============================================================
// PARSER RULES
// ============================================================

// Regra de entrada principal
parse
    : sql_stmt EOF
    ;

sql_stmt
    : select_stmt
    | insert_stmt
    | update_stmt
    | delete_stmt
    ;

// ============================================================
// SELECT STATEMENT
// ============================================================

select_stmt
    : K_SELECT result_column ( COMMA result_column )*
      K_FROM table_name
      ( where_clause )?
      ( order_by_clause )?
      ( group_by_clause )?
    ;

result_column
    : STAR                                          # SelectAll
    | K_COUNT                                       # SelectCount
    | column_name ( COMMA column_name )*            # SelectColumns
    ;

where_clause
    : K_WHERE expr
    ;

order_by_clause
    : K_ORDER K_BY column_name ( K_ASC | K_DESC )?
    ;

group_by_clause
    : K_GROUP K_BY column_name
    ;

// ============================================================
// INSERT STATEMENT
// ============================================================

insert_stmt
    : K_INSERT K_INTO table_name
      K_VALUES OPEN_PAR literal_value ( COMMA literal_value )* CLOSE_PAR
    ;

// ============================================================
// UPDATE STATEMENT
// ============================================================

update_stmt
    : K_UPDATE table_name
      K_SET assignment ( COMMA assignment )*
      where_clause
    ;

assignment
    : column_name EQ literal_value
    ;

// ============================================================
// DELETE STATEMENT
// ============================================================

delete_stmt
    : K_DELETE K_FROM table_name
      where_clause
    ;

// ============================================================
// EXPRESSIONS
// ============================================================

expr
    : column_name EQ literal_value                              # EqualsExpr
    | column_name K_LIKE literal_value                          # LikeExpr
    | column_name K_BETWEEN literal_value K_AND literal_value   # BetweenExpr
    | expr K_AND expr                                           # AndExpr
    | expr K_OR expr                                            # OrExpr
    | OPEN_PAR expr CLOSE_PAR                                   # ParenExpr
    ;

// ============================================================
// BASIC ELEMENTS
// ============================================================

literal_value
    : NUMERIC_LITERAL
    | STRING_LITERAL
    ;

table_name
    : IDENTIFIER
    ;

column_name
    : IDENTIFIER
    ;

// ============================================================
// LEXER RULES - KEYWORDS (Case Insensitive)
// ============================================================

K_SELECT    : S E L E C T ;
K_FROM      : F R O M ;
K_WHERE     : W H E R E ;
K_INSERT    : I N S E R T ;
K_INTO      : I N T O ;
K_VALUES    : V A L U E S ;
K_UPDATE    : U P D A T E ;
K_SET       : S E T ;
K_DELETE    : D E L E T E ;
K_ORDER     : O R D E R ;
K_BY        : B Y ;
K_ASC       : A S C ;
K_DESC      : D E S C ;
K_GROUP     : G R O U P ;
K_AND       : A N D ;
K_OR        : O R ;
K_LIKE      : L I K E ;
K_BETWEEN   : B E T W E E N ;
K_COUNT     : C O U N T ;

// ============================================================
// LEXER RULES - IDENTIFIERS AND LITERALS
// ============================================================

IDENTIFIER
    : [a-zA-Z_] [a-zA-Z_0-9]*
    ;

NUMERIC_LITERAL
    : [0-9]+ ( '.' [0-9]* )?
    ;

STRING_LITERAL
    : '\'' ( ~'\'' | '\'\'' )* '\''
    ;

// ============================================================
// LEXER RULES - OPERATORS
// ============================================================

COMMA       : ',' ;
OPEN_PAR    : '(' ;
CLOSE_PAR   : ')' ;
STAR        : '*' ;
EQ          : '=' ;

// ============================================================
// LEXER RULES - WHITESPACE
// ============================================================

SPACES
    : [ \t\r\n]+ -> skip
    ;

// ============================================================
// FRAGMENTS - Case Insensitive Letters
// ============================================================

fragment A : [aA] ;
fragment B : [bB] ;
fragment C : [cC] ;
fragment D : [dD] ;
fragment E : [eE] ;
fragment F : [fF] ;
fragment G : [gG] ;
fragment H : [hH] ;
fragment I : [iI] ;
fragment J : [jJ] ;
fragment K : [kK] ;
fragment L : [lL] ;
fragment M : [mM] ;
fragment N : [nN] ;
fragment O : [oO] ;
fragment P : [pP] ;
fragment Q : [qQ] ;
fragment R : [rR] ;
fragment S : [sS] ;
fragment T : [tT] ;
fragment U : [uU] ;
fragment V : [vV] ;
fragment W : [wW] ;
fragment X : [xX] ;
fragment Y : [yY] ;
fragment Z : [zZ] ;
