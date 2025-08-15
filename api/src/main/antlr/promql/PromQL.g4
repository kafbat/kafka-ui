grammar PromQL;

@header {package promql;}

options {
    caseInsensitive = true;
}

expression: vectorOperation EOF;

// Binary operations are ordered by precedence

// Unary operations have the same precedence as multiplications

vectorOperation
    : <assoc=right> vectorOperation powOp vectorOperation
    | <assoc=right> vectorOperation subqueryOp
    | unaryOp vectorOperation
    | vectorOperation multOp vectorOperation
    | vectorOperation addOp vectorOperation
    | vectorOperation compareOp vectorOperation
    | vectorOperation andUnlessOp vectorOperation
    | vectorOperation orOp vectorOperation
    | vectorOperation vectorMatchOp vectorOperation
    | vectorOperation AT vectorOperation
    | vector
    ;

// Operators

unaryOp:        (ADD | SUB);
powOp:          POW grouping?;
multOp:         (MULT | DIV | MOD) grouping?;
addOp:          (ADD | SUB) grouping?;
compareOp:      (DEQ | NE | GT | LT | GE | LE) BOOL? grouping?;
andUnlessOp:    (AND | UNLESS) grouping?;
orOp:           OR grouping?;
vectorMatchOp:  (ON | UNLESS) grouping?;
subqueryOp:     SUBQUERY_RANGE offsetOp?;
offsetOp:       OFFSET DURATION;

vector
    : function_
    | aggregation
    | instantSelector
    | matrixSelector
    | offset
    | literal
    | parens
    ;

parens: LEFT_PAREN vectorOperation RIGHT_PAREN;

// Selectors

instantSelector
    : METRIC_NAME (LEFT_BRACE labelMatcherList? RIGHT_BRACE)?
    | LEFT_BRACE labelMatcherList RIGHT_BRACE
    ;

labelMatcher:         labelName labelMatcherOperator STRING;
labelMatcherOperator: EQ | NE | RE | NRE;
labelMatcherList:     labelMatcher (COMMA labelMatcher)* COMMA?;

matrixSelector: instantSelector TIME_RANGE;

offset
    : instantSelector OFFSET DURATION
    | matrixSelector OFFSET DURATION
    ;

// Functions

function_: FUNCTION LEFT_PAREN (parameter (COMMA parameter)*)? RIGHT_PAREN;

parameter:     literal | vectorOperation;
parameterList: LEFT_PAREN (parameter (COMMA parameter)*)? RIGHT_PAREN;

// Aggregations

aggregation
    : AGGREGATION_OPERATOR parameterList
    | AGGREGATION_OPERATOR (by | without) parameterList
    | AGGREGATION_OPERATOR parameterList ( by | without)
    ;
by:      BY labelNameList;
without: WITHOUT labelNameList;

// Vector one-to-one/one-to-many joins

grouping:   (on_ | ignoring) (groupLeft | groupRight)?;
on_:         ON labelNameList;
ignoring:   IGNORING labelNameList;
groupLeft:  GROUP_LEFT labelNameList?;
groupRight: GROUP_RIGHT labelNameList?;

// Label names

labelName:     keyword | METRIC_NAME | LABEL_NAME;
labelNameList: LEFT_PAREN (labelName (COMMA labelName)*)? RIGHT_PAREN;

keyword
    : AND
    | OR
    | UNLESS
    | BY
    | WITHOUT
    | ON
    | IGNORING
    | GROUP_LEFT
    | GROUP_RIGHT
    | OFFSET
    | BOOL
    | AGGREGATION_OPERATOR
    | FUNCTION
    ;

literal: NUMBER | STRING;

fragment NUMERAL: [0-9]+ ('.' [0-9]+)?;

fragment SCIENTIFIC_NUMBER
   : NUMERAL ('e' [-+]? NUMERAL)?
   ;

NUMBER
    : NUMERAL
    | SCIENTIFIC_NUMBER;

STRING
    : '\'' (~('\'' | '\\') | '\\' .)* '\''
    | '"' (~('"' | '\\') | '\\' .)* '"'
    ;

// Binary operators

ADD:  '+';
SUB:  '-';
MULT: '*';
DIV:  '/';
MOD:  '%';
POW:  '^';

AND:    'and';
OR:     'or';
UNLESS: 'unless';

// Comparison operators

EQ:  '=';
DEQ: '==';
NE:  '!=';
GT:  '>';
LT:  '<';
GE:  '>=';
LE:  '<=';
RE:  '=~';
NRE: '!~';

// Aggregation modifiers

BY:      'by';
WITHOUT: 'without';

// Join modifiers

ON:          'on';
IGNORING:    'ignoring';
GROUP_LEFT:  'group_left';
GROUP_RIGHT: 'group_right';

OFFSET: 'offset';

BOOL: 'bool';

AGGREGATION_OPERATOR
    : 'sum'
    | 'min'
    | 'max'
    | 'avg'
    | 'group'
    | 'stddev'
    | 'stdvar'
    | 'count'
    | 'count_values'
    | 'bottomk'
    | 'topk'
    | 'quantile'
    ;

FUNCTION
    : 'abs'
    | 'absent'
    | 'absent_over_time'
    | 'ceil'
    | 'changes'
    | 'clamp_max'
    | 'clamp_min'
    | 'day_of_month'
    | 'day_of_week'
    | 'days_in_month'
    | 'delta'
    | 'deriv'
    | 'exp'
    | 'floor'
    | 'histogram_quantile'
    | 'holt_winters'
    | 'hour'
    | 'idelta'
    | 'increase'
    | 'irate'
    | 'label_join'
    | 'label_replace'
    | 'ln'
    | 'log2'
    | 'log10'
    | 'minute'
    | 'month'
    | 'predict_linear'
    | 'rate'
    | 'resets'
    | 'round'
    | 'scalar'
    | 'sort'
    | 'sort_desc'
    | 'sqrt'
    | 'time'
    | 'timestamp'
    | 'vector'
    | 'year'
    | 'avg_over_time'
    | 'min_over_time'
    | 'max_over_time'
    | 'sum_over_time'
    | 'count_over_time'
    | 'quantile_over_time'
    | 'stddev_over_time'
    | 'stdvar_over_time'
    | 'last_over_time'
    | 'acos'
    | 'acosh'
    | 'asin'
    | 'asinh'
    | 'atan'
    | 'atanh'
    | 'cos'
    | 'cosh'
    | 'sin'
    | 'sinh'
    | 'tan'
    | 'tanh'
    | 'deg'
    | 'pi'
    | 'rad'
    ;

LEFT_BRACE:  '{';
RIGHT_BRACE: '}';

LEFT_PAREN:  '(';
RIGHT_PAREN: ')';

LEFT_BRACKET:  '[';
RIGHT_BRACKET: ']';

COMMA: ',';

AT: '@';

SUBQUERY_RANGE
     : LEFT_BRACKET DURATION ':' DURATION? RIGHT_BRACKET;

TIME_RANGE
    : LEFT_BRACKET DURATION RIGHT_BRACKET;

// The proper order (longest to the shortest) must be validated after parsing
DURATION: ([0-9]+ ('ms' | [smhdwy]))+;

METRIC_NAME: [a-z_:] [a-z0-9_:]*;
LABEL_NAME:  [a-z_] [a-z0-9_]*;



WS: [\r\t\n ]+ -> channel(HIDDEN);
SL_COMMENT
    : '#' .*? '\n' -> channel(HIDDEN)
    ;
