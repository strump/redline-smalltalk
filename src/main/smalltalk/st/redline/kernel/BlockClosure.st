"Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution."

Object subclass: #BlockClosure.

!BlockClosure messageFor: 'evaluating'!

value
  "Activate the receiver and evaluate the block."
  JVM aload: 1; "lambda object"
      aload: 2; "context argument"
      invokeVirtual: 'st/redline/core/PrimObject' method: 'primitiveEval' matching: '(Lst/redline/core/PrimContext;)Lst/redline/core/PrimObject;'.
! !
