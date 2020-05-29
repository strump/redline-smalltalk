"Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution."

Object subclass: #BlockClosure.

!BlockClosure messageFor: 'evaluating'!

value
  "Activate the receiver and evaluate the block."
  JVM aload: 1; "lambda object"
      aload: 2; "context argument"
      invokeVirtual: 'st/redline/core/PrimObject' method: 'primitiveEval' matching: '(Lst/redline/core/PrimContext;)Lst/redline/core/PrimObject;'.
! !

!BlockClosure messageFor: 'controlling'!

whileFalse
	"Ordinarily compiled in-line, and therefore not overridable.
	This is in case the message is sent to other than a literal block.
	Evaluate the receiver, as long as its value is false."

	self value ifFalse: [ self whileFalse ]. !

whileFalse: aBlock
	"Ordinarily compiled in-line, and therefore not overridable.
	This is in case the message is sent to other than a literal block.
	Evaluate the argument, aBlock, as long as the value of the receiver is false."

	self value ifFalse: [ aBlock value. self whileFalse: aBlock ].
	^ nil !

whileNil: aBlock
	"Unlike #whileTrue/False: this is not compiled inline."
	^ [self value isNil] whileTrue: [aBlock value] !

whileNotNil: aBlock
	"Unlike #whileTrue/False: this is not compiled inline."
	^ [self value notNil] whileTrue: [aBlock value] !

whileTrue
	"Ordinarily compiled in-line, and therefore not overridable.
	This is in case the message is sent to other than a literal block.
	Evaluate the receiver, as long as its value is true."

	self value ifTrue: [ self whileTrue ] !

whileTrue: aBlock
	"Ordinarily compiled in-line, and therefore not overridable.
	This is in case the message is sent to other than a literal block.
	Evaluate the argument, aBlock, as long as the value of the receiver is true."

	self value ifTrue: [ aBlock value. self whileTrue: aBlock ].
	^ nil !
!
