This project uses https://github.com/ldthomas/apg-java to generate a parser and test it. At the moment, I've pulled the APG project and this project and put them in the same eclipse workspace together so this can reference that one.

Below are random notes.

-----------------------

; TODO
;   generics (maybe just go in type spec)
;   lambdas
;   multiline string literals
; TODO: Specify semantically meaningful rules for identifiers to make a cleaner AST (eg. VarName instead of identifier)
;       So then we can remove identifier from the AST

; Newline delimited should be semicolon delimited and inferred by newlines and indentation.
; Now that I think about it, it seems unnecessary to have the . before the field names in a record declaration. 
; All {} can be implied via indentation. A single ModuleDecl can be standalone and declare the default module for the file.

; Record memory allocation: RecordName ()
; Record memory allocation + initialization: RecordName (field list or map or whatever to specify some it all values)
; Heap allocation uses allocator libraries: alloc RecordName (init stuff if desired)
; ref = alloc Rec
; dealloc ref

; set recordObj .fieldName value

; set rec (init stuff)

; rec.field = value
; This works because it is pattern matching
; rec = (a,b,c,d)

; function definition is naming a code block
; lambda is referencing a non-evaluated anonymous code block

; CodeBlock evaluates to the result of evaluting the last Expr in it
; If module decl without block delims, must be first thing in file


; [a, b, c] := { expr1; expr2; expr3; [O1(), O2(), O3()]; } ; a bound to result of O1(), etc.
; (a, b, c) @= { a @= f1(); (b, c) @= f2() }
; Does the above make sense? Desirable?


; {i:=0; while i<10; print i++; }
; because a single ended expression is a codeblock when no explicit nor implicit braces
; TODO: but that would work if those 3 were on lines of their own and that would be very ugly and confusing so maybe we should disallow that somehow?


; Partial application/function calls and pattern matching can use same grammar
